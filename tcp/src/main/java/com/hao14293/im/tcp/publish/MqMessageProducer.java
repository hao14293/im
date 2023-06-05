package com.hao14293.im.tcp.publish;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.proto.Message;
import com.hao14293.im.codec.proto.MessageHeader;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.CommandType;
import com.hao14293.im.tcp.utils.MqFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Slf4j
public class MqMessageProducer {

    public static void sendMessage(Message message, Integer command) {
        Channel channel = null;
        String com = command.toString();
        String commandSub = com.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(commandSub);
        String channelName = "";

        if (commandType == CommandType.MESSAGE) {
            channelName = Constants.RabbitConstants.Im2MessageService;
        } else if (commandType == CommandType.GROUP) {
            channelName = Constants.RabbitConstants.Im2GroupService;
        } else if (commandType == CommandType.FRIEND) {
            channelName = Constants.RabbitConstants.Im2FriendshipService;
        } else if (commandType == CommandType.USER) {
            channelName = Constants.RabbitConstants.Im2UserService;
        }

        try {
            channel = MqFactory.getChannel(channelName);
            // 将要投递的信息转换成JSON对象
            JSONObject jsonObject = (JSONObject) JSON.toJSON(message.getMessagePack());
            jsonObject.put("command", command);
            jsonObject.put("clientType", message.getMessageHeader().getClientType());
            jsonObject.put("imei", message.getMessageHeader().getImei());
            jsonObject.put("appId", message.getMessageHeader().getAppId());
            channel.basicPublish(channelName, "", null, jsonObject.toJSONString().getBytes());
        } catch (Exception e) {
            log.error("发送消息出现异常：{}", e.getMessage());
        }
    }

    public static void sendMessage(Object message, MessageHeader header, Integer command) {
        Channel channel = null;
        String com = command.toString();
        String commandSub = com.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(commandSub);
        String channelName = "";

        if(commandType == CommandType.MESSAGE){
            channelName = Constants.RabbitConstants.Im2MessageService;
        }else if(commandType == CommandType.GROUP){
            channelName = Constants.RabbitConstants.Im2GroupService;
        }else if(commandType == CommandType.FRIEND){
            channelName = Constants.RabbitConstants.Im2FriendshipService;
        }else if(commandType == CommandType.USER){
            channelName = Constants.RabbitConstants.Im2UserService;
        }

        try {
            channel = MqFactory.getChannel(channelName);
            // 将要投递的信息转换成Json对象
            JSONObject jsonObject = (JSONObject) JSON.toJSON(message);
            jsonObject.put("command", command);
            jsonObject.put("clientType", header.getClientType());
            jsonObject.put("imei", header.getImei());
            jsonObject.put("appId", header.getAppId());

            channel.basicPublish(channelName, ""
                    , null, jsonObject.toJSONString().getBytes());
        }catch (Exception e){
            log.error("发送消息出现异常：{}", e.getMessage());
        }
    }
}
