package com.hao14293.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.pack.message.ChatMessageAck;
import com.hao14293.im.codec.pack.message.MessageReciveServerAckPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.MessageCommand;
import com.hao14293.im.common.enums.ConversationTypeEnum;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.common.model.message.MessageContent;
import com.hao14293.im.common.model.message.OfflineMessageContent;
import com.hao14293.im.service.message.model.req.SendMessageReq;
import com.hao14293.im.service.message.model.resp.SendMessageResp;
import com.hao14293.im.service.seq.RedisSeq;
import com.hao14293.im.service.utils.CallbackService;
import com.hao14293.im.service.utils.ConversationIdGenerate;
import com.hao14293.im.service.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: hao14293
 * @data 2023/4/24
 * @time 20:04
 */
@Service
public class P2PMessageService {

    private static Logger logger = LoggerFactory.getLogger(P2PMessageService.class);

    @Autowired
    private CheckSendMessageService checkSendMessageService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private MessageStoreService messageStoreService;

    @Autowired
    private RedisSeq redisSeq;

    @Resource
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    private final ThreadPoolExecutor threadPoolExecutor;

    {
        final AtomicInteger atomicInteger = new AtomicInteger(0);

        threadPoolExecutor
                = new ThreadPoolExecutor(8, 8,
                60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setDaemon(true);
                        thread.setName("message-process-thread-" + atomicInteger.getAndIncrement());
                        return thread;
                    }
                });
    }

    public void process(MessageContent messageContent){
        // 前置校验
        // 这个用户是否被禁言，是否被禁用，
        // 发送方和接受方是否是好友
        String fromId = messageContent.getFromId();
        String toId = messageContent.getToId();
        Integer appId = messageContent.getAppId();

        // TODO 用messageId 从缓存中获取消息
        MessageContent messageFromMessageIdCache
                = messageStoreService.getMessageFromMessageIdCache
                (messageContent.getAppId(), messageContent.getMessageId(), MessageContent.class);

        // 如果缓存命中了，说明就是处理过这个messageId这条消息，所以就可以执行同步即可
        if(messageFromMessageIdCache != null){
            threadPoolExecutor.execute(()->{
                // 1、回ack
                ack(messageContent, ResponseVO.successResponse());
                //2.发消息给同步在线端
                syncToSender(messageFromMessageIdCache, messageFromMessageIdCache);
                //3.发消息给对方在线端
                List<ClientInfo> clientInfos = dispatchMessage(messageFromMessageIdCache);
                if(clientInfos.isEmpty()){
                    //发送接收确认给发送方，要带上是服务端发送的标识
                    recicerAck(messageFromMessageIdCache);
                }
            });
            return;
        }

        // 回调前
        ResponseVO responseVO = ResponseVO.successResponse();
        if(appConfig.isSendMessageBeforeCallback()){
            responseVO = callbackService.beforecallback(messageContent.getAppId()
                    , Constants.CallbackCommand.SendMessageBefore, JSONObject.toJSONString(messageContent));
        }

        if(!responseVO.isOk()){
            ack(messageContent, responseVO);
            return;
        }

        // appId + seq + (from + to) / group
        long seq = redisSeq.doGetSeq(messageContent.getAppId() + ":"
                + Constants.SeqConstants.Message + ":" + ConversationIdGenerate.generateP2PId(messageContent.getFromId(),
                messageContent.getToId()));
        messageContent.setMessageSequence(seq);

        // 缓存没有命中的话，就执行正常逻辑
        threadPoolExecutor.execute(()->{

            // 插入数据
            messageStoreService.storeP2PMessage(messageContent);

            // TODO 插入离线消息
            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(messageContent, offlineMessageContent);
            offlineMessageContent.setConversationType(ConversationTypeEnum.P2P.getCode());
            messageStoreService.storeOfflineMessage(offlineMessageContent);

            // 1、回ACK成功给自己
            ack(messageContent, ResponseVO.successResponse());
            // 2、消息给同步在线端
            syncToSender(messageContent, messageContent);
            // 3、发消息给对方在线端
            List<ClientInfo> list = dispatchMessage(messageContent);

            // 将messageId存储到缓存中
            messageStoreService.setMessageFromMessageIdCache(messageContent.getAppId(), messageContent.getMessageId(),
                    messageContent);

            if(list.isEmpty()){
                // 要发送接受确认给发送方
                recicerAck(messageContent);
            }

            // 回调后
            if(appConfig.isSendMessageAfterCallback()){
                callbackService.callback(messageContent.getAppId()
                        , Constants.CallbackCommand.SendMessageAfter, JSONObject.toJSONString(messageContent));
            }

            logger.info("消息处理完成：{}",messageContent.getMessageId());
        });
    }

    // 发消息给同步在线端
    private void syncToSender(MessageContent messageContent, ClientInfo clientInfo) {
        messageProducer.sendToUserExceptClient(messageContent.getFromId(), MessageCommand.MSG_P2P
                , messageContent, messageContent);
    }

    // 发消息给对方在线端
    private List<ClientInfo> dispatchMessage(MessageContent messageContent) {
        List<ClientInfo> list
                = messageProducer.sendToUser(messageContent.getToId(), MessageCommand.MSG_P2P,
                messageContent, messageContent.getAppId());
        return list;
    }

    // 回复ack
    private void ack(MessageContent messageContent, ResponseVO responseVO){
        logger.info("msg ack, msgId = {}, checkResult = {}", messageContent.getMessageId(), responseVO.getCode());
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId(),
                messageContent.getMessageSequence());
        responseVO.setData(chatMessageAck);
        // 发消息
        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_ACK, responseVO, messageContent);
    }

    // 当对方没有在线的时候，就由服务端回给客户端一个ack
    public void recicerAck(MessageContent messageContent){
        MessageReciveServerAckPack pack = new MessageReciveServerAckPack();
        pack.setFromId(messageContent.getToId());
        pack.setToId(messageContent.getFromId());
        pack.setMessageKey(messageContent.getMessageKey());
        pack.setMessageSequence(messageContent.getMessageSequence());
        pack.setServerSend(true);
        messageProducer.sendToUser(messageContent.getFromId()
                , MessageCommand.MSG_RECIVE_ACK, pack, new ClientInfo(messageContent.getAppId(),
                        messageContent.getClientType(), messageContent.getImei()));
    }

    // 封装前置校验
    public ResponseVO imeServerPermissionCheck(String fromId, String toId, Integer appId) {
        // 是否被禁用
        ResponseVO responseVO = checkSendMessageService.checkSenderForvidAndMute(fromId, appId);
        if (!responseVO.isOk()) {
            return responseVO;
        }
        // 是否是好友或者是黑名单
        ResponseVO responseVO1 = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO1;
    }

    // 发送单聊消息
    public SendMessageResp send(SendMessageReq req) {
        SendMessageResp sendMessageResp = new SendMessageResp();
        MessageContent message = new MessageContent();
        BeanUtils.copyProperties(req, message);
        // 插入数据
        messageStoreService.storeP2PMessage(message);
        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());
        // 同步我方在线端
        syncToSender(message, message);
        // 同步对方在线端
        dispatchMessage(message);
        return sendMessageResp;
    }
}
