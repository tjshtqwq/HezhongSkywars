package com.hezhong.hezhongskywars.command;

import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class CommandProcessor implements CommandExecutor {

    private final List<HezhongSkywarsCommand> commands = new ArrayList<>();
    private static final int PAGE_SIZE = 7;

    public CommandProcessor() {
        registerCommand(new VersionCommand());
        registerCommand(new CreateCommand());
        registerCommand(new ModifyCommand());
        registerCommand(new ListMapsCommand());
        registerCommand(new StartCommand());
        registerCommand(new ListGamesCommand());
        registerCommand(new PlayCommand());
        registerCommand(new HubCommand());
        registerCommand(new SelectKitCommand());
        registerCommand(new GuiCommand());
        registerCommand(new SpectateCommand());
    }

    @Override
    public boolean onCommand(CommandSender cs, Command command, String label, String[] args) {
        if (args.length < 1) {
            cs.sendMessage(ColorT.t("&c命令参数不足。"));
            processHelp(cs, 1);
            return true;
        }

        if (Objects.equals(args[0], "help")) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {}
            }
            processHelp(cs, page);
            return true;
        }

        for (HezhongSkywarsCommand cmd : commands) {
            if (Objects.equals(args[0], cmd.getCommandName())) {
                if (!cmd.canExecute(cs)) {
                    cs.sendMessage(ColorT.t("&c命令权限不足，配置&e" + "hsw.command." + cmd.getCommandName() + "&c节点！"));
                    return true;
                }
                cmd.runCommand(cs, command, label, args);
                return true;
            }
        }

        cs.sendMessage(ColorT.t("&c未知命令，使用 /hsw help 查看帮助"));
        return true;
    }

    public void registerCommand(HezhongSkywarsCommand cmd) {
        commands.add(cmd);
    }

    private void processHelp(CommandSender cs, int page) {
        List<HezhongSkywarsCommand> displayCommands = new ArrayList<>(commands);
        int totalCommands = displayCommands.size() + 1;
        int totalPages = (int) Math.ceil((double) totalCommands / PAGE_SIZE);

        // 抗越界
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;


        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalCommands);

        cs.sendMessage(ColorT.t("&b&l Hezhong Skywars 命令帮助"));
        cs.sendMessage(ColorT.t("&c&l <>为必选参数，[]为可选参数"));

        for (int i = start; i < end; i++) {
            if (i == 0) {
                // 因为Help命令没有注册，是硬写的
                // 因此这里也要硬写
                cs.sendMessage(ColorT.t("&e /hsw help &a显示帮助"));
            } else {
                HezhongSkywarsCommand cmd = displayCommands.get(i - 1);
                String permStr = cmd.isPermission() ? "&c[需要权限]" : "&a[无需权限]";
                String needArgs = cmd.getNeedArgs().isEmpty() ? "" : " " + cmd.getNeedArgs();
                cs.sendMessage(ColorT.t("&e /hsw " + cmd.getCommandName() + needArgs + " " + permStr + " " + cmd.getDescription()));
            }
        }

        if (cs instanceof Player) {

            TextComponent msg = new TextComponent("");

            if (page > 1) {
                TextComponent prev = new TextComponent("<<< ");
                prev.setColor(ChatColor.GRAY);
                prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hsw help " + (page - 1)));
                prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("上一页").color(ChatColor.GRAY).create()));
                msg.addExtra(prev);
            }

            TextComponent pageText = new TextComponent("第" + page + "/" + totalPages + "页");
            pageText.setColor(ChatColor.WHITE);
            msg.addExtra(pageText);

            if (page < totalPages) {
                TextComponent next = new TextComponent(" >>>");
                next.setColor(ChatColor.GRAY);
                next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hsw help " + (page + 1)));
                next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("下一页").color(ChatColor.GRAY).create()));
                msg.addExtra(next);
            }

            ((Player) cs).spigot().sendMessage(msg);
        } else {
            cs.sendMessage(ColorT.t("&f第" + page + "/" + totalPages + "页"));
        }

        if (cs instanceof Player) {
            Player pp = (Player) cs;
            SwPlayer sp = SwPlayerManager.getPlayer(pp);
            if (sp != null && sp.isSettingUpMap()) {
                cs.sendMessage(ColorT.t("&a&b正在修改地图 " + sp.getSetUpMapName()));
                cs.sendMessage(ColorT.t("&e /hsw modify " + sp.getSetUpMapName() + " &a帮助"));
                cs.sendMessage(ColorT.t("&e /hsw modify " + sp.getSetUpMapName() + " exit &a退出修改模式&a&l并保存"));
                cs.sendMessage(ColorT.t("&e /hsw modify " + sp.getSetUpMapName() + " editChest <chestType> &a修改或添加箱子"));
                cs.sendMessage(ColorT.t("&e /hsw modify " + sp.getSetUpMapName() + " rmChest &a删除箱子"));
                cs.sendMessage(ColorT.t("&e /hsw modify " + sp.getSetUpMapName() + " listChest &a列出箱子"));
                cs.sendMessage(ColorT.t("&e /hsw modify " + sp.getSetUpMapName() + " addSpawn &a增加出生点"));
                cs.sendMessage(ColorT.t("&e /hsw modify " + sp.getSetUpMapName() + " rmSpawn <id> &a删除出生点"));
                cs.sendMessage(ColorT.t("&e /hsw modify " + sp.getSetUpMapName() + " listSpawn &a列出出生点"));
            }
        }
    }
}
