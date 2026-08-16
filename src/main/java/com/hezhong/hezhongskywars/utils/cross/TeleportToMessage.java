package com.hezhong.hezhongskywars.utils.cross;

import java.util.UUID;

public class TeleportToMessage {
    private final String serverName;
    private final UUID playerUuid;
    public TeleportToMessage(String serverName, UUID playerUuid) {
        this.serverName = serverName;
        this.playerUuid = playerUuid;
    }
}
