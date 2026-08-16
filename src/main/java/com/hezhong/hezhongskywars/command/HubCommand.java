package com.hezhong.hezhongskywars.command;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class HubCommand extends HezhongSkywarsCommand {
    public HubCommand() {
        super("hub", false, "", "返回大厅");
    }

    @Override
    public void runCommand(CommandSender cs, Command command, String label, String[] args) {
        if (!(cs instanceof Player)) {
            cs.sendMessage(ColorT.t("&c仅限玩家操作！"));
            return;
        }
        Player pp = (Player) cs;
        SwPlayer sp = SwPlayerManager.getPlayer(pp);
        if (ConfigValues.bungeeEnabled) {
            if (ConfigValues.serverType == ServerInfoMessage.ServerType.LOBBY) {
                pp.sendMessage(ColorT.t("&a你已经在大厅了！"));
                return;
            } else {
                int minRemaining = 114514;
                String best = null;
                for (Map.Entry<String, Integer> entry : HezhongSkywars.INSTANCE.getGameManager().getHubServers().entrySet()) {
                    if (entry.getValue() > 0 && entry.getValue() < minRemaining) {
                        minRemaining = entry.getValue();
                        best = entry.getKey();
                    }
                }
                if (best != null) {
                    pp.sendMessage(ColorT.t("&a传送到大厅......"));
                    sp.sendTo(best, ServerInfoMessage.ServerType.LOBBY);
                } else {
                    pp.sendMessage(ColorT.t("&c无法找到可用的服务器"));
                }
            }
        } else {
            if (pp.getWorld() == HezhongSkywars.INSTANCE.getLobbySpawnLocation().getWorld()) {
                pp.sendMessage(ColorT.t("&a你已经在大厅了！"));
                return;
            }
            pp.teleport(HezhongSkywars.INSTANCE.getLobbySpawnLocation());
            sp.setPlayingGame(null);
            pp.sendMessage(ColorT.t("&a传送到大厅......"));
        }
    }
}
