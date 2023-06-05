package com.hao14293.message.service;

import com.hao14293.im.common.model.message.GroupChatMessageContent;
import com.hao14293.im.common.model.message.MessageContent;
import com.hao14293.message.model.dto.DoStoreGroupMessageDto;
import com.hao14293.message.model.dto.DoStoreP2PMessageDto;
import com.hao14293.message.model.entity.ImGroupMessageHistoryEntity;
import com.hao14293.message.model.entity.ImMessageBodyEntity;
import com.hao14293.message.model.entity.ImMessageHistoryEntity;
import com.hao14293.message.model.mapper.ImGroupMessageHistoryMapper;
import com.hao14293.message.model.mapper.ImMessageBodyMapper;
import com.hao14293.message.model.mapper.ImMessageHistoryMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Service
public class StoreMessageService {
    @Resource
    private ImMessageHistoryMapper imMessageHistoryMapper;

    @Autowired
    private ImMessageBodyMapper imMessageBodyMapper;

    @Resource
    private ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;

    // 持久化私聊消息
    @Transactional
    public void doStoreP2PMessage(DoStoreP2PMessageDto dto){
        imMessageBodyMapper.insert(dto.getImMessageBodyEntity());

        List<ImMessageHistoryEntity> imMessageHistoryEntities
                = extractToP2PMessageHistory(dto.getMessageContent(), dto.getImMessageBodyEntity());

        imMessageHistoryMapper.insertBatchSomeColumn(imMessageHistoryEntities);
    }

    public List<ImMessageHistoryEntity> extractToP2PMessageHistory(MessageContent messageContent
            , ImMessageBodyEntity imMessageBodyEntity){
        List<ImMessageHistoryEntity> list = new ArrayList<>();

        ImMessageHistoryEntity fromHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        // 雪花算法生成
        fromHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());
        fromHistory.setSequence(messageContent.getMessageSequence());

        ImMessageHistoryEntity toHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        toHistory.setSequence(messageContent.getMessageSequence());
        toHistory.setCreateTime(System.currentTimeMillis());

        list.add(fromHistory);
        list.add(toHistory);

        return list;
    }

    // 持久化群聊消息
    @Transactional
    public void doStoreGroupMessage(DoStoreGroupMessageDto dto) {
        imMessageBodyMapper.insert(dto.getImMessageBodyEntity());
        ImGroupMessageHistoryEntity imGroupMessageHistoryEntity
                = extractToGroupMessageHistory(dto.getGroupChatMessageContent(),
                dto.getImMessageBodyEntity());
        imGroupMessageHistoryMapper.insert(imGroupMessageHistoryEntity);
    }


    // 转化成 MessageHistory
    public ImGroupMessageHistoryEntity extractToGroupMessageHistory(GroupChatMessageContent groupChatMessageContent, ImMessageBodyEntity imMessageBodyEntity){
        ImGroupMessageHistoryEntity result
                = new ImGroupMessageHistoryEntity();
        BeanUtils.copyProperties(groupChatMessageContent, result);
        result.setGroupId(groupChatMessageContent.getGroupId());
        // 雪花算法生成
        result.setMessageKey(imMessageBodyEntity.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());

        return result;
    }
}

