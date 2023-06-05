package com.hao14293.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hao14293.im.codec.pack.message.MessageReadedPack;
import com.hao14293.im.codec.pack.message.RecallMessageNotifyPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.Command;
import com.hao14293.im.common.enums.Command.GroupEventCommand;
import com.hao14293.im.common.enums.Command.MessageCommand;
import com.hao14293.im.common.enums.ConversationTypeEnum;
import com.hao14293.im.common.enums.DelFlagEnum;
import com.hao14293.im.common.enums.MessageErrorCode;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.common.model.SyncResp;
import com.hao14293.im.common.model.message.MessageReadedContent;
import com.hao14293.im.common.model.message.MessageReciveAckContent;
import com.hao14293.im.common.model.message.OfflineMessageContent;
import com.hao14293.im.common.model.message.RecallMessageContent;
import com.hao14293.im.service.conversation.service.ConversationService;
import com.hao14293.im.service.group.service.ImGroupMemberService;
import com.hao14293.im.service.message.dao.ImMessageBodyEntity;
import com.hao14293.im.service.message.dao.mapper.ImMessageBodyMapper;
import com.hao14293.im.service.seq.RedisSeq;
import com.hao14293.im.service.utils.ConversationIdGenerate;
import com.hao14293.im.service.utils.GroupMessageProducer;
import com.hao14293.im.service.utils.MessageProducer;
import com.hao14293.im.service.utils.SnowflakeIdWorker;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @Author: hao14293
 * @data 2023/4/27
 * @time 16:14
 */
@Service
public class MessageSyncService {

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ImMessageBodyMapper imMessageBodyMapper;

    @Autowired
    private RedisSeq redisSeq;

    @Autowired
    private ImGroupMemberService imGroupMemberService;

    @Autowired
    GroupMessageProducer groupMessageProducer;

    public void receiveMark(MessageReciveAckContent reciveAckContent){
        // 同步我方在线端
        messageProducer.sendToUser(reciveAckContent.getToId(),
                MessageCommand.MSG_RECIVE_ACK,reciveAckContent, reciveAckContent.getAppId());
    }

    // 同步消息已读
    // 更新会话的seq，通知在线的同步端发送指定command，发送已读回执，通知对方（消息发起方）我已读
    public void readMark(MessageReadedContent messageReadedContent) {
        // 更新会话已读的位置
        conversationService.messageMarkRead(messageReadedContent);
        MessageReadedPack messageReadedPack = new MessageReadedPack();
        BeanUtils.copyProperties(messageReadedContent, messageReadedPack);

        // 把已读消息同步到自己的其他端
        syncToSender(messageReadedPack, messageReadedContent, MessageCommand.MSG_READED_NOTIFY);

        // 发送给原消息发送端
        messageProducer.sendToUser(messageReadedContent.getToId(), MessageCommand.MSG_READED_RECEIPT,
                messageReadedPack, messageReadedContent.getAppId());
    }

    // 把已读消息同步到自己的其他端
    public void syncToSender(MessageReadedPack pack, MessageReadedContent messageReadedContent, Command command){
        // 发送给自己的其它端
        messageProducer.sendToUserExceptClient(pack.getFromId(),
                command, pack, messageReadedContent);
    }

    // 群组已读
    public void groupReadMark(MessageReadedContent messageReaded) {
        // 更新群组会话已读的位置
        conversationService.messageMarkRead(messageReaded);
        MessageReadedPack messageReadedPack = new MessageReadedPack();
        BeanUtils.copyProperties(messageReaded, messageReadedPack);
        // 同步给自己的端
        syncToSender(messageReadedPack, messageReaded, GroupEventCommand.MSG_GROUP_READED_NOTIFY);
        // 分发给其他群成员
        messageProducer.sendToUser(messageReadedPack.getToId(), GroupEventCommand.MSG_GROUP_READED_RECEIPT,
                messageReaded, messageReaded.getAppId());
    }

    // 增量拉取离线消息
    public ResponseVO syncOfflineMessage(SyncReq req) {

        SyncResp<OfflineMessageContent> resp = new SyncResp<>();

        String key = req.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + req.getOperater();

        // 获取最大的seq
        Long maxSeq = 0L;

        // 获取到有序集合
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();

        // 调用api获取到最大的那个有序集合的set
        Set set = zSetOperations.reverseRangeWithScores(key, 0, 0);

        if(!CollectionUtils.isEmpty(set)){
            List list = new ArrayList(set);
            DefaultTypedTuple o = (DefaultTypedTuple)list.get(0);
            // 获取到最大的seq
            maxSeq = o.getScore().longValue();
        }
        resp.setMaxSequence(maxSeq);

        List<OfflineMessageContent> respList = new ArrayList<>();
        // 这里就像是查数据库一样
        // 调用api 截取limit的数量的  满足分值区间的 set
        Set<ZSetOperations.TypedTuple> querySet = zSetOperations.rangeByScoreWithScores(
                key, req.getLastSequence(), maxSeq, 0, req.getMaxLimit());
        for (ZSetOperations.TypedTuple<String> typedTuple : querySet) {
            // 获取道符合条件的离线消息
            String value = typedTuple.getValue();
            // Json转换
            OfflineMessageContent offlineMessageContent = JSONObject.parseObject(value, OfflineMessageContent.class);
            // 放到respList中
            respList.add(offlineMessageContent);
        }
        resp.setDataList(respList);

        if(!CollectionUtils.isEmpty(respList)){
            OfflineMessageContent offlineMessageContent = respList.get(respList.size() - 1);
            resp.setCompleted(offlineMessageContent.getMessageKey() >= maxSeq);
        }
        return ResponseVO.successResponse(resp);
    }

    // 撤回消息
    public void recallMessage(RecallMessageContent messageContent) {

        // 如果消息发送超过一定的时间就不可以撤回了
        Long messageTime = messageContent.getMessageTime();
        Long now = System.currentTimeMillis();
        RecallMessageNotifyPack pack = new RecallMessageNotifyPack();
        BeanUtils.copyProperties(messageContent, pack);
        if(120000L < now - messageTime){
            recallAck(pack, ResponseVO.errorResponse(MessageErrorCode.MESSAGE_RECALL_TIME_OUT), messageContent);
            return;
        }

        LambdaQueryWrapper<ImMessageBodyEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImMessageBodyEntity::getAppId, messageContent.getAppId());
        lqw.eq(ImMessageBodyEntity::getMessageKey, messageContent.getMessageKey());
        ImMessageBodyEntity body = imMessageBodyMapper.selectOne(lqw);

        // 如果查不到该消息的话
        if(body == null){
            // TODO ack失败 不存在的消息体不能撤回
            recallAck(pack, ResponseVO.errorResponse(MessageErrorCode.MESSAGEBODY_IS_NOT_EXIST), messageContent);
            return;
        }

        // 如果该消息已经被撤回
        if(body.getDelFlag() == DelFlagEnum.DELETE.getCode()){
            recallAck(pack, ResponseVO.errorResponse(MessageErrorCode.MESSAGE_IS_RECALLED), messageContent);
            return;
        }

        // 经过上面的判断，这时候的该信息就是没有撤回且正常的消息，下面就该进行修改历史信息
        body.setDelFlag(DelFlagEnum.DELETE.getCode());
        imMessageBodyMapper.update(body, lqw);

        // 如果撤回的消息的单聊的话
        if(messageContent.getConversationType() == ConversationTypeEnum.P2P.getCode()){

            // fromId的队列
            String fromKey = messageContent.getAppId() + ":"
                    + Constants.RedisConstants.OfflineMessage + ":" + messageContent.getFromId();

            // toId的队列
            String toKey = messageContent.getAppId() + ":"
                    + Constants.RedisConstants.OfflineMessage + ":" + messageContent.getToId();

            // 构建离线消息体
            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(messageContent, offlineMessageContent);
            offlineMessageContent.setDelFlag(DelFlagEnum.DELETE.getCode());
            offlineMessageContent.setMessageKey(messageContent.getMessageKey());
            offlineMessageContent.setConversationType(ConversationTypeEnum.P2P.getCode());
            offlineMessageContent.setConversationId(conversationService.conversationConversationId(
                    offlineMessageContent.getConversationType(), messageContent.getFromId(), messageContent.getToId()));
            offlineMessageContent.setMessageBody(body.getMessageBody());

            long seq = redisSeq.doGetSeq(messageContent.getAppId()
                    + ":" + Constants.SeqConstants.Message
                    + ":" + ConversationIdGenerate.generateP2PId(messageContent.getFromId(), messageContent.getToId()));
            offlineMessageContent.setMessageSequence(seq);

            long messageKey = SnowflakeIdWorker.nextId();

            redisTemplate.opsForZSet().add(fromKey, JSONObject.toJSONString(offlineMessageContent), messageKey);
            redisTemplate.opsForZSet().add(toKey, JSONObject.toJSONString(offlineMessageContent), messageKey);

            // ack
            recallAck(pack, ResponseVO.successResponse(), messageContent);

            // 分发给同步端
            messageProducer.sendToUserExceptClient(messageContent.getFromId(), MessageCommand.MSG_RECALL_NOTIFY,
                    pack, messageContent);

            // 分发给对方
            messageProducer.sendToUser(messageContent.getToId(), MessageCommand.MSG_RECALL_NOTIFY,
                    pack, messageContent);
        }else{
            List<String> groupMemberId
                    = imGroupMemberService.getGroupMemberId(messageContent.getToId(), messageContent.getAppId());
            long seq = redisSeq.doGetSeq(messageContent.getAppId() + ":" + Constants.SeqConstants.Message
                    + ":" + ConversationIdGenerate.generateP2PId(messageContent.getFromId(), messageContent.getToId()));
            // ack
            recallAck(pack, ResponseVO.successResponse(), messageContent);

            // 发送给同步端
            messageProducer.sendToUserExceptClient(messageContent.getFromId(), MessageCommand.MSG_RECALL_NOTIFY,
                    pack, messageContent);

            // 同步给对方端
            for (String memberId : groupMemberId) {
                String toKey = messageContent.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":"
                        + memberId;
                OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
                offlineMessageContent.setDelFlag(DelFlagEnum.DELETE.getCode());
                BeanUtils.copyProperties(messageContent, offlineMessageContent);
                offlineMessageContent.setConversationType(ConversationTypeEnum.GROUP.getCode());
                offlineMessageContent.setConversationId(conversationService.conversationConversationId(
                        offlineMessageContent.getConversationType(), messageContent.getFromId(), messageContent.getToId()
                ));
                offlineMessageContent.setMessageBody(body.getMessageBody());
                offlineMessageContent.setMessageSequence(seq);
                redisTemplate.opsForZSet().add(toKey, JSONObject.toJSONString(offlineMessageContent), seq);

                groupMessageProducer.producer(messageContent.getFromId(), MessageCommand.MSG_RECALL_NOTIFY
                        ,pack, messageContent);
            }
        }
    }

    // 撤回消息的ack
    private void recallAck(RecallMessageNotifyPack pack, ResponseVO<Object> errorResponse, ClientInfo clientInfo) {
        messageProducer.sendToUser(pack.getFromId(), MessageCommand.MSG_RECALL_ACK, errorResponse, clientInfo);
    }
}
