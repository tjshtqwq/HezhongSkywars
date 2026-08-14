package com.hezhong.hezhongskywars.manager;

import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.player.party.SwParty;
import com.hezhong.hezhongskywars.utils.ColorT;

import java.util.ArrayList;
import java.util.List;

public class SwPartyManager {
    public final List<SwParty> parties = new ArrayList<>();
    public boolean createParty(SwPlayer owner, String name) {
        if (owner.getParty() == null) {
            if (owner.getPlayingGame() == null) {
                // 创建队伍
                parties.add(new SwParty(name, owner));
                owner.getPlayer().sendMessage(ColorT.t("&a你已成功创建 &e" + name + " &a队伍！"));
                return true;
            } else {
                owner.getPlayer().sendMessage(ColorT.t("&c你正在游戏中，不能创建队伍！"));
                return false;
            }
        } else {
            owner.getPlayer().sendMessage(ColorT.t("&c你已有队伍！"));
            return false;
        }
    }
    public boolean deleteParty(SwPlayer owner) {
        if (owner.getParty() != null) {
            SwParty party = owner.getParty();
            if (party.isOwn(owner)) {
                party.removeAll();
                owner.setParty(null);
                parties.remove(party);
                owner.getPlayer().sendMessage(ColorT.t("&a你已成功解散 &e" + party.getName() + " &a队伍！"));
                // Party的引用应该会被GC干掉
                return true;
            } else {
                owner.getPlayer().sendMessage(ColorT.t("&c权限不足！"));
                return false;
            }
        } else {
            owner.getPlayer().sendMessage(ColorT.t("&c你没有队伍！"));
            return false;
        }
    }
}
