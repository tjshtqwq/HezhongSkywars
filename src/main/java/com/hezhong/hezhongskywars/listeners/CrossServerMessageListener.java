package com.hezhong.hezhongskywars.listeners;

import com.hezhong.hezhongskywars.utils.type.CrossServerMessagePacket;

public class CrossServerMessageListener {
    // 逻辑在这里
    // BC，VC的监听器调用即可
    public void receive(CrossServerMessagePacket packet) {
        String message = packet.getMessage();
        String[] cmds = message.split("\\|"); // | 分隔
        MessageCommand messageCommand = MessageCommand.valueOf(cmds[0].toUpperCase());

        if (messageCommand == MessageCommand.SERVER_INFO) {

        }
    }

    public enum MessageCommand {
        TELEPORT,
        SERVER_INFO,
    }
}
