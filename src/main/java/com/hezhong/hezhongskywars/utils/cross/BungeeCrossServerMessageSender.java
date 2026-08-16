package com.hezhong.hezhongskywars.utils.cross;

import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import org.bukkit.plugin.java.JavaPlugin;

import static com.hezhong.hezhongskywars.HezhongSkywars.CHANNEL_NAME;

public class BungeeCrossServerMessageSender implements CrossServerMessageSender {

    private JavaPlugin plugin;

    @Override
    public void registerChannel(JavaPlugin plugin) {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_NAME);
        this.plugin = plugin;
    }

    @Override
    public void sendTo(CrossServerMessagePacket packet) {
        if (plugin != null) {
            plugin.getServer().sendPluginMessage(plugin, CHANNEL_NAME, packet.toBytes());
        }
    }
}
