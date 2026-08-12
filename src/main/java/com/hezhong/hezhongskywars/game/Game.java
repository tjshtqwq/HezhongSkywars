package com.hezhong.hezhongskywars.game;

import com.connorlinfoot.titleapi.TitleAPI;
import com.cryptomorin.xseries.XSound;
import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.config.KitConfig;
import com.hezhong.hezhongskywars.events.HSWGameStartEvent;
import com.hezhong.hezhongskywars.events.HSWGameStatusChangeEvent;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import com.hezhong.hezhongskywars.utils.SimpleMath;
import com.hezhong.hezhongskywars.utils.SpecialItems;
import com.hezhong.hezhongskywars.utils.bukkit.CTask;
import com.hezhong.hezhongskywars.utils.type.CustomItem;
import com.hezhong.hezhongskywars.utils.type.Pair;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class Game {
    // 完整的游戏单例

    private final String mapName;
    private final World world;
    private final int maxPlayers;
    private final int playersToAutostart;
    private GameStatus gameStatus;
    private List<Player> allPlayers;
    private Map<UUID, SwPlayingGamePlayer> playingPlayerStatus;
    private int runnedTime = 0;
    private final Map<Location, Chest> chests;
    private final List<Pair<Location, Player>> spawns; // y 占用出生点的玩家，用于分配
    private final List<GameEvent> events;
    private final List<GameEvent> eventsInFuture; // 还没执行的事件，[0]即下一个事件，用于计分板
    private String winnerName = "";
    // 游戏需要的Tasks
    private GameTaskManager taskManager;
    private final int countdown;
    private int countdownRemaining;


    public Game(String mapName, World world, Map<Location, Chest> chests, List<Location> spawns, List<GameEvent> events) {
        this.mapName = mapName;
        this.world = world;
        setGameStatus(GameStatus.WAITING);
        allPlayers = new ArrayList<>();
        playingPlayerStatus = new HashMap<>();
        this.chests = chests;
        this.spawns = new ArrayList<>();
        this.events = events;
        this.eventsInFuture = new ArrayList<>(events);
        this.taskManager = new GameTaskManager(this);
        for (Location location : spawns) {
            Location newLocation = location.clone();
            newLocation.setX(location.getBlockX() + 0.5);
            newLocation.setY(location.getBlockY());
            newLocation.setZ(location.getBlockZ() + 0.5);
            this.spawns.add(new Pair<>(newLocation, null));
        }
        // maxPlayers
        maxPlayers = Math.min(spawns.size(), ConfigValues.mapConfigs.get(mapName).getMaxPlayers());
        playersToAutostart = ConfigValues.mapConfigs.get(mapName).getMinPlayersToAutostart();
        countdown = ConfigValues.mapConfigs.get(mapName).getCountdown();

        // 对events，按照时间排序
        // 从小到大
        Collections.sort(events, Comparator.comparingInt(GameEvent::getTime));

        taskManager.setCheckIfNoPlayer(new CTask(HezhongSkywars.INSTANCE.getPlugin(), () -> {
            if (gameStatus == GameStatus.PLAYING && getAlivePlayers().isEmpty()) {
                normalEnd();
            }
        }));
       taskManager.getCheckIfNoPlayer().runTimer(100, 0);
    }

    public boolean addPlayer(Player player, boolean spectate) {
        // 必须设置SwPlayer状态。不能重复调用。
        if ((gameStatus == GameStatus.WAITING || gameStatus == GameStatus.STARTING) && !spectate) {
            if (getAlivePlayers().size() >= maxPlayers) {
                return false;
            }
            allPlayers.add(player);
            restoreHide(player);
            playingPlayerStatus.put(player.getUniqueId(), new SwPlayingGamePlayer(player, false));
            // 将玩家传送到出生点
            Location selectedLocation = selectSpawnPoint(player); // 选择出生点
            assert selectedLocation != null : "?";
            createCage(selectedLocation); // 出生点笼子
            player.teleport(selectedLocation);
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 1));
            player.setGameMode(GameMode.SURVIVAL);
            ItemStack kitSelector = SpecialItems.kitSelector();
            player.getInventory().addItem(kitSelector);
            ItemStack hubTeleporter = SpecialItems.lobbyTeleporter();
            player.getInventory().setItem(8, hubTeleporter);

            if (getAlivePlayers().size() >= playersToAutostart) {
                // 准备开始
                countdownAndAutoStart();
            }
        } else if (gameStatus == GameStatus.PLAYING || gameStatus == GameStatus.STOPPED || gameStatus == GameStatus.WAITING || gameStatus == GameStatus.STARTING) {
            // 中途进入，直接旁观
            if (!playingPlayerStatus.containsKey(player.getUniqueId())) {
                playingPlayerStatus.put(player.getUniqueId(), new SwPlayingGamePlayer(player, true));
            }
            allPlayers.add(player);
            toSpectate(player);
            player.teleport(world.getSpawnLocation());
        } else return false;
        return true;
    }

    public void startGame() {
        // 接收到了StartEvent，开始游戏
        countdownRemaining = 0;
        if (taskManager.getCountdownTask() != null) taskManager.getCountdownTask().cancel();
        refreshAllChests();
        for (Pair<Location, Player> spawn : spawns) {
            Location spawnLocation = spawn.getX();
            removeCage(spawnLocation);
        }
        for (Player player : allPlayers) {
            player.getInventory().clear();
            TitleAPI.sendTitle(player, 0, 60, 20, ColorT.t("&c&l战斗！"));

            SwPlayingGamePlayer swpgp = playingPlayerStatus.get(player.getUniqueId());
            // 把职业物品给玩家
            if (swpgp.getStatus() == SwPlayingGamePlayer.PlayerStatus.ALIVE) { // 保险处理
                if (!Objects.equals(swpgp.getSelectedKit(), "None")) {
                    KitConfig kit = ConfigValues.kitConfigs.get(swpgp.getSelectedKit());
                    if (kit != null) {
                        for (CustomItem customItem : kit.getItems()) {
                            player.getInventory().addItem(customItem.toItem());
                        }
                    }
                }
            }

            SwPlayer sp = SwPlayerManager.getPlayer(player);
            sp.getStats().gamesPlayed++;
        }
        setGameStatus(GameStatus.PLAYING);
        sendMessage("&c&l战斗！");
        gameEventRunner();
    }

    private void createCage(Location spawnPoint) {
        // 大粪，能跑就行，大不了再写一个
        World world = spawnPoint.getWorld();
        int baseX = spawnPoint.getBlockX();
        int baseY = spawnPoint.getBlockY() - 1;
        int baseZ = spawnPoint.getBlockZ();

        int height = 3;

        for (int y = 0; y <= height + 1; y++) {
            int currentY = baseY + y;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    boolean isEdge = (Math.abs(x) == 1 || Math.abs(z) == 1);
                    boolean isTopOrBottom = (y == 0 || y == height + 1);

                    if (isEdge || isTopOrBottom) {
                        Location blockLoc = new Location(world, baseX + x, currentY, baseZ + z);
                        blockLoc.getBlock().setType(Material.GLASS);
                    }
                }
            }
        }
    }

    private void removeCage(Location spawnPoint) {
        World world = spawnPoint.getWorld();
        int baseX = spawnPoint.getBlockX();
        int baseY = spawnPoint.getBlockY() - 1;
        int baseZ = spawnPoint.getBlockZ();

        int height = 3;

        for (int y = 0; y <= height + 1; y++) {
            int currentY = baseY + y;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    boolean isEdge = (Math.abs(x) == 1 || Math.abs(z) == 1);
                    boolean isTopOrBottom = (y == 0 || y == height + 1);

                    if (isEdge || isTopOrBottom) {
                        Location blockLoc = new Location(world, baseX + x, currentY, baseZ + z);
                        if (blockLoc.getBlock().getType() == Material.GLASS) {
                            blockLoc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    public String processDeath(Player killed, Player killer, boolean quit) {
        if (killed == null) {
            HezhongSkywars.INSTANCE.getLogger().warning("ProcessDeath: Killed is null?");
            return "";
        }
        // quit说明不是被杀的，是自己退的。ChangeWorld和QuitEvent都算
        // 因为QuitEvent会清除SwPlayer，因此我们把QuitEvent的处理和SwPlayer的销毁放在一起，注意先后顺序
        Location location = killed.getLocation();
        if (location.getY() <= 5) {
            location = world.getSpawnLocation();
        }
        SwPlayer sp = SwPlayerManager.getPlayer(killed);
        SwPlayer killerSp = SwPlayerManager.getPlayer(killer);
        SwPlayingGamePlayer swpgpKilled = playingPlayerStatus.get(killed.getUniqueId());
        assert sp != null : "?"; // 不会吧？
        sp.setNextSpawnLocation(location);
        String deathMessage = ColorT.t("&7" + killed.getName() + " &e死了。");
        if (gameStatus == GameStatus.PLAYING) { // 此时游戏还在进行
            killed.getInventory().clear();
            if (killer != null && !quit) { // 被杀了
                if (swpgpKilled.getStatus() == SwPlayingGamePlayer.PlayerStatus.ALIVE) {
                    SwPlayingGamePlayer swpgpKiller = playingPlayerStatus.get(killer.getUniqueId());
                    swpgpKiller.setKills(swpgpKiller.getKills() + 1); // 游戏内状态

                    killerSp.addKills(); // 统计状态
                    notifyKill(killer, killed, ConfigValues.coinsKillAdd, ConfigValues.expKillAdd); // 击杀提示

                    swpgpKilled.setStatus(SwPlayingGamePlayer.PlayerStatus.DEAD);
                    sp.getStats().deaths++;
                    killed.getInventory().clear();
                    killed.spigot().respawn();
                    killed.teleport(location);
                }
                toSpectate(killed);
                if (getAlivePlayers().size() <= 1) {
                    normalEnd();
                }
                deathMessage = ColorT.t("&7" + killed.getName() + " &e被 &7" + killer.getName() + " &e杀死了！");
            } else if (killer == null && !quit) {
                if (swpgpKilled.getStatus() == SwPlayingGamePlayer.PlayerStatus.ALIVE) {
                    swpgpKilled.setStatus(SwPlayingGamePlayer.PlayerStatus.DEAD);
                    sp.getStats().deaths++;
                    // 此处预留，需要持久化保存统计数据
                    killed.spigot().respawn();
                    killed.teleport(location);
                }
                toSpectate(killed);
                // 直接默认返回值（亡语）
            } else if (quit) {
                // Quit需要restoreHide+setAllowFlight
                restoreHide(killed);
                killed.setAllowFlight(false);
                allPlayers.remove(killed);
                if (swpgpKilled.getStatus() == SwPlayingGamePlayer.PlayerStatus.ALIVE) {
                    sp.getStats().deaths++;
                    sendMessage("&7" + killed.getName() + " &e退出了。");
                }
                swpgpKilled.setStatus(SwPlayingGamePlayer.PlayerStatus.QUIT);
                sp.setPlayingGame(null);
                if (getAlivePlayers().size() <= 1) {
                    normalEnd();
                }
                deathMessage = "";
            }

            processAssist:
            {
                for (Map.Entry<UUID, Double> entry : swpgpKilled.getDamageByAttack().entrySet()) {
                    if (killer == null || entry.getKey() != killer.getUniqueId()) { // 不算击杀者（击杀者已计算过了）。
                        // 当然可能没击杀者，需要排除null

                        SwPlayer eSp = SwPlayerManager.getPlayer(entry.getKey());
                        SwPlayingGamePlayer eSwpgp = playingPlayerStatus.get(entry.getKey());
                        if (eSp != null && eSwpgp != null && eSp.getPlayingGame() == this && swpgpKilled.totalDamage != 0) {
                            // 玩家还在这局游戏里
                            double pct = entry.getValue() / swpgpKilled.totalDamage;
                            int coinsAdd = SimpleMath.floor(ConfigValues.coinsKillAdd * pct);
                            int expAdd = SimpleMath.floor(ConfigValues.expKillAdd * pct);

                            eSp.addAssists(coinsAdd, expAdd);
                            notifyAssist(eSp.getPlayer(), killed, coinsAdd, expAdd, entry.getValue(), swpgpKilled.totalDamage); // 提示助攻
                        }
                    }
                }
            }


        } else if (gameStatus == GameStatus.WAITING || gameStatus == GameStatus.STARTING || gameStatus == GameStatus.STOPPED || gameStatus == GameStatus.RESETTING) {
            if (!quit) {
                killed.spigot().respawn();
                killed.teleport(location);
            } else {
                // Quit.
                // Quit需要restoreHide+setAllowFlight
                allPlayers.remove(killed);
                playingPlayerStatus.remove(killed.getUniqueId());
                restoreHide(killed);
                killed.setAllowFlight(false);
                // 解除出生点的占用
                Pair<Location, Player> removingSpawn = null;
                for (Pair<Location, Player> spawn : spawns) {
                    if (spawn.getY() != null && spawn.getY().getUniqueId() == killed.getUniqueId()) {
                        removingSpawn = spawn;
                        spawn.setY(null);
                        break;
                    }
                }
                HezhongSkywars.INSTANCE.getLogger().info("Removing Spawn Point:" + removingSpawn);
                sp.setPlayingGame(null);
            }
        }
        if (getAlivePlayers().size() <= 1) {
            normalEnd();
        }
        return deathMessage;
    }

    private void normalEnd() {
        if (gameStatus != GameStatus.PLAYING) return;
        setGameStatus(GameStatus.STOPPED);
        if (getAlivePlayers().isEmpty()) {
            HezhongSkywars.INSTANCE.getLogger().warning("Will Force reset game " + mapName + " after 20s, because of no player in game!");
            Bukkit.getScheduler().runTaskLater(HezhongSkywars.INSTANCE.getPlugin(), () -> {
                taskManager.stopAll();

                for (Player player : getPlayersInWorld()) {
                    SwPlayer sp = SwPlayerManager.getPlayer(player);
                    sp.setPlayingGame(null);
                    restoreHide(player);
                    player.setAllowFlight(false);
                    player.getInventory().clear();
                    player.getInventory().setArmorContents(null);
                    player.teleport(Bukkit.getWorld(ConfigValues.lobbyWorld).getSpawnLocation());
                }

                resetGame();
            }, 20L * 20);
            return;
        }
        // 判断活人数量
        if (getAlivePlayers().size() > 1) {
            winnerName = "多个";
        } else {
            winnerName = getAlivePlayers().get(0).getName();
        }
        List<Pair<Integer, Player>> mostKilled = new ArrayList<>();
        // 按击杀数排序
        for (Player player : allPlayers) {
            SwPlayingGamePlayer swpgp = playingPlayerStatus.get(player.getUniqueId());
            if (!swpgp.isAsSpectator()) {
                mostKilled.add(new Pair<>(swpgp.getKills(), player));
            }
        }
        // 从大到小排
        Collections.sort(mostKilled, Comparator.comparingInt(Pair::getX));
        Collections.reverse(mostKilled);
        // 展示击杀Top3
        int i = 1;

        sendMessage("&e&l胜者 &f" + winnerName);
        sendMessage("&7====================&a&l统计&7====================");

        // 展示数据前3的人
        for (Pair<Integer, Player> pair : mostKilled) {
            if (i > 3) break;
            sendMessage(String.format("&cTop %s &7%s &a击杀 %s", i, pair.getY().getName(), pair.getX()));
            i++;
        }

        // 此处处理胜利者
        for (Player winner : getAlivePlayers()) {
           winner.getInventory().setItem(8, SpecialItems.lobbyTeleporter()); // 给返回大厅的东西
            SwPlayer sp = SwPlayerManager.getPlayer(winner);
            TitleAPI.sendTitle(winner, 0, 90, 10, ColorT.t("&e&lVICTORY"));
            sp.addWins(); // 统计输赢
            // 非赢即输
        }

        // 烟花声庆祝
        // 其实可以生成烟花，但这个先不搞，画个大饼先
        // TODO: 生成烟花
        // TODO: 把这个Task也统一管理了
        BukkitRunnable fireworkTask = new BukkitRunnable() {
            private int count = 0;

            @Override
            public void run() {
                playSound(XSound.ENTITY_FIREWORK_ROCKET_SHOOT);
                if (++count >= 20 || getAlivePlayers().isEmpty()) {
                    this.cancel();
                }
            }
        };
        fireworkTask.runTaskTimer(HezhongSkywars.INSTANCE.getPlugin(), 0, 20);

        Bukkit.getScheduler().runTaskLater(HezhongSkywars.INSTANCE.getPlugin(), () -> {
            taskManager.stopAll();

            for (Player player : getPlayersInWorld()) {
                SwPlayer sp = SwPlayerManager.getPlayer(player);
                sp.setPlayingGame(null);
                restoreHide(player);
                player.setAllowFlight(false);
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.teleport(Bukkit.getWorld(ConfigValues.lobbyWorld).getSpawnLocation());
            }

            resetGame();
        }, 20 * 20);
    }

    private void refreshAllChests() {
        // 批量刷新全图箱子
        for (Map.Entry<Location, Chest> entry : chests.entrySet()) {
            Location loc = entry.getKey();
            refreshChest(loc);
        }
    }

    private void refreshChest(Location location) {
        Chest gameChest = chests.get(location);
        if (gameChest == null) {
            HezhongSkywars.INSTANCE.getLogger().warning("HSW Game Chest is null!");
            throw new RuntimeException("[HSW] Cannot get Game Chest");
        }
        // 随机生成一些箱子物品
        List<CustomItem> generated = gameChest.randomGenerateItems();
        List<ItemStack> items = new ArrayList<>();
        // 构造并转成ItemStack
        for (CustomItem item : generated) items.add(item.toItem());

        Block block = location.getBlock();
        if (!(block.getState() instanceof org.bukkit.block.Chest)) return;
        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) block.getState();

        Inventory inv = chest.getInventory();
        int size = inv.getSize();

        // 打乱，然后塞进箱子
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) slots.add(i);
        Collections.shuffle(slots);

        inv.clear();
        for (int i = 0; i < items.size(); i++) {
            inv.setItem(slots.get(i), items.get(i));
        }
    }

    private Location selectSpawnPoint(Player pp) {
        for (Pair<Location, Player> pair : spawns) {
            if (pair.getY() == null) {
                pair.setY(pp);
                return pair.getX();
            }
        }
        return null;
    }

    private void countdownAndAutoStart() {
        if (taskManager.getCountdownTask() == null || taskManager.getCountdownTask().isCancelled()) {
            AtomicInteger time = new AtomicInteger(countdown);
            setGameStatus(GameStatus.STARTING);
            taskManager.setCountdownTask(new CTask(HezhongSkywars.INSTANCE.getPlugin(), () -> {
                if (getAlivePlayers().size() < ConfigValues.mapConfigs.get(mapName).getMinPlayersToAutostart()) {
                    for (Player player : allPlayers) {
                        TitleAPI.sendTitle(player, 0, 60, 20, ColorT.t("&e倒计时取消"), ColorT.t("&c人数不足！至少需要 " + ConfigValues.mapConfigs.get(mapName).getMinPlayersToAutostart() + " 人"));
                        player.sendMessage(ColorT.t("&e倒计时取消 &c人数不足！至少需要 " + ConfigValues.mapConfigs.get(mapName).getMinPlayersToAutostart() + " 人"));
                    }
                    taskManager.getCountdownTask().cancel();
                    setGameStatus(GameStatus.WAITING);
                    return;
                }
                if (time.get() > 20 && time.get() % 10 == 0) {
                    for (Player player : allPlayers) {
                        TitleAPI.sendTitle(player, 0, 60, 20, ColorT.t("&a" + time));
                        player.sendMessage(ColorT.t("&a倒计时 " + time));
                    }
                } else if (time.get() <= 20 && time.get() > 5 && time.get() % 5 == 0) {
                    for (Player player : allPlayers) {
                        TitleAPI.sendTitle(player, 0, 60, 20, ColorT.t("&e" + time));
                        player.sendMessage(ColorT.t("&e倒计时 " + time));
                    }
                } else if (time.get() <= 5 && time.get() > 0) {
                    for (Player player : allPlayers) {
                        TitleAPI.sendTitle(player, 0, 60, 20, ColorT.t("&c" + time), ColorT.t("&e准备战斗！"));
                        player.sendMessage(ColorT.t("&c倒计时 " + time));
                    }
                }
                if (time.get() == 0) {
                    Bukkit.getPluginManager().callEvent(new HSWGameStartEvent(mapName));
                    taskManager.getCountdownTask().cancel();
                }
                countdownRemaining = time.get();
                time.getAndDecrement();
            }));
            taskManager.getCountdownTask().runTimer(20, 0);
        }
    }

    private void gameEventRunner() {
       taskManager.setEventRunnerTask(new CTask(HezhongSkywars.INSTANCE.getPlugin(), () -> {
            runnedTime++;
            for (GameEvent e : events) {
                if (e.getTime() == runnedTime) {
                    if (e.getType().equals(GameEvent.EventType.RESET_CHEST)) {
                        refreshAllChests();
                        playSound(XSound.BLOCK_CHEST_OPEN);
                    }
                    if (e.getType().equals(GameEvent.EventType.STOP_GAME)) {
                        normalEnd();
                    }
                    eventsInFuture.remove(e);
                }
            }
            for (Player p : allPlayers) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 1));
            }
        }));
        taskManager.getEventRunnerTask().runTimer(20, 0);
    }

    private void toSpectate(Player pp) {
        SwPlayingGamePlayer swpgp = playingPlayerStatus.get(pp.getUniqueId());
        swpgp.setStatus(SwPlayingGamePlayer.PlayerStatus.SPECTATE);
        pp.setGameMode(GameMode.ADVENTURE);
        pp.getInventory().clear();
        pp.getInventory().setArmorContents(null);
        pp.getInventory().setItem(8, SpecialItems.lobbyTeleporter());
        pp.setAllowFlight(true);
        pp.setFlying(true);
        for (Player aP : getAlivePlayers()) {
            SwPlayer aPsP = SwPlayerManager.getPlayer(aP);
            aPsP.hidePlayer(pp);
        }
        TitleAPI.sendTitle(pp, 0, 40, 20, ColorT.t("&a你是旁观者"));
    }

    private void playSound(XSound sound) {
        for (Player player : getPlayersInWorld()) {
            sound.play(player);
        }
    }

    private void sendMessage(String message) {
        for (Player player : getPlayersInWorld()) {
            player.sendMessage(ColorT.t(message));
        }
    }

    private List<Player> getPlayersInWorld() {
        return world.getPlayers();
    }


    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (SwPlayingGamePlayer swpgp : playingPlayerStatus.values()) {
            if (swpgp.getStatus() == SwPlayingGamePlayer.PlayerStatus.ALIVE) {
                alive.add(swpgp.getPlayer());
            }
        }
        return alive;
    }
    private void restoreHide(Player p) {
        SwPlayer sp = SwPlayerManager.getPlayer(p);
        for (Player player : allPlayers) {
            if (player != p) {
                sp.showPlayer(player);
                SwPlayer sp2 = SwPlayerManager.getPlayer(player);
                sp2.showPlayer(sp.getPlayer());
            }
        }
    }

    public List<Player> getSpectators() {
        List<Player> specs = new ArrayList<>();
        for (SwPlayingGamePlayer swpgp : playingPlayerStatus.values()) {
            if (swpgp.getStatus() == SwPlayingGamePlayer.PlayerStatus.SPECTATE) {
                specs.add(swpgp.getPlayer());
            }
        }
        return specs;
    }

    public SwPlayingGamePlayer getPlayingPlayer(UUID uuid) { // 暴露给外
        return  playingPlayerStatus.get(uuid);
    }
    public void setPlayerKit(Player player, String kitName) {
        UUID uuid = player.getUniqueId();
        SwPlayingGamePlayer swpgp = playingPlayerStatus.get(uuid);
        swpgp.setSelectedKit(kitName);
    }
    public String getPlayerKit(Player player) {
        UUID uuid = player.getUniqueId();
        SwPlayingGamePlayer swpgp = playingPlayerStatus.get(uuid);
        return swpgp.getSelectedKit();
    }
    public void notifyKill(Player p, Player killed, int addCoins, int addExps) {
        p.sendMessage(ColorT.t("&c击杀 &e" + killed.getName() + ""));
        p.sendMessage(ColorT.t("&a+&e" + addCoins + " &a硬币 |&e" + addExps + " &a经验！"));
    }
    public void notifyAssist(Player p, Player killed, int addCoins, int addExps, double damage, double totalDamage) {
        p.sendMessage(ColorT.t("&c助攻 &e" + killed.getName() + " &7[&e" + damage + " &7/&e " + totalDamage + " &7伤害]"));
        p.sendMessage(ColorT.t("&a+&e" + addCoins + " &a硬币 |&e" + addExps + " &a经验！"));
    }
    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
        HSWGameStatusChangeEvent changeEvent = new HSWGameStatusChangeEvent(this, gameStatus);
        Bukkit.getPluginManager().callEvent(changeEvent);
    }
    public void resetGame() {
        setGameStatus(GameStatus.RESETTING);
        Bukkit.getScheduler().runTaskAsynchronously(HezhongSkywars.INSTANCE.getPlugin(), () -> {
            try {
                HezhongSkywars.INSTANCE.getGameManager().resetGame(mapName);
            } catch (Exception e) {
                HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to reset game");
                e.printStackTrace();
            }
        });
    }
}
