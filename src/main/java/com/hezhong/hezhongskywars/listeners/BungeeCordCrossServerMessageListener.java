package com.hezhong.hezhongskywars.listeners;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.manager.ListenerManager;
import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import static com.hezhong.hezhongskywars.HezhongSkywars.CHANNEL_NAME;

public class BungeeCordCrossServerMessageListener implements PluginMessageListener {

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals(CHANNEL_NAME)) {
            CrossServerMessagePacket packet = CrossServerMessagePacket.fromBytes(message);
            if (packet != null) {
                ListenerManager.crossServerMessageListener.receive(packet);
            }
        }
    }
}
