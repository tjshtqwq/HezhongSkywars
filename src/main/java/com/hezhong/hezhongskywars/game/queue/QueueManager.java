package com.hezhong.hezhongskywars.game.queue;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.game.Game;
import com.hezhong.hezhongskywars.game.GameStatus;
import com.hezhong.hezhongskywars.game.SwGameView;
import com.hezhong.hezhongskywars.manager.GameManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class QueueManager {
    public void findGame(SwPlayer sp) {
        if (sp.getPlayingGame() != null) {
            sp.getPlayer().sendMessage(ColorT.t("&c你正在游戏中！"));
            return;
        }
        // 寻找还没开始，且maxPlayers - players最大的游戏
        List<SwGameView> sortedList = HezhongSkywars.INSTANCE.getGameManager().getAllGameViews().stream()
                .filter(g -> (g.getGameStatus() == GameStatus.WAITING || g.getGameStatus() == GameStatus.STARTING) && (g.getMaxPlayers() - g.getPlayers()) >= 1)
                .sorted(Comparator.comparingInt(g -> g.getMaxPlayers() - g.getPlayers()))
                .collect(Collectors.toList());

        if (sortedList.isEmpty()) {
            sp.getPlayer().sendMessage(ColorT.t("&c&l无可用游戏！"));
            return;
        } else {
            SwGameView best = sortedList.get(0);
            sp.getPlayer().sendMessage(ColorT.t("&a把你发送到 " + best.getMapName()));
            sp.joinGame(best, false);
        }
    }
}
