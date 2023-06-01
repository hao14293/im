package com.hao14293.im.service.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.enums.GroupErrorCode;
import com.hao14293.im.common.enums.GroupMemberRoleEnum;
import com.hao14293.im.service.group.entity.ImGroupMemberEntity;
import com.hao14293.im.service.group.mapper.ImGroupMemberMapper;
import com.hao14293.im.service.group.model.req.GroupMemberDto;
import com.hao14293.im.service.group.model.req.ImportGroupMemberReq;
import com.hao14293.im.service.group.service.ImGroupMemberService;
import com.hao14293.im.service.group.service.ImGroupService;
import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import com.hao14293.im.service.user.service.ImUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
}
