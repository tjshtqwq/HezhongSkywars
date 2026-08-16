package com.hezhong.hezhongskywars.command;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.utils.ColorT;
import com.hezhong.hezhongskywars.utils.cross.SwGameView;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ListGamesCommand extends HezhongSkywarsCommand {
    public ListGamesCommand() {
        super("listGames", false, "", "列出游戏");
    }

    @Override
    public void runCommand(CommandSender cs, Command command, String label, String[] args) {
        cs.sendMessage(ColorT.t("&a游戏"));
        for (SwGameView view : HezhongSkywars.INSTANCE.getGameManager().getAllGameViews()) {
            cs.sendMessage(ColorT.t(String.format("&b游戏 &7%s&b 服务器 &7%s&b 状态 &7%s&b 人数 &7%s/%s",
                    view.getMapName(), view.getServerName(), view.getGameStatus(), view.getPlayers(), view.getMaxPlayers())));
        }
    }
}
