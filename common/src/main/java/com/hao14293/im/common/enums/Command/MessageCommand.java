package com.hao14293.im.common.enums.Command;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum MessageCommand implements Command{
    //单聊消息 1103
    MSG_P2P(0x44F),

    //单聊消息ACK 1046
    MSG_ACK(0x416),

    //消息收到ack 1107
    MSG_RECIVE_ACK(1107),

    //发送消息已读   1106
    MSG_READED(0x452),

    //消息已读通知给同步端 1053
    MSG_READED_NOTIFY(0x41D),

    //消息已读回执，给原消息发送方 1054
    MSG_READED_RECEIPT(0x41E),

    //消息撤回 1050
    MSG_RECALL(0x41A),

    //消息撤回通知 1052
    MSG_RECALL_NOTIFY(0x41C),

    //消息撤回回报 1051
    MSG_RECALL_ACK(0x41B),
    ;
    private int code;

    MessageCommand(int code) {
        this.code = code;
    }

    @Override
    public int getCommand() {
        return this.code;
    }
}
