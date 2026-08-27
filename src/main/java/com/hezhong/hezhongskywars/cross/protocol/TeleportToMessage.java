package com.hezhong.hezhongskywars.cross.protocol;

import lombok.Getter;

import java.util.UUID;

@Getter
public class TeleportToMessage {
    private final String targetName;
    private final UUID playerUuid;
    public TeleportToMessage(String targetName, UUID playerUuid) {
        this.targetName = targetName;
        this.playerUuid = playerUuid;
    }
}
