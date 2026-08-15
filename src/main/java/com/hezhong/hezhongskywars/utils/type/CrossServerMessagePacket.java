package com.hezhong.hezhongskywars.utils.type;

import com.google.gson.Gson;
import lombok.Getter;

@Getter
public class CrossServerMessagePacket {
    private final String from;
    private final String to;
    private final String message;

    private static final Gson GSON = new Gson(); // Gson序列化整个Packet类
    public CrossServerMessagePacket(String from, String to, String message) {
        this.from = from;
        this.to = to;
        this.message = message;
    }

    public byte[] toBytes() {
        return GSON.toJson(this).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static CrossServerMessagePacket fromBytes(byte[] data) {
        String json = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        return GSON.fromJson(json, CrossServerMessagePacket.class);
    }

    public enum MsgCommand {
        TELEPORT,
        SERVER_INFO,
    }
}
