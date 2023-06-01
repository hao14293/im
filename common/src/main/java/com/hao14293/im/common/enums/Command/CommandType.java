package com.hao14293.im.common.enums.Command;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum CommandType {
    MESSAGE("1"),
    GROUP("2"),
    FRIEND("3"),
    USER("4"),
    ;

    private String commandType;

    public static CommandType getCommandType(String ordinal) {
        for (int i = 0; i < CommandType.values().length; i++) {
            if (CommandType.values()[i].getCommandType().equals(ordinal)){
                return CommandType.values()[i];
            }
        }
        return null;
    }

    CommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getCommandType() {
        return commandType;
    }
}
