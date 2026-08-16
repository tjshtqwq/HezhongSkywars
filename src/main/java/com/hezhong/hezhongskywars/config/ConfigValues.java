package com.hezhong.hezhongskywars.config;

import com.hezhong.hezhongskywars.utils.cross.ServerInfoMessage;
import org.bukkit.GameMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigValues {
    // 储存配置数值
    // 纯静态类
    public static String serverIp;
    public static String serverName;
    public static String lobbyWorld;
    public static GameMode defaultGameMode;
    public static DataBaseConfig dataBaseConfig;

    public static boolean bungeeEnabled;
    public static ProxyType proxyType;
    public static ServerInfoMessage.ServerType serverType;
    public static String BCserverName;

    public static Map<String, ChestConfig> chestConfigs = new HashMap<>(); // K:V type:config
    public static Map<String, MapConfig> mapConfigs = new HashMap<>(); // K:V mapName:config
    public static Map<String, KitConfig> kitConfigs = new HashMap<>(); // K:V kitName:config
    public static int coinsWinAdd;
    public static int coinsKillAdd;
    public static int expWinAdd;
    public static int expKillAdd;
    public static List<Integer> levelNeedExps = new ArrayList<>();
    public static boolean multiWorldIndependentChat;
    public static boolean multiWorldIndependentTab;

    public enum ProxyType {
        BC,
        VC
    }
}
