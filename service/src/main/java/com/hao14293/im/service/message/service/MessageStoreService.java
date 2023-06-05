package com.hao14293.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.ConversationTypeEnum;
import com.hao14293.im.common.enums.DelFlagEnum;
import com.hao14293.im.common.model.message.*;
import com.hao14293.im.service.conversation.service.ConversationService;
import com.hao14293.im.service.message.dao.ImGroupMessageHistoryEntity;
import com.hao14293.im.service.message.dao.ImMessageBodyEntity;
import com.hao14293.im.service.message.dao.ImMessageHistoryEntity;
import com.hao14293.im.service.utils.SnowflakeIdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hao14293
 * @data 2023/4/25
 * @time 17:56
 */
@Service
public class MessageStoreService {

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private AppConfig appConfig;

    // 单聊消息持久化
    @Transactional
    public void storeP2PMessage(MessageContent messageContent){
        // 1、messageContent 转换成 messageBody

        /**
         * 本来持久化的步骤在sevice层，这里优化将持久化的过程通过rabbitmq异步投递到其他的服务中去，解耦提高效率
         */
//        // 2、插入messageBody
//        imMessageBodyMapper.insert(imMessageBodyEntity);
//        // 3、转化成 MessageHistory
//        List<ImMessageHistoryEntity> imMessageHistoryEntities
//                = extractToP2PMessageHistory(messageContent, imMessageBodyEntity);
//        // 4、批量插入
//        imMessageHistoryMapper.insertBatchSomeColumn(imMessageHistoryEntities);
//        messageContent.setMessageKey(imMessageBodyEntity.getMessageKey());
        ImMessageBody imMessageBody = extractMessageBody(messageContent);
        DoStoreP2PMessageDto dto = new DoStoreP2PMessageDto();
        dto.setMessageContent(messageContent);
        dto.setMessageBody(imMessageBody);
        messageContent.setMessageKey(imMessageBody.getMessageKey());
        // TODO 发送mq消息 异步解耦
        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreP2PMessage
                , "", JSONObject.toJSONString(dto));
    }

    // 将messageContent 转换成 messageBody
    public ImMessageBody extractMessageBody(MessageContent messageContent){
        ImMessageBody imMessageBodyEntity = new ImMessageBody();
        imMessageBodyEntity.setAppId(messageContent.getAppId());
        imMessageBodyEntity.setMessageKey(snowflakeIdWorker.nextId());
        imMessageBodyEntity.setCreateTime(System.currentTimeMillis());
        imMessageBodyEntity.setSecurityKey("");
        imMessageBodyEntity.setExtra(messageContent.getExtra());
        imMessageBodyEntity.setDelFlag(DelFlagEnum.NORMAL.getCode());
        imMessageBodyEntity.setMessageTime(messageContent.getMessageTime());
        imMessageBodyEntity.setMessageBody(messageContent.getMessageBody());
        return imMessageBodyEntity;
    }

    // 3、转化成 MessageHistory
    public List<ImMessageHistoryEntity> extractToP2PMessageHistory(MessageContent messageContent
            , ImMessageBodyEntity imMessageBodyEntity){
        List<ImMessageHistoryEntity> list = new ArrayList<>();

        ImMessageHistoryEntity fromHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        // 雪花算法生成
        fromHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());

        ImMessageHistoryEntity toHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());

        list.add(fromHistory);
        list.add(toHistory);
        return list;
    }

    // 存储群聊消息
    @Transactional
    public void storeGroupMessage(GroupChatMessageContent groupChatMessageContent){
//        // 转换成imMessageBody
//        ImMessageBodyEntity imMessageBodyEntity
//                = extractMessageBody(groupChatMessageContent);
//        // 插入
//        imMessageBodyMapper.insert(imMessageBodyEntity);
//        // 转换成MessageHistory
//        ImGroupMessageHistoryEntity imGroupMessageHistoryEntity
//                = extractToGroupMessageHistory(groupChatMessageContent, imMessageBodyEntity);
//        // 批量插入
//        imGroupMessageHistoryMapper.insert(imGroupMessageHistoryEntity);
//        groupChatMessageContent.setMessageKey(imMessageBodyEntity.getMessageKey());
        ImMessageBody imMessageBody = extractMessageBody(groupChatMessageContent);
        DoStoreGroupMessageDto dto = new DoStoreGroupMessageDto();
        dto.setMessageBody(imMessageBody);
        dto.setGroupChatMessageContent(groupChatMessageContent);
        groupChatMessageContent.setMessageKey(imMessageBody.getMessageKey());

        // TODO 发送mq消息 异步解耦
        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreGroupMessage
                , "", JSONObject.toJSONString(dto));
    }

    // 转化成 MessageHistory
    public ImGroupMessageHistoryEntity extractToGroupMessageHistory(GroupChatMessageContent groupChatMessageContent,
                                                                    ImMessageBodyEntity imMessageBodyEntity){
        ImGroupMessageHistoryEntity result
                = new ImGroupMessageHistoryEntity();
        BeanUtils.copyProperties(groupChatMessageContent, result);
        result.setGroupId(groupChatMessageContent.getGroupId());
        // 雪花算法生成
        result.setMessageKey(imMessageBodyEntity.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());

        return result;
    }

    // 设置缓存消息
    public void setMessageFromMessageIdCache(Integer appId, String messageId, Object messageContent){
        // appId : cache : messageId
        String key = appId + ":" + Constants.RedisConstants.cacheMessage + ":" + messageId;
        // 往缓存中插入消息
        stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(messageContent), 3000, TimeUnit.SECONDS);
    }

    // 获取缓存消息
    public <T>T getMessageFromMessageIdCache(Integer appId, String messageId, Class<T> clazz) {
        String key = appId + ":" + Constants.RedisConstants.cacheMessage + ":"
                + messageId;
        String msg = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.isBlank(msg)){
            return null;
        }
        return JSONObject.parseObject(msg, clazz);
    }

    // 存储单人离线消息(Redis)
    // 存储策略是数量
    public void storeOfflineMessage(OfflineMessageContent offlineMessageContent){
        // 找到fromId的队列
        String fromKey = offlineMessageContent.getAppId() + ":"
                + Constants.RedisConstants.OfflineMessage + ":" + offlineMessageContent.getFromId();
        // 找到toId的队列
        String toKey = offlineMessageContent.getAppId() + ":"
                + Constants.RedisConstants.OfflineMessage + ":" + offlineMessageContent.getToId();
        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();

        // 判断 队列中的数据 是否 超过设定值
        if(operations.zCard(fromKey) > appConfig.getOfflineMessageCount()){
            operations.remove(fromKey, 0, 0);
        }
        offlineMessageContent.setConversationId(conversationService.conversationConversationId(
                        ConversationTypeEnum.P2P.getCode()
                        , offlineMessageContent.getFromId(), offlineMessageContent.getToId()
                )
        );
        // 插入 数据 根据messageKey 作为分值
        operations.add(fromKey, JSONObject.toJSONString(offlineMessageContent),
                offlineMessageContent.getMessageKey());

        if(operations.zCard(toKey) > appConfig.getOfflineMessageCount()){
            operations.remove(toKey, 0, 0);
        }

        offlineMessageContent.setConversationId(conversationService.conversationConversationId(
                        ConversationTypeEnum.P2P.getCode()
                        , offlineMessageContent.getToId(), offlineMessageContent.getFromId()
                )
        );
        // 插入 数据 根据messageKey 作为分值
        operations.add(toKey, JSONObject.toJSONString(offlineMessageContent),
                offlineMessageContent.getMessageKey());
    }


    // 存储群组离线消息(Redis)
    // 存储策略是数量
    public void storeGroupOfflineMessage(OfflineMessageContent offlineMessageContent,
                                         List<String> memberIds){
        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        offlineMessageContent.setConversationType(ConversationTypeEnum.GROUP.getCode());
        for (String memberId : memberIds) {
            // 找到toId的队列
            String toKey = offlineMessageContent.getAppId() + ":"
                    + Constants.RedisConstants.OfflineMessage + ":" + memberId;

            offlineMessageContent.setConversationId(conversationService.conversationConversationId(
                    ConversationTypeEnum.GROUP.getCode()
                    , memberId, offlineMessageContent.getToId())
            );
            // 判断 队列中的数据 是否 超过设定值
            if(operations.zCard(toKey) > appConfig.getOfflineMessageCount()){
                operations.remove(toKey, 0, 0);
            }
            // 插入 数据 根据messageKey 作为分值
            operations.add(toKey, JSONObject.toJSONString(offlineMessageContent),
                    offlineMessageContent.getMessageKey());
        }
    }
}
