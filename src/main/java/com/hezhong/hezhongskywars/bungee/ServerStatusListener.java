package com.hezhong.hezhongskywars.bungee;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.utils.cross.RedisMessageManager;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.nio.charset.StandardCharsets;

public class ServerStatusListener implements Listener {
    private static final Gson gson = new Gson();
    private final RedisMessageManager redis;

    public ServerStatusListener(RedisMessageManager redis) {
        this.redis = redis;
    }

    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent e) {
        // 服务器掉线（含崩溃），广播下线，子服端会移除该服的游戏缓存
        ServerInfoMessage msg = new ServerInfoMessage(e.getTarget().getName(), ServerInfoMessage.ServerType.PROXY, false, null, 0);
        CrossServerMessagePacket packet = new CrossServerMessagePacket("proxy", "", ServerInfoMessage.ServerType.PROXY, ServerInfoMessage.ServerType.ANY, CrossServerMessagePacket.MsgCommand.SERVER_INFO, gson.toJson(msg));
        redis.publish(new String(packet.toBytes(), StandardCharsets.UTF_8));
    }
}
