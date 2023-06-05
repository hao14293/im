package com.hao14293.im.codec.utils;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.proto.Message;
import com.hao14293.im.codec.proto.MessageHeader;
import io.netty.buffer.ByteBuf;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public class ByteBufToMessageUtils {

    public static Message transition(ByteBuf in) {

        int command = in.readInt();

        int version = in.readInt();

        int clientType = in.readInt();

        int messageType = in.readInt();

        int appId = in.readInt();

        int imeiLength = in.readInt();

        int bodyLen = in.readInt();

        if (in.readableBytes() < bodyLen + imeiLength) {
            //总长度小于数据长度则不处理
            in.resetReaderIndex();
            return null;
        }

        byte[] imeiData = new byte[imeiLength];
        in.readBytes(imeiData);
        String imei = new String(imeiData);

        byte[] bodyData = new byte[bodyLen];
        in.readBytes(bodyData);

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setCommand(command);
        messageHeader.setVersion(version);
        messageHeader.setClientType(clientType);
        messageHeader.setMessageType(messageType);
        messageHeader.setAppId(appId);
        messageHeader.setImei(imei);

        Message message = new Message();
        message.setMessageHeader(messageHeader);

        if (messageType == 0x0) {
            String body = new String(bodyData);
            JSONObject parse = (JSONObject) JSONObject.parse(body);
            message.setMessagePack(parse);
        }
        //标记当前readerIndex位置，如果读到后面发现只有一半，使用resetReaderIndex重置回来
        in.markReaderIndex();
        return message;
    }
}
