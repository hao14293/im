package com.hao14293.im.tcp.utils;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.pack.user.UserStatusChangeNotifyPack;
import com.hao14293.im.codec.proto.MessageHeader;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.UserEventCommand;
import com.hao14293.im.common.enums.ImConnectStatusEnum;
import com.hao14293.im.common.model.UserClientDto;
import com.hao14293.im.common.model.UserSession;
import com.hao14293.im.tcp.publish.MqMessageProducer;
import com.hao14293.im.tcp.redis.RedisManager;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public class SessionSocketHolder {

    public static final Map<UserClientDto, NioSocketChannel> CHANNELS = new ConcurrentHashMap<>();

    /**
     * 存储channel
     */
    public static void put(Integer appId, String userId, Integer clientType, String imei, NioSocketChannel channel) {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(userId);
        userClientDto.setAppId(appId);
        userClientDto.setImei(imei);
        userClientDto.setClientType(clientType);
        CHANNELS.put(userClientDto, channel);
    }

    /**
     * 获取channel
     */
    public static NioSocketChannel get(Integer appId, String userId, Integer clientType, String imei) {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        userClientDto.setImei(imei);
        return CHANNELS.get(userClientDto);
    }

    /**
     * 根据用户ID获取channel
     */
    public static List<NioSocketChannel> get(Integer appId, String id) {
        Set<UserClientDto> channelInfos = CHANNELS.keySet();
        List<NioSocketChannel> channels = new ArrayList<>();

        channelInfos.forEach(channel ->{
            if (channel.getAppId().equals(appId) && id.equals(channel.getUserId())) {
                channels.add(CHANNELS.get(channel));
            }
        });

        return channels;
    }

    /**
     * 移除channel
     */
    public static void remove(Integer appId, String userId, Integer clientType, String imei){
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        userClientDto.setImei(imei);
        CHANNELS.remove(userClientDto);
    }

    /**
     * 移除某个channel
     */
    public static void remove(NioSocketChannel channel) {
        CHANNELS.entrySet().stream().filter(entity -> entity.getValue() == channel)
                .forEach(entry -> CHANNELS.remove(entry.getKey()));
    }

    /**
     * 登出
     */
    public static void removeUserSession(NioSocketChannel channel) {
        // 从CHANNELS中删除session
        String userId = (String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.Imei)).get();

        SessionSocketHolder.remove(appId, userId, clientType, imei);

        // 删除Redis中的session
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<Object, String> map
                = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        map.remove(clientType + ":" + imei);

        // 登出ack
        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setAppId(appId);
        messageHeader.setImei(imei);
        messageHeader.setClientType(clientType);

        UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
        userStatusChangeNotifyPack.setAppId(appId);
        userStatusChangeNotifyPack.setUserId(userId);
        userStatusChangeNotifyPack.setStatus(ImConnectStatusEnum.OFFLINE_STATUS.getCode());

        // 发送给mq
        MqMessageProducer.sendMessage(userStatusChangeNotifyPack, messageHeader, UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());

        channel.close();
    }

    /**
     * 离线
     */
    public static void offLineUserSession(NioSocketChannel channel) {
        // 删除session
        String userId = (String) channel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) channel
                .attr(AttributeKey.valueOf(Constants.Imei)).get();

        SessionSocketHolder.remove(appId, userId, clientType, imei);

        // 修改redis中的session的ConnectState
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map
                = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        // 获取session
        String sessionStr = map.get(clientType.toString() + ":" + imei);
        if(!StringUtils.isBlank(sessionStr)){
            // 将session转换为对象
            UserSession userSession = JSONObject.parseObject(sessionStr, UserSession.class);
            // 修改连接状态为离线
            userSession.setConnectState(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
            // 再写入redis中
            map.put(clientType.toString() + ":" + imei, JSONObject.toJSONString(userSession));
        }

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setAppId(appId);
        messageHeader.setImei(imei);
        messageHeader.setClientType(clientType);

        UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
        userStatusChangeNotifyPack.setAppId(appId);
        userStatusChangeNotifyPack.setUserId(userId);
        userStatusChangeNotifyPack.setStatus(ImConnectStatusEnum.OFFLINE_STATUS.getCode());

        // 发送给mq
        MqMessageProducer.sendMessage(userStatusChangeNotifyPack, messageHeader, UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());
        channel.close();
    }

}
