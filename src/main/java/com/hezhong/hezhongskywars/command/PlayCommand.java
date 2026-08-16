package com.hezhong.hezhongskywars.command;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.game.GameStatus;
import com.hezhong.hezhongskywars.game.SwGameView;
import com.hezhong.hezhongskywars.game.queue.QueueManager;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayCommand extends HezhongSkywarsCommand {
    public PlayCommand() {
        super("play", false, "[mapName]", "游玩一个地图");
    }

    @Override
    public void runCommand(CommandSender cs, Command command, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;
            SwPlayer sp = SwPlayerManager.getPlayer(p);
            if (args.length < 2) {
                p.sendMessage(ColorT.t("&e自动寻找中......"));
                HezhongSkywars.INSTANCE.getGameManager().getQueueManager().findGame(sp);
                return;
            }

            SwGameView view = HezhongSkywars.INSTANCE.getGameManager().getGameView(args[1], false);
            if (view == null) {
                cs.sendMessage(ColorT.t("&c地图不存在"));
                return;
            }
            if (view.getGameStatus() == GameStatus.RESETTING) {
                cs.sendMessage(ColorT.t("&c地图正在重置"));
                return;
            }
            sp.joinGame(view, false);
            cs.sendMessage(ColorT.t("&a把你发送到游戏 " + args[1]));
        } else {
            cs.sendMessage(ColorT.t("&c仅限玩家操作！"));
        }
    }
}
