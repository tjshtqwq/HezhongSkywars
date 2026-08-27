package com.hezhong.hezhongskywars.cross.networking;

import com.hezhong.hezhongskywars.cross.protocol.CrossServerMessagePacket;
import org.bukkit.plugin.java.JavaPlugin;

public interface CrossServerMessageSender {
    void registerChannel(JavaPlugin plugin);
    void sendTo(CrossServerMessagePacket message);
    void shutdown();
}
