package com.hezhong.hezhongskywars.utils.cross;

import lombok.Getter;

import java.util.UUID;

@Getter
public class TeleportRequestMessage { // 传送请求，收到后回Confirm，源服继续处理
    private final String serverName;
    private final String targetGame;
    private final boolean spectate;
    private final UUID playerUuid;
    public TeleportRequestMessage(String serverName, String targetGame, boolean spectate, UUID playerUuid) {
        this.serverName = serverName;
        this.targetGame = targetGame;
        this.spectate = spectate;
        this.playerUuid = playerUuid;
    }
}
