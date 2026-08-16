package com.hezhong.hezhongskywars.bungee;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;

public class MessageListener implements Listener {
    private static final String CHANNEL_NAME = "hezhongsw:csm";
    private final Plugin plugin;

    private static final Gson gson = new Gson();

    private final Map<String, ServerInfoMessage.ServerType> servers = new HashMap<>();

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

                if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.SERVER_INFO) {
                    // Gson反序列化到ServerInfoMessage
                    ServerInfoMessage msg = gson.fromJson(packet.getMessage(), ServerInfoMessage.class);
                    servers.put(msg.getServerName(), msg.getServerType()); // 记录服务器

                    if (packet.getToType() != ServerInfoMessage.ServerType.PROXY) {
                        // 转发
                        for (Map.Entry<String, ServerInfoMessage.ServerType> entry : servers.entrySet()) {
                            if (entry.getValue().equals(msg.getServerType())) {
                                ServerInfo s = plugin.getProxy().getServerInfo(entry.getKey());
                                if (s != null) {
                                    s.sendData(CHANNEL_NAME, packet.toBytes());
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}