package com.hao14293.im.common.enums;

/**
 * 聊天类型
 * @Author: hao14293
 * @Date: 2023/5/31
 */
public enum ConversationTypeEnum {
    /**
     * 0 单聊 1 群聊 2 机器人 3 公众号
     */
    P2P(0),
    GROUP(1),
    ROBOT(2),
    ;
    private int code;

    ConversationTypeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
