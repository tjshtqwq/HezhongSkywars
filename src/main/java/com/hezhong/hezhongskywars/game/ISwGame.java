package com.hezhong.hezhongskywars.game;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public interface ISwGame {
    String getMapName();

    World getWorld();

    int getMaxPlayers();

    int getPlayersToAutostart();

    GameStatus getGameStatus();

    int getRunnedTime();

    int getCountdownRemaining();

    String getWinnerName();

    List<Player> getAllPlayers();

    List<Player> getAlivePlayers();

    List<Player> getSpectators();

    SwPlayingGamePlayer getPlayingPlayer(UUID uuid);

    List<GameEvent> getEventsInFuture();

    String getPlayerKit(Player player);

    void setPlayerKit(Player player, String kitName);

    boolean addPlayer(Player player, boolean spectate);

    String processDeath(Player killed, Player killer, boolean quit);
}
