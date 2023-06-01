package com.hao14293.im.service.user.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import com.hao14293.im.service.user.model.req.*;
import com.hao14293.im.service.user.model.resp.GetUserInfoResp;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public interface ImUserService {
    /**
     * 导入用户
     */
    ResponseVO importUser(ImportUserReq userReq);

    /**
     * 批量获取用户信息
     */
    ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    /**
     * 获取单个用户信息
     */
    ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId);
    /**
     * 登录
     */
    ResponseVO login(LoginReq req);

    /**
     * 批量删除用户信息
     */
    ResponseVO deleteUser(DeleteUserReq req);

    /**
     * 修改用户信息
     */
    ResponseVO modifyUserInfo(ModifyUserInfoReq req);

    ResponseVO getUserSequence(GetUserSequenceReq req);
}
