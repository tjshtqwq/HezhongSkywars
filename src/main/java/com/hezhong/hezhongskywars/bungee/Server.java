package com.hezhong.hezhongskywars.bungee;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Server {
    private final String name;
    @Setter
    private long lastInfoPacket;
    public Server(String name) {
        this.name = name;
    }
}
