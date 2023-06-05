package com.hao14293.im.codec.proto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
/**
 * 消息服务发送给tcp服务器的包体,服务器再根据该包体解析成Message发给客户端
 */
public class MessagePack<T> implements Serializable {

    private String userId;

    private Integer appId;

    private String toId;

    private int clientType;

    private String messageId;

    private String imei;

    private Integer command;

    private T data;
}
