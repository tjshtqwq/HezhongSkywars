package com.hezhong.hezhongskywars.task;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import com.hezhong.hezhongskywars.utils.cross.CrossServerMessagePacket;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class CrossServerAutoReport extends BukkitRunnable {

    @Override
    public void run() {
        ServerInfoMessage infoMsg = new ServerInfoMessage(ConfigValues.BCserverName, ConfigValues.serverType, true, HezhongSkywars.INSTANCE.getGameManager().getLocalGameViews(), Bukkit.getServer().getOnlinePlayers().size(), Bukkit.getServer().getMaxPlayers());
        CrossServerMessagePacket packet = new CrossServerMessagePacket(ConfigValues.BCserverName, "", ConfigValues.serverType, ServerInfoMessage.ServerType.ANY, CrossServerMessagePacket.MsgCommand.SERVER_INFO,
                CrossServerMessagePacket.GSON.toJson(infoMsg));
        HezhongSkywars.INSTANCE.getCrossServerMessageSender().sendTo(packet);
    }
}
