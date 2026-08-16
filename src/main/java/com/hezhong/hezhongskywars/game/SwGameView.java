package com.hezhong.hezhongskywars.game;

import lombok.Getter;

@Getter
public class SwGameView {
    private final String mapName; // 标识地图名
    private final String serverName; // 用于跨服，当前服就写local
    private final GameStatus gameStatus;
    private final int players;
    private final int spectators;
    private final int maxPlayers;
    private final int playersToAutostart;
    private final int countdownRemaining;

    public SwGameView(String mapName, String serverName, GameStatus gameStatus, int players, int spectators, int maxPlayers, int playersToAutostart, int countdownRemaining) {
        this.mapName = mapName;
        this.serverName = serverName;
        this.gameStatus = gameStatus;
        this.players = players;
        this.spectators = spectators;
        this.maxPlayers = maxPlayers;
        this.playersToAutostart = playersToAutostart;
        this.countdownRemaining = countdownRemaining;
    }
}
