package com.hezhong.hezhongskywars.bungee;

import com.google.gson.Gson;
import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.cross.networking.RedisMessageManager;
import com.hezhong.hezhongskywars.cross.protocol.CrossServerMessagePacket;
import com.hezhong.hezhongskywars.cross.protocol.ServerInfoMessage;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HezhongSkywarsBungee extends Plugin {
    private RedisMessageManager redis;
    private static final Gson gson = new Gson();
    @Getter
    private final Map<String, Server> allServers = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        try {
            // 拷贝配置
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("bungee-config.yml")) {
                    if (in != null) Files.copy(in, configFile.toPath());
                }
            }
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            String host = config.getString("redis.host");
            int port = config.getInt("redis.port");
            String password = config.getString("redis.password");
            boolean ssl = config.getBoolean("redis.ssl");
            String channel = config.getString("redis.channel", "hsw:csm");

            redis = new RedisMessageManager(host, port, password, channel, getLogger(), ssl);
            new MessageListener(this, redis);

            getLogger().info("HezhongSkywars BungeeCord > Redis OK");

            getProxy().getScheduler().schedule(this, () -> {
                // 检测服务器存活
                List<String> willRemoved = new ArrayList<>();
                for (Map.Entry<String, Server> entry : allServers.entrySet()) {
                    if ((System.currentTimeMillis() - entry.getValue().getLastInfoPacket()) >= (24 * 1000)) {
                        willRemoved.add(entry.getKey());
                    }
                }
                for (String s : willRemoved) {
                    allServers.remove(s);
                    // 发包
                    ServerInfoMessage msg = new ServerInfoMessage(s, ServerInfoMessage.ServerType.ANY, false, null, 0, -1);
                    CrossServerMessagePacket packet = new CrossServerMessagePacket("proxy", "", ServerInfoMessage.ServerType.PROXY, ServerInfoMessage.ServerType.ANY, CrossServerMessagePacket.MsgCommand.SERVER_INFO, gson.toJson(msg));
                    redis.publish(new String(packet.toBytes(), StandardCharsets.UTF_8));
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            getLogger().severe("HezhongSkywars failed to load config: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (redis != null) redis.stop();
    }
}
