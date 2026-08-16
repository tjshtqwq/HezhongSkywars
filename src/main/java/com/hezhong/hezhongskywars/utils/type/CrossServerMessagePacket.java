package com.hezhong.hezhongskywars.utils.type;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage.ServerType;
import lombok.Getter;

@Getter
public class CrossServerMessagePacket {
    private final ServerType fromType;
    private final ServerType toType;
    private final String from;
    private final String to;
    private final MsgCommand command;
    private final String message; // 可以存json字串

    public static final Gson GSON = new Gson(); // Gson序列化整个Packet类
    public CrossServerMessagePacket(String from, String to, ServerType fromType, ServerType toType, MsgCommand command, String message) {
        this.from = from;
        this.to = to;
        this.fromType = fromType;
        this.toType = toType;
        this.command = command;
        this.message = message;
    }

    public byte[] toBytes() {
        return GSON.toJson(this).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static CrossServerMessagePacket fromBytes(byte[] data) {
        String json = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        return GSON.fromJson(json, CrossServerMessagePacket.class);
    }

    // 去大厅可以直接TpTo
    public enum MsgCommand {
        TELEPORT_REQUEST,
        SERVER_INFO,
        TELEPORT_CONFIRM,
        TELEPORT_TO,
        UPDATE_GAME,
    }
}
