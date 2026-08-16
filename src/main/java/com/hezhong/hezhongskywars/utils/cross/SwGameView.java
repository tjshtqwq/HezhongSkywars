package com.hezhong.hezhongskywars.utils.cross;

import com.hezhong.hezhongskywars.game.GameStatus;
import lombok.Getter;

@Getter
public class SwGameView {
    private final String mapName; // 标识地图名，不是copyName
    private final String serverName; // 用于跨服，当前服就写local
    private final GameStatus gameStatus;
    private final int players; // 当前游玩玩家数
    private final int spectators; // 当前旁观玩家数
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
