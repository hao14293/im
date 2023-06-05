package com.hao14293.im.tcp.utils;

import com.hao14293.im.codec.config.BootstrapConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public class MqFactory {

    private static ConnectionFactory factory = null;

    private static ConcurrentHashMap<String, Channel> channelMap = new ConcurrentHashMap<>();

    public static void init(BootstrapConfig.Rabbitmq rabbitmq) {
        // 如果连接为空进行初始化
        if (factory == null) {
            factory = new ConnectionFactory();
            factory.setHost(rabbitmq.getHost());
            factory.setUsername(rabbitmq.getUserName());
            factory.setPassword(rabbitmq.getPassword());
            factory.setPort(rabbitmq.getPort());
            factory.setVirtualHost(rabbitmq.getVirtualHost());
        }
    }

    public static Channel getChannel(String channelName) throws IOException, TimeoutException {
        Channel channel = channelMap.get(channelName);
        if (channel == null) {
            channel = getConnection().createChannel();
            channelMap.put(channelName, channel);
        }
        return channel;
    }

    private static Connection getConnection() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        return connection;
    }
}
