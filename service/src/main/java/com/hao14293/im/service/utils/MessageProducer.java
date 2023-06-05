package com.hao14293.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.proto.MessagePack;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.Command;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.common.model.UserSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Service
public class MessageProducer {

    private static Logger logger = LoggerFactory.getLogger(MessageProducer.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private UserSessionUtils userSessionUtils;

    private String queueName = Constants.RabbitConstants.MessageService2Im;

    // 将要发送的信息弄到rabbitmq中去
    public boolean sendMessage(UserSession userSession, Object message){
        try {
            logger.info("send message == " + message);
            rabbitTemplate.convertAndSend(queueName, userSession.getBrokerId() + "", message);
            return true;
        }catch (Exception e){
            logger.error("send error :" + e.getMessage());
            return false;
        }
    }

    // 发送数据报包，包装数据，调用sendMessage
    public boolean sendPack(String toId, Command command, Object msg, UserSession userSession){
        MessagePack messagePack = new MessagePack();

        messagePack.setCommand(command.getCommand());
        messagePack.setToId(toId);
        messagePack.setClientType(userSession.getClientType());
        messagePack.setAppId(userSession.getAppId());
        messagePack.setImei(userSession.getImei());

        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(msg));
        messagePack.setData(jsonObject);
        String body = JSONObject.toJSONString(messagePack);

        return sendMessage(userSession, body);
    }

    // 发送给某个用户所有端
    public List<ClientInfo> sendToUser(String toId, Command command, Object msg, Integer appId){
        List<UserSession> userSession
                = userSessionUtils.getUserSession(appId, toId);
        List<ClientInfo> list = new ArrayList<>();
        for (UserSession session : userSession) {
            boolean b = sendPack(toId, command, msg, session);
            if(b){
                list.add(new ClientInfo(session.getAppId()
                        , session.getClientType(), session.getImei()));
            }
        }
        return list;
    }

    // 发送给除了某一端的其他端（这个相当于是对下面那个方法做了一个再封装）
    public void sendToUser(String toId, Integer clientType, String imei,
                           Command command, Object msg, Integer appId){
        // 如果imei好和clientType不为空的话，说明就是正常的用户，那就把这个信息发送给除了这个端的其他用户
        if(clientType != null && StringUtils.isNotBlank(imei)){
            ClientInfo clientInfo = new ClientInfo(appId, clientType, imei);
            sendToUserExceptClient(toId, command, msg, clientInfo);
        }else{
            sendToUser(toId, command, msg, appId);
        }
    }

    // 发送给除了某一端的其他端
    public void sendToUserExceptClient(String toId, Command command, Object msg, ClientInfo clientInfo){
        List<UserSession> userSession
                = userSessionUtils.getUserSession(clientInfo.getAppId(), toId);
        for (UserSession session : userSession) {
            if(!isMatch(session, clientInfo)){
                sendPack(toId, command, msg, session);
            }
        }
    }

    // 发送给某个用户的指定客户端
    public void sendToUser(String toId, Command command, Object data, ClientInfo clientInfo){
        UserSession userSession = userSessionUtils.getUserSession(clientInfo.getAppId(),
                toId, clientInfo.getClientType(), clientInfo.getImei());
        sendPack(toId, command, data, userSession);
    }

    private boolean isMatch(UserSession sessionDto, ClientInfo clientInfo) {
        return Objects.equals(sessionDto.getAppId(), clientInfo.getAppId())
                && Objects.equals(sessionDto.getImei(), clientInfo.getImei())
                && Objects.equals(sessionDto.getClientType(), clientInfo.getClientType());
    }
}

