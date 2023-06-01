package com.hao14293.im.common.enums.Command;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum UserEventCommand implements Command{
    //用户修改command 4000
    USER_MODIFY(4000),

    //4001
    USER_ONLINE_STATUS_CHANGE(4001),

    //4004 用户在线状态通知报文
    USER_ONLINE_STATUS_CHANGE_NOTIFY(4004),

    //4005 用户在线状态通知同步报文
    USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC(4005),

    // 4006 用户自定义状态通知报文
    USER_ONLINE_STATUS__SET_CHANGE_NOTIFY(4006),

    // 4006 用户自定义状态通知同步报文
    USER_ONLINE_STATUS__SET_CHANGE_NOTIFY_SYNC(4007),


    ;

    private int command;

    UserEventCommand(int command){
        this.command=command;
    }


    @Override
    public int getCommand() {
        return command;
    }
}
