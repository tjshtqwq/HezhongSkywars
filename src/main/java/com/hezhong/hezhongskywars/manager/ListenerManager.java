package com.hezhong.hezhongskywars.manager;

import com.hezhong.hezhongskywars.game.GameListener;
import com.hezhong.hezhongskywars.gui.GUIListener;
import com.hezhong.hezhongskywars.listeners.CrossServerMessageListener;
import com.hezhong.hezhongskywars.listeners.GeneralListener;
import com.hezhong.hezhongskywars.listeners.JoinQuitListener;
import com.hezhong.hezhongskywars.multiworld.IndependentWorldManager;
import com.hezhong.hezhongskywars.setup.SetupListener;

public class ListenerManager {
    public static GameListener gameListener;
    public static JoinQuitListener joinQuitListener;
    public static SetupListener setupListener;
    public static GUIListener guiListener;
    public static GeneralListener generalListener;
    public static IndependentWorldManager independentWorldManager;

    public static CrossServerMessageListener crossServerMessageListener;
}
