package com.hezhong.hezhongskywars.utils.cross;

import lombok.Getter;

import java.util.UUID;

@Getter
public class TeleportConfirmMessage { // Lobby收到就能传了
    private final String serverName;
    private final String targetGame;
    private final boolean spectate;
    private final UUID playerUuid;
    public TeleportConfirmMessage(String serverName, String targetGame, boolean spectate, UUID playerUuid) {
        this.serverName = serverName;
        this.targetGame = targetGame;
        this.spectate = spectate;
        this.playerUuid = playerUuid;
    }
}
