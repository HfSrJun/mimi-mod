package io.github.tofodroid.mods.mimi.common.network;

import java.util.UUID;

import io.github.tofodroid.mods.mimi.common.MIMIMod;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

public class ServerMidiUploadPacket {
    public static final int MAX_DATA_SIZE = 30000;
    public static final Byte UPLOAD_SUCCESS = Integer.valueOf(0).byteValue();
    public static final Byte UPLOAD_RESEND = Integer.valueOf(1).byteValue();
    public static final Byte UPLOAD_FAIL = Integer.valueOf(2).byteValue();

    public String fileName;
    public UUID fileId;
    public Byte part;
    public Byte totalParts;
    public byte[] data;

    public ServerMidiUploadPacket(UUID fileId, Byte responseStatus) {
        this(fileId, responseStatus, Integer.valueOf(0).byteValue(), new byte[]{});
    }

    public ServerMidiUploadPacket(UUID fileId, Byte responseStatus, Byte data) {
        this(fileId, responseStatus, data, new byte[]{});
    }

    public ServerMidiUploadPacket(UUID fileId, Byte totalParts, Byte part, byte[] data) {
        this("", fileId, part, totalParts, data);
    }

    public ServerMidiUploadPacket(String fileName, Byte totalParts, Byte part, byte[] data) {
        this(fileName, new UUID(0,0), totalParts, part, data);
    }

    private ServerMidiUploadPacket(String fileName, UUID fileId, Byte totalParts, Byte part, byte[] data) {
        this.fileName = fileName;
        this.fileId = fileId;
        this.part = part;
        this.totalParts = totalParts;

        if(data.length > MAX_DATA_SIZE) {
            MIMIMod.LOGGER.error("ServerMidiUploadPacket data contained too many bytes!");
            this.data = new byte[]{};
        } else {
            this.data = data;
        }
    }
    
    public static ServerMidiUploadPacket decodePacket(FriendlyByteBuf buf) {
        try {
            String fileName = buf.readUtf(32);
            UUID fileId = buf.readUUID();
            Byte part = buf.readByte();
            Byte totalParts = buf.readByte();
            byte[] data = buf.readByteArray(MAX_DATA_SIZE);

            return new ServerMidiUploadPacket(fileName, fileId, part, totalParts, data);
        } catch(IndexOutOfBoundsException e) {
            MIMIMod.LOGGER.error("ServerMidiUploadPacket did not contain enough bytes. Exception: " + e);
            return null;
        } catch(DecoderException e) {
            MIMIMod.LOGGER.error("ServerMidiUploadPacket contained invalid bytes. Exception: " + e);
            return null;
        }
    }

    public static void encodePacket(ServerMidiUploadPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.fileName, 32);
        buf.writeUUID(pkt.fileId);
        buf.writeByte(pkt.part);
        buf.writeByte(pkt.totalParts);
        buf.writeByteArray(pkt.data);
    }
}
