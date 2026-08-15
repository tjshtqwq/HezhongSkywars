package com.hezhong.hezhongskywars.events;

import com.hezhong.hezhongskywars.game.ISwGame;
import com.hezhong.hezhongskywars.game.GameStatus;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class HSWGameStatusChangeEvent extends Event{
    private final ISwGame game;
    private final GameStatus gameStatus;
    public HSWGameStatusChangeEvent(ISwGame game, GameStatus gameStatus) {
        this.game = game;
        this.gameStatus = gameStatus;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
