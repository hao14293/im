package com.hao14293.im.service.user.model.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hao14293.im.codec.pack.user.UserModifyPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.UserEventCommand;
import com.hao14293.im.common.enums.DelFlagEnum;
import com.hao14293.im.common.enums.UserErrorCode;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.service.group.service.ImGroupService;
import com.hao14293.im.service.user.mapper.ImUserDataMapper;
import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import com.hao14293.im.service.user.model.req.*;
import com.hao14293.im.service.user.model.resp.DeleteUserResp;
import com.hao14293.im.service.user.model.resp.GetUserInfoResp;
import com.hao14293.im.service.user.model.resp.ImportUserResp;
import com.hao14293.im.service.user.service.ImUserService;
import com.hao14293.im.service.utils.CallbackService;
import com.hao14293.im.service.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Service
public class ImUserServiceImpl implements ImUserService {

    @Resource
    private ImUserDataMapper imUserDataMapper;

    @Resource
    private AppConfig appConfig;

    @Resource
    private CallbackService callbackService;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ImGroupService imGroupService;

    /**
     * 导入用户
     */
    @Override
    public ResponseVO importUser(ImportUserReq req) {
        if (req.getUserData().size() > 100) {
            return ResponseVO.errorResponse(UserErrorCode.IMPORT_SIZE_BEYOND);
        }

        // 将导入成功的和导入失败的返回给客户端
        List<String> successId = new ArrayList<>();
        List<String> errorId= new ArrayList<>();

        // 遍历，填充成功和失败列表
        req.getUserData().forEach(e -> {
            try {
                e.setAppId(req.getAppId());
                int insert = imUserDataMapper.insert(e);
                if (insert == 1) {
                    successId.add(e.getUserId());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                errorId.add(e.getUserId());
            }
        });

        // 定义返回的类
        ImportUserResp importUserResp = new ImportUserResp();
        importUserResp.setSuccessId(successId);
        importUserResp.setErrorId(errorId);

        return ResponseVO.successResponse(importUserResp);
    }

    /**
     * 批量获取用户信息
     */
    @Override
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req) {
        LambdaQueryWrapper<ImUserDataEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImUserDataEntity::getAppId, req.getAppId());
        lqw.in(ImUserDataEntity::getUserId, req.getUserIds());
        lqw.eq(ImUserDataEntity::getDelFlag, DelFlagEnum.NORMAL.getCode());

        List<ImUserDataEntity> imUserDataEntities = imUserDataMapper.selectList(lqw);

        // 过滤出查询失败的用户一并返回
        HashMap<String, ImUserDataEntity> map = new HashMap<>();
        for (ImUserDataEntity data : imUserDataEntities) {
            map.put(data.getUserId(), data);
        }
        // 通过Map的containsKey 来判断没有在查询成功的用户，将他们添加到查询失败的用户中
        List<String> failUser = new ArrayList<>();
        for (String userId : req.getUserIds()) {
            if (!map.containsKey(userId)) {
                failUser.add(userId);
            }
        }
        GetUserInfoResp resp = new GetUserInfoResp();
        resp.setFailUser(failUser);
        resp.setUserDataItem(imUserDataEntities);

        return ResponseVO.successResponse(resp);
    }

    /**
     * 获取单个用户信息
     */
    @Override
    public ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId) {
        LambdaQueryWrapper<ImUserDataEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImUserDataEntity::getAppId, appId);
        lqw.eq(ImUserDataEntity::getUserId, userId);
        lqw.eq(ImUserDataEntity::getDelFlag, DelFlagEnum.NORMAL.getCode());

        ImUserDataEntity userData = imUserDataMapper.selectOne(lqw);
        if (userData == null) {
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(userData);
    }

    @Override
    public ResponseVO login(LoginReq req) {
        return ResponseVO.successResponse();
    }

    /**
     * 批量删除用户
     * 这里删除是逻辑删除，修改DelFlag为1
     */
    @Override
    public ResponseVO deleteUser(DeleteUserReq req) {
        ImUserDataEntity data = new ImUserDataEntity();
        data.setDelFlag(DelFlagEnum.DELETE.getCode());

        // 删除成功和失败的返回给客户端
        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();

        for (String userId : req.getUserIds()) {
            // 使用update 修改delFlag
            LambdaQueryWrapper<ImUserDataEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(ImUserDataEntity::getAppId, req.getAppId());
            lqw.in(ImUserDataEntity::getUserId, req.getUserIds());
            lqw.eq(ImUserDataEntity::getDelFlag, DelFlagEnum.NORMAL.getCode());

            int update = 0;
            try {
                update = imUserDataMapper.update(data, lqw);
                if (update > 0) {
                    successId.add(userId);
                } else {
                    errorId.add(userId);
                }
            } catch (Exception e) {
                errorId.add(userId);
            }
        }
        DeleteUserResp resp = new DeleteUserResp();
        resp.setErrorId(errorId);
        resp.setSuccessId(successId);

        return ResponseVO.successResponse(resp);
    }

    @Override
    @Transactional
    public ResponseVO modifyUserInfo(ModifyUserInfoReq req) {
        // 先查询要修改的用户是否合法
        LambdaQueryWrapper<ImUserDataEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImUserDataEntity::getAppId, req.getAppId());
        lqw.eq(ImUserDataEntity::getUserId, req.getUserId());
        lqw.eq(ImUserDataEntity::getDelFlag, DelFlagEnum.NORMAL.getCode());

        ImUserDataEntity data = imUserDataMapper.selectOne(lqw);

        if (data == null) {
            throw new ApplicationException(UserErrorCode.USER_IS_NOT_EXIST);
        }
        ImUserDataEntity update = new ImUserDataEntity();
        BeanUtils.copyProperties(req, update);

        // 不能修改appId 和 userId
        update.setAppId(null);
        update.setUserId(null);
        int update1 = imUserDataMapper.update(update, lqw);

        if (update1 == 1) {

            // 通知
            UserModifyPack userModifyPack = new UserModifyPack();
            BeanUtils.copyProperties(req, userModifyPack);
            messageProducer.sendToUser(req.getUserId(), req.getClientType(), req.getImei(),
                    UserEventCommand.USER_MODIFY, userModifyPack, req.getAppId());

            // 回调
            if(appConfig.isModifyUserAfterCallback()){
                // 开启修改用户信息回调
                callbackService.callback(req.getAppId(), Constants.CallbackCommand.ModifyUserAfter
                        , JSONObject.toJSONString(req));
            }

            return ResponseVO.successResponse();
        }
        throw  new ApplicationException(UserErrorCode.MODIFY_USER_ERROR);
    }

    @Override
    public ResponseVO getUserSequence(GetUserSequenceReq req) {
        // 这里的map中有  好友关系的 好友申请的  会话的，没有群组的，因为之前设计的时候，
        // 就考虑到如果一个群组里面的任何成员，发生了修改都会去修改
        // 该群的seq值，每次修改都要去redis中去更新seq，太繁琐了
        // 但是我觉得可以用那个Redis绝对自增序列的那个，不用非得从数据库中获取最新的，可能是数据库的比redis中的更加精准，
        // 我这个想法有待考量
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(
                req.getAppId() + ":" + Constants.RedisConstants.SeqPrefix + ":" + req.getUserId());
        Long groupSeq = imGroupService.getUserGroupMaxSeq(req.getUserId(), req.getAppId());
        map.put(Constants.SeqConstants.Group, groupSeq);
        return ResponseVO.successResponse(map);
    }
}
