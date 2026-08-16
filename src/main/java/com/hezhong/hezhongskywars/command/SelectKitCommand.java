package com.hezhong.hezhongskywars.command;

import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.config.KitConfig;
import com.hezhong.hezhongskywars.game.Game;
import com.hezhong.hezhongskywars.manager.SwPlayerManager;
import com.hezhong.hezhongskywars.player.SwPlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SelectKitCommand extends HezhongSkywarsCommand {
    public SelectKitCommand() {
        super("selectKit", false, "<kitName>", "选择或购买职业");
    }

    @Override
    public void runCommand(CommandSender cs, Command command, String label, String[] args) {
        if (args.length < 2) {
            cs.sendMessage(ColorT.t("&c命令参数不足"));
            return;
        }
        String kitName = args[1];
        if (cs instanceof Player) {
            Player p = (Player) cs;
            SwPlayer sp = SwPlayerManager.getPlayer(p);
            if (sp.getPlayingGame() != null) {
                // 在游戏中，允许选择职业
                Game playing = sp.getPlayingGame();
                if (!ConfigValues.kitConfigs.containsKey(kitName)) {
                    p.sendMessage(ColorT.t("&c职业不存在。"));
                } else if (sp.hasKit(kitName)) {
                    playing.setPlayerKit(p, kitName);
                    p.sendMessage(ColorT.t("&a&l你选择了职业：" + kitName));
                } else {
                    p.sendMessage(ColorT.t("&c你还没有 &e" + kitName + " &c职业，请回大厅购买！"));
                }
            } else {
                if (ConfigValues.kitConfigs.containsKey(kitName)) {
                    KitConfig kit = ConfigValues.kitConfigs.get(kitName);
                    if (sp.hasKit(kitName)) {
                        p.sendMessage(ColorT.t("&a你已经拥有 &e" + kitName + " &a职业！"));
                        return;
                    }
                    if (sp.getStats().coins >= kit.getCoins() || kit.getCoins() <= 0) {
                        sp.getStats().coins -= kit.getCoins();
                        sp.getStats().kits.add(new SwPlayer.SwPlayerKit(kitName));
                        p.sendMessage(ColorT.t("&a你成功花费 &e" + kit.getCoins() + "&a 硬币购买了 &e " + kitName + " &a职业！"));
                    } else {
                        p.sendMessage(ColorT.t("&c硬币不足！需要&e " + kit.getCoins() + " &c硬币，但你只有 &e" + sp.getStats().coins));
                    }
                } else {
                    p.sendMessage(ColorT.t("&c职业不存在。"));
                }
            }
        }
    }
}
