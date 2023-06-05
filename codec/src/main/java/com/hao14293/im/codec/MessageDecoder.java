package com.hao14293.im.codec;

import com.hao14293.im.codec.proto.Message;
import com.hao14293.im.codec.utils.ByteBufToMessageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 消息解码
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf byteBuf, List<Object> out) throws Exception {
        //请求头（指令
        // 版本
        // clientType
        // 消息解析类型
        // appId
        // imei长度
        // bodylen）+ imei号 + 请求体

        // messageHeader 长度为 28 字节
        if (byteBuf.readableBytes() < 28) {
            return;
        }
        Message message = ByteBufToMessageUtils.transition(byteBuf);
        if (message == null) {
            return;
        }
        out.add(message);
    }
}
