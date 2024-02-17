package io.github.tofodroid.mods.mimi.common.tile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.tofodroid.mods.mimi.common.block.AConfigurableMidiPowerSourceBlock;
import io.github.tofodroid.mods.mimi.util.MidiNbtDataUtils;
import io.github.tofodroid.mods.mimi.util.MidiNbtDataUtils.TriggerMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AConfigurableMidiPowerSourceTile extends AConfigurableMidiNoteResponsiveTile {
    public static final Integer MAX_NOTE_ON_SECONDS = 8;
    
    // Runtime data
    private Map<Byte, Map<Byte, Long>> heldNotes = new HashMap<>();
    private Integer offCounter = 0;

    // Config data
    private TriggerMode triggerMode = TriggerMode.NOTE_ON;
    private Byte holdTicks = 0;

    public AConfigurableMidiPowerSourceTile(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 1);
    }

    protected AConfigurableMidiPowerSourceTile(BlockEntityType<?> type, BlockPos pos, BlockState state, Integer inventorySize) {
        super(type, pos, state, inventorySize);
    }

    public static void doTick(Level world, BlockPos pos, BlockState state, AConfigurableMidiPowerSourceTile self) {
        self.tick(world, pos, state);
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();

        if(!this.getLevel().isClientSide()) {
            this.offCounter = 0;
        }
    }
 
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    
        if(!this.getLevel().isClientSide()) {
            this.offCounter = 0;
        }
    }
    
    @Override
    protected void cacheMidiSettings() {
        super.cacheMidiSettings();
        this.setInverted(MidiNbtDataUtils.getInvertSignal(getSourceStack()));
        this.triggerMode = MidiNbtDataUtils.getTriggerMode(getSourceStack());
        this.holdTicks = MidiNbtDataUtils.getHoldTicks(getSourceStack());
        this.clearNotes();
    }

    @Override
    public void execServerTick(ServerLevel world, BlockPos pos, BlockState state) {
        if(this.isBlockValid()) {
            Boolean shouldBePowered = false;
            
            if(this.triggerMode == TriggerMode.NOTE_HELD) {
                shouldBePowered = tickNotes();
            }

            if(shouldBePowered) {
                this.setPowered(true);
                this.offCounter = 0;
            } else if(state.getValue(AConfigurableMidiPowerSourceBlock.POWERED)) {
                if(this.offCounter >= this.holdTicks) {
                    this.offCounter = 0;
                    this.setPowered(false);
                } else {
                    this.offCounter++;
                }
            }
        }
    }

    public Boolean isBlockValid() {
        return getBlockState().getBlock() instanceof AConfigurableMidiPowerSourceBlock;
    }

    public Boolean isPowered() {
        return getBlockState().getValue(AConfigurableMidiPowerSourceBlock.POWERED);
    }

    public Boolean isInverted() {
        return getBlockState().getValue(AConfigurableMidiPowerSourceBlock.INVERTED);
    }

    public Boolean stackIsInverted() {
        return MidiNbtDataUtils.getInvertSignal(getSourceStack());
    }

    public void setInverted(Boolean inverted) {
        if(this.getBlockState().getValue(AConfigurableMidiPowerSourceBlock.INVERTED) != inverted) {
            this.getLevel().setBlockAndUpdate(
                getBlockPos(), 
                getBlockState()
                    .setValue(AConfigurableMidiPowerSourceBlock.INVERTED, inverted)
            );
            
            getLevel().updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    public void setPowered(Boolean powered) {
        if(this.getBlockState().getValue(AConfigurableMidiPowerSourceBlock.POWERED) != powered) {
            this.getLevel().setBlockAndUpdate(
                getBlockPos(), 
                getBlockState()
                    .setValue(AConfigurableMidiPowerSourceBlock.POWERED, powered)
            );
            
            getLevel().updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    protected Boolean tickNotes() {
        List<Byte> groupsToRemove = new ArrayList<>();
        Long nowTime = Instant.now().toEpochMilli();

        // Find notes held for longer than MAX_NOTE_ON_SECONDS and time out
        for(Byte groupId : this.heldNotes.keySet()) {
            Map<Byte, Long> group = this.heldNotes.get(groupId);
            List<Byte> notesToRemove = new ArrayList<>();
        
            for(Map.Entry<Byte, Long> note : group.entrySet()) {
                if(nowTime - note.getValue() >= MAX_NOTE_ON_SECONDS * 1000) {
                    notesToRemove.add(note.getKey());
                }
            }

            for(Byte note : notesToRemove) {
                group.remove(note);
            }

            if(group.isEmpty()) {
                groupsToRemove.add(groupId);
            }
        }

        for(Byte groupId : groupsToRemove) {
            this.heldNotes.remove(groupId);
        }

        return !this.heldNotes.isEmpty();
    }

    protected void clearNotes() {
        this.offCounter = 0;
        this.heldNotes.clear();
    }

    protected Boolean hasNotesOn() {
        return !this.heldNotes.isEmpty();
    }
    
    public void onNoteOn(@Nullable Byte channel, @Nonnull Byte note, @Nonnull Byte velocity, @Nullable Byte instrumentId, Long noteTime) {
        Byte groupKey = getNoteGroupKey(channel, instrumentId);

        if(this.triggerMode == TriggerMode.NOTE_HELD) {
            this.heldNotes.computeIfAbsent(groupKey, (key) -> new HashMap<>()).put(note, noteTime);
            this.setPowered(true);
        } else if(this.triggerMode == TriggerMode.NOTE_ON || this.triggerMode == TriggerMode.NOTE_ON_OFF) {
            this.setPowered(true);
        }
    }

    public void onNoteOff(@Nullable Byte channel, @Nonnull Byte note, @Nonnull Byte velocity, @Nullable Byte instrumentId) {
        Byte groupKey = getNoteGroupKey(channel, instrumentId);
    
        if(this.triggerMode == TriggerMode.NOTE_HELD) {
            Map<Byte, Long> groupMap = this.heldNotes.get(groupKey);
            
            if(groupMap != null) {
                groupMap.remove(note);

                if(groupMap.isEmpty()) {
                    this.heldNotes.remove(groupKey);

                    if(this.holdTicks == 0 && this.heldNotes.isEmpty()) {
                        this.setPowered(false);
                    }
                }
            }
        } else if(this.triggerMode == TriggerMode.NOTE_OFF || this.triggerMode == TriggerMode.NOTE_ON_OFF) {
            this.setPowered(true);
        }
    }

    public void onAllNotesOff(@Nullable Byte channel, @Nullable Byte instrumentId) {
        Byte groupKey = getNoteGroupKey(channel, instrumentId);

        if(this.triggerMode == TriggerMode.NOTE_HELD) {
            if(groupKey == null) {
                this.heldNotes.clear();

                if(this.holdTicks == 0) {
                    this.setPowered(false);
                }
            } else {
                this.heldNotes.remove(groupKey);

                if(this.holdTicks == 0 && this.heldNotes.isEmpty()) {
                    this.setPowered(false);
                }
            }
        } else if(this.triggerMode == TriggerMode.NOTE_OFF || this.triggerMode == TriggerMode.NOTE_ON_OFF) {
            this.setPowered(true);
        }
    }

    public abstract Byte getNoteGroupKey(@Nullable Byte channel, @Nullable Byte instrumentId);
}
