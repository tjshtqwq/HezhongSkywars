package com.hezhong.hezhongskywars.bungee;

import com.hezhong.hezhongskywars.utils.cross.RedisMessageManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class HezhongSkywarsBungee extends Plugin {
    private RedisMessageManager redis;

    @Override
    public void onEnable() {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("bungee-config.yml")) {
                    if (in != null) Files.copy(in, configFile.toPath());
                }
            }
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            String host = config.getString("redis.host", "127.0.0.1");
            int port = config.getInt("redis.port", 6379);
            String password = config.getString("redis.password", "");
            String channel = config.getString("redis.channel", "hsw:csm");

            redis = new RedisMessageManager(host, port, password, channel, getLogger());
            new MessageListener(this, redis);
            getProxy().getPluginManager().registerListener(this, new ServerStatusListener(redis));

            getLogger().info("Hezhong Skywars BungeeCord > Redis transport online");
        } catch (IOException e) {
            getLogger().severe("HezhongSkywars failed to load config: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (redis != null) redis.stop();
    }
}
