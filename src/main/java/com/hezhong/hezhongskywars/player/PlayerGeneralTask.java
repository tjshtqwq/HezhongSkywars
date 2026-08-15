package com.hezhong.hezhongskywars.player;

import com.hezhong.hezhongskywars.config.ConfigValues;
import com.hezhong.hezhongskywars.game.ISwGame;
import com.hezhong.hezhongskywars.game.GameEvent;
import com.hezhong.hezhongskywars.game.GameStatus;
import com.hezhong.hezhongskywars.game.SwPlayingGamePlayer;
import com.hezhong.hezhongskywars.utils.ColorT;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class PlayerGeneralTask {

    private static final String[] ENTRIES = {
            "§8§a§r", "§8§b§r", "§8§c§r", "§8§d§r", "§8§e§r", "§8§f§r",
            "§8§0§r", "§8§1§r", "§8§2§r", "§8§3§r", "§8§4§r", "§8§5§r",
            "§8§6§r", "§8§7§r", "§8§8§r", "§8§9§r"
    };

    private final List<Team> teamCache = new ArrayList<>();
    private final SwPlayer sp;

    public PlayerGeneralTask(SwPlayer sp) {
        this.sp = sp;
    }

    public void update() {
        if (sp.getPlayingGame() == null) {
            updateLobbyScoreboard();
            sp.getPlayer().setExp(0);
            sp.getPlayer().setLevel(sp.getStats().getInGameLevel());
        }
        else updateInGameScoreboard(sp.getPlayingGame());

    }

    private void updateLobbyScoreboard() {
        setTitle(ConfigValues.serverName);
        setScoreBoardLines(new String[]{
                "&a&l您好 " + sp.getPlayer().getName(),
                "",
                "&b经验&7: &e" + sp.getStats().exps,
                "&e硬币&7: &e" + sp.getStats().coins,
                "&b等级&7: &e" + sp.getStats().getInGameLevel(),
                "",
                ConfigValues.serverIp
        });
    }

    private void updateInGameScoreboard(ISwGame game) {
        setTitle("&e&lSKYWARS &c&l⚔");

        if (game.getGameStatus() == GameStatus.WAITING) {
            setScoreBoardLines(new String[]{
                    "&6玩家数 &a" + game.getAlivePlayers().size() + "&7/&e" + game.getMaxPlayers(),
                    "&e还剩 " + (game.getPlayersToAutostart() - game.getAlivePlayers().size()),
                    "",
                    "&b职业&7: &e" + game.getPlayerKit(sp.getPlayer()),
                    "&e等待中......",
                    "",
                    "&7" + game.getMapName(),
                    ConfigValues.serverIp
            });
        } else if (game.getGameStatus() == GameStatus.STARTING) {
            setScoreBoardLines(new String[]{
                    "&6玩家数 &a" + game.getAlivePlayers().size() + "&7/&e" + game.getMaxPlayers(),
                    "&a倒计时 &e" + game.getCountdownRemaining(),
                    "",
                    "&b职业&7: &e" + game.getPlayerKit(sp.getPlayer()),
                    "&e启动中......",
                    "",
                    "&7" + game.getMapName(),
                    ConfigValues.serverIp
            });
        } else if (game.getGameStatus() == GameStatus.PLAYING) {
            SwPlayingGamePlayer swpgp = game.getPlayingPlayer(sp.getPlayer().getUniqueId());
            if (swpgp == null) {
                throw new RuntimeException("HSW Cannot get SWPGP in scoreboard processing");
            }
            int kills = swpgp.getKills();
            int alive = game.getAlivePlayers().size();
            int total = game.getAllPlayers().size();
            GameEvent nextEvent = null;
            if (game.getEventsInFuture() != null && !game.getEventsInFuture().isEmpty()) {
                nextEvent = game.getEventsInFuture().get(0);
            }
            setScoreBoardLines(new String[]{
                    "&b职业: &f" + game.getPlayerKit(sp.getPlayer()),
                    "&c击杀: &f" + kills,
                    "&a存活: &f" + alive + "&7/&f" + total,
                    "",
                    "&e时间: &f" + formatTime(game.getRunnedTime()),
                    "&e下一事件: &f" + (nextEvent == null ? "无" : nextEvent.getType().getEventName()) + (nextEvent == null ? "" : ("&e" + formatTime(nextEvent.getTime() - game.getRunnedTime()))),
                    "",
                    "&7" + game.getMapName(),
                    ConfigValues.serverIp
            });
        } else if (game.getGameStatus() == GameStatus.STOPPED || game.getGameStatus() == GameStatus.RESETTING) {
            setScoreBoardLines(new String[]{
                    "&e游戏结束",
                    "",
                    "&e胜者 &f" + game.getWinnerName(),
                    "",
                    "&a即将自动返回大厅...",
                    "&7" + game.getMapName(),
                    ConfigValues.serverIp
            });
        }
    }

    private void setScoreBoardLines(String[] lines) {
        // 从队伍缓冲区清除多余的 Teams，再在计分板中删除他们
        while (teamCache.size() > lines.length) {
            int index = teamCache.size() - 1;
            Team t = teamCache.remove(index);
            t.unregister();
            sp.getScoreBoard().resetScores(ENTRIES[index % ENTRIES.length]);
        }
        // 补充不足的队伍数
        while (teamCache.size() < lines.length) {
            int index = teamCache.size();
            String entry = ENTRIES[index % ENTRIES.length];
            Team team = sp.getScoreBoard().getTeam("HSW_" + index);
            if (team == null) {
                team = sp.getScoreBoard().registerNewTeam("HSW_" + index);
            }
            team.addEntry(entry);
            sp.getScoreBoardObjective().getScore(entry).setScore(lines.length - index);
            teamCache.add(team);
        }

        for (int i = 0; i < lines.length; i++) {
            Team team = teamCache.get(i);
            String text = "&r" + lines[i];
            text = ColorT.t(text);

            if (text.length() <= 16) {
                team.setPrefix(text);
                team.setSuffix("");
            } else {
                // ===== 修复：安全截断，避免颜色代码被拆断 =====
                String prefix = safeCut(text, 16);
                String remaining = text.substring(prefix.length());

                // 提取 prefix 中最后生效的颜色/格式代码
                String lastColor = getLastColors(prefix);

                // suffix 需要预留颜色代码占用的长度
                int suffixMaxLen = 16 - lastColor.length();
                String suffix = safeCut(remaining, Math.max(suffixMaxLen, 0));

                if (!suffix.isEmpty()) {
                    suffix = lastColor + suffix;
                }

                team.setPrefix(prefix);
                team.setSuffix(suffix);
            }
        }
    }

    /**
     * 安全截断字符串，确保不会在颜色代码（§x）中间截断。
     * 如果截断点恰好落在 § 之后，则回退一位。
     */
    private String safeCut(String text, int maxLen) {
        if (maxLen <= 0) return "";
        if (text.length() <= maxLen) return text;
        String cut = text.substring(0, maxLen);
        // 末尾是 §，说明颜色代码被截断，回退一位
        if (cut.endsWith("§")) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut;
    }

    // 以下修复由Qwen3.8-Max完成
    // 截断问题
    /**
     * 提取字符串中最后一个生效的颜色/格式代码组合。
     * - 颜色代码 (0-9, a-f) 会清除之前的格式
     * - 格式代码 (k-o) 会追加
     * - §r 重置所有
     */
    private String getLastColors(String text) {
        String color = "";
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) == '§') {
                char c = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdef".indexOf(c) != -1) {
                    // 颜色代码：重置格式，只保留颜色
                    color = "§" + c;
                } else if ("klmno".indexOf(c) != -1) {
                    // 格式代码（粗体/斜体等）：追加
                    color += "§" + c;
                } else if (c == 'r') {
                    // 重置代码：清空
                    color = "";
                }
                i++; // 跳过颜色代码的第二个字符
            }
        }
        return color;
    }

    private void setTitle(String title) {
        sp.getScoreBoardObjective().setDisplayName(ColorT.t(title));
    }

    private static String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}