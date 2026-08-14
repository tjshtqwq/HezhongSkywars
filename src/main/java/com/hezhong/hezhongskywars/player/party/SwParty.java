package com.hezhong.hezhongskywars.player.party;

import com.hezhong.hezhongskywars.game.Game;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class SwParty {
    private final List<SwPlayer.SwPartyPlayer> players = new ArrayList<>();
    private String name;
    private Game playingGame;
    // owner必须无队伍才可创建队伍
    public SwParty(String name, SwPlayer owner) {
        this.name = name;
        // 赋予Owner
        players.add(new SwPlayer.SwPartyPlayer(owner.getPlayer().getUniqueId(), this, true));
        owner.setParty(this);
    }
    public boolean addPlayer(SwPlayer player) {
        if (player.getParty() == null) {
            // 可以加入
            if (players.size() > 2) {
                player.getPlayer().sendMessage(ColorT.t("&c&l此队伍已满！"));
                return false;
            }
            players.add(new SwPlayer.SwPartyPlayer(player.getPlayer().getUniqueId(), this, false));
            player.setParty(this);
            player.getPlayer().sendMessage(ColorT.t("&a你已加入队伍！"));
            return true;
        } else {
            player.getPlayer().sendMessage(ColorT.t("&c你在其它队伍中！"));
            return false;
        }
    }
    public boolean removePlayer(SwPlayer player) {
        if (player.getParty() == this) {
            boolean removed = players.removeIf(spp -> spp.getPlayerUuid() == player.getPlayer().getUniqueId());
            if (removed) {
                player.setParty(null);
                player.getPlayer().sendMessage(ColorT.t("&a你已成功退出 &e " + name + " &a队伍！"));
                return true;
            } else {
                player.getPlayer().sendMessage(ColorT.t("&c无法将你从 &e" + name + " &c队伍中删除！"));
                return false;
            }
        } else {
            player.getPlayer().sendMessage(ColorT.t("&c你不在 &e" + name + " &c队伍中！"));
            return false;
        }
    }
    public void removeAll() {
        for (SwPlayer.SwPartyPlayer spp : players) {
            SwPlayer player = SwPlayerManager.getPlayer(spp.getPlayerUuid());
            if (player != null) {
                player.setParty(null);
                player.getPlayer().sendMessage(ColorT.t("&c你的队伍 &e" + name + " &c 已被解散！"));
            }
        }
        players.clear();
    }

    public boolean isOwn(SwPlayer sp) {
        boolean found = false;
        for (SwPlayer.SwPartyPlayer spp : players) {
            if (spp.getPlayerUuid() == sp.getPlayer().getUniqueId() && spp.isOwn()) {
                found = true;
                break;
            }
        }
        return found;
    }
}
