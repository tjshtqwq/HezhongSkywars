package com.hezhong.hezhongskywars.bungee;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import com.hezhong.hezhongskywars.utils.cross.TeleportToMessage;
import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
                        // ServerInfo是广播包
                        for (Map.Entry<String, ServerInfoMessage.ServerType> entry : servers.entrySet()) {
                            if (entry.getValue().equals(packet.getToType()) || packet.getToType() == ServerInfoMessage.ServerType.ANY) { // 广播到符合type的服务器
                                ServerInfo s = plugin.getProxy().getServerInfo(entry.getKey());
                                if (s != null) {
                                    s.sendData(CHANNEL_NAME, packet.toBytes());
                                } else {
                                    plugin.getLogger().severe("BungeeHSW Cannot find the server " + entry.getKey());
                                }
                            }
                        }
                    }
                }

                if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.TELEPORT_CONFIRM || packet.getCommand() == CrossServerMessagePacket.MsgCommand.TELEPORT_REQUEST ||
                        packet.getCommand() == CrossServerMessagePacket.MsgCommand.UPDATE_GAME) {
                    // 直接转发这两个包
                    ServerInfo s = plugin.getProxy().getServerInfo(packet.getTo());
                    if (s != null) {
                        s.sendData(CHANNEL_NAME, packet.toBytes());
                    } else {
                        plugin.getLogger().severe("BungeeHSW Cannot find the server " + packet.getTo());
                    }
                }

                if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.TELEPORT_TO) {
                    // 直接传送，然后转发
                    TeleportToMessage msg = gson.fromJson(packet.getMessage(), TeleportToMessage.class);
                    ProxiedPlayer pplayer = plugin.getProxy().getPlayer(msg.getPlayerUuid());
                    if (pplayer != null) {
                        ServerInfo target = plugin.getProxy().getServerInfo(msg.getTargetName());
                        if (target != null) {
                            pplayer.connect(target);
                        } else {
                            plugin.getLogger().severe("BungeeHSW Cannot find the server " + msg.getTargetName());
                        }
                    } else {
                        plugin.getLogger().severe("BungeeHSW Cannot find the proxied player " + msg.getPlayerUuid());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}