package com.hao14293.im.tcp.reciver.process;

import com.hao14293.im.codec.proto.MessagePack;
import com.hao14293.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public abstract class BaseProcess {

    public abstract void processBefore();

    public void process(MessagePack messagePack){
        processBefore();
        // 通过从rabbitmq中拿到的数据报，找到我们要发送给哪个客户端的channel
        NioSocketChannel channel = SessionSocketHolder.get(messagePack.getAppId(), messagePack.getToId()
                , messagePack.getClientType(), messagePack.getImei());
        if(channel != null){
            // 如果不为空的话
            channel.writeAndFlush(messagePack);
        }
        processAfter();
    }

    public abstract void processAfter();
}
