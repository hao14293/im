package com.hao14293.im.common.enums.Command;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum ConversationEventCommand implements Command{
    // 删除会话
    CONVERSATION_DELETE(5000),

    // 更新会话
    CONVERSATION_UPDATE(5001),
    ;

    private int command;

    ConversationEventCommand(int command) {
        this.command = command;
    }

    @Override
    public int getCommand() {
        return this.command;
    }
}
