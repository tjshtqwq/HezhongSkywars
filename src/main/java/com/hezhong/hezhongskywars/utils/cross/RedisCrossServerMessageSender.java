package com.hezhong.hezhongskywars.utils.cross;

import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.manager.ListenerManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;

public class RedisCrossServerMessageSender implements CrossServerMessageSender {

    private JavaPlugin plugin;
    private RedisMessageManager redis;

    @Override
    public void registerChannel(JavaPlugin plugin) {
        this.plugin = plugin;
        redis = new RedisMessageManager(ConfigValues.redisHost, ConfigValues.redisPort, ConfigValues.redisPassword, ConfigValues.redisChannel, plugin.getLogger());
        redis.start(message -> {
            // 订阅线程收到消息，解析后丢回主线程处理
            CrossServerMessagePacket packet = CrossServerMessagePacket.fromBytes(message.getBytes(StandardCharsets.UTF_8));
            Bukkit.getScheduler().runTask(plugin, () -> ListenerManager.crossServerMessageListener.receive(packet));
        });
    }

    @Override
    public void sendTo(CrossServerMessagePacket packet) {
        if (redis != null) {
            redis.publish(new String(packet.toBytes(), StandardCharsets.UTF_8));
        }
    }

    @Override
    public void shutdown() {
        if (redis != null) redis.stop();
    }
}
