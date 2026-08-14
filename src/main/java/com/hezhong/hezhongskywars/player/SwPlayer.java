package com.hezhong.hezhongskywars.player;

import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.game.Game;
import com.hezhong.hezhongskywars.player.party.SwParty;
import com.hezhong.hezhongskywars.utils.SpecialItems;
import com.hezhong.hezhongskywars.utils.type.DatabaseStatsData;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.util.*;

@Getter
@Setter
public class SwPlayer {
    private final Player player;
    private DatabaseStatsData stats; // Null说明未加载
    private Game playingGame; // 玩家在什么游戏内，包括游玩和旁观。
    private final Scoreboard scoreBoard;
    private final Objective scoreBoardObjective;
    private final PlayerScoreboard scoreBoardUpdater;

    private boolean settingUpMap = false;
    private String setUpMapName = ""; // 是游戏地图名，不是MC服务器世界名。取世界名需要读配置！
    private SwPlayerSetupMapStatus setupMapStatus = new SwPlayerSetupMapStatus();
    private Location nextSpawnLocation;

    private SwParty party;

    // 独立世界功能会利用隐藏玩家来控制TAB
    // 为了避免控制TAB的隐藏和游戏中旁观者隐藏打架，我们需要这个Set
    // 此Set非TAB控制隐藏的所有隐藏。
    private Set<Player> hideList = new HashSet<>();


    public SwPlayer(Player player) {
        this.player = player;
        stats = new DatabaseStatsData(this.player.getUniqueId(), this.player.getName());
        // Scoreboard
        scoreBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreBoardObjective = scoreBoard.registerNewObjective("HSWScoreBoard", "dummy");
        scoreBoardObjective.setDisplayName(ConfigValues.serverName);
        scoreBoardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreBoard);
        scoreBoardUpdater = new PlayerScoreboard(this);
    }

    public boolean joinGame(Game g, boolean spectate) {
        if (playingGame != null) {
            return false;
        }
        if (g.addPlayer(player, spectate)) playingGame = g;
        return true;
    }

    public boolean hasKit(SwPlayerKit kit) {
        String kitName = kit.kitName;
        boolean has = stats.kits.stream().anyMatch(k -> k.kitName.equals(kitName));
        return has;
    }

    public boolean hasKit(String kitName) {
        boolean has = stats.kits.stream().anyMatch(k -> k.kitName.equals(kitName));
        return has;
    }

    public void addKills() {
        stats.kills++;
        stats.exps += ConfigValues.expKillAdd;
        stats.coins += ConfigValues.coinsKillAdd;
    }

    public void addWins() {
        stats.wins++;
        stats.exps += ConfigValues.expWinAdd;
        stats.coins += ConfigValues.coinsWinAdd;
    }

    public void addAssists(int coinsA, int expsA) {
        stats.assists++;
        stats.exps += expsA;
        stats.coins += coinsA;
    }

    public void hidePlayer(Player target) {
        // 老版本必须使用单参数方法。新版Bukkit兼容。
        player.hidePlayer(target);

        hideList.add(target);
    }
    public void showPlayer(Player target) {
        if (hideList.contains(target)) { // 确保不覆盖Tab独立的隐藏
            player.showPlayer(target);

            hideList.remove(target);
        }
    }
    public void giveLobbyItems() {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItem(0, SpecialItems.toPlay());
        player.getInventory().setItem(1, SpecialItems.mapSelector());
        // 2: [Space]
        player.getInventory().setItem(3, SpecialItems.hubGUI());
        player.getInventory().setItem(4, SpecialItems.kitSelector());

    }

    @Getter
    @Setter
    public static class SwPlayerSetupMapStatus {
        private Block controllingBlock = null;
        private boolean listeningChat = false;

        private List<Vector> spawns = new ArrayList<>();
        private Map<Vector, String> chests = new LinkedHashMap<>(); // 确保遍历顺序

        public SwPlayerSetupMapStatus() {


        }
    }


    @Getter
    @Setter
    public static class SwPlayerKit {
        private final String kitName;

        public SwPlayerKit(String kitName) {
            this.kitName = kitName;
        }
    }

    @Getter
    @Setter
    public static class SwPartyPlayer {
        private final UUID playerUuid;
        private SwParty parent;
        private boolean own = false;
        public SwPartyPlayer(UUID playerUuid, SwParty parent, boolean own) {
            this.playerUuid = playerUuid;
            this.parent = parent;
            this.own = own;
        }
    }

}
