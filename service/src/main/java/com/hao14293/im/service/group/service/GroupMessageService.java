package com.hao14293.im.service.group.service;

import com.hao14293.im.codec.pack.message.ChatMessageAck;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.GroupEventCommand;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.common.model.message.GroupChatMessageContent;
import com.hao14293.im.common.model.message.OfflineMessageContent;
import com.hao14293.im.service.group.model.req.SendGroupMessageReq;
import com.hao14293.im.service.message.model.resp.SendMessageResp;
import com.hao14293.im.service.message.service.CheckSendMessageService;
import com.hao14293.im.service.message.service.MessageStoreService;
import com.hao14293.im.service.seq.RedisSeq;
import com.hao14293.im.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: hao14293
 * @Date: 2023/6/2
 */
public class GroupMessageService {
    private static Logger logger = LoggerFactory.getLogger(GroupMessageService.class);

    @Resource
    private CheckSendMessageService checkSendMessageService;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private ImGroupMemberService imGroupMemberService;

    @Resource
    private MessageStoreService messageStoreService;

    @Resource
    private RedisSeq redisSeq;

    private final ThreadPoolExecutor threadPoolExecutor;
    {
        AtomicInteger num = new AtomicInteger();
        threadPoolExecutor = new ThreadPoolExecutor(8, 8
                , 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("message-group-thread-" + num.getAndIncrement());
                return thread;
            }
        });
    }

    public void process(GroupChatMessageContent messageContent){
        // 前置校验
        String fromId = messageContent.getFromId();
        String groupId = messageContent.getGroupId();
        Integer appId = messageContent.getAppId();
        GroupChatMessageContent messageFromMessageIdCache = messageStoreService.getMessageFromMessageIdCache(
                messageContent.getAppId(), messageContent.getMessageId(), GroupChatMessageContent.class);
        // 如果缓存命中，就是之前处理过这个messageId的消息
        if(messageFromMessageIdCache != null){
            threadPoolExecutor.execute(()->{
                // 1、回ACK成功给自己
                ack(messageContent, ResponseVO.successResponse());
                // 2、消息给同步在线端
                syncToSender(messageContent, messageContent);
                // 3、发消息给对方在线端
                dispatchMessage(messageContent);
            });
        }
        // 保证有序性（生成绝对递增的序列号）
        long seq = redisSeq.doGetSeq(messageContent.getAppId() + ":"
                + Constants.SeqConstants.GroupMessage + ":" + messageContent.getGroupId());
        messageContent.setMessageSequence(seq);
        // 引入线程池
        threadPoolExecutor.execute(()->{
            // 持久化消息
            messageStoreService.storeGroupMessage(messageContent);

            // TODO 插入离线消息
            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(messageContent, offlineMessageContent);

            List<String> groupMemberId = imGroupMemberService.getGroupMemberId(messageContent.getGroupId(),
                    messageContent.getAppId());
            messageContent.setMemberId(groupMemberId);

            offlineMessageContent.setToId(messageContent.getGroupId());
            messageStoreService.storeGroupOfflineMessage(offlineMessageContent, groupMemberId);

            // 1、回ACK成功给自己
            ack(messageContent, ResponseVO.successResponse());
            // 2、消息给同步在线端
            syncToSender(messageContent, messageContent);
            // 3、发消息给对方在线端
            dispatchMessage(messageContent);
            messageStoreService.setMessageFromMessageIdCache(messageContent.getAppId()
                    , messageContent.getMessageId(), messageContent);
        });
    }

    // 发消息给同步在线端
    private void syncToSender(GroupChatMessageContent messageContent, ClientInfo clientInfo) {
        messageProducer.sendToUserExceptClient(messageContent.getFromId(), GroupEventCommand.MSG_GROUP
                , messageContent, messageContent);
    }

    // 发消息给对方在线端
    private void dispatchMessage(GroupChatMessageContent messageContent) {
        for (String memeberId : messageContent.getMemberId()) {
            if(!memeberId.equals(messageContent.getFromId())){
                messageProducer.sendToUser(memeberId, GroupEventCommand.MSG_GROUP,
                        messageContent, messageContent.getAppId());
            }
        }
    }

    // 回复ack
    private void ack(GroupChatMessageContent messageContent, ResponseVO responseVO){
        logger.info("msg ack, msgId = {}, checkResult = {}", messageContent.getMessageId(), responseVO.getCode());
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId());
        responseVO.setData(chatMessageAck);
        // 发消息
        messageProducer.sendToUser(messageContent.getFromId()
                , GroupEventCommand.GROUP_MSG_ACK, responseVO, messageContent);
    }


    // 封装前置校验
    private ResponseVO imeServerPermissionCheck(String fromId, String groupId, Integer appId) {
        // 是否被禁用
        ResponseVO responseVO = checkSendMessageService.checkGroupMessage(fromId, groupId, appId);
        return responseVO;
    }

    // 发送群聊消息
    public SendMessageResp send(SendGroupMessageReq req) {
        SendMessageResp sendMessageResp = new SendMessageResp();
        GroupChatMessageContent message = new GroupChatMessageContent();
        BeanUtils.copyProperties(req, message);
        // 插入
        messageStoreService.storeGroupMessage(message);

        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());
        // 我方同步在线端
        syncToSender(message, message);
        // 对方同步
        dispatchMessage(message);
        return sendMessageResp;
    }
}
