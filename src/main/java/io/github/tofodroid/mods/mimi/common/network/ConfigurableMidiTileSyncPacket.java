package io.github.tofodroid.mods.mimi.common.network;

import java.util.UUID;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.github.tofodroid.mods.mimi.util.InstrumentDataUtils;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class ConfigurableMidiTileSyncPacket {
    public final BlockPos tilePos;
    public final UUID midiSource;
    public final String midiSourceName;
    public final Byte filterOct;
    public final Byte filterNote;
    public final Boolean invertNoteOct;
    public final Byte instrumentId;
    public final Integer enabledChannelsInt;
    public final Boolean invertInstrument;
    public final Boolean invertSignal;

    public ConfigurableMidiTileSyncPacket(BlockPos tilePos, UUID midiSource, String midiSourceName, Byte filterOct, Byte filterNote, Boolean invertNoteOct, Integer enabledChannelsInt, Byte instrumentId, Boolean invertInstrument, Boolean invertSignal) {
        this.tilePos = tilePos;
        this.midiSource = midiSource;
        this.midiSourceName = midiSourceName;
        this.filterOct = filterOct;
        this.filterNote = filterNote;
        this.invertNoteOct = invertNoteOct;
        this.enabledChannelsInt = enabledChannelsInt;
        this.instrumentId = instrumentId;
        this.invertInstrument = invertInstrument;
        this.invertSignal = invertSignal;
    }
    
    public ConfigurableMidiTileSyncPacket(ItemStack sourceStack, BlockPos tilePos) {
        this.tilePos = tilePos;
        this.midiSource = InstrumentDataUtils.getMidiSource(sourceStack);
        this.midiSourceName = InstrumentDataUtils.getMidiSourceName(sourceStack, false);
        this.filterOct = InstrumentDataUtils.getFilterOct(sourceStack);
        this.filterNote = InstrumentDataUtils.getFilterNote(sourceStack);
        this.invertNoteOct = InstrumentDataUtils.getInvertNoteOct(sourceStack);
        this.enabledChannelsInt = InstrumentDataUtils.getEnabledChannelsInt(sourceStack);
        this.instrumentId = InstrumentDataUtils.getFilterInstrument(sourceStack);
        this.invertInstrument = InstrumentDataUtils.getInvertInstrument(sourceStack);
        this.invertSignal = InstrumentDataUtils.getInvertSignal(sourceStack);
    }

    public static ConfigurableMidiTileSyncPacket decodePacket(FriendlyByteBuf buf) {
         try {
            BlockPos tilePos = buf.readBlockPos();

            UUID midiSource = null;
            if(buf.readBoolean()) {
                midiSource = buf.readUUID();
            }

            String midiSourceName = null;
            if(buf.readBoolean()) {
                midiSourceName = buf.readUtf(64);
            }

            Byte filterOct = buf.readByte();
            Byte filterNote = buf.readByte();
            Boolean invertNoteOct = buf.readBoolean();
            Integer enabledChannelsInt = buf.readInt();
            Byte instrumentId = buf.readByte();
            Boolean invertInstrument = buf.readBoolean();
            Boolean invertSignal = buf.readBoolean();

            return new ConfigurableMidiTileSyncPacket(tilePos, midiSource, midiSourceName, filterOct, filterNote, invertNoteOct, enabledChannelsInt, instrumentId, invertInstrument, invertSignal);
        } catch(IndexOutOfBoundsException e) {
            MIMIMod.LOGGER.error("ConfigurableMidiTileSyncPacket did not contain enough bytes. Exception: " + e);
            return null;
        } catch(DecoderException e) {
            MIMIMod.LOGGER.error("ConfigurableMidiTileSyncPacket contained invalid bytes. Exception: " + e);
            return null;
        }
    }
    
    public static void encodePacket(ConfigurableMidiTileSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.tilePos);

        if(pkt.midiSource != null) {
            buf.writeBoolean(true);
            buf.writeUUID(pkt.midiSource);
        } else {
            buf.writeBoolean(false);
        }

        if(pkt.midiSourceName != null) {
            buf.writeBoolean(true);
            buf.writeUtf(pkt.midiSourceName, 64);
        } else {
            buf.writeBoolean(false);
        }

        buf.writeByte(pkt.filterOct);
        buf.writeByte(pkt.filterNote);
        buf.writeBoolean(pkt.invertNoteOct);
        buf.writeInt(pkt.enabledChannelsInt);
        buf.writeByte(pkt.instrumentId);
        buf.writeBoolean(pkt.invertInstrument);
        buf.writeBoolean(pkt.invertSignal);
    }
}
