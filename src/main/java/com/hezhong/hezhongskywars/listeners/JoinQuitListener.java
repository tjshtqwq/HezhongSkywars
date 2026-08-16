package com.hezhong.hezhongskywars.listeners;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.game.Game;
import com.hezhong.hezhongskywars.game.SwGameView;
import com.hezhong.hezhongskywars.manager.ListenerManager;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.task.PlayerScoreBoardTask;
import com.hezhong.hezhongskywars.utils.ColorT;
import com.hezhong.hezhongskywars.utils.SpecialItems;
import com.hezhong.hezhongskywars.utils.cross.TeleportRequestMessage;
import com.hezhong.hezhongskywars.utils.cross.TeleportToMessage;
import com.hezhong.hezhongskywars.utils.type.DatabaseStatsData;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JoinQuitListener implements Listener {
    @Getter
    private final Map<UUID, TeleportRequestMessage> requested = new ConcurrentHashMap<>();
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Pre-Process
        final Player player = event.getPlayer();
        if (HezhongSkywars.INSTANCE.getDatabase().writingPlayers.contains(player.getUniqueId())) {
            player.kickPlayer(ColorT.t("&b&lHSW &a你的数据还未刷新，请等待一下再进入！"));
            return;
        }
        World lobbyWorld = Bukkit.getWorld(ConfigValues.lobbyWorld);
        SwPlayerManager.addPlayer(player);
        final SwPlayer p = SwPlayerManager.getPlayer(player);
        if (lobbyWorld != null) {
            player.teleport(lobbyWorld.getSpawnLocation());
            // 重置经验
            player.setLevel(0);
            player.setExp(0);
            p.setNextSpawnLocation(lobbyWorld.getSpawnLocation());
        } else {
            HezhongSkywars.INSTANCE.getLogger().warning("HSW LobbyWorld is Null?");
        }

        Bukkit.getScheduler().runTaskAsynchronously(HezhongSkywars.INSTANCE.getPlugin(), () -> {
            p.setStats(HezhongSkywars.INSTANCE.getDatabase().getDatabaseStats(player.getUniqueId()));
            if (p.getStats() == null) {
                p.setStats(new DatabaseStatsData(player.getUniqueId(), player.getName()));
            }
            p.getStats().REFRESHED = true;
        });
        // Post-Process


        p.giveLobbyItems();
        player.setGameMode(ConfigValues.defaultGameMode);

        ListenerManager.independentWorldManager.handlePostJoin(event);

        // 尝试进游戏
        if (requested.containsKey(player.getUniqueId())) {
            TeleportRequestMessage msg = requested.remove(player.getUniqueId());
            SwGameView gameView = HezhongSkywars.INSTANCE.getGameManager().getGameView(msg.getTargetGame(), true);
            p.joinGame(gameView, msg.isSpectate());
        }

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 先处理游戏退出
        SwPlayer sp = SwPlayerManager.getPlayer(event.getPlayer());
        if (sp != null) {
            if (sp.getPlayingGame() != null) {
                Game playing = sp.getPlayingGame();
                playing.processDeath(event.getPlayer(), null, true);
            }
            SwPlayerManager.removePlayer(event.getPlayer());
            // 写数据（持有sp对象）
            final DatabaseStatsData data = sp.getStats();
            UUID uuid = event.getPlayer().getUniqueId();
            HezhongSkywars.INSTANCE.getDatabase().writingPlayers.add(uuid);

            Bukkit.getScheduler().runTaskAsynchronously(HezhongSkywars.INSTANCE.getPlugin(), () -> {
                try {
                    if (data.REFRESHED && !sp.isDbSaved()) {
                        HezhongSkywars.INSTANCE.getLogger().info("Saving data for " + uuid);
                        HezhongSkywars.INSTANCE.getDatabase().setDatabaseStats(uuid, data);
                        HezhongSkywars.INSTANCE.getLogger().info("Saved data for " + uuid);
                    }
                } catch (Exception e) {
                    // 数据库写失败也要解除锁号，否则玩家再也进不来
                    HezhongSkywars.INSTANCE.getLogger().severe("Failed to save data for " + uuid + ", data may be lost!");
                    e.printStackTrace();
                } finally {
                    HezhongSkywars.INSTANCE.getDatabase().writingPlayers.remove(uuid);
                }
            });
        }
    }

}
