package com.hao14293.message.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.message.model.dto.DoStoreGroupMessageDto;
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
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;


@Service
public class StoreGroupMessageReceiver {
    private static Logger logger = LoggerFactory.getLogger(StoreGroupMessageReceiver.class);

    @Resource
    private StoreMessageService storeMessageService;

    // 这个注解就是消费者获取rabbitmq中的信息
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitConstants.StoreGroupMessage, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitConstants.StoreGroupMessage, durable = "true")
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
            DoStoreGroupMessageDto dto = jsonObject.toJavaObject(DoStoreGroupMessageDto.class);
            ImMessageBodyEntity messageBody = jsonObject.getObject("messageBody", ImMessageBodyEntity.class);
            dto.setImMessageBodyEntity(messageBody);
            // 持久化
            storeMessageService.doStoreGroupMessage(dto);
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
