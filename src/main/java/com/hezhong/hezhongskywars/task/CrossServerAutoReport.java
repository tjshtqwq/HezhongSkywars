package com.hezhong.hezhongskywars.task;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;
import com.hezhong.hezhongskywars.utils.type.DatabaseStatsData;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.xml.crypto.Data;
import java.util.List;
import java.util.UUID;

public class CrossServerAutoReport extends BukkitRunnable {

    @Override
    public void run() {
        ServerInfoMessage infoMsg = new ServerInfoMessage(ConfigValues.BCserverName, ConfigValues.serverType, true, HezhongSkywars.INSTANCE.getGameManager().getLocalGameViews(), Bukkit.getServer().getOnlinePlayers().size());
        CrossServerMessagePacket packet = new CrossServerMessagePacket(ConfigValues.BCserverName, "", ConfigValues.serverType, ServerInfoMessage.ServerType.ANY, CrossServerMessagePacket.MsgCommand.SERVER_INFO,
                CrossServerMessagePacket.GSON.toJson(infoMsg));
        HezhongSkywars.INSTANCE.getCrossServerMessageSender().sendTo(packet);
    }
}
