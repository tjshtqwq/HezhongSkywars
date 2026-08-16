package com.hezhong.hezhongskywars.command;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.events.HSWGameStartEvent;
import com.hezhong.hezhongskywars.game.Game;
import com.hezhong.hezhongskywars.game.GameStatus;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommand extends HezhongSkywarsCommand {
    public StartCommand() {
        super("start", true, "", "启动你正在游玩的地图");
    }

    @Override
    public void runCommand(CommandSender cs, Command command, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;
            SwPlayer sp = SwPlayerManager.getPlayer(p);
            if (sp.getPlayingGame() != null) {
                Game playing = sp.getPlayingGame();
                GameStatus status = playing.getGameStatus();
                if (status != GameStatus.WAITING && status != GameStatus.STARTING) {
                    p.sendMessage(ColorT.t("&c当前游戏状态（" + status + "）无法手动开始！"));
                    return;
                }
                HezhongSkywars.INSTANCE.getPlugin().getServer().getPluginManager().callEvent(new HSWGameStartEvent(playing.getMapName()));
                p.sendMessage(ColorT.t("&a立即启动"));
            } else {
                p.sendMessage(ColorT.t("&c你不在任何游戏中"));
            }
        } else {
            cs.sendMessage(ColorT.t("&c仅限玩家操作！"));
        }
    }
}
