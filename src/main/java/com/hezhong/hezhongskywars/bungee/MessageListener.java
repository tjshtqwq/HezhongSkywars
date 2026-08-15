package com.hezhong.hezhongskywars.bungee;

import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class MessageListener implements Listener {
    private static final String CHANNEL_NAME = "hezhongsw:csm";
    private final Plugin plugin;

    public MessageListener(Plugin plugin) {
        plugin.getProxy().registerChannel(CHANNEL_NAME);
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(CHANNEL_NAME)) return;
        if (!(event.getSender() instanceof Server)) return;

        ProxyServer.getInstance().getScheduler().runAsync((Plugin) event.getReceiver(), () -> {
            try {
                byte[] data = event.getData();
                CrossServerMessagePacket packet = CrossServerMessagePacket.fromBytes(data);

                net.md_5.bungee.api.config.ServerInfo targetServer =
                    ProxyServer.getInstance().getServerInfo(packet.getTo());

                if (targetServer == null) {
                    plugin.getLogger().warning("Target server " + packet.getTo() + " not found!");
                    return;
                }

                targetServer.sendData(CHANNEL_NAME, data);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}