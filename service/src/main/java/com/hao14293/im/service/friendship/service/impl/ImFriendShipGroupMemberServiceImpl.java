package com.hao14293.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hao14293.im.codec.pack.friendship.AddFriendGroupMemberPack;
import com.hao14293.im.codec.pack.friendship.DeleteFriendGroupMemberPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.enums.Command.FriendshipEventCommand;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.service.friendship.entity.ImFriendShipGroupEntity;
import com.hao14293.im.service.friendship.entity.ImFriendShipGroupMemberEntity;
import com.hao14293.im.service.friendship.mapper.ImFriendShipGroupMemberMapper;
import com.hao14293.im.service.friendship.model.req.AddFriendShipGroupMemberReq;
import com.hao14293.im.service.friendship.model.req.DeleteFriendShipGroupMemberReq;
import com.hao14293.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.hao14293.im.service.friendship.service.ImFriendShipGroupService;
import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import com.hao14293.im.service.user.service.ImUserService;
import com.hao14293.im.service.utils.MessageProducer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Service
public class ImFriendShipGroupMemberServiceImpl implements ImFriendShipGroupMemberService {

    @Resource
    private ImFriendShipGroupMemberMapper imFriendShipGroupMemberMapper;

    @Resource
    private ImFriendShipGroupService imFriendShipGroupService;

    @Resource
    private ImUserService imUserService;

    @Resource
    private MessageProducer messageProducer;

    /**
     * 添加组内成员
     */
    @Override
    public ResponseVO addGroupMember(AddFriendShipGroupMemberReq req) {
        // 判断该组是否合法
        ResponseVO<ImFriendShipGroupEntity> group
                = imFriendShipGroupService.getGroup(req.getFromId(), req.getGroupName(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        ArrayList<String> successId = new ArrayList<>();
        for (String toId : req.getToIds()) {
            ResponseVO<ImUserDataEntity> singleUserInfo
                    = imUserService.getSingleUserInfo(toId, req.getAppId());
            // 新增组的成员的话，就要判断一下每个成员是否都合法
            if(singleUserInfo.isOk()){
                int i = this.doAddGroupMember(group.getData().getGroupId(), toId);
                if(i == 1){
                    successId.add(toId);
                }
            }
        }

         Long seq = imFriendShipGroupService.updateSeq(req.getFromId(), req.getGroupName(), req.getAppId());

         // TCP通知
         AddFriendGroupMemberPack pack = new AddFriendGroupMemberPack();
         pack.setFromId(req.getFromId());
         pack.setGroupName(req.getGroupName());
         pack.setToIds(successId);
         pack.setSequence(seq);
         messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_MEMBER_ADD,
         pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

         return ResponseVO.successResponse(successId);
    }

    /**
     * 删除组内成员
     */
    @Override
    public ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req) {
        ResponseVO<ImFriendShipGroupEntity> group
                = imFriendShipGroupService.getGroup(req.getFromId(), req.getGroupName(), req.getAppId());
        if(!group.isOk()){
            return group;
        }
        ArrayList list = new ArrayList();
        for (String toId : req.getToIds()) {
            // 删除组成员的时候，也要判断一下组成员的合法性
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if(singleUserInfo.isOk()){
                int i = deleteGroupMember(group.getData().getGroupId(), req.getToIds());
                if(i == 1){
                    list.add(toId);
                }
            }
        }

        Long seq = imFriendShipGroupService.updateSeq(req.getFromId(), req.getGroupName(), req.getAppId());
        DeleteFriendGroupMemberPack pack = new DeleteFriendGroupMemberPack();
        pack.setFromId(req.getFromId());
        pack.setGroupName(req.getGroupName());
        pack.setToIds(list);
        pack.setSequence(seq);
        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_MEMBER_DELETE,
                pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

        return ResponseVO.successResponse(list);
    }

    // 删除组成员具体逻辑
    public int deleteGroupMember(Long groupId, List<String> toIds) {
        LambdaQueryWrapper<ImFriendShipGroupMemberEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImFriendShipGroupMemberEntity::getGroupId,groupId);
        queryWrapper.in(ImFriendShipGroupMemberEntity::getToId,toIds);
        try {
            int delete = imFriendShipGroupMemberMapper.delete(queryWrapper);
            return delete;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 添加组内成员具体逻辑
     */
    @Override
    public int doAddGroupMember(Long groupId, String toIds) {
        LambdaQueryWrapper<ImFriendShipGroupMemberEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImFriendShipGroupMemberEntity::getGroupId,groupId);
        queryWrapper.in(ImFriendShipGroupMemberEntity::getToId, toIds);
        try {
            int delete = imFriendShipGroupMemberMapper.delete(queryWrapper);
            return delete;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 清空组内所有成员
     */
    @Override
    public int clearGroupMember(Long groupId) {
        LambdaQueryWrapper<ImFriendShipGroupMemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipGroupMemberEntity::getGroupId, groupId);
        int delete = imFriendShipGroupMemberMapper.delete(lqw);
        return delete;
    }
}
