package com.hezhong.hezhongskywars.manager;

import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.config.ChestConfig;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.config.MapConfig;
import com.hezhong.hezhongskywars.game.Chest;
import com.hezhong.hezhongskywars.game.Game;
import com.hezhong.hezhongskywars.game.queue.QueueManager;
import com.hezhong.hezhongskywars.utils.type.CustomItem;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class GameManager {
    private final Plugin serverPlugin;
    @Getter
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    @Getter
    private final QueueManager queueManager = new QueueManager();

    // K:V => 地图名 : 游戏实例
    public GameManager(Plugin serverPlugin) {
        this.serverPlugin = serverPlugin;
    }


    // 此代码需要异步执行 Async
    public void init() {
        // 必须主线程运行
        // 必须确保配置已加载
        Bukkit.getScheduler().runTaskAsynchronously(serverPlugin, () -> {
            try {
                for (Map.Entry<String, MapConfig> entry : ConfigValues.mapConfigs.entrySet()) {
                    // 遍历，读取世界
                    resetGame(entry.getKey());
                }
            } catch (Exception e) {
                HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to init games");
                e.printStackTrace();
            }
        });

    }

    public void resetGame(String mapName) throws FileNotFoundException {
        try {
            // 主线程执行会导致死锁主线程，绝对不能主线程执行
            if (Bukkit.isPrimaryThread()) {
                HezhongSkywars.INSTANCE.getLogger().severe("HSW resetGame called from main thread!");
                return;
            }
            if (ConfigValues.mapConfigs.containsKey(mapName)) {
                MapConfig mc = ConfigValues.mapConfigs.get(mapName); // 配置
                // 去寻找原世界
                File originalWorld = new File(serverPlugin.getDataFolder(), "maps/" + mc.getOriginalWorld());
                if (!originalWorld.exists()) {
                    throw new FileNotFoundException("Cannot find original world " + mc.getOriginalWorld());
                }
                CompletableFuture<Boolean> futureUnload = new CompletableFuture<>();
                Bukkit.getScheduler().runTask(HezhongSkywars.INSTANCE.getPlugin(), () -> {
                    if (Bukkit.getWorld(mc.getCopyWorld()) != null) {
                        // 必须保存，否则爆炸
                        boolean success = Bukkit.unloadWorld(mc.getCopyWorld(), true);
                        if (!success) {
                            HezhongSkywars.INSTANCE.getLogger().severe("HSW unload world " + mc.getCopyWorld() + " failed!!!");
                        }
                    }
                    Bukkit.getScheduler().runTaskLater(HezhongSkywars.INSTANCE.getPlugin(), () -> {
                        // 如果不延迟的话，可能导致文件句柄不释放，导致重置异常
                        futureUnload.complete(true);
                    }, 20);
                });
                futureUnload.join();
                // 世界卸载后，复制一份地图
                copyWorld(mc.getOriginalWorld(), mc.getCopyWorld());
                // 文件复制后，加载世界
                CompletableFuture<World> futureLoad = new CompletableFuture<>();
                Bukkit.getScheduler().runTask(HezhongSkywars.INSTANCE.getPlugin(), () -> {
                    WorldCreator worldCreator = new WorldCreator(mc.getCopyWorld());
                    World w = worldCreator.createWorld();
                    if (w != null) {
                        // 禁止自然刷怪
                        w.setGameRuleValue("doMobSpawning", "false");
                        w.setGameRuleValue("keepInventory", "true"); // 自己清理背包
                    }
                    futureLoad.complete(w);
                });
                // 等着
                futureLoad.join();
                World w = futureLoad.get();
                // 此时全部载入完成
                if (w != null) {
                    try {
                        Map<Location, Chest> chests = new HashMap<>();
                        for (Map.Entry<Vector, String> entry : mc.getChests().entrySet()) {
                            // 初始化真正的箱子

                            ChestConfig cc = ConfigValues.chestConfigs.get(entry.getValue());
                            Chest chest = new Chest(mapName, cc.getMinFilled(), cc.getMaxFilled());
                            // 遍历所有的物品
                            for (Map.Entry<CustomItem, Integer> itemEntry : cc.getItem().entrySet()) {
                                chest.addChestItem(itemEntry.getValue(), itemEntry.getKey());
                            }
                            chests.put(new Location(w, entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()), chest);
                        }
                        List<Location> spawns = new ArrayList<>();
                        for (Vector v : mc.getSpawns()) {
                            spawns.add(new Location(w, v.getX(), v.getY(), v.getZ()));
                        }
                        games.put(mapName, new Game(mapName, w, chests, spawns, mc.getGameEvents()));
                    } catch (Exception e) {
                        HezhongSkywars.INSTANCE.getLogger().severe("HSW Failed to init game " + mapName + " while init-ing chest");
                        e.printStackTrace();
                    }
                }
                return;

            }
        } catch (ExecutionException | InterruptedException e) {
            HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to reset world");
            e.printStackTrace();
        }
    }

    private void copyWorld(String original, String target) {
        // 必须确保世界已经卸载
        // 此代码必须异步执行
        try {
            // 删除世界
            File originalWorld = new File(serverPlugin.getDataFolder(), "maps/" + original);
            File targetWorld = new File(Bukkit.getWorldContainer(), target);
            if (targetWorld.exists()) {
                deleteSafely(targetWorld, 10, 1000);
            }
            FileUtils.copyDirectory(originalWorld, targetWorld);
            // 删session.lock
            File sessionLock = new File(targetWorld, "session.lock");
            File uid = new File(targetWorld, "uid.dat");
            if (sessionLock.exists()) {
                FileUtils.delete(sessionLock);
            }
            if (uid.exists()) {
                FileUtils.delete(uid);
            }
        } catch (Exception e) {
            HezhongSkywars.INSTANCE.getLogger().warning("HSW Failed to copy world");
            e.printStackTrace();
        }


    }

    // 轮询删除
    // 猎奇文件句柄你无敌了
    private void deleteSafely(File file, int maxRetries, long delay) throws IOException {
        for (int tried = 1; tried <= maxRetries; tried++) {
            try {
                if (file.isDirectory()) {
                    FileUtils.deleteDirectory(file);
                } else {
                    FileUtils.forceDelete(file);
                }
                // 删了！
                return;
            } catch (IOException e) {
                HezhongSkywars.INSTANCE.getLogger().warning(
                        String.format("HSW Delete file %s failed, tried %d (max %d) %s",
                                file.getName(), tried, maxRetries, e.getMessage())
                );

                if (tried == maxRetries) {
                    throw e;
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Reset thread is interrupted?", ie);
                }
            }
        }
    }
}
