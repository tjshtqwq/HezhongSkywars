package com.hezhong.hezhongskywars.player;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.game.Game;
import com.hezhong.hezhongskywars.game.SwGameView;
import com.hezhong.hezhongskywars.player.party.SwParty;
import com.hezhong.hezhongskywars.utils.SpecialItems;
import com.hezhong.hezhongskywars.cross.protocol.ServerInfoMessage;
import com.hezhong.hezhongskywars.cross.protocol.TeleportRequestMessage;
import com.hezhong.hezhongskywars.cross.protocol.TeleportToMessage;
import com.hezhong.hezhongskywars.cross.protocol.CrossServerMessagePacket;
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
    private final PlayerGeneralTask scoreBoardUpdater;

    private boolean dbSaved = false; // 被跨服传送走的，这个为true

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
        scoreBoardUpdater = new PlayerGeneralTask(this);
    }

    public boolean joinGame(SwGameView view, boolean spectate) {
        if (!ConfigValues.bungeeEnabled || view.getServerName().equals(ConfigValues.BCserverName)) {
            // 本服：靠mapName索引到Game
            Game g = HezhongSkywars.INSTANCE.getGameManager().getGames().get(view.getMapName());
            if (g == null) {
                return false;
            }
            if (playingGame != null) {
                return false;
            }
            if (!spectate) {
                // 不旁观，考虑队伍
                if (party != null) {
                    SwPartyPlayer spp = party.getPlayers().get(player.getUniqueId());
                    if (spp.isOwn()) {
                        // 预留
                    }
                }
            }
            if (g.addPlayer(player, spectate)) {
                playingGame = g;
                return true;
            } else {
                return false;
            }
        } else {
            if (ConfigValues.serverType == ServerInfoMessage.ServerType.LOBBY) {
                // 在大厅，可以传送了
                // 构造包
                TeleportRequestMessage request = new TeleportRequestMessage(view.getServerName(), view.getMapName(), spectate, player.getUniqueId());
                CrossServerMessagePacket packet = new CrossServerMessagePacket(ConfigValues.BCserverName, view.getServerName(),
                        ConfigValues.serverType, ServerInfoMessage.ServerType.GAME, CrossServerMessagePacket.MsgCommand.TELEPORT_REQUEST,
                        CrossServerMessagePacket.GSON.toJson(request));
                HezhongSkywars.INSTANCE.getCrossServerMessageSender().sendTo(packet);
            }
            return true;
        }
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
        if (!ConfigValues.bungeeEnabled || ConfigValues.serverType == ServerInfoMessage.ServerType.LOBBY) {
            player.getInventory().setItem(0, SpecialItems.toPlay());
            player.getInventory().setItem(1, SpecialItems.mapSelector());
            // 2: [Space]
            player.getInventory().setItem(3, SpecialItems.hubGUI());
            player.getInventory().setItem(4, SpecialItems.kitSelector());
        }

    }

    public void sendTo(String toServer, ServerInfoMessage.ServerType toType) {
        Bukkit.getScheduler().runTaskAsynchronously(HezhongSkywars.INSTANCE.getPlugin(), () -> {
            if (ConfigValues.bungeeEnabled) {
                // 先存数据
                try {
                    if (stats.REFRESHED && !dbSaved) {
                        HezhongSkywars.INSTANCE.getLogger().info("Saving data for " + player.getUniqueId());
                        HezhongSkywars.INSTANCE.getDatabase().setDatabaseStats(player.getUniqueId(), stats);
                        HezhongSkywars.INSTANCE.getLogger().info("Saved data for " + player.getUniqueId());
                        dbSaved = true;
                    }
                } catch (Exception e) {
                    HezhongSkywars.INSTANCE.getLogger().severe("Failed to save data for " + player.getUniqueId() + ", data may be lost!");
                }

                // 构造TP包
                TeleportToMessage msg = new TeleportToMessage(toServer, player.getUniqueId());
                CrossServerMessagePacket packet = new CrossServerMessagePacket(ConfigValues.BCserverName, toServer, ConfigValues.serverType, toType, CrossServerMessagePacket.MsgCommand.TELEPORT_TO,
                        CrossServerMessagePacket.GSON.toJson(msg));
                HezhongSkywars.INSTANCE.getCrossServerMessageSender().sendTo(packet);
            }
        });
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
