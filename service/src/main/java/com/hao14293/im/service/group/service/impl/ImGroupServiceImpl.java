package com.hao14293.im.service.group.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.enums.GroupErrorCode;
import com.hao14293.im.common.enums.GroupMemberRoleEnum;
import com.hao14293.im.common.enums.GroupStatusEnum;
import com.hao14293.im.common.enums.GroupTypeEnum;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.service.group.entity.ImGroupEntity;
import com.hao14293.im.service.group.mapper.ImGroupMapper;
import com.hao14293.im.service.group.model.req.CreateGroupReq;
import com.hao14293.im.service.group.model.req.GroupMemberDto;
import com.hao14293.im.service.group.model.req.ImportGroupReq;
import com.hao14293.im.service.group.service.ImGroupMemberService;
import com.hao14293.im.service.group.service.ImGroupService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

        // TODO TCP 通知每个群成员

        return ResponseVO.successResponse();
    }
}
