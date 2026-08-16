package com.hezhong.hezhongskywars.utils.cross;

import com.hezhong.hezhongskywars.game.SwGameView;
import lombok.Getter;

import java.util.List;

@Getter
public class ServerInfoMessage { // 广播包
    private final String serverName;
    private final ServerType serverType;
    private final boolean run; // 为false代表服务器下线
    private final List<SwGameView> games; // Only serverType==GAME
    private final int players;
    public ServerInfoMessage(String serverName, ServerType serverType, boolean run, List<SwGameView> games, int players) {
        this.serverName = serverName;
        this.serverType = serverType;
        this.run = run;
        this.games = games;
        this.players = players;
    }
    public enum ServerType {
        GAME,
        LOBBY,
        PROXY,
        ANY
    }
}
