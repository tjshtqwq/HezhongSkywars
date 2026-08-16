package com.hezhong.hezhongskywars.listeners;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;

public class CrossServerMessageListener {
    // 逻辑在这里
    // BC，VC的监听器调用即可
    private static final Gson gson = new Gson();
    public void receive(CrossServerMessagePacket packet) {
        if (packet.getFromType() == ServerInfoMessage.ServerType.GAME) {
            // GAME服更新数据。我们这里也要同步更新
            String rawJson = packet.getMessage();
            // 反序列化过去
            ServerInfoMessage msg = gson.fromJson(rawJson, ServerInfoMessage.class);
            if (msg.isRun()) {
                HezhongSkywars.INSTANCE.getGameManager().updateRemoteGameViews(msg.getServerName(), msg.getGames());
            } else {
                // 下线了
                HezhongSkywars.INSTANCE.getGameManager().removeRemoteServer(msg.getServerName());
            }
        }
    }
}
