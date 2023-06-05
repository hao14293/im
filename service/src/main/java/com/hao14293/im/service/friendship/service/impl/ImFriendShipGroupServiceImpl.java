package com.hao14293.im.service.friendship.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hao14293.im.codec.pack.friendship.AddFriendGroupPack;
import com.hao14293.im.codec.pack.friendship.DeleteFriendGroupPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.FriendshipEventCommand;
import com.hao14293.im.common.enums.DelFlagEnum;
import com.hao14293.im.common.enums.FriendShipErrorCode;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.service.friendship.entity.ImFriendShipGroupEntity;
import com.hao14293.im.service.friendship.mapper.ImFriendShipGroupMapper;
import com.hao14293.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.hao14293.im.service.friendship.model.req.AddFriendShipGroupReq;
import com.hao14293.im.service.friendship.model.req.DeleteFriendShipGroupReq;
import com.hao14293.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.hao14293.im.service.friendship.service.ImFriendShipGroupService;
import com.hao14293.im.service.seq.RedisSeq;
import com.hao14293.im.service.user.service.ImUserService;
import com.hao14293.im.service.utils.MessageProducer;
import com.hao14293.im.service.utils.WriteUserSeq;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Service
public class ImFriendShipGroupServiceImpl implements ImFriendShipGroupService {

    @Resource
    private ImFriendShipGroupMapper imFriendShipGroupMapper;

    @Resource
    private ImFriendShipGroupMemberService imFriendShipGroupMemberService;

    @Resource
    private ImUserService imUserService;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private RedisSeq redisSeq;

    @Resource
    private WriteUserSeq writeUserSeq;

    /**
     * 添加分组
     */
    @Override
    public ResponseVO addGroup(AddFriendShipGroupReq req) {
        LambdaQueryWrapper<ImFriendShipGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipGroupEntity::getGroupName, req.getGroupName());
        lqw.eq(ImFriendShipGroupEntity::getAppId, req.getAppId());
        lqw.eq(ImFriendShipGroupEntity::getFromId, req.getFromId());
        lqw.eq(ImFriendShipGroupEntity::getDelFlag, DelFlagEnum.NORMAL.getCode());
        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(lqw);

        if(entity != null){
            // 好友分组已经存在
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }

        // 写入db
        ImFriendShipGroupEntity insert = new ImFriendShipGroupEntity();
        insert.setAppId(req.getAppId());
        insert.setCreateTime(System.currentTimeMillis());
        insert.setDelFlag(DelFlagEnum.NORMAL.getCode());
        insert.setGroupName(req.getGroupName());
        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
        insert.setSequence(seq);
        insert.setFromId(req.getFromId());
        try {
            int insert1 = imFriendShipGroupMapper.insert(insert);
            if (insert1 != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_CREATE_ERROR);
            }
            if (insert1 == 1 && CollectionUtil.isNotEmpty(req.getToIds())) {
                AddFriendShipGroupMemberReq addFriendShipGroupMemberReq = new AddFriendShipGroupMemberReq();
                addFriendShipGroupMemberReq.setFromId(req.getFromId());
                addFriendShipGroupMemberReq.setGroupName(req.getGroupName());
                addFriendShipGroupMemberReq.setToIds(req.getToIds());
                addFriendShipGroupMemberReq.setAppId(req.getAppId());
                imFriendShipGroupMemberService.addGroupMember(addFriendShipGroupMemberReq);
                return ResponseVO.successResponse();
            }
        } catch (DuplicateKeyException e) {
            e.getStackTrace();
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }

        // TCP通知
        AddFriendGroupPack addFriendGropPack = new AddFriendGroupPack();
        addFriendGropPack.setFromId(req.getFromId());
        addFriendGropPack.setGroupName(req.getGroupName());
        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_ADD,
                addFriendGropPack, new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
        writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendshipGroup, seq);

        return ResponseVO.successResponse();
    }

    /**
     * 删除分组
     */
    @Override
    public ResponseVO deleteGroup(DeleteFriendShipGroupReq req) {
        for (String groupName : req.getGroupName()) {
            LambdaQueryWrapper<ImFriendShipGroupEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(ImFriendShipGroupEntity::getGroupName, groupName);
            lqw.eq(ImFriendShipGroupEntity::getAppId, req.getAppId());
            lqw.eq(ImFriendShipGroupEntity::getFromId, req.getFromId());
            lqw.eq(ImFriendShipGroupEntity::getDelFlag, DelFlagEnum.NORMAL.getCode());
            ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(lqw);

            if(entity != null){
                long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
                ImFriendShipGroupEntity update = new ImFriendShipGroupEntity();
                update.setGroupId(entity.getGroupId());
                update.setSequence(seq);

                // 逻辑删除
                update.setDelFlag(DelFlagEnum.DELETE.getCode());
                imFriendShipGroupMapper.updateById(update);
                // 清空组成员
                imFriendShipGroupMemberService.clearGroupMember(entity.getGroupId());

                //TCP通知
                DeleteFriendGroupPack deleteFriendGroupPack = new DeleteFriendGroupPack();
                deleteFriendGroupPack.setFromId(req.getFromId());
                deleteFriendGroupPack.setGroupName(groupName);
                messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_DELETE,
                        deleteFriendGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
                writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendshipGroup, seq);

            }
        }
        return ResponseVO.successResponse();
    }

    /**
     * 获取分组
     */
    @Override
    public ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId, String groupName, Integer appId) {
        LambdaQueryWrapper<ImFriendShipGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipGroupEntity::getGroupName, groupName);
        lqw.eq(ImFriendShipGroupEntity::getFromId, fromId);
        lqw.eq(ImFriendShipGroupEntity::getAppId, appId);
        lqw.eq(ImFriendShipGroupEntity::getDelFlag, DelFlagEnum.NORMAL.getCode());
        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(lqw);
        if(entity == null){
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }


    @Override
    public Long updateSeq(String fromId, String groupName, Integer appId) {
        QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
        query.eq("group_name", groupName);
        query.eq("app_id", appId);
        query.eq("from_id", fromId);

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

        long seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.FriendshipGroup);
        ImFriendShipGroupEntity group = new ImFriendShipGroupEntity();
        group.setGroupId(entity.getGroupId());
        group.setSequence(seq);
        imFriendShipGroupMapper.updateById(group);
        return seq;
    }
}
