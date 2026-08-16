package com.hezhong.hezhongskywars.utils.cross;

import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import org.bukkit.plugin.java.JavaPlugin;

public interface CrossServerMessageSender {
    void registerChannel(JavaPlugin plugin);
    void sendTo(CrossServerMessagePacket message);
    void shutdown();
}
