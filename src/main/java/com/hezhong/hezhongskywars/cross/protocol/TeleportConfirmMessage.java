package com.hezhong.hezhongskywars.cross.protocol;

import lombok.Getter;

import java.util.UUID;

@Getter
public class TeleportConfirmMessage { // Lobby收到就能传了
    private final String serverName;
    private final String targetGame;
    private final boolean spectate;
    private final UUID playerUuid;
    private final Reason denyReason;
    public TeleportConfirmMessage(String serverName, String targetGame, boolean spectate, UUID playerUuid, Reason denyReason) {
        this.serverName = serverName;
        this.targetGame = targetGame;
        this.spectate = spectate;
        this.playerUuid = playerUuid;
        this.denyReason = denyReason;
    }
    public enum Reason {
        NO,
        NOT_FOUND,
        FULL
    }
}
