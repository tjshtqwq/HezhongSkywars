package com.hezhong.hezhongskywars.utils.cross;

import lombok.Getter;

import java.util.List;

@Getter
public class ServerInfoMessage {
    private final ServerType serverType;
    private final List<Object> games; // Only serverType==GAME
    private final int players;
    public  ServerInfoMessage(ServerType serverType, List<Object> games, int players) {
        this.serverType = serverType;
        this.games = games;
        this.players = players;
    }

    public enum ServerType {
        GAME,
        LOBBY
    }
}
