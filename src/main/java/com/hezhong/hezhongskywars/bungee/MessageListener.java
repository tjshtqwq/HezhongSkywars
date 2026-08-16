package com.hezhong.hezhongskywars.bungee;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.utils.cross.RedisMessageManager;
import com.hezhong.hezhongskywars.utils.cross.TeleportToMessage;
import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.nio.charset.StandardCharsets;

public class MessageListener {
    private static final Gson gson = new Gson();
    private final Plugin plugin;

    public MessageListener(Plugin plugin, RedisMessageManager redis) {
        this.plugin = plugin;
        // 所有消息都会经过BC（订阅即看到），BC只处理TELEPORT_TO，其余消息只读不处理
        redis.start(message -> ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> handle(message)));
    }

    private void handle(String message) {
        try {
            CrossServerMessagePacket packet = CrossServerMessagePacket.fromBytes(message.getBytes(StandardCharsets.UTF_8));
            if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.TELEPORT_TO) {
                TeleportToMessage msg = gson.fromJson(packet.getMessage(), TeleportToMessage.class);
                ProxiedPlayer pplayer = ProxyServer.getInstance().getPlayer(msg.getPlayerUuid());
                if (pplayer == null) {
                    plugin.getLogger().severe("BungeeHSW Cannot find the proxied player " + msg.getPlayerUuid());
                    return;
                }
                ServerInfo target = ProxyServer.getInstance().getServerInfo(msg.getTargetName());
                if (target == null) {
                    plugin.getLogger().severe("BungeeHSW Cannot find the server " + msg.getTargetName());
                    return;
                }
                pplayer.connect(target);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
