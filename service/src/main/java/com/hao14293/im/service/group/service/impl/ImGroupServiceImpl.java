package com.hao14293.im.service.group.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hao14293.im.codec.pack.group.CreateGroupPack;
import com.hao14293.im.codec.pack.group.DestroyGroupPack;
import com.hao14293.im.codec.pack.group.UpdateGroupInfoPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.GroupEventCommand;
import com.hao14293.im.common.enums.GroupErrorCode;
import com.hao14293.im.common.enums.GroupMemberRoleEnum;
import com.hao14293.im.common.enums.GroupStatusEnum;
import com.hao14293.im.common.enums.GroupTypeEnum;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.common.model.SyncResp;
import com.hao14293.im.service.group.entity.ImGroupEntity;
import com.hao14293.im.service.group.mapper.ImGroupMapper;
import com.hao14293.im.service.group.model.callback.DestroyGroupCallbackDto;
import com.hao14293.im.service.group.model.req.*;
import com.hao14293.im.service.group.model.resp.GetGroupResp;
import com.hao14293.im.service.group.model.resp.GetRoleInGroupResp;
import com.hao14293.im.service.group.service.ImGroupMemberService;
import com.hao14293.im.service.group.service.ImGroupService;
import com.hao14293.im.service.seq.RedisSeq;
import com.hao14293.im.service.utils.CallbackService;
import com.hao14293.im.service.utils.GroupMessageProducer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Service
public class ImGroupServiceImpl implements ImGroupService {

    @Resource
    private ImGroupMapper imGroupMapper;

    @Resource
    private ImGroupMemberService imGroupMemberService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private CallbackService callbackService;

    @Resource
    private GroupMessageProducer groupMessageProducer;

    @Resource
    private RedisSeq redisSeq;

    /**
     * 导入群
     */
    @Override
    public ResponseVO importGroup(ImportGroupReq req) {
        LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();

        // 判断群id是否为空，为空的话就自动生成一个
        if (StringUtils.isEmpty(req.getGroupId())) {
            req.setGroupId(UUID.randomUUID().toString().replace("-", ""));
        } else {
            // 查询该group是否存在
            lqw.eq(ImGroupEntity::getGroupId, req.getGroupId());
            lqw.eq(ImGroupEntity::getAppId, req.getAppId());
            Integer count = imGroupMapper.selectCount(lqw);
            if (count > 0) {
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_EXIST);
            }
        }
        // 导入群
        ImGroupEntity group = new ImGroupEntity();
        // 如果公开群且群主为空，报错
        if (req.getGroupType() == GroupTypeEnum.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }

        group.setStatus(GroupStatusEnum.NORMAL.getCode());
        BeanUtils.copyProperties(req, group);

        // 填充数据
        if (req.getCreateTime() == null) {
            group.setCreateTime(System.currentTimeMillis());
        }
        int insert = imGroupMapper.insert(group);

        if (insert != 1) {
            return ResponseVO.errorResponse(GroupErrorCode.IMPORT_GROUP_ERROR);
        }

        return ResponseVO.successResponse();
    }

    /**
     * 新建群
     */
    @Override
    @Transactional
    public ResponseVO createGroup(CreateGroupReq req) {
        boolean flag = false;
        if (!flag) {
            req.setOperater(req.getOperater());
        }
        LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();
        if (StringUtils.isEmpty(req.getGroupId())) {
            req.setGroupId(UUID.randomUUID().toString().replace("-", ""));
        } else {
            lqw.eq(ImGroupEntity::getGroupId, req.getGroupId());
            lqw.eq(ImGroupEntity::getAppId, req.getAppId());
            Integer integer = imGroupMapper.selectCount(lqw);
            if (integer > 0) {
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_EXIST);
            }
        }
        // 公开群需要群主，如果没有则报错
        if (req.getGroupType() == GroupTypeEnum.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }
        ImGroupEntity imGroupEntity = new ImGroupEntity();
        // TODO redis seq
        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Group);

        BeanUtils.copyProperties(req, imGroupEntity);

        imGroupEntity.setCreateTime(System.currentTimeMillis());
        imGroupEntity.setStatus(GroupStatusEnum.NORMAL.getCode());
        int insert = imGroupMapper.insert(imGroupEntity);

        if (insert != 1) {
            return ResponseVO.successResponse(GroupErrorCode.GROUP_CREATE_ERROR);
        }

        // 插入群主
        GroupMemberDto groupMemberDto = new GroupMemberDto();
        groupMemberDto.setMemberId(req.getOwnerId());
        groupMemberDto.setRole(GroupMemberRoleEnum.OWNER.getCode());
        groupMemberDto.setJoinTime(System.currentTimeMillis());
        imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), groupMemberDto);

        // 插入群成员
        for (GroupMemberDto dto : req.getMember()) {
            imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
        }

        // TODO 创建群后回调
        if(appConfig.isCreateGroupAfterCallback()){
            callbackService.callback(req.getAppId(), Constants.CallbackCommand.CreateGroupAfter,
                    JSONObject.toJSONString(imGroupEntity));
        }
        // TODO TCP 通知每个群成员
        CreateGroupPack createGroupPack = new CreateGroupPack();
        BeanUtils.copyProperties(imGroupEntity, createGroupPack);
        createGroupPack.setSequence(seq);
        groupMessageProducer.producer(req.getOperater(), GroupEventCommand.CREATED_GROUP, createGroupPack
                , new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    /**
     * 修改群信息
     */
    @Override
    public ResponseVO updateBaseGroupInfo(UpdateGroupReq req) {
        LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupEntity::getAppId, req.getAppId());
        lqw.eq(ImGroupEntity::getGroupId, req.getGroupId());
        ImGroupEntity groupEntity = imGroupMapper.selectOne(lqw);
        if (groupEntity == null) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }
        // 群组是否解散
        if (groupEntity.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        boolean isAdmin = false;
        if (!isAdmin) {
            ResponseVO<GetRoleInGroupResp> role
                    = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
            if (!role.isOk()) {
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();
            boolean isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode() || roleInfo == GroupMemberRoleEnum.OWNER.getCode();

            if (!isManager && GroupTypeEnum.PUBLIC.getCode() == groupEntity.getGroupType()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }

        ImGroupEntity update = new ImGroupEntity();
        // TODO redis 获取 seq
        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Group);

        BeanUtils.copyProperties(req, update);
        update.setSequence(seq);
        update.setUpdateTime(System.currentTimeMillis());
        int row = imGroupMapper.update(update, lqw);

        if (row != 1) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
        }

        // TODO 回调函数
        if(appConfig.isModifyGroupAfterCallback()){
            callbackService.callback(req.getAppId(), Constants.CallbackCommand.UpdateGroupAfter,
                    JSONObject.toJSONString(imGroupMapper.selectOne(lqw)));
        }
        // TODO TCP 通知
        UpdateGroupInfoPack pack = new UpdateGroupInfoPack();
        BeanUtils.copyProperties(req, pack);
        pack.setSequence(seq);
        groupMessageProducer.producer(req.getOperater(), GroupEventCommand.UPDATED_GROUP,
                pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    /**
     * 获取群信息
     */
    @Override
    public ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId) {
        LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupEntity::getAppId, appId);
        lqw.eq(ImGroupEntity::getGroupId, groupId);
        ImGroupEntity entity = imGroupMapper.selectOne(lqw);
        if (entity == null) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    /**
     * 获取群组具体信息
     */
    @Override
    public ResponseVO getGroupInfo(GetGroupInfoReq req) {
        // 群组是否合法
        ResponseVO<ImGroupEntity> group = this.getGroup(req.getGroupId(), req.getAppId());
        if (!group.isOk()) {
            return group;
        }
        GetGroupResp groupResp = new GetGroupResp();
        BeanUtils.copyProperties(group.getData(), groupResp);
        try {
            ResponseVO<List<GroupMemberDto>> groupMember = imGroupMemberService.getGroupMember(req.getGroupId(), req.getAppId());
            if (!groupMember.isOk()) {
                return groupMember;
            }
            groupResp.setMemberList(groupMember.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseVO.successResponse(groupResp);
    }

    /**
     * 获取用户加入的群组
     */
    @Override
    public ResponseVO getJoinedGroup(GetJoinedGroupReq req) {
        ResponseVO<Collection<String>> memberJoinedGroup
                = imGroupMemberService.getMemberJoinedGroup(req);

        if (CollectionUtils.isEmpty(memberJoinedGroup.getData())) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_LIST_ERROR);
        }
        List<ImGroupEntity> list = new ArrayList<>();

        LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupEntity::getAppId, req.getAppId());
        lqw.in(ImGroupEntity::getGroupId, memberJoinedGroup.getData());
        if(!CollectionUtils.isEmpty(req.getGroupType())) {
            lqw.in(ImGroupEntity::getGroupType, req.getGroupType());
        }
        list = imGroupMapper.selectList(lqw);
        return ResponseVO.successResponse(list);
    }

    /**
     * 解散群组
     */
    @Override
    public ResponseVO destroyGroup(DestroyGroupReq req) {
        boolean isAdmin = false;

        LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupEntity::getGroupId, req.getGroupId());
        lqw.eq(ImGroupEntity::getAppId, req.getAppId());
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(lqw);

        if (imGroupEntity == null) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }

        if (imGroupEntity.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        if (!isAdmin) {
            if (imGroupEntity.getGroupType() == GroupTypeEnum.PUBLIC.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

            if (imGroupEntity.getGroupType() == GroupTypeEnum.PUBLIC.getCode() &&
                    !imGroupEntity.getOwnerId().equals(req.getOperater())) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }

        ImGroupEntity update = new ImGroupEntity();
        // TODO redis seq
        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Group);
        // 群组删除应该是软删除
        update.setStatus(GroupStatusEnum.DESTROY.getCode());

        int update1 = imGroupMapper.update(update, lqw);
        if (update1 != 1) {
            throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }

        // 回调
        if(appConfig.isDestroyGroupAfterCallback()){
            DestroyGroupCallbackDto destroyGroupCallbackDto = new DestroyGroupCallbackDto();
            destroyGroupCallbackDto.setGroupId(req.getGroupId());
            callbackService.callback(req.getAppId(), Constants.CallbackCommand.DestoryGroupAfter,
                    JSONObject.toJSONString(destroyGroupCallbackDto));
        }

        // TCP 通知
        DestroyGroupPack pack = new DestroyGroupPack();
        pack.setGroupId(req.getGroupId());
        pack.setSequence(seq);
        groupMessageProducer.producer(req.getOperater(),
                GroupEventCommand.DESTROY_GROUP, pack, new ClientInfo(req.getAppId()
                        , req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    /**
     * 转让群主
     */
    @Override
    public ResponseVO transferGroup(TransferGroupReq req) {
        // 先判断要转让的用户是不是群主
        ResponseVO<GetRoleInGroupResp> roleInGroupOne
                = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }
        if (roleInGroupOne.getData().getRole() != GroupMemberRoleEnum.OWNER.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
        }
        ResponseVO<GetRoleInGroupResp> newOwnerRole
                = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOwnerId(), req.getAppId());
        if (!newOwnerRole.isOk()) {
            return newOwnerRole;
        }

        // 检查一下群组状态是否正常
        LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupEntity::getAppId, req.getAppId());
        lqw.eq(ImGroupEntity::getGroupId, req.getGroupId());
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(lqw);

        if (imGroupEntity.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        ImGroupEntity updateGroup = new ImGroupEntity();
        updateGroup.setOwnerId(req.getOwnerId());
        lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupEntity::getAppId, req.getAppId());
        lqw.eq(ImGroupEntity::getGroupId, req.getGroupId());
        int update = imGroupMapper.update(updateGroup, lqw);

        if (update != 1) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
        }

        imGroupMemberService.transferGroupMember(req.getOwnerId(), req.getGroupId(), req.getAppId());

        return ResponseVO.successResponse();
    }

    /**
     * 群组禁言
     */
    @Override
    public ResponseVO muteGroup(MuteGroupReq req) {
        ResponseVO<ImGroupEntity> group = this.getGroup(req.getGroupId(), req.getAppId());
        if (!group.isOk()) {
            return group;
        }

        if (group.getData().getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        boolean isAdmin = false;

        if (!isAdmin) {
            ResponseVO<GetRoleInGroupResp> role = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
            if (!role.isOk()) {
                return role;
            }
            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            boolean isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode() || roleInfo == GroupMemberRoleEnum.OWNER.getCode();
            if (!isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }
        ImGroupEntity update = new ImGroupEntity();
        update.setMute(req.getMute());

        LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupEntity::getGroupId, req.getGroupId());
        lqw.eq(ImGroupEntity::getAppId, req.getAppId());
        imGroupMapper.update(update, lqw);

        return ResponseVO.successResponse();
    }

    /**
     * 增量同步群组成员列表
     */
    @Override
    public ResponseVO syncJoinedGroupList(SyncReq req) {
        // 单次拉取最大
        if (req.getMaxLimit() > 100) {
            req.setMaxLimit(100);
        }
        SyncResp<ImGroupEntity> resp = new SyncResp<>();

        // 获取该用户加入的所有的群
        ResponseVO<Collection<String>> collectionResponseVO = imGroupMemberService.syncMemberJoinedGroup(req.getOperater(), req.getAppId());

        if (collectionResponseVO.isOk()) {
            Collection<String> data = collectionResponseVO.getData();
            LambdaQueryWrapper<ImGroupEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(ImGroupEntity::getAppId, req.getAppId());
            lqw.in(ImGroupEntity::getGroupId, data);
            lqw.gt(ImGroupEntity::getSequence, req.getLastSequence());
            lqw.last("limit " + req.getMaxLimit());
            lqw.orderByAsc(ImGroupEntity::getSequence);
            List<ImGroupEntity> list = imGroupMapper.selectList(lqw);

            if(!CollectionUtils.isEmpty(list)) {
                ImGroupEntity maxSeqEntity = list.get(list.size() - 1);
                resp.setDataList(list);

                // TODO 设置最大seq
                Long maxSeq = imGroupMapper.getGroupMaxSeq(data, req.getAppId());
                resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
                return ResponseVO.successResponse(resp);
            }
        }
        resp.setCompleted(true);
        return ResponseVO.successResponse();
    }

    /**
     * 动态获取群组列表中最大的seq
     */
    @Override
    public Long getUserGroupMaxSeq(String userId, Integer appId) {
        // 该用户加入的groupId
        ResponseVO<Collection<String>> memberJoinedGroup
                = imGroupMemberService.syncMemberJoinedGroup(userId, appId);
        if(!memberJoinedGroup.isOk()){
            throw new ApplicationException(500,"");
        }

        // 获取他加入的群组列表中最大的seq
        Long maxSeq =
                imGroupMapper.getGroupMaxSeq(memberJoinedGroup.getData(),
                        appId);
        return maxSeq;
    }
}
