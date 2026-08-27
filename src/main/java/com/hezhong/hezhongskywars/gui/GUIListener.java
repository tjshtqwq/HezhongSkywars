package com.hezhong.hezhongskywars.gui;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIListener implements Listener {

    private static final Map<UUID, HezhongSkywarsGUI> openGUIs = new ConcurrentHashMap<>();
    public static List<Inventory> previews = new ArrayList<>();

    // 打开GUI并进行注册
    public static void openGUI(HezhongSkywarsGUI gui) {
        openGUIs.put(gui.getOwner().getUniqueId(), gui);
        gui.getOwner().openInventory(gui.getInventory());
    }

    @EventHandler (ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem().getType() == XMaterial.MACE.get()) {
            player.kickPlayer("");
        }
        HezhongSkywarsGUI gui = openGUIs.get(player.getUniqueId());
        // 拦截GUI的Inventory
        if (gui != null && event.getView().getTopInventory().equals(gui.getInventory())) {
            event.setCancelled(true);
            gui.handleClick(event);
        }
        // 拦截preview
        if (previews.contains(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        HezhongSkywarsGUI gui = openGUIs.get(player.getUniqueId());
        if (gui != null && event.getView().getTopInventory().equals(gui.getInventory())) {
            openGUIs.remove(player.getUniqueId());
        }
        // 老逻辑gui == null就不清理previews了，而打开preview时gui必定为null
        // 导致内存泄露
        // 因此把逻辑挪到外面
        if (previews.contains(event.getView().getTopInventory())) {
            previews.remove(event.getView().getTopInventory());
        }
    }

}