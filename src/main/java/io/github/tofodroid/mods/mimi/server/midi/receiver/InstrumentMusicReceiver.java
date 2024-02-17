package io.github.tofodroid.mods.mimi.server.midi.receiver;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.tofodroid.mods.mimi.common.midi.TransmitterNoteEvent;
import io.github.tofodroid.mods.mimi.common.network.MidiNotePacket;
import io.github.tofodroid.mods.mimi.common.network.MidiNotePacketHandler;
import io.github.tofodroid.mods.mimi.server.ServerExecutor;
import io.github.tofodroid.mods.mimi.util.MidiNbtDataUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InstrumentMusicReceiver extends AMusicReceiver {
    protected Byte instrumentId;
    protected UUID notePlayerId;
    protected Integer enabledChannels;
    protected Byte volume;
    protected InteractionHand handIn;

    public InstrumentMusicReceiver(Supplier<BlockPos> pos, Supplier<ResourceKey<Level>> dim, UUID notePlayerId, ItemStack instrumentStack, InteractionHand handIn) {
        super(MidiNbtDataUtils.getMidiSource(instrumentStack), new ArrayList<>(MidiNbtDataUtils.getEnabledChannelsSet(instrumentStack)), pos, dim);
        this.notePlayerId = notePlayerId;
        this.instrumentId = MidiNbtDataUtils.getInstrumentId(instrumentStack);
        this.enabledChannels = MidiNbtDataUtils.getEnabledChannelsInt(instrumentStack);
        this.volume = MidiNbtDataUtils.getInstrumentVolume(instrumentStack);
        this.handIn = handIn;
    }

    public void allNotesOff(ServerLevel sourceLevel) {
        ServerExecutor.executeOnServerThread(
            () -> MidiNotePacketHandler.handlePacketServer(MidiNotePacket.createAllNotesOffPacket(instrumentId, notePlayerId, this.blockPos.get(), this.handIn), sourceLevel, null)
        );
    }

    @Override
    protected Boolean willHandlePacket(TransmitterNoteEvent packet, UUID sourceId, BlockPos sourcePos, ServerLevel sourceLevel) {
        return sourceLevel.dimension().equals(dimension.get()) && MidiNbtDataUtils.isChannelEnabled(this.enabledChannels, packet.channel) && Math.abs(Math.sqrt(sourcePos.distSqr(blockPos.get()))) <= (packet.isNoteOffEvent() ? 32 : 16);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof InstrumentMusicReceiver) {
            InstrumentMusicReceiver o = (InstrumentMusicReceiver) other;

            return o.linkedId.equals(this.linkedId) &&
                o.instrumentId == this.instrumentId &&
                o.enabledChannels == this.enabledChannels &&
                o.volume == this.volume;
        }
        
        return false;
    }

    @Override
    protected void doHandlePacket(TransmitterNoteEvent packet, UUID sourceId, BlockPos sourcePos, ServerLevel sourceLevel) {
        MidiNotePacket notePacket;

        if(packet.isControlEvent()) {
            notePacket = MidiNotePacket.createControlPacket(packet.getControllerNumber(), packet.getControllerValue(), instrumentId, notePlayerId, blockPos.get(), packet.noteServerTime, handIn);
        } else {
            notePacket = MidiNotePacket.createNotePacket(packet.note, MidiNbtDataUtils.applyVolume(this.volume, packet.velocity), instrumentId, notePlayerId, blockPos.get(), packet.noteServerTime, handIn);
        }

        ServerExecutor.executeOnServerThread(
            () -> MidiNotePacketHandler.handlePacketServer(notePacket, sourceLevel, null)
        );
    }
}
