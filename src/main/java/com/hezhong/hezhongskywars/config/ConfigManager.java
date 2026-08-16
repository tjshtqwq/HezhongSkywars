package com.hezhong.hezhongskywars.config;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.db.DataBaseType;
import com.hezhong.hezhongskywars.game.GameEvent;
import com.hezhong.hezhongskywars.utils.ColorT;
import com.hezhong.hezhongskywars.utils.MathUtil;
import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import com.hezhong.hezhongskywars.utils.type.CustomItem;
import com.hezhong.hezhongskywars.utils.type.Pair;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public class ConfigManager {
    private Plugin serverPlugin;
    // 共4个配置
    // config.yml 主配置
    // chests.yml 箱子配置
    // maps.yml 地图配置
    // ranks.yml 等级配置
    private File mainConfigFile;
    private File chestsConfigFile;
    private File mapsConfigFile;
    private File ranksConfigFile;
    private File kitsConfigFile;

    private YamlConfiguration mainConfig;
    private YamlConfiguration chestsConfig;
    private YamlConfiguration mapsConfig;
    private YamlConfiguration ranksConfig;
    private YamlConfiguration kitsConfig;
    public ConfigManager(Plugin serverPlugin) {
        this.serverPlugin = serverPlugin;
    }
    public void loadConfig() {
        // 从jar释放文件
        mainConfigFile = new File(serverPlugin.getDataFolder(), "config.yml");
        if (!mainConfigFile.exists()) {
            serverPlugin.saveResource("config.yml", false); // 不覆盖
        }
        chestsConfigFile = new File(serverPlugin.getDataFolder(), "chests.yml");
        if (!chestsConfigFile.exists()) {
            serverPlugin.saveResource("chests.yml", false);
        }
        mapsConfigFile = new File(serverPlugin.getDataFolder(), "maps.yml");
        if (!mapsConfigFile.exists()) {
            serverPlugin.saveResource("maps.yml", false);
        }
        ranksConfigFile = new File(serverPlugin.getDataFolder(), "ranks.yml");
        if (!ranksConfigFile.exists()) {
            serverPlugin.saveResource("ranks.yml", false);
        }
        kitsConfigFile = new File(serverPlugin.getDataFolder(), "kits.yml");
        if (!kitsConfigFile.exists()) {
            serverPlugin.saveResource("kits.yml", false);
        }

        // 载入到Yaml配置对象中
        mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
        chestsConfig = YamlConfiguration.loadConfiguration(chestsConfigFile);
        mapsConfig = YamlConfiguration.loadConfiguration(mapsConfigFile);
        ranksConfig = YamlConfiguration.loadConfiguration(ranksConfigFile);
        kitsConfig = YamlConfiguration.loadConfiguration(kitsConfigFile);

        resolveConfigValues();
    }

    public void reload() {
        mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
        chestsConfig = YamlConfiguration.loadConfiguration(chestsConfigFile);
        mapsConfig = YamlConfiguration.loadConfiguration(mapsConfigFile);
        ranksConfig = YamlConfiguration.loadConfiguration(ranksConfigFile);
        kitsConfig = YamlConfiguration.loadConfiguration(kitsConfigFile);

        resolveConfigValues();
    }

    private void resolveConfigValues() {
        try {
            // 清空老配置
            ConfigValues.levelNeedExps.clear();
            ConfigValues.chestConfigs.clear();
            ConfigValues.mapConfigs.clear();
            ConfigValues.kitConfigs.clear();
            // 负责真正读取配置信息
            ConfigValues.serverIp = ColorT.t(mainConfig.getString("basicInfo.serverIp"));
            ConfigValues.serverName = ColorT.t(mainConfig.getString("basicInfo.serverName"));
            ConfigValues.lobbyWorld = mainConfig.getString("basicInfo.lobbyWorld");
            ConfigValues.defaultGameMode = GameMode.valueOf(mainConfig.getString("basicInfo.defaultGameMode").toUpperCase());

            // 跨服
            ConfigValues.bungeeEnabled = mainConfig.getBoolean("bungee.bungee");
            ConfigValues.proxyType = ConfigValues.ProxyType.valueOf(mainConfig.getString("bungee.proxyType").toUpperCase());
            ConfigValues.serverType = ServerInfoMessage.ServerType.valueOf(mainConfig.getString("bungee.serverType").toUpperCase());
            ConfigValues.BCserverName = mainConfig.getString("bungee.serverName");

            ConfigValues.coinsWinAdd = mainConfig.getInt("coins.winAdd");
            ConfigValues.coinsKillAdd = mainConfig.getInt("coins.killAdd");
            ConfigValues.expKillAdd = mainConfig.getInt("exp.killAdd");
            ConfigValues.expWinAdd = mainConfig.getInt("exp.winAdd");
            List<Integer> levelNeedExps = new ArrayList<>();
            // 获取List
            levelNeedExps = mainConfig.getIntegerList("levels.needExps");
            ConfigValues.levelNeedExps = levelNeedExps;

            ConfigValues.multiWorldIndependentChat = mainConfig.getBoolean("multiWorld.independentChat");
            ConfigValues.multiWorldIndependentTab =  mainConfig.getBoolean("multiWorld.independentTab");



            // 数据库配置
            // database下
            try {
                String dbTypeStr = mainConfig.getString("database.type");

                DataBaseType dbType = null;
                dbType = DataBaseType.valueOf(dbTypeStr.toUpperCase());

                // SQLite 配置
                String sqliteFile = mainConfig.getString("database.sqlite.file");
                String sqliteTablePrefix = mainConfig.getString("database.sqlite.table-prefix");

                // MySQL 配置
                String mysqlHost = mainConfig.getString("database.mysql.host");
                int mysqlPort = mainConfig.getInt("database.mysql.port");
                String mysqlUser = mainConfig.getString("database.mysql.user");
                String mysqlPassword = mainConfig.getString("database.mysql.password");
                String mysqlDatabase = mainConfig.getString("database.mysql.database");
                String mysqlTablePrefix = mainConfig.getString("database.mysql.table-prefix");
                int mysqlReconnectTimeout = mainConfig.getInt("database.mysql.reconnect-timeout");

                ConfigValues.dataBaseConfig = new DataBaseConfig(
                        dbType,
                        sqliteFile, sqliteTablePrefix,
                        mysqlHost, mysqlPort, mysqlUser,
                        mysqlPassword, mysqlDatabase,
                        mysqlTablePrefix, mysqlReconnectTimeout
                );
            } catch (Exception e) {
                HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to load database config.");
                e.printStackTrace();
            }

            // 箱子读取
            // 单独的try catch
            try {
                ConfigurationSection chestsCS = chestsConfig.getConfigurationSection(""); // 根目录
                if (chestsCS != null) {
                    Set<String> chestTypes = chestsCS.getKeys(false);
                    for (String type : chestTypes) {
                        // 权重 : 物品 : 数量 : [附魔，用;分开] : [附魔等级，用;分开，一一对应] : [耐久/药水数据，用;分开] : [药水等级，用;分开，一一对应] : [药水时长，用;分开，一一对应]
                        // 共8项，分别对应rawItem的0~7
                        int minFilled = chestsConfig.getInt(type + ".minFilled");
                        int maxFilled = chestsConfig.getInt(type + ".maxFilled");
                        List<String> chestItems = chestsConfig.getStringList(type + ".items");
                        Map<CustomItem, Integer> finalItems = new HashMap<>();
                        for (String chestItem : chestItems) {
                            chestItem = chestItem.replaceAll("\\s", "");
                            String[] rawItem = chestItem.split(":");
                            if (rawItem.length < 3) {
                                HezhongSkywars.INSTANCE.getLogger().warning("Found a chest config with invalid item config, raw = " + chestItem);
                                continue;
                            }
                            int weight = Integer.parseInt(rawItem[0]);

                            // 把权重丢掉
                            String itemStr = String.join(":", Arrays.copyOfRange(rawItem, 1, rawItem.length));
                            // 解析物品
                            CustomItem item = CustomItem.parseItem(itemStr);
                            if (item == null) {
                                HezhongSkywars.INSTANCE.getLogger().warning("Found a chest config with invalid item config, raw = " + chestItem);
                                continue;
                            }
                            finalItems.put(item, weight);
                        }
                        // 物品解析完了，存进去
                        ChestConfig finalChestConfig = new ChestConfig(type, minFilled, maxFilled, finalItems);
                        ConfigValues.chestConfigs.put(type, finalChestConfig);

                    }
                }
            } catch (Exception e) {
                HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to load chests config.");
                e.printStackTrace();
            }
            // 读取地图配置
            try {
                ConfigurationSection mapsCS = mapsConfig.getConfigurationSection("");
                if (mapsCS != null) {
                    Set<String> mapNames = mapsCS.getKeys(false);
                    for (String mapName : mapNames) {
                        // mapName是游戏内地图名，不是世界名！
                        String originalWorldName = mapsConfig.getString(mapName + ".original_world");
                        String copyWorldName = mapsConfig.getString(mapName + ".copy_world");
                        int minPlayersToAutostart = mapsConfig.getInt(mapName + ".min_players_to_autostart");
                        int maxPlayers = mapsConfig.getInt(mapName + ".max_players");
                        int countdown  = mapsConfig.getInt(mapName + ".countdown");
                        boolean ok = mapsConfig.getBoolean(mapName + ".ok");
                        // 出生点选段
                        ConfigurationSection mapSpawnsCS = mapsConfig.getConfigurationSection(mapName + ".spawns");
                        Set<String> spawnsIds = mapSpawnsCS.getKeys(false);

                        List<Vector> spawns = new ArrayList<>();
                        Map<Vector, String> chests = new HashMap<>();
                        for (String spawnId : spawnsIds) {
                            double spawnX = mapsConfig.getDouble(mapName + ".spawns." + spawnId + ".x");
                            double spawnY = mapsConfig.getDouble(mapName + ".spawns." + spawnId + ".y");
                            double spawnZ = mapsConfig.getDouble(mapName + ".spawns." + spawnId + ".z");
                            spawns.add(new Vector(spawnX, spawnY, spawnZ));
                        }
                        // 箱子位置选段
                        ConfigurationSection chestLocationsCS = mapsConfig.getConfigurationSection(mapName + ".chests");
                        Set<String> chestIds = chestLocationsCS.getKeys(false);
                        for (String chestId : chestIds) {
                            double chestX = mapsConfig.getDouble(mapName + ".chests." + chestId + ".x");
                            double chestY = mapsConfig.getDouble(mapName + ".chests." + chestId + ".y");
                            double chestZ = mapsConfig.getDouble(mapName + ".chests." + chestId + ".z");
                            String type = mapsConfig.getString(mapName + ".chests." + chestId + ".type");
                            chests.put(new Vector(chestX, chestY, chestZ), type);
                        }
                        // 事件选段
                        List<GameEvent> gameEvents = new ArrayList<>();
                        ConfigurationSection eventsCS = mapsConfig.getConfigurationSection(mapName + ".events");
                        Set<String> eventIds = eventsCS.getKeys(false);
                        for (String eventId : eventIds) {
                            int time = mapsConfig.getInt(mapName + ".events." + eventId + ".time");
                            String typeStr = mapsConfig.getString(mapName + ".events." + eventId + ".type");
                            GameEvent.EventType type = GameEvent.EventType.valueOf(typeStr.toUpperCase());
                            if (type == null) continue;
                            gameEvents.add(new GameEvent(time, type));

                        }
                        MapConfig finalMapConfig = new MapConfig(originalWorldName, copyWorldName, minPlayersToAutostart, maxPlayers, countdown, ok, spawns, chests, gameEvents);
                        ConfigValues.mapConfigs.put(mapName, finalMapConfig);
                    }
                }
            } catch (Exception e) {
                HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to load maps config.");
                e.printStackTrace();
            }
            // 读取职业配置
            try {
                ConfigurationSection kitsCS = kitsConfig.getConfigurationSection("");
                if (kitsCS != null) {
                    Set<String> names  = kitsCS.getKeys(false);
                    for (String kitName : names) {
                        int coins = kitsConfig.getInt(kitName + ".coins");
                        String permission = kitsConfig.getString(kitName + ".permission");
                        String material = kitsConfig.getString(kitName + ".material");
                        List<CustomItem> items = new ArrayList<>();
                        for (String itemStr : kitsConfig.getStringList(kitName + ".items")) {
                            CustomItem item = CustomItem.parseItem(itemStr);
                            if (item != null) items.add(item);
                        }
                        KitConfig kitConfig = new KitConfig(coins, permission, material, items);
                        ConfigValues.kitConfigs.put(kitName, kitConfig);
                    }
                }
            } catch (Exception e) {
                HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to load kits config.");
                e.printStackTrace();
            }
        } catch (Exception e) {
            HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to load config file.");
            e.printStackTrace();
        }
    }

    public void createMap(String mapName, String originalMap, String copyMap) {
        // 是否已存在？
        try {
            if (mapsConfig.contains(mapName)) {
                return;
            }
            mapsConfig.createSection(mapName);
            mapsConfig.set(mapName + ".original_world", originalMap);
            mapsConfig.set(mapName + ".copy_world", copyMap);
            mapsConfig.set(mapName + ".min_players_to_autostart", 2);
            mapsConfig.set(mapName + ".max_players", 4);
            mapsConfig.set(mapName + ".countdown", 30);
            mapsConfig.set(mapName + ".ok", false);
            mapsConfig.createSection(mapName + ".spawns");
            mapsConfig.createSection(mapName + ".chests");
            mapsConfig.createSection(mapName + ".events");
            mapsConfig.save(mapsConfigFile);

            HezhongSkywars.INSTANCE.getLogger().info("HSW Created Map " + mapName + ".");
            reload();
        } catch (Exception e) {
            HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to create map config.");
            e.printStackTrace();
        }
    }

    public void setUpMap(String mapName, List<Vector> spawns, Map<Vector, String> chests) {
        // 检查存在
        try {
            if (mapsConfig.contains(mapName)) {
                // spawns，chests都是完整的，所以清空这些选段
                mapsConfig.set(mapName + ".spawns", null);
                mapsConfig.set(mapName + ".chests", null);
                ConfigurationSection spawnsCS = mapsConfig.createSection(mapName + ".spawns");
                ConfigurationSection chestsCS = mapsConfig.createSection(mapName + ".chests");
                for (int i = 1; i <= spawns.size(); i++) { // 出生点
                    // 使用set会自动创建选段，我不需要额外搞
                    mapsConfig.set(mapName + ".spawns." + i + ".x", spawns.get(i - 1).getX());
                    mapsConfig.set(mapName + ".spawns." + i + ".y", spawns.get(i - 1).getY());
                    mapsConfig.set(mapName + ".spawns." + i + ".z", spawns.get(i - 1).getZ());
                }
                int i = 1;
                for (Map.Entry<Vector, String> entry : chests.entrySet()) { // 箱子
                    mapsConfig.set(mapName + ".chests." + i + ".x", entry.getKey().getX());
                    mapsConfig.set(mapName + ".chests." + i + ".y", entry.getKey().getY());
                    mapsConfig.set(mapName + ".chests." + i + ".z", entry.getKey().getZ());
                    mapsConfig.set(mapName + ".chests." + i + ".type", entry.getValue());
                    i++;
                }
                // 保存配置到本地
                mapsConfig.save(mapsConfigFile);

                HezhongSkywars.INSTANCE.getLogger().info("HSW Modified Map " + mapName + ".");
                reload();
            }
        } catch (Exception e) {
            HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to modify map config.");
            e.printStackTrace();
        }

    }
}
