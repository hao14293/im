package com.hao14293.message.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.message.model.dto.DoStoreP2PMessageDto;
import com.hao14293.message.model.entity.ImMessageBodyEntity;
import com.hao14293.message.service.StoreMessageService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;


@Service
public class StoreP2PMessageReceiver {
    private static Logger logger = LoggerFactory.getLogger(StoreP2PMessageReceiver.class);

    @Autowired
    private StoreMessageService storeMessageService;

    // 这个注解就是消费者获取rabbitmq中的信息
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitConstants.StoreP2PMessage, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.StoreP2PMessage, durable = "true")
            ),concurrency = "1"
    )
    public void onChatMessage(@Payload Message message, @Headers Map<String, Object> headers,
                              Channel channel) throws IOException {
        String msg = new String(message.getBody(), "utf-8");
        logger.info("CHAT MSG FROM QUEUE ::: {}", msg);
        long deliveryTag = (long) headers.get(AmqpHeaders.DELIVERY_TAG);

        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            // json解析
            DoStoreP2PMessageDto dto = jsonObject.toJavaObject(DoStoreP2PMessageDto.class);
            ImMessageBodyEntity messageBody = jsonObject.getObject("messageBody", ImMessageBodyEntity.class);
            dto.setImMessageBodyEntity(messageBody);
            // 持久化
            storeMessageService.doStoreP2PMessage(dto);
            channel.basicAck(deliveryTag, false);
        }catch (Exception e){
            logger.error("处理消息出现异常：{}", e.getMessage());
            logger.error("RMQ_CHAT_TRAN_ERROR", e);
            logger.error("NACK_MSG:{}", msg);
            //第一个false 表示不批量拒绝，第二个false表示不重回队列
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
