package com.hao14293.im.tcp.reciver;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.proto.MessagePack;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.tcp.reciver.process.BaseProcess;
import com.hao14293.im.tcp.reciver.process.ProcessFactory;
import com.hao14293.im.tcp.utils.MqFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Slf4j
public class MessageReciver {
    private static String brokerId;

    public static void startReciverMessage() {
        try {
            Channel channel = MqFactory.getChannel(Constants.RabbitConstants.MessageService2Im
                    + brokerId);
            // 绑定队列
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im + brokerId,
                    true,false, false, null);
            // 绑定交换机
            channel.queueBind(Constants.RabbitConstants.MessageService2Im  + brokerId,
                    Constants.RabbitConstants.MessageService2Im,
                    brokerId);

            channel.basicConsume(Constants.RabbitConstants.MessageService2Im + brokerId, false
                    , new DefaultConsumer(channel){
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            try {
                                String msgStr = new String(body);
                                log.info(msgStr);
                                MessagePack messagePack =
                                        JSONObject.parseObject(msgStr, MessagePack.class);
                                // 通过工厂获取要使用的处理类
                                BaseProcess messageProcess = ProcessFactory
                                        .getMessageProcess(messagePack.getCommand());
                                // 处理
                                messageProcess.process(messagePack);
                                // 确认 第一个参数 getDeliveryTag是标记符 第二个参数是 是否批量的意思
                                channel.basicAck(envelope.getDeliveryTag(),false);
                            }catch (Exception e){
                                e.printStackTrace();
                                // 第三个参数是 是否重回队列
                                channel.basicNack(envelope.getDeliveryTag(),false,false);
                            }                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void init() {
        startReciverMessage();
    }

    public static void init(String brokerId) {
        if (StringUtils.isBlank(MessageReciver.brokerId)) {
            MessageReciver.brokerId = brokerId;
        }
        startReciverMessage();
    }
}
