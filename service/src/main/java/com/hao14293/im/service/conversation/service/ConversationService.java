package com.hao14293.im.service.conversation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hao14293.im.codec.pack.conversation.DeleteConversationPack;
import com.hao14293.im.codec.pack.conversation.UpdateConversationPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.ConversationEventCommand;
import com.hao14293.im.common.enums.ConversationErrorCode;
import com.hao14293.im.common.enums.ConversationTypeEnum;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.common.model.SyncResp;
import com.hao14293.im.common.model.message.MessageReadedContent;
import com.hao14293.im.service.conversation.entity.ImConversationSetEntity;
import com.hao14293.im.service.conversation.mapper.ImConversationSetMapper;
import com.hao14293.im.service.conversation.model.req.DeleteConversationReq;
import com.hao14293.im.service.conversation.model.req.UpdateConversationReq;
import com.hao14293.im.service.seq.RedisSeq;
import com.hao14293.im.service.utils.MessageProducer;
import com.hao14293.im.service.utils.WriteUserSeq;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: hao14293
 * @data 2023/4/30
 * @time 15:59
 */
@Service
public class ConversationService {

    @Resource
    private ImConversationSetMapper imConversationSetMapper;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisSeq redisSeq;

    @Resource
    private WriteUserSeq writeUserSeq;

    public String conversationConversationId(Integer type, String fromId, String toId){
        return type + "_" + fromId + "_" + toId;
    }

    // 已读到哪里了（已读标志）
    public void messageMarkRead(MessageReadedContent messageReadedContent){
        // 如果是单聊就是toId
        String toId = messageReadedContent.getToId();
        // 如果是群聊的话就是groupId
        if(messageReadedContent.getConversationType() == ConversationTypeEnum.GROUP.getCode()){
            toId = messageReadedContent.getGroupId();
        }
        String conversationId = conversationConversationId(messageReadedContent.getConversationType()
                , messageReadedContent.getFromId(), toId);
        // 获取会话 通过 appId 和 会话id
        LambdaQueryWrapper<ImConversationSetEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImConversationSetEntity::getConversationId, conversationId);
        lqw.eq(ImConversationSetEntity::getAppId, messageReadedContent.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(lqw);
        if(imConversationSetEntity == null){
            // 不存在
            imConversationSetEntity = new ImConversationSetEntity();
            long seq = redisSeq.doGetSeq(messageReadedContent.getAppId() + ":" + Constants.SeqConstants.Conversation);
            imConversationSetEntity.setConversationId(conversationId);
            BeanUtils.copyProperties(messageReadedContent, imConversationSetEntity);
            imConversationSetEntity.setReadedSequence(messageReadedContent.getMessageSequence());
            imConversationSetEntity.setToId(toId);
            imConversationSetEntity.setReadedSequence(seq);
            imConversationSetMapper.insert(imConversationSetEntity);
            writeUserSeq.writeUserSeq(messageReadedContent.getAppId(),
                    messageReadedContent.getFromId(), Constants.SeqConstants.Conversation, seq);
        }else{
            long seq = redisSeq.doGetSeq(messageReadedContent.getAppId() + ":" + Constants.SeqConstants.Conversation);
            // 存在，就更新已读的标志
            imConversationSetEntity.setReadedSequence(seq);
            imConversationSetEntity.setReadedSequence(messageReadedContent.getMessageSequence());
            imConversationSetMapper.readMark(imConversationSetEntity);
            writeUserSeq.writeUserSeq(messageReadedContent.getAppId(),
                    messageReadedContent.getFromId(),Constants.SeqConstants.Conversation, seq);
        }
    }

    // 删除会话
    public ResponseVO deleteConversation(DeleteConversationReq req){
        // 置顶  免打扰
//        QueryWrapper<ImConversationSetEntity> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("conversation_id",req.getConversationId());
//        queryWrapper.eq("app_id",req.getAppId());
//        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(queryWrapper);
//        if(imConversationSetEntity != null){
//            imConversationSetEntity.setIsMute(0);
//            imConversationSetEntity.setIsTop(0);
//            imConversationSetMapper.update(imConversationSetEntity,queryWrapper);
//        }

        // 需要发送给其他端
        if(appConfig.getDeleteConversationSyncMode() == 1){
            DeleteConversationPack pack = new DeleteConversationPack();
            pack.setConversationId(req.getConversationId());
            // 同步给其他端
            messageProducer.sendToUserExceptClient(req.getFromId(), ConversationEventCommand.CONVERSATION_DELETE,
                    pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        }

        return ResponseVO.successResponse();
    }


    // 更新会话
    public ResponseVO updateConversation(UpdateConversationReq req){
        // 如果置顶和免打扰都为空的话，就没有必要进行修改会话的信息了
        if(req.getIsTop() == null && req.getIsMute() == null){
            // TODO 返回失败
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_PARAM_ERROR);
        }
        // 找到该会话
        LambdaQueryWrapper<ImConversationSetEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImConversationSetEntity::getConversationId, req.getConversationId());
        lqw.eq(ImConversationSetEntity::getAppId, req.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(lqw);
        // 如果该会话不为空就进行下面的操作

        if(imConversationSetEntity != null){
            long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Conversation);
            // 更新免打扰
            if(req.getIsMute() != null){
               imConversationSetEntity.setIsMute(req.getIsMute());
            }
            // 更新置顶
            if(req.getIsTop() != null){
               imConversationSetEntity.setIsTop(req.getIsTop());
            }
            imConversationSetEntity.setSequence(seq);
            // 更新
            imConversationSetMapper.update(imConversationSetEntity, lqw);
            writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(),
                    Constants.SeqConstants.Conversation, seq);

            // 更新完了以后同步给其他端
            UpdateConversationPack pack = new UpdateConversationPack();
            pack.setConversationId(req.getConversationId());
            pack.setIsMute(imConversationSetEntity.getIsMute());
            pack.setIsTop(imConversationSetEntity.getIsTop());
            pack.setConversationType(req.getClientType());
            pack.setSequence(seq);
            messageProducer.sendToUserExceptClient(req.getFromId(), ConversationEventCommand.CONVERSATION_UPDATE,
                    pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        }

        return ResponseVO.successResponse();
    }

    // 增量拉取会话
    public ResponseVO syncConversationSet(SyncReq req) {
        // 单次拉取最大数
        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }

        SyncResp<ImConversationSetEntity> resp = new SyncResp<>();
        LambdaQueryWrapper<ImConversationSetEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImConversationSetEntity::getFromId, req.getOperater());
        lqw.gt(ImConversationSetEntity::getSequence, req.getLastSequence());
        lqw.eq(ImConversationSetEntity::getAppId, req.getAppId());
        lqw.last("limit " + req.getMaxLimit());
        lqw.orderByAsc(ImConversationSetEntity::getReadedSequence);

        List<ImConversationSetEntity> list = imConversationSetMapper.selectList(lqw);

        if(!CollectionUtils.isEmpty(list)){
            ImConversationSetEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            // 设置最大seq
            Long maxSeq
                    = imConversationSetMapper.getConversationSetMaxSeq(req.getAppId(), req.getOperater());
            resp.setMaxSequence(maxSeq);
            // 设置是否拉取完
            resp.setCompleted(maxSeqEntity.getReadedSequence() >= maxSeq);
            return ResponseVO.successResponse(resp);
        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }
}
