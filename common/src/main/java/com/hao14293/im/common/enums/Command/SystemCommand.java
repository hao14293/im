package com.hao14293.im.common.enums.Command;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public enum SystemCommand implements Command{
    /**
     * 心跳 9999
     */
    PING(0x270f),
    /**
     * 登录 9000
     */
    LOGIN(0x2328),

    //登录ack  9001
    LOGINACK(0x2329),

    /**
     * 退出  9003
     */
    LOGOUT(0x232b),

    //下线通知 用于多端互斥  9002
    MUTUALLOGIN(0x232a),

    ;

    private int code;
    SystemCommand(int code){
        this.code = code;
    }

    @Override
    public int getCommand() {
        return code;
    }
}
