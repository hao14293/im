package com.hao14293.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hao14293.im.codec.pack.friendship.ApproverFriendRequestPack;
import com.hao14293.im.codec.pack.friendship.ReadAllFriendRequestPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.ApproverFriendRequestStatusEnum;
import com.hao14293.im.common.enums.Command.FriendshipEventCommand;
import com.hao14293.im.common.enums.FriendShipErrorCode;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.service.friendship.entity.ImFriendShipRequestEntity;
import com.hao14293.im.service.friendship.mapper.ImFriendShipRequestMapper;
import com.hao14293.im.service.friendship.model.req.ApproverFriendRequestReq;
import com.hao14293.im.service.friendship.model.req.FriendDto;
import com.hao14293.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.hao14293.im.service.friendship.service.ImFriendShipRequestService;
import com.hao14293.im.service.friendship.service.ImFriendShipService;
import com.hao14293.im.service.seq.RedisSeq;
import com.hao14293.im.service.utils.MessageProducer;
import com.hao14293.im.service.utils.WriteUserSeq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Service
public class ImFriendShipRequestServiceImpl implements ImFriendShipRequestService {

    @Resource
    private ImFriendShipRequestMapper imFriendShipRequestMapper;

    @Resource
    private ImFriendShipService imFriendShipService;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private RedisSeq redisSeq;

    @Resource
    private WriteUserSeq writeUserSeq;

    /**
     * 新增好友请求
     */
    @Override
    public ResponseVO addFriendshipRequest(String fromId, FriendDto dto, Integer appId) {

        LambdaQueryWrapper<ImFriendShipRequestEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipRequestEntity::getAppId, appId);
        lqw.eq(ImFriendShipRequestEntity::getFromId, fromId);
        lqw.eq(ImFriendShipRequestEntity::getToId, dto.getToId());
        ImFriendShipRequestEntity entity = imFriendShipRequestMapper.selectOne(lqw);

        long seq = redisSeq.doGetSeq(appId+":"+ Constants.SeqConstants.FriendshipRequest);

        if(entity == null){
            entity = new ImFriendShipRequestEntity();
            entity.setAddSource(dto.getAddSource());
            entity.setAddWording(dto.getAddWorking());
            entity.setAppId(appId);
            entity.setSequence(seq);
            entity.setFromId(fromId);
            entity.setToId(dto.getToId());
            entity.setReadStatus(0);
            entity.setApproveStatus(0);
            entity.setRemark(dto.getRemark());
            entity.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipRequestMapper.insert(entity);
            if(insert != 1){
                return ResponseVO.errorResponse();
            }
        }
        // 有这条好友请求的记录，就更新
        else{
            // 修改记录内容和时间
            if(StringUtils.isNotEmpty(dto.getAddSource())){
                entity.setAddSource(dto.getAddSource());
            }
            if(StringUtils.isNotEmpty(dto.getRemark())){
                entity.setRemark(dto.getRemark());
            }
            if(StringUtils.isNotEmpty(dto.getAddWorking())){
                entity.setAddWording(dto.getAddWorking());
            }
            entity.setSequence(seq);
            entity.setApproveStatus(0);
            entity.setReadStatus(0);
            imFriendShipRequestMapper.updateById(entity);
        }

         writeUserSeq.writeUserSeq(appId,dto.getToId(), Constants.SeqConstants.FriendshipRequest,seq);
         // 发送好友申请的tcp给接收方
         messageProducer.sendToUser(dto.getToId(), null, "",
         FriendshipEventCommand.FRIEND_REQUEST, entity, appId);

        return ResponseVO.successResponse();
    }

    /**
     * 审批好友请求
     */
    @Override
    @Transactional
    public ResponseVO approverFriendRequest(ApproverFriendRequestReq req) {
        ImFriendShipRequestEntity imFriendShipRequestEntity = imFriendShipRequestMapper.selectById(req.getId());
        if(imFriendShipRequestEntity == null){
            throw new ApplicationException(FriendShipErrorCode. FRIEND_REQUEST_IS_NOT_EXIST);
        }

        if(!req.getOperater().equals(imFriendShipRequestEntity.getToId())){
            //只能审批发给自己的好友请求
            throw new ApplicationException(FriendShipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }

        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendshipRequest);

        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        // 这里审批是指同意或者拒绝，所以要写活
        update.setApproveStatus(req.getStatus());
        update.setUpdateTime(System.currentTimeMillis());
        update.setId(req.getId());
        update.setSequence(seq);
        imFriendShipRequestMapper.updateById(update);

        writeUserSeq.writeUserSeq(req.getAppId(),req.getOperater(), Constants.SeqConstants.FriendshipRequest,seq);

        // 如果是统一的话，就可以直接调用添加好友的逻辑了
        if(ApproverFriendRequestStatusEnum.AGREE.getCode() == req.getStatus()){
            FriendDto dto = new FriendDto();
            dto.setAddSource(imFriendShipRequestEntity.getAddSource());
            dto.setAddWorking(imFriendShipRequestEntity.getAddWording());
            dto.setRemark(imFriendShipRequestEntity.getRemark());
            dto.setToId(imFriendShipRequestEntity.getToId());
            ResponseVO responseVO = imFriendShipService.doAddFriend(req
                    , imFriendShipRequestEntity.getFromId(), dto, req.getAppId());
            if(!responseVO.isOk() && responseVO.getCode() != FriendShipErrorCode.TO_IS_YOUR_FRIEND.getCode()){
                return responseVO;
            }
        }

         // 通知审批人的其他端
         ApproverFriendRequestPack approverFriendRequestPack = new ApproverFriendRequestPack();
         approverFriendRequestPack.setStatus(req.getStatus());
         approverFriendRequestPack.setId(req.getId());
         approverFriendRequestPack.setSequence(seq);
         messageProducer.sendToUser(imFriendShipRequestEntity.getToId(), req.getClientType(), req.getImei(),
         FriendshipEventCommand.FRIEND_REQUEST_APPROVER, approverFriendRequestPack, req.getAppId());

        return ResponseVO.successResponse();
    }

    /**
     * 已读好友请求
     */
    @Override
    public ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req) {

        LambdaQueryWrapper<ImFriendShipRequestEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipRequestEntity::getAppId, req.getAppId());
        query.eq(ImFriendShipRequestEntity::getToId, req.getFromId());

        long seq = redisSeq.doGetSeq(req.getAppId()+":"+ Constants.SeqConstants.FriendshipRequest);
        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setReadStatus(1);
        update.setSequence(seq);

        int update1 = imFriendShipRequestMapper.update(update, query);

        writeUserSeq.writeUserSeq(req.getAppId(),req.getOperater(), Constants.SeqConstants.FriendshipRequest,seq);
        if(update1 != 1){
            return ResponseVO.errorResponse();
        }

         // TCP通知
         ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
         readAllFriendRequestPack.setFromId(req.getFromId());
         readAllFriendRequestPack.setSequence(seq);
         messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(),
         FriendshipEventCommand.FRIEND_REQUEST_READ, readAllFriendRequestPack, req.getAppId());

         return ResponseVO.successResponse();
    }

    /**
     * 获得好友请求
     */
    @Override
    public ResponseVO getFriendRequest(String fromId, Integer appId) {
        LambdaQueryWrapper<ImFriendShipRequestEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipRequestEntity::getAppId, appId);
        lqw.eq(ImFriendShipRequestEntity::getToId, fromId);
        List<ImFriendShipRequestEntity> requestEntities
                = imFriendShipRequestMapper.selectList(lqw);
        return ResponseVO.successResponse(requestEntities);
    }
}
