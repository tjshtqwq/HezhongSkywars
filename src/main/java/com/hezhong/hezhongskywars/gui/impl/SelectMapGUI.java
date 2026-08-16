package com.hezhong.hezhongskywars.gui.impl;

import com.cryptomorin.xseries.XMaterial;
import com.hezhong.hezhongskywars.HezhongSkywars;
import com.hezhong.hezhongskywars.game.GameStatus;
import com.hezhong.hezhongskywars.gui.MultiPageGUI;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import com.hezhong.hezhongskywars.game.SwGameView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectMapGUI extends MultiPageGUI {
    private static final int ROWS = 6;

    private final List<SwGameView> gamesList = new ArrayList<>();
    private final Map<Integer, SwGameView> slotMapMap = new HashMap<>();
    public SelectMapGUI(Player owner, SwPlayer ownerSp) {
        super(owner, ownerSp, "&a&l所有地图", ROWS, createPageItem("&a下一页"), createPageItem("&a上一页"));
        build();
    }

    private void build() {
        int slot = 0;
        for (SwGameView view : HezhongSkywars.INSTANCE.getGameManager().getAllGameViews()) {
            ItemStack displayItem = buildDisplayItem(view);
            gamesList.add(view);
            multiPageInventory.put(slot, displayItem);
            slot++;
        }
        showPage();
    }
    private ItemStack buildDisplayItem(SwGameView view) {
        String mapName = view.getMapName();
        ItemStack dis = null;
        int canJoinPlayers = view.getMaxPlayers() - view.getPlayers();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&f地图名: &e" + mapName);
        lore.add("&f玩家: &e" + view.getPlayers() + " / " + view.getMaxPlayers());
        if (view.getGameStatus() == GameStatus.WAITING) {
            if (canJoinPlayers > 0) {
                dis = new ItemStack(XMaterial.EMERALD_BLOCK.parseMaterial());
            } else {
                dis = new ItemStack(XMaterial.LAPIS_BLOCK.parseMaterial());
            }
            lore.add("&a等待中");
            lore.add("&9点击加入&7|&e右键旁观");

        } else if (view.getGameStatus() == GameStatus.STARTING) {
            if (canJoinPlayers > 0) {
                dis = new ItemStack(XMaterial.EMERALD_BLOCK.parseMaterial());
            } else {
                dis = new ItemStack(XMaterial.LAPIS_BLOCK.parseMaterial());
            }
            lore.add("&b启动中");
            lore.add("&9点击加入&7|&e右键旁观");

        } if (view.getGameStatus() == GameStatus.PLAYING) {
            dis = new ItemStack(XMaterial.REDSTONE_BLOCK.parseMaterial());
            lore.add("&c游玩中");
            lore.add("&9点击旁观");
        } else if (view.getGameStatus() == GameStatus.STOPPED) {
            dis = new ItemStack(XMaterial.COAL_BLOCK.parseMaterial());
            lore.add("&c已停止");
            lore.add("&9点击旁观");
        } else if (view.getGameStatus() == GameStatus.RESETTING) {
            dis = new ItemStack(XMaterial.CLOCK.parseMaterial());
            lore.add("&f重置中");
        }
        ItemMeta meta = dis.getItemMeta();
        meta.setDisplayName(ColorT.t("&f&l" + mapName));
        meta.setLore(ColorT.t(lore));
        dis.setItemMeta(meta);
        return dis;
    }
    @Override
    protected void showPage() {
        slotMapMap.clear();
        super.showPage();
        int itemsPerPage = (ROWS - 1) * 9; // MultiPageGUI占用最后一行，因此我们只用rows - 1行
        int start = currentPage * itemsPerPage; // 0-based，不需要+1
        int end = Math.min(start + itemsPerPage, gamesList.size());

        for (int i = start; i < end; i++) {
            slotMapMap.put(i - start, gamesList.get(i));
        }
    }
    @Override
    protected void handleMultiPageClick(InventoryClickEvent event) {
        int slot = event.getSlot();

       SwGameView game = slotMapMap.getOrDefault(slot, null);
        if (game == null) return;

        Player player = owner;

        if (event.isLeftClick()) {
            String mapName = game.getMapName();
            player.performCommand("hsw play " + mapName);
        } else if (event.isRightClick()) {
            String mapName = game.getMapName();
            player.performCommand("hsw spectate " + mapName);
        }
    }
}
