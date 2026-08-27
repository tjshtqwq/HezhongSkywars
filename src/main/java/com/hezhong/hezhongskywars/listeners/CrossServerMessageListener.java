package com.hezhong.hezhongskywars.listeners;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.game.SwGameView;
import com.hezhong.hezhongskywars.manager.ListenerManager;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.cross.protocol.ServerInfoMessage;
import com.hezhong.hezhongskywars.cross.protocol.TeleportConfirmMessage;
import com.hezhong.hezhongskywars.cross.protocol.TeleportRequestMessage;
import com.hezhong.hezhongskywars.cross.protocol.UpdateGameMessage;
import com.hezhong.hezhongskywars.cross.protocol.CrossServerMessagePacket;
import com.hezhong.hezhongskywars.utils.ColorT;

public class CrossServerMessageListener {
    // 逻辑在这里
    // BC，VC的监听器调用即可
    private static final Gson gson = new Gson();
    public void receive(CrossServerMessagePacket packet) {
        if (packet.getFrom().equals(ConfigValues.BCserverName)) return; // 避免回环，或者收到自己不该收的包
        if (packet.getTo().equals(ConfigValues.BCserverName)) {
            // 定向给本服，放
        } else if (packet.getTo().equals("")) {
            // 广播检查toType
            if (packet.getToType() != ServerInfoMessage.ServerType.ANY
                    && packet.getToType() != ConfigValues.serverType) return;
        } else {
            return; // 给别人的包。丢弃。
        }
        if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.SERVER_INFO) {
            // 任意来源的服务器状态：GAME上报游戏列表/上线，PROXY广播下线
            ServerInfoMessage msg = gson.fromJson(packet.getMessage(), ServerInfoMessage.class);
            if (msg.isRun()) {
                if (packet.getFromType() == ServerInfoMessage.ServerType.GAME) {
                    HezhongSkywars.INSTANCE.getGameManager().updateRemoteGameViews(msg.getServerName(), msg.getGames());
                }
                if (packet.getFromType() == ServerInfoMessage.ServerType.LOBBY) {
                    HezhongSkywars.INSTANCE.getGameManager().getHubServers().put(msg.getServerName(), (msg.getMaxPlayers() - msg.getPlayers()));
                }
            } else {
                // 下线了（GAME服自报或BC检测到掉线广播）
                HezhongSkywars.INSTANCE.getGameManager().removeRemoteServer(msg.getServerName());
                HezhongSkywars.INSTANCE.getGameManager().getHubServers().remove(msg.getServerName());
            }
            return;
        }
        if (packet.getFromType() == ServerInfoMessage.ServerType.GAME) {
            if (packet.getCommand() == CrossServerMessagePacket.MsgCommand.TELEPORT_CONFIRM) {
                String rawJson = packet.getMessage();

                TeleportConfirmMessage msg = gson.fromJson(rawJson, TeleportConfirmMessage.class);

                SwPlayer sp = SwPlayerManager.getPlayer(msg.getPlayerUuid());
                if (sp == null) return;

                if (msg.getDenyReason() == TeleportConfirmMessage.Reason.NO) {

                    // 可以去send了

                    sp.sendTo(msg.getServerName(), ServerInfoMessage.ServerType.GAME);
                } else if (msg.getDenyReason() ==  TeleportConfirmMessage.Reason.NOT_FOUND) {
                    sp.getPlayer().sendMessage(ColorT.t("&c&l没有找到对应的游戏，你无法传送"));
                } else if (msg.getDenyReason() == TeleportConfirmMessage.Reason.FULL) {
                    sp.getPlayer().sendMessage(ColorT.t("&c&l目标游戏或队列已满，你无法传送"));
                }
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
                TeleportConfirmMessage message = null;
                // 先检查游戏
                SwGameView gameView = HezhongSkywars.INSTANCE.getGameManager().getGameView(requestMessage.getTargetGame(), true);
                if (gameView == null) {
                    message = new TeleportConfirmMessage(requestMessage.getServerName(), requestMessage.getTargetGame(), requestMessage.isSpectate(), requestMessage.getPlayerUuid(), TeleportConfirmMessage.Reason.NOT_FOUND);
                } else {

                    int playersInGame = gameView.getPlayers();
                    // 还要检查requests里面的排队人
                    int playersInQueue = (int) ListenerManager.joinQuitListener.getRequested().entrySet().stream().
                            filter(e -> e.getValue().getTargetGame().equals(gameView.getMapName())).count();
                    playersInGame += playersInQueue;
                    if ((gameView.getMaxPlayers() - playersInGame) >= 1) {
                        // 至少还能进1人


                        // 暂存请求
                        ListenerManager.joinQuitListener.getRequested().put(requestMessage.getPlayerUuid(), requestMessage);

                        // 构造Confirm包
                        // getServerName()是本服，源服是getFrom
                        message = new TeleportConfirmMessage(requestMessage.getServerName(), requestMessage.getTargetGame(), requestMessage.isSpectate(), requestMessage.getPlayerUuid(), TeleportConfirmMessage.Reason.NO);
                    } else {
                        message = new TeleportConfirmMessage(requestMessage.getServerName(), requestMessage.getTargetGame(), requestMessage.isSpectate(), requestMessage.getPlayerUuid(), TeleportConfirmMessage.Reason.FULL);
                    }
                }
                CrossServerMessagePacket sendPacket = new CrossServerMessagePacket(ConfigValues.BCserverName, packet.getFrom(),
                        ServerInfoMessage.ServerType.GAME, packet.getFromType(), CrossServerMessagePacket.MsgCommand.TELEPORT_CONFIRM, gson.toJson(message));
                HezhongSkywars.INSTANCE.getCrossServerMessageSender().sendTo(sendPacket);
            }
        }
    }
}
