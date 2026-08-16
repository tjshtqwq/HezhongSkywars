package com.hezhong.hezhongskywars.listeners;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.manager.ListenerManager;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import com.hezhong.hezhongskywars.utils.cross.TeleportConfirmMessage;
import com.hezhong.hezhongskywars.utils.cross.TeleportRequestMessage;
import com.hezhong.hezhongskywars.utils.cross.UpdateGameMessage;
import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;

public class CrossServerMessageListener {
    // 逻辑在这里
    // BC，VC的监听器调用即可
    private static final Gson gson = new Gson();
    public void receive(CrossServerMessagePacket packet) {
        if (!packet.getTo().equals(ConfigValues.BCserverName) || packet.getFrom().equals(ConfigValues.BCserverName)) return; // 避免回环，或者收到自己不该收的包
        if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.SERVER_INFO) {
            // 任意来源的服务器状态：GAME上报游戏列表/上线，PROXY广播下线
            ServerInfoMessage msg = gson.fromJson(packet.getMessage(), ServerInfoMessage.class);
            if (msg.isRun()) {
                if (packet.getFromType() == ServerInfoMessage.ServerType.GAME) {
                    HezhongSkywars.INSTANCE.getGameManager().updateRemoteGameViews(msg.getServerName(), msg.getGames());
                }
            } else {
                // 下线了（GAME服自报或BC检测到掉线广播）
                HezhongSkywars.INSTANCE.getGameManager().removeRemoteServer(msg.getServerName());
            }
            return;
        }
        if (packet.getFromType() == ServerInfoMessage.ServerType.GAME) {
            if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.TELEPORT_CONFIRM) {
                String rawJson = packet.getMessage();

                TeleportConfirmMessage msg = gson.fromJson(rawJson, TeleportConfirmMessage.class);

                // 可以去send了

                SwPlayer sp = SwPlayerManager.getPlayer(msg.getPlayerUuid());
                if (sp == null) return;
                sp.sendTo(msg.getServerName(), ServerInfoMessage.ServerType.GAME);
            } else if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.UPDATE_GAME) {
                String rawJson = packet.getMessage();

                UpdateGameMessage msg = gson.fromJson(rawJson, UpdateGameMessage.class);
                HezhongSkywars.INSTANCE.getGameManager().updateRemoteGameViews(msg.getServerName(), msg.getGameName(), msg.getGameView());
            }
        }
        if (packet.getFromType() == ServerInfoMessage.ServerType.LOBBY && ConfigValues.serverType == ServerInfoMessage.ServerType.GAME) {
            if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.TELEPORT_REQUEST) {
                String rawJson = packet.getMessage();
                TeleportRequestMessage requestMessage = gson.fromJson(rawJson, TeleportRequestMessage.class);

                // 暂存请求
                ListenerManager.joinQuitListener.getRequested().put(requestMessage.getPlayerUuid(), requestMessage);

                // 构造Confirm包
                TeleportConfirmMessage message = new TeleportConfirmMessage(requestMessage.getServerName(), requestMessage.getTargetGame(), requestMessage.isSpectate(), requestMessage.getPlayerUuid());
                CrossServerMessagePacket sendPacket = new CrossServerMessagePacket(ConfigValues.BCserverName, requestMessage.getServerName(),
                        ServerInfoMessage.ServerType.GAME, packet.getFromType(), CrossServerMessagePacket.MsgCommand.TELEPORT_CONFIRM, gson.toJson(message));
                HezhongSkywars.INSTANCE.getCrossServerMessageSender().sendTo(sendPacket);
            }
        }
    }
}
