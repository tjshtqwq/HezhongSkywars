package com.hezhong.hezhongskywars.cross.protocol;

import com.hezhong.hezhongskywars.game.SwGameView;
import lombok.Getter;

@Getter
public class UpdateGameMessage {
    private final String serverName;
    private final String gameName;
    private final SwGameView gameView;
    public UpdateGameMessage(SwGameView gameView) {
        this.gameView = gameView;
        serverName = gameView.getServerName();
        gameName = gameView.getMapName();
    }
}
