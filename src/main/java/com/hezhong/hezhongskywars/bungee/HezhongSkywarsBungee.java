package com.hezhong.hezhongskywars.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public class HezhongSkywarsBungee extends Plugin {
    @Override
    public void onEnable() {
        new MessageListener(this);
        getLogger().info("Hezhong Skywars BungeeCord >>> Message Transport Plugin");
    }
}
