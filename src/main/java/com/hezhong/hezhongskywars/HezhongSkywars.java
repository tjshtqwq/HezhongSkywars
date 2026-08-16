package com.hezhong.hezhongskywars;

import com.hezhong.hezhongskywars.command.CommandProcessor;
import com.hezhong.hezhongskywars.config.ConfigManager;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.db.DataBaseController;
import com.hezhong.hezhongskywars.game.GameListener;
// import com.hezhong.hezhongskywars.game.gui.GUIListener;
import com.hezhong.hezhongskywars.gui.GUIListener;
import com.hezhong.hezhongskywars.listeners.BungeeCordCrossServerMessageListener;
import com.hezhong.hezhongskywars.listeners.CrossServerMessageListener;
import com.hezhong.hezhongskywars.listeners.GeneralListener;
import com.hezhong.hezhongskywars.listeners.JoinQuitListener;
import com.hezhong.hezhongskywars.manager.GameManager;
import com.hezhong.hezhongskywars.manager.ListenerManager;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.multiworld.IndependentWorldManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.setup.SetupListener;
import com.hezhong.hezhongskywars.task.CrossServerAutoReport;
import com.hezhong.hezhongskywars.task.PlayerScoreBoardTask;
import com.hezhong.hezhongskywars.task.ServerDatabaseUpdateTask;
import com.hezhong.hezhongskywars.utils.ColorT;
import com.hezhong.hezhongskywars.utils.cross.BungeeCrossServerMessageSender;
import com.hezhong.hezhongskywars.utils.cross.CrossServerMessageSender;
import com.hezhong.hezhongskywars.utils.type.DatabaseStatsData;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

@Slf4j
@Getter
public enum HezhongSkywars {
    INSTANCE;

    private HezhongSkywarsLoader plugin;
    private ConfigManager configManager;
    private GameManager gameManager;
    private DataBaseController database;
    private Logger logger;
    private CrossServerMessageSender crossServerMessageSender;

    public static final String CHANNEL_NAME = "hezhongsw:csm";

    public void start(HezhongSkywarsLoader plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        logger.info(ColorT.t("&aStarting &bHSW"));
        int pluginId = 33372;
        Metrics metrics = new Metrics(plugin, pluginId);

        configManager = new ConfigManager(plugin);
        configManager.loadConfig();
        if (ConfigValues.dataBaseConfig == null) {
            logger.severe(ColorT.t("&c&lCannot use Database. Plugin will be disabled......"));
            Bukkit.getPluginManager().disablePlugin(this.getPlugin());
            return;
        } else {
            database = new DataBaseController();
            database.connect();
        }
        logger.info(ColorT.t("Config OK"));
        gameManager = new GameManager(plugin);
        gameManager.init();

        // 监听器

        if (ListenerManager.gameListener == null)
            ListenerManager.gameListener = new GameListener();
        if (ListenerManager.joinQuitListener == null)
            ListenerManager.joinQuitListener = new JoinQuitListener();
        if (ListenerManager.setupListener == null)
            ListenerManager.setupListener = new SetupListener();
        if (ListenerManager.guiListener == null)
            ListenerManager.guiListener = new GUIListener();
        if (ListenerManager.generalListener == null)
            ListenerManager.generalListener = new GeneralListener();
        if (ListenerManager.independentWorldManager == null)
            ListenerManager.independentWorldManager = new IndependentWorldManager();
        if (ListenerManager.crossServerMessageListener == null)
            ListenerManager.crossServerMessageListener = new CrossServerMessageListener();
        if (ListenerManager.bungeeCordCrossServerMessageListener == null)
            ListenerManager.bungeeCordCrossServerMessageListener = new BungeeCordCrossServerMessageListener();

        plugin.getServer().getPluginManager().registerEvents(ListenerManager.gameListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(ListenerManager.joinQuitListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(ListenerManager.setupListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(ListenerManager.generalListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(ListenerManager.guiListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(ListenerManager.independentWorldManager, plugin);

        if (ConfigValues.bungeeEnabled) {
            if (ConfigValues.proxyType == ConfigValues.ProxyType.BC) {
                crossServerMessageSender = new BungeeCrossServerMessageSender();
                crossServerMessageSender.registerChannel(plugin);
                plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_NAME, ListenerManager.bungeeCordCrossServerMessageListener);
            }
        }

        // 命令
        plugin.getCommand("hsw").setExecutor(new CommandProcessor());

        // 任务
        new PlayerScoreBoardTask().runTaskTimer(plugin, 0, 10);
        new ServerDatabaseUpdateTask().runTaskTimerAsynchronously(plugin, 0, 60 * 20);
        if (ConfigValues.bungeeEnabled) {
            new CrossServerAutoReport().runTaskTimerAsynchronously(plugin,0, 10 * 20);
        }



        logger.info(ColorT.t("Listeners OK"));
        logger.info(ColorT.t("&b&lHSW &a&lStarted successfully!"));


    }

    public void stop() {
        logger.info("Stopping HSW......");
        if (database != null) { // 万一是因为没数据库关闭的呢？
            final Map<UUID, DatabaseStatsData> datas = new HashMap<>();
            for (Player pp : Bukkit.getOnlinePlayers()) {
                SwPlayer sp = SwPlayerManager.getPlayer(pp.getUniqueId());
                if (sp != null) {
                    final DatabaseStatsData statsData = sp.getStats();
                    if (statsData.REFRESHED) {
                        datas.put(pp.getUniqueId(), statsData);
                        logger.info("DB Will save data for " + pp.getUniqueId());
                    }
                }
            }
            // 批量保存节省时间
            CompletableFuture<Void> dbSaving = CompletableFuture.runAsync(() -> {
                logger.info("DB Async saving data......");
                database.setAllDatabaseStats(datas);

            });
            // 阻塞主线程等待保存
            try {
                dbSaving.get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.severe("DB Saving Timed out! Will lose datas!");
                dbSaving.cancel(true);
            } catch (Exception e) {
                logger.severe("DB Saving caused an exception!");
                e.printStackTrace();
            }
        }

        // 注销监听器
        HandlerList.unregisterAll(plugin);
        logger.info("HSW Stopped.");

    }

    public Location getLobbySpawnLocation() {
        World lobbyWorld = Bukkit.getWorld(ConfigValues.lobbyWorld);
        return lobbyWorld.getSpawnLocation();
    }
}
