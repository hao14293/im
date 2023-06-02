package com.hao14293.im.service.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.enums.GroupErrorCode;
import com.hao14293.im.common.enums.GroupMemberRoleEnum;
import com.hao14293.im.common.enums.GroupStatusEnum;
import com.hao14293.im.common.enums.GroupTypeEnum;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.service.group.entity.ImGroupEntity;
import com.hao14293.im.service.group.entity.ImGroupMemberEntity;
import com.hao14293.im.service.group.mapper.ImGroupMemberMapper;
import com.hao14293.im.service.group.model.req.*;
import com.hao14293.im.service.group.model.resp.AddMemberResp;
import com.hao14293.im.service.group.model.resp.GetRoleInGroupResp;
import com.hao14293.im.service.group.service.ImGroupMemberService;
import com.hao14293.im.service.group.service.ImGroupService;
import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import com.hao14293.im.service.user.service.ImUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Service
public class ImGroupMemberServiceImpl implements ImGroupMemberService {

    @Resource
    private ImGroupMemberMapper imGroupMemberMapper;

    @Resource
    private ImGroupService imGroupService;

    @Resource
    private ImUserService userService;

    /**
     * 导入群成员
     * @param req
     * @return
     */
    @Override
    public ResponseVO importGroupMember(ImportGroupMemberReq req) {
        return null;
    }

    /**
     * 添加群成员
     * @param groupId
     * @param appId
     * @param dto
     * @return
     */
    @Override
    public ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto) {
        // 判断该成员是否合法
        ResponseVO<ImUserDataEntity> singleUserInfo = userService.getSingleUserInfo(dto.getMemberId(), appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        // 判断群是否已经有群主了
        if (dto.getRole() != null && GroupMemberRoleEnum.OWNER.getCode() == dto.getRole()) {
            LambdaQueryWrapper<ImGroupMemberEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(ImGroupMemberEntity::getGroupId, groupId);
            lqw.eq(ImGroupMemberEntity::getAppId, appId);
            lqw.eq(ImGroupMemberEntity::getRole, GroupMemberRoleEnum.OWNER.getCode());
            Integer integer = imGroupMemberMapper.selectCount(lqw);
            if (integer > 0) {
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_HAVE_OWNER);
            }
        }
        LambdaQueryWrapper<ImGroupMemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupMemberEntity::getGroupId, groupId);
        lqw.eq(ImGroupMemberEntity::getAppId, appId);
        lqw.eq(ImGroupMemberEntity::getMemberId, dto.getMemberId());
        ImGroupMemberEntity imGroupMemberEntity = imGroupMemberMapper.selectOne(lqw);

        long l = System.currentTimeMillis();
        if (imGroupMemberEntity == null) {
            // 如果是空，表示首次加群
            imGroupMemberEntity = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, imGroupMemberEntity);
            imGroupMemberEntity.setGroupId(groupId);
            imGroupMemberEntity.setAppId(appId);
            imGroupMemberEntity.setJoinTime(l);
            int insert = imGroupMemberMapper.insert(imGroupMemberEntity);
            if (insert != 1) {
                return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
            }
            return ResponseVO.successResponse();
        } else if (GroupMemberRoleEnum.LEAVE.getCode() == imGroupMemberEntity.getRole()) {
            // 如果不为空且role为3表示再次加入群
            imGroupMemberEntity = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, imGroupMemberEntity);
            imGroupMemberEntity.setJoinTime(l);
            int insert = imGroupMemberMapper.insert(imGroupMemberEntity);
            if (insert != 1) {
                return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
            }
            return ResponseVO.successResponse();
        }
        return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
    }

    /**
     * 获取群组中某成员的role
     */
    @Override
    public ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId) {
        GetRoleInGroupResp resp = new GetRoleInGroupResp();

        LambdaQueryWrapper<ImGroupMemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupMemberEntity::getAppId, groupId);
        lqw.eq(ImGroupMemberEntity::getGroupId, groupId);
        lqw.eq(ImGroupMemberEntity::getMemberId, memberId);

        ImGroupMemberEntity memberEntity = imGroupMemberMapper.selectOne(lqw);
        if (memberEntity == null || memberEntity.getRole() == GroupMemberRoleEnum.LEAVE.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
        }

        resp.setSpeakDate(memberEntity.getSpeakDate());
        resp.setGroupMemberId(memberEntity.getGroupMemberId());
        resp.setMemberId(memberEntity.getMemberId());
        resp.setRole(memberEntity.getRole());

        return ResponseVO.successResponse(resp);
    }

    /**
     * 获取群组中所有成员信息
     */
    @Override
    public ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId) {
        List<GroupMemberDto> groupMember = imGroupMemberMapper.getGroupMember(appId, groupId);
        return ResponseVO.successResponse(groupMember);
    }

    /**
     * 获取用户加入的所有群的 id
     */
    @Override
    public ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req) {
        return ResponseVO.successResponse(imGroupMemberMapper.getJoinedGroupId(req.getAppId(), req.getMemberId()));
    }

    /**
     * 转让群主
     */
    @Override
    public ResponseVO transferGroupMember(String owner, String groupId, Integer appId) {

        // 更新旧群主的信息，更新群主的角色
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.ORDINARY.getCode());
        LambdaUpdateWrapper<ImGroupMemberEntity> luw = new LambdaUpdateWrapper<>();
        luw.eq(ImGroupMemberEntity::getAppId, appId);
        luw.eq(ImGroupMemberEntity::getGroupId, groupId);
        luw.eq(ImGroupMemberEntity::getMemberId, owner);
        imGroupMemberMapper.update(imGroupMemberEntity, luw);

        // 更新新群主
        ImGroupMemberEntity newOwner = new ImGroupMemberEntity();
        newOwner.setRole(GroupMemberRoleEnum.OWNER.getCode());
        luw = new LambdaUpdateWrapper<>();
        luw.eq(ImGroupMemberEntity::getAppId, appId);
        luw.eq(ImGroupMemberEntity::getGroupId, groupId);
        luw.eq(ImGroupMemberEntity::getMemberId, owner);
        imGroupMemberMapper.update(newOwner, luw);

        return ResponseVO.successResponse();
    }

    /**
     * 拉人入群
     */
    @Override
    public ResponseVO addMember(AddGroupMemberReq req) {
        boolean isAdmin = false;
        // 先判断群组是否存在
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        List<GroupMemberDto> memberDtos = req.getMembers();
        // TODO 回调


        List<AddMemberResp> resp = new ArrayList<>();
        ImGroupEntity group = groupResp.getData();

        if (!isAdmin && GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
        }

        List<String> successId = new ArrayList<>();
        for (GroupMemberDto memberId : memberDtos) {
            ResponseVO responseVO = null;
            try {
                responseVO
                        = this.addGroupMember(req.getGroupId(), req.getAppId(), memberId);
            } catch (Exception e) {
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse();
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(memberId.getMemberId());
            if (responseVO.isOk()) {
                successId.add(memberId.getMemberId());
                addMemberResp.setResult(0);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
                addMemberResp.setResultMessage(responseVO.getMsg());
            } else {
                addMemberResp.setResult(1);
                addMemberResp.setResultMessage(responseVO.getMsg());
            }
            resp.add(addMemberResp);
        }
        // TODO TCP通知

        // TODO 回调

        return ResponseVO.successResponse(resp);
    }

    /**
     * 踢人出群
     */
    @Override
    public ResponseVO removeMember(RemoveGroupMemberReq req) {
        boolean isAdmin = false;

        // 判断群是否存在
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        ImGroupEntity group = groupResp.getData();
        if (!isAdmin) {
            if (GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
                //获取操作人的权限 是管理员or群主or群成员
                ResponseVO<GetRoleInGroupResp> role
                        = getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
                if (!role.isOk()) {
                    return role;
                }

                GetRoleInGroupResp data = role.getData();
                Integer roleInfo = data.getRole();

                boolean isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
                boolean isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode();

                if (!isOwner && !isManager) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                //私有群必须是群主才能踢人
                if (!isOwner && GroupTypeEnum.PRIVATE.getCode() == group.getGroupType()) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }

                //公开群管理员和群主可踢人，但管理员只能踢普通群成员
                if (GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
//                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                    //获取被踢人的权限
                    ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                    if (!roleInGroupOne.isOk()) {
                        return roleInGroupOne;
                    }
                    GetRoleInGroupResp memberRole = roleInGroupOne.getData();
                    if (memberRole.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
                        throw new ApplicationException(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
                    }
                    //是管理员并且被踢人不是群成员，无法操作
                    if (isManager && memberRole.getRole() != GroupMemberRoleEnum.ORDINARY.getCode()) {
                        throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                    }
                }
            }else{
                ResponseVO<GetRoleInGroupResp> role
                        = getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
                if (!role.isOk()) {
                    return role;
                }

                GetRoleInGroupResp data = role.getData();
                Integer roleInfo = data.getRole();

                boolean isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();

                if (!isOwner && GroupTypeEnum.PRIVATE.getCode() == group.getGroupType()) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }
            }
        }
        ResponseVO responseVO
                = this.removeGroupMember(req.getGroupId(), req.getAppId(), req.getMemberId());
        // TODO 回调 TCP通知

        return responseVO;
    }

    /**
     * 踢人出群具体
     */
    @Override
    public ResponseVO removeGroupMember(String groupId, Integer appId, String memberId) {
        // 判断被踢人是否合法
        ResponseVO<ImUserDataEntity> singleUserInfo = userService.getSingleUserInfo(memberId, appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        // 获取被踢人的信息
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(groupId, memberId, appId);
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }

        GetRoleInGroupResp data = roleInGroupOne.getData();

        // 踢人
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.LEAVE.getCode());
        imGroupMemberEntity.setLeaveTime(System.currentTimeMillis());
        imGroupMemberEntity.setGroupMemberId(data.getGroupMemberId());
        imGroupMemberMapper.updateById(imGroupMemberEntity);

        return ResponseVO.successResponse();
    }

    /**
     * 退出群聊
     */
    @Override
    public ResponseVO exitGroup(ExitGroupReq req) {
        // 安全检查
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if(!group.isOk()){
            return group;
        }

        ResponseVO<GetRoleInGroupResp> roleInGroupOne
                = getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
        if(!roleInGroupOne.isOk()){
            return roleInGroupOne;
        }

        ImGroupMemberEntity update = new ImGroupMemberEntity();
        update.setRole(GroupMemberRoleEnum.LEAVE.getCode());

        LambdaQueryWrapper<ImGroupMemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupMemberEntity::getAppId, req.getAppId());
        lqw.eq(ImGroupMemberEntity::getGroupId, req.getGroupId());
        lqw.eq(ImGroupMemberEntity::getMemberId, req.getOperater());
        imGroupMemberMapper.update(update, lqw);

        return ResponseVO.successResponse();
    }

    /**
     * 修改群成员信息
     */
    @Override
    public ResponseVO updateGroupMember(UpdateGroupMemberReq req) {
        boolean isAdmin = false;

        // 安全检查该群成员
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!group.isOk()) {
            return group;
        }

        // 安全检查该群组
        ImGroupEntity groupData = group.getData();
        if (groupData.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        //是否是自己修改自己的资料
        boolean isMeOperate = req.getOperater().equals(req.getMemberId());

        if (!isAdmin) {
            if (StringUtils.isNotBlank(req.getAlias()) && !isMeOperate) {
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_ONESELF);
            }

            if (req.getRole() != null) {
                if (groupData.getGroupType() == GroupTypeEnum.PRIVATE.getCode() &&
                        req.getRole() != null && (req.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() ||
                        req.getRole() == GroupMemberRoleEnum.OWNER.getCode())) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
                }

                //获取被操作人的是否在群内
                ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                if (!roleInGroupOne.isOk()) {
                    return roleInGroupOne;
                }

                //获取操作人权限
                ResponseVO<GetRoleInGroupResp> operateRoleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
                if (!operateRoleInGroupOne.isOk()) {
                    return operateRoleInGroupOne;
                }

                GetRoleInGroupResp data = operateRoleInGroupOne.getData();
                Integer roleInfo = data.getRole();
                boolean isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
                boolean isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode();

                //不是管理员不能修改权限
                if (req.getRole() != null && !isOwner && !isManager) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                //管理员只有群主能够设置
                if (req.getRole() != null && req.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() && !isOwner) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }
            }
        }
        ImGroupMemberEntity update = new ImGroupMemberEntity();

        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(req.getAlias())) {
            update.setAlias(req.getAlias());
        }

        // 不能直接修改为群主
        if(req.getRole() != null && req.getRole() != GroupMemberRoleEnum.OWNER.getCode()){
            update.setRole(req.getRole());
        }

        LambdaQueryWrapper<ImGroupMemberEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImGroupMemberEntity::getAppId, req.getAppId());
        lqw.eq(ImGroupMemberEntity::getMemberId, req.getMemberId());
        lqw.eq(ImGroupMemberEntity::getGroupId, req.getGroupId());
        imGroupMemberMapper.update(update, lqw);

        //TODO TCP

        return ResponseVO.successResponse();
    }

    /**
     * 禁言群成员
     */
    @Override
    public ResponseVO speak(SpeakMemberReq req) {
        // 判断群组是否合法
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        boolean isAdmin = false;
        boolean isOwner = false;
        boolean isManager = false;
        GetRoleInGroupResp memberRole = null;

        if (!isAdmin) {
            //获取操作人的权限 是管理员or群主or群成员
            ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
            if (!role.isOk()) {
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
            isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode();

            if (!isOwner && !isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

            //获取被操作的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }

            memberRole = roleInGroupOne.getData();
            //被操作人是群主只能app管理员操作
            if (memberRole.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
            }

            //是管理员并且被操作人不是群成员，无法操作
            if (isManager && memberRole.getRole() != GroupMemberRoleEnum.ORDINARY.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        if (memberRole == null) {
            //获取被操作的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            memberRole = roleInGroupOne.getData();
        }

        imGroupMemberEntity.setGroupMemberId(memberRole.getGroupMemberId());

        if (req.getSpeakDate() > 0) {
            imGroupMemberEntity.setSpeakDate(System.currentTimeMillis() + req.getSpeakDate());
        } else {
            imGroupMemberEntity.setSpeakDate(req.getSpeakDate());
        }

        int i = imGroupMemberMapper.updateById(imGroupMemberEntity);

        // TODO 回调

        return ResponseVO.successResponse();

    }

    /**
     * 获取指定群的所有群成员ID
     */
    @Override
    public List<String> getGroupMemberId(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupMemberId(appId, groupId);
    }

    /**
     * 获取群组中的管理员具体信息
     */
    @Override
    public List<GroupMemberDto> getGroupManager(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupManager(groupId, appId);
    }

    /**
     * 增量同步用户加入的群组
     */
    @Override
    public ResponseVO<Collection<String>> syncMemberJoinedGroup(String operater, Integer appId) {
        return ResponseVO.successResponse(imGroupMemberMapper.syncJoinedGroupId(appId, operater, GroupMemberRoleEnum.LEAVE.getCode()));
    }
}
