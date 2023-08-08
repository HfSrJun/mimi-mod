package io.github.tofodroid.mods.mimi.common.tile;

import java.util.UUID;

import javax.annotation.Nullable;

import io.github.tofodroid.mods.mimi.common.block.BlockBroadcaster;
import io.github.tofodroid.mods.mimi.common.container.ContainerBroadcaster;
import io.github.tofodroid.mods.mimi.common.item.ItemFloppyDisk;
import io.github.tofodroid.mods.mimi.common.item.ModItems;
import io.github.tofodroid.mods.mimi.server.midi.MusicPlayerMidiHandler;
import io.github.tofodroid.mods.mimi.server.midi.ServerMusicPlayerMidiManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

public class TileBroadcaster extends AContainerTile implements BlockEntityTicker<TileBroadcaster> {
    protected Boolean diskError = false;

    // Persist
    public static final String BROADCAST_PUBLIC_TAG = "broadcast_public";

    public TileBroadcaster(BlockPos pos, BlockState state) {
        super(ModTiles.BROADCASTER, pos, state, 1);
        items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    public static void doTick(Level world, BlockPos pos, BlockState state, TileBroadcaster self) {
        self.tick(world, pos, state, self);
    }

    public ItemStack getActiveFloppyDiskStack() {
        if(items.isEmpty() || items.get(0) == null) {
            return ItemStack.EMPTY;
        } else {
            return items.get(0);
        }
    }

    public Boolean hasActiveFloppyDisk() {
        return !getActiveFloppyDiskStack().isEmpty();
    }

    public UUID getMusicPlayerId() {
        String idString = "tile-music-player-" + this.getBlockPos().getX() + "-" + this.getBlockPos().getY() + "-" + this.getBlockPos().getZ();
        return UUID.nameUUIDFromBytes(idString.getBytes());
    }

    public Boolean isPublicBroadcast() {
        return this.getPersistentData().contains(BROADCAST_PUBLIC_TAG) && this.getPersistentData().getBoolean(BROADCAST_PUBLIC_TAG);
    }

    public void togglePublicBroadcast() {
        if(this.hasLevel() && !this.level.isClientSide && this.level instanceof ServerLevel) {
            this.getPersistentData().putBoolean(BROADCAST_PUBLIC_TAG, !this.isPublicBroadcast());
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
        }
    }
    
    public Boolean isPowered() {
        return this.getBlockState().getValue(BlockBroadcaster.POWER) > 0;
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return stack.getItem().equals(ModItems.FLOPPYDISK) && ItemFloppyDisk.isWritten(stack);
    }
    
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        return new ContainerBroadcaster(id, playerInventory, this.getBlockPos());
    }

    @Override
    public Component getDefaultName() {
		return Component.translatable(this.getBlockState().getBlock().asItem().getDescriptionId());
    }

    @Override
    public void setItem(int i, ItemStack item) {
        if(this.level instanceof ServerLevel && !this.level.isClientSide) {
            if(item.getItem() instanceof ItemFloppyDisk) {
                // Unload existing disk, if present
                if(!this.getActiveFloppyDiskStack().isEmpty() && !ItemStack.matches(this.getActiveFloppyDiskStack(), item)) {
                    unloadDisk();
                }

                loadDisk(item);
            } else {
                unloadDisk();
            }
        }

        super.setItem(i, item);
    }

    @Override
    public ItemStack removeItem(int i, int count) {
        unloadDisk();
        return super.removeItem(i, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        unloadDisk();
        return super.removeItemNoUpdate(i);
    }


    public void setUnpowered() {
        BlockState state = this.getBlockState().setValue(BlockBroadcaster.POWER, 0);
        this.level.setBlock(this.getBlockPos(), state, 3);
        setChanged();
    }

    public void setPowered() {
        BlockState state = this.getBlockState().setValue(BlockBroadcaster.POWER, 15);
        this.level.setBlock(this.getBlockPos(), state, 3);
        setChanged();
    }

    public void unloadDisk() {
        this.setUnpowered();
        diskError = false;
        ServerMusicPlayerMidiManager.stopBroadcaster(this.getMusicPlayerId());
    }

    public void loadDisk(ItemStack stack) {
        this.setPowered();        
        
        if(ServerMusicPlayerMidiManager.createBroadcaster(this, ItemFloppyDisk.getMidiUrl(stack))) {
            ServerMusicPlayerMidiManager.playBroadcaster(this.getMusicPlayerId());
        } else {
            this.setUnpowered();
            this.diskError = true;
        }
    }

    public void playMusic() {
        MusicPlayerMidiHandler handler = ServerMusicPlayerMidiManager.getBroadcaster(this.getMusicPlayerId());

        if(handler != null) {
            this.setPowered();
            handler.play();
        }
    }

    public void stopMusic() {
        MusicPlayerMidiHandler handler = ServerMusicPlayerMidiManager.getBroadcaster(this.getMusicPlayerId());

        if(handler != null) {
            this.setUnpowered();
            handler.stop();
        }
    }

    public void pauseMusic() {
        MusicPlayerMidiHandler handler = ServerMusicPlayerMidiManager.getBroadcaster(this.getMusicPlayerId());

        if(handler != null) {
            handler.pause();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if(!this.level.isClientSide && this.level instanceof ServerLevel) {
            ServerMusicPlayerMidiManager.stopBroadcaster(this.getMusicPlayerId());
        }
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, TileBroadcaster self) {
        if(this.hasLevel() && !this.level.isClientSide && world instanceof ServerLevel) {
            // If removed, stop playing and return immediately
            MusicPlayerMidiHandler handler = ServerMusicPlayerMidiManager.getBroadcaster(this.getMusicPlayerId());
            if(this.isRemoved() && handler != null) {
                unloadDisk();
                return;
            } else if(this.isRemoved()) {
                return;
            }
            
            // If loaded but has no handler, create one
            if(this.hasActiveFloppyDisk() && handler == null && !diskError) {
                this.loadDisk(this.getActiveFloppyDiskStack());
            }
        }
    }
}