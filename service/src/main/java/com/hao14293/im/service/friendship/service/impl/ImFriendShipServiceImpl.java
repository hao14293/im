package com.hao14293.im.service.friendship.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hao14293.im.codec.pack.friendship.AddFriendBlackPack;
import com.hao14293.im.codec.pack.friendship.AddFriendPack;
import com.hao14293.im.codec.pack.friendship.DeleteFriendPack;
import com.hao14293.im.codec.pack.friendship.UpdateFriendPack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.AllowFriendTypeEnum;
import com.hao14293.im.common.enums.CheckFriendShipTypeEnum;
import com.hao14293.im.common.enums.Command.FriendshipEventCommand;
import com.hao14293.im.common.enums.FriendShipErrorCode;
import com.hao14293.im.common.enums.FriendShipStatusEnum;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.common.model.RequestBase;
import com.hao14293.im.common.model.SyncReq;
import com.hao14293.im.common.model.SyncResp;
import com.hao14293.im.service.friendship.entity.ImFriendShipEntity;
import com.hao14293.im.service.friendship.mapper.ImFriendShipMapper;
import com.hao14293.im.service.friendship.model.callback.AddFriendAfterCallbackDto;
import com.hao14293.im.service.friendship.model.callback.AddFriendBlackAfterCallbackDto;
import com.hao14293.im.service.friendship.model.callback.DeleteFriendAfterCallbackDto;
import com.hao14293.im.service.friendship.model.req.*;
import com.hao14293.im.service.friendship.model.resp.CheckFriendShipResp;
import com.hao14293.im.service.friendship.model.resp.ImportFriendShipResp;
import com.hao14293.im.service.friendship.service.ImFriendShipRequestService;
import com.hao14293.im.service.friendship.service.ImFriendShipService;
import com.hao14293.im.service.seq.RedisSeq;
import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import com.hao14293.im.service.user.service.ImUserService;
import com.hao14293.im.service.utils.CallbackService;
import com.hao14293.im.service.utils.MessageProducer;
import com.hao14293.im.service.utils.WriteUserSeq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Service
public class ImFriendShipServiceImpl implements ImFriendShipService {

    @Resource
    private ImFriendShipMapper imFriendShipMapper;

    @Resource
    private ImUserService imUserService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private CallbackService callbackService;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private ImFriendShipRequestService imFriendShipRequestService;

    @Resource
    private WriteUserSeq writeUserSeq;

    @Resource
    private RedisSeq redisSeq;

    @Override
    public ResponseVO importFriendShip(ImportFriendShipReq req) {

        // 导入数据数量的限制
        if(req.getFriendItem().size() > 100){
            return ResponseVO.errorResponse(FriendShipErrorCode.IMPORT_SIZE_BEYOND);
        }
        List<String> errorId = new ArrayList<>();
        List<String> successId = new ArrayList<>();

        for (ImportFriendShipReq.ImportFriendDto dto : req.getFriendItem()) {
            // 数据填充
            ImFriendShipEntity entity = new ImFriendShipEntity();
            BeanUtils.copyProperties(dto, entity);
            entity.setAppId(req.getAppId());
            entity.setFromId(req.getFromId());

            try {
                int insert = imFriendShipMapper.insert(entity);
                if(insert == 1){
                    successId.add(dto.getToId());
                }else{
                    errorId.add(dto.getToId());
                }
            }catch (Exception e){
                e.printStackTrace();
                errorId.add(dto.getToId());
            }
        }
        ImportFriendShipResp resp = new ImportFriendShipResp();
        resp.setSuccessId(successId);
        resp.setErrorId(errorId);

        return ResponseVO.successResponse(resp);
    }

    /**
     * 添加好友
     */
    @Override
    public ResponseVO addFriendShip(AddFriendShipReq req) {

        ResponseVO<ImUserDataEntity> fromInfo
                = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()){
            return fromInfo;
        }

        ResponseVO<ImUserDataEntity> toInfo
                = imUserService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if(!toInfo.isOk()){
            return toInfo;
        }

        // TODO 回调
        if(appConfig.isAddFriendBeforeCallback()){
            ResponseVO beforecallback
                    = callbackService.beforecallback(req.getAppId(), Constants.CallbackCommand.AddFriendBefore,
                    JSONObject.toJSONString(req));
            if(!beforecallback.isOk()){
                return beforecallback;
            }
        }

        ImUserDataEntity data = toInfo.getData();

        // 加好友不需要审批
        if (data.getFriendAllowType() != null &&
            data.getFriendAllowType() == AllowFriendTypeEnum.NOT_NEED.getCode()){
            return this.doAddFriend(req, req.getFromId(), req.getToItem(), req.getAppId());
        } else {
            LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(ImFriendShipEntity::getAppId, req.getAppId());
            lqw.eq(ImFriendShipEntity::getFromId, req.getFromId());
            lqw.eq(ImFriendShipEntity::getToId, req.getToItem().getToId());
            ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(lqw);
            // 不是好友
            if (fromItem == null || fromItem.getStatus() !=
                    FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                // 插入一条好友申请数据
                ResponseVO responseVO
                        = imFriendShipRequestService.addFriendshipRequest(req.getFromId(), req.getToItem(), req.getAppId());
                if(!responseVO.isOk()){
                    return responseVO;
                }
            } else {
                // 已经是好友
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            }
        }
        return ResponseVO.successResponse();
    }

    /**
     * 添加好友具体逻辑
     */
    @Override
    @Transactional
    public ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId) {
        // Friend 表中要插入 A 和 B 两条数据
        // A --> B   B --> A

        // A ---> B
        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getAppId, appId);
        lqw.eq(ImFriendShipEntity::getFromId, fromId);
        lqw.eq(ImFriendShipEntity::getToId, dto.getToId());
        ImFriendShipEntity entity = imFriendShipMapper.selectOne(lqw);

        long seq = 0L;
        if (entity == null) {
            entity = new ImFriendShipEntity();
            seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);
            entity.setAppId(appId);
            entity.setFromId(fromId);
            entity.setFriendSequence(seq);
            BeanUtils.copyProperties(dto, entity);
            entity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            entity.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(entity);
            if (insert != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSeq.writeUserSeq(appId, fromId, Constants.SeqConstants.Friendship, seq);
        } else {
            // 存在好友关系，需要判断状态
            if (entity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            } else {
                ImFriendShipEntity update = new ImFriendShipEntity();
                if (StringUtils.isNotEmpty(dto.getAddSource())) {
                    update.setAddSource(dto.getAddSource());
                }
                if (StringUtils.isNotEmpty(dto.getRemark())) {
                    update.setRemark(dto.getRemark());
                }
                if (StringUtils.isNotEmpty(dto.getExtra())) {
                    update.setExtra(dto.getExtra());
                }
                seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);

                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());

                int res = imFriendShipMapper.update(update, lqw);
                if (res != 1) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                }
                writeUserSeq.writeUserSeq(appId, fromId, Constants.SeqConstants.Friendship, seq);
            }
        }

        // B ---> A
        LambdaQueryWrapper<ImFriendShipEntity> lqw1 = new LambdaQueryWrapper<>();
        lqw1.eq(ImFriendShipEntity::getAppId, appId);
        lqw1.eq(ImFriendShipEntity::getFromId, dto.getToId());
        lqw1.eq(ImFriendShipEntity::getToId, fromId);
        ImFriendShipEntity entity1 = imFriendShipMapper.selectOne(lqw1);

        if(entity1 == null){
            entity1 = new ImFriendShipEntity();
            entity1.setAppId(appId);
            entity1.setFromId(dto.getToId());
            BeanUtils.copyProperties(dto, entity1);
            entity1.setToId(fromId);
            entity1.setFriendSequence(seq);
            entity1.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            entity1.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(entity1);
            if(insert != 1){
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSeq.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.Friendship, seq);
        }else{
            // 存在就判断状态
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != entity1.getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            }else{
                ImFriendShipEntity entity2 = new ImFriendShipEntity();
                entity2.setFriendSequence(seq);
                entity2.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                imFriendShipMapper.update(entity2, lqw1);
                writeUserSeq.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.Friendship, seq);
            }
        }
        // TODO TCP通知
        // A B 添加好友，要把添加好友的信息，发送给除了A其他的端，还要发送给B的所有端
        AddFriendPack addFriendPack = new AddFriendPack();
        BeanUtils.copyProperties(entity, addFriendPack);
        addFriendPack.setSequence(seq);
        if(requestBase != null){
            messageProducer.sendToUser(fromId, requestBase.getClientType(), requestBase.getImei(),
                    FriendshipEventCommand.FRIEND_ADD, addFriendPack, requestBase.getAppId());
        }else{
            messageProducer.sendToUser(fromId,
                    FriendshipEventCommand.FRIEND_ADD, addFriendPack, requestBase.getAppId());
        }

        // 发送给to
        AddFriendPack addFriendToPack = new AddFriendPack();
        BeanUtils.copyProperties(entity1, addFriendToPack);
        messageProducer.sendToUser(entity1.getFromId(), FriendshipEventCommand.FRIEND_ADD, addFriendToPack,
                requestBase.getAppId());

        if(appConfig.isDestroyGroupAfterCallback()){
            AddFriendAfterCallbackDto addFriendAfterCallbackDto = new AddFriendAfterCallbackDto();
            addFriendAfterCallbackDto.setFromId(fromId);
            addFriendAfterCallbackDto.setToItem(dto);
            callbackService.callback(appId, Constants.CallbackCommand.AddFriendAfter,
                    JSONObject.toJSONString(addFriendAfterCallbackDto));
        }
        return ResponseVO.successResponse();
    }

    /**
     * 更新好友关系
     */
    @Override
    public ResponseVO updateFriend(UpdateFriendReq req) {
        ResponseVO<ImUserDataEntity> fromInfo
                = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()){
            return fromInfo;
        }
        ResponseVO<ImUserDataEntity> toInfo
                = imUserService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if(!toInfo.isOk()){
            return toInfo;
        }
        ResponseVO responseVO = doUpateFriend(req.getFromId(), req.getToItem(), req.getAppId());

        if (responseVO.isOk()){

            // TCP通知
            UpdateFriendPack updateFriendPack = new UpdateFriendPack();
            updateFriendPack.setFromId(req.getFromId());
            updateFriendPack.setRemark(req.getToItem().getRemark());
            updateFriendPack.setToId(req.getToItem().getToId());
            messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(),
                    FriendshipEventCommand.FRIEND_UPDATE, updateFriendPack, req.getAppId());

            // 添加回调
            if(appConfig.isModifyGroupAfterCallback()) {
                AddFriendAfterCallbackDto addFriendAfterCallbackDto = new AddFriendAfterCallbackDto();
                addFriendAfterCallbackDto.setToItem(req.getToItem());
                addFriendAfterCallbackDto.setFromId(req.getFromId());
                callbackService.beforecallback(req.getAppId(), Constants.CallbackCommand.UpdateGroupAfter,
                        JSONObject.toJSONString(addFriendAfterCallbackDto));
            }

        }

        return ResponseVO.successResponse();
    }

    // 修改好友关系的逻辑
    @Transactional
    public ResponseVO doUpateFriend(String fromId, FriendDto dto, Integer appId){

        long seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.Friendship);
        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(ImFriendShipEntity::getAddSource, dto.getAddSource())
                .set(ImFriendShipEntity::getExtra, dto.getExtra())
                .set(ImFriendShipEntity::getFriendSequence, seq)
                .set(ImFriendShipEntity::getRemark, dto.getRemark())
                .eq(ImFriendShipEntity::getAppId, appId)
                .eq(ImFriendShipEntity::getToId, dto.getToId())
                .eq(ImFriendShipEntity::getFromId, fromId);

        int update = imFriendShipMapper.update(null, updateWrapper);
        if(update == 1){
            if(appConfig.isModifyGroupAfterCallback()){
                AddFriendAfterCallbackDto addFriendAfterCallbackDto = new AddFriendAfterCallbackDto();
                addFriendAfterCallbackDto.setToItem(dto);
                addFriendAfterCallbackDto.setFromId(fromId);
                callbackService.beforecallback(appId, Constants.CallbackCommand.UpdateGroupAfter,
                        JSONObject.toJSONString(addFriendAfterCallbackDto));
            }
            writeUserSeq.writeUserSeq(appId, fromId, Constants.SeqConstants.Friendship, seq);
            return ResponseVO.successResponse();
        }
        return ResponseVO.errorResponse();
    }

    /**
     * 删除好友关系
     */
    @Override
    public ResponseVO deleteFriend(DeleteFriendReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getAppId, req.getAppId());
        lqw.eq(ImFriendShipEntity::getFromId, req.getFromId());
        lqw.eq(ImFriendShipEntity::getToId, req.getToId());

        ImFriendShipEntity entity = imFriendShipMapper.selectOne(lqw);

        if(entity == null){
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }else {
            if (entity.getStatus() != null
                    && entity.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                ImFriendShipEntity update = new ImFriendShipEntity();
                long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
                // 逻辑删除
                imFriendShipMapper.update(update, lqw);
                writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.Friendship, seq);

                 // TCP通知
                 DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
                 deleteFriendPack.setFromId(req.getFromId());
                 deleteFriendPack.setToId(req.getToId());
                 deleteFriendPack.setSequence(seq);
                 messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(),
                 FriendshipEventCommand.FRIEND_DELETE, deleteFriendPack, req.getAppId());

                 // 回调
                 if(appConfig.isDeleteFriendAfterCallback()){
                 DeleteFriendAfterCallbackDto deleteFriendAfterCallbackDto = new DeleteFriendAfterCallbackDto();
                 deleteFriendAfterCallbackDto.setFromId(req.getFromId());
                 deleteFriendAfterCallbackDto.setToId(req.getToId());
                 callbackService.callback(req.getAppId(), Constants.CallbackCommand.DeleteFriendAfter,
                 JSONObject.toJSONString(deleteFriendAfterCallbackDto));
                 }
            } else {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }
        }
        return ResponseVO.successResponse();
    }

    /**
     * 删除所有好友
     */
    @Override
    public ResponseVO deleteAllFriend(DeleteFriendReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getAppId, req.getAppId());
        lqw.eq(ImFriendShipEntity::getFromId, req.getFromId());
        lqw.eq(ImFriendShipEntity::getStatus, FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());

        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
        imFriendShipMapper.update(update, lqw);

         // TCP通知
        DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
        deleteFriendPack.setToId(req.getToId());
        deleteFriendPack.setFromId(req.getFromId());
        messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(),
        FriendshipEventCommand.FRIEND_ALL_DELETE, deleteFriendPack, req.getAppId());

        return ResponseVO.successResponse();
    }

    /**
     * 拉取指定好友信息
     */
    @Override
    public ResponseVO getRelation(GetRelationReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getAppId, req.getAppId());
        lqw.eq(ImFriendShipEntity::getFromId, req.getFromId());
        lqw.eq(ImFriendShipEntity::getToId, req.getToId());
        ImFriendShipEntity entity = imFriendShipMapper.selectOne(lqw);
        if(entity == null){
            return ResponseVO.errorResponse(FriendShipErrorCode.REPEATSHIP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    /**
     * 拉取所有好友信息
     */
    @Override
    public ResponseVO getAllFriend(GetAllFriendShipReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getAppId, req.getAppId());
        lqw.eq(ImFriendShipEntity::getFromId, req.getFromId());
        List<ImFriendShipEntity> lists = imFriendShipMapper.selectList(lqw);
        return ResponseVO.successResponse(lists);
    }

    /**
     * 校验好友关系
     */
    @Override
    public ResponseVO checkFriendShip(CheckFriendShipReq req) {
        // 双向校验的修改
        // 1、先是把req中的所有的toIds都转化为key为属性，value为0的map
        Map<String, Integer> result
                = req.getToIds().stream().collect(Collectors.toMap(Function.identity(), s-> 0));

        List<CheckFriendShipResp> resp = new ArrayList<>();

        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()){
            resp = imFriendShipMapper.checkFriendShip(req);
        }else{
            resp = imFriendShipMapper.checkFriendShipBoth(req);
        }

        // 2、将复杂sql查询出来的数据转换为map
        Map<String, Integer> collect = resp.stream()
                .collect(Collectors.toMap(CheckFriendShipResp::getToId,
                        CheckFriendShipResp::getStatus));

        // 3、最后比对之前result中和collect是否完全相同，collect中没有的话，就将这个数据封装起来放到resp中去
        for (String toId : result.keySet()){
            if(!collect.containsKey(toId)){
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setStatus(result.get(toId));
                resp.add(checkFriendShipResp);
            }
        }

        return ResponseVO.successResponse(resp);
    }

    /**
     * 加入黑名单
     */
    @Override
    public ResponseVO addFriendShipBlack(AddFriendShipBlackReq req) {
        ResponseVO<ImUserDataEntity> fromUser
                = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromUser.isOk()){
            return fromUser;
        }

        ResponseVO<ImUserDataEntity> toUser
                = imUserService.getSingleUserInfo(req.getToId(), req.getAppId());

        if(!fromUser.isOk()){
            return toUser;
        }
        return doAddFriendSipBlack(req.getFromId(), req.getToId(), req.getAppId(), req);
    }

    /**
     * 加入黑名单逻辑
     */
    @Transactional
    public ResponseVO doAddFriendSipBlack(String fromId, String toId, Integer appid, AddFriendShipBlackReq req){

        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getAppId, appid);
        lqw.eq(ImFriendShipEntity::getFromId, fromId);
        lqw.eq(ImFriendShipEntity::getToId, toId);

        ImFriendShipEntity entity = imFriendShipMapper.selectOne(lqw);

        Long seq = 0L;
        // 没有添加好友呢，就直接拉黑名单了
        if(entity == null){
            seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
            entity = new ImFriendShipEntity();
            entity.setFromId(fromId);
            entity.setToId(toId);
            entity.setFriendSequence(seq);
            entity.setAppId(appid);
            entity.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
            entity.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(entity);
            if(insert != 1){
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.Friendship, seq);
        }else{
            // 如果存在那么就判断，是否已经被拉入黑名单了
            if(entity.getBlack() != null && entity.getBlack() == FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
            }else{
                seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setFriendSequence(seq);
                update.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
                int update1 = imFriendShipMapper.update(update, lqw);
                if(update1 != 1){
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_BLACK_ERROR);
                }
                writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.Friendship, seq);
            }
        }

        // TCP通知
        AddFriendBlackPack addFriendBlackPack = new AddFriendBlackPack();
        addFriendBlackPack.setFromId(req.getFromId());
        addFriendBlackPack.setToId(req.getToId());
        addFriendBlackPack.setSequence(seq);
        messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(),
                FriendshipEventCommand.FRIEND_BLACK_ADD, addFriendBlackPack, req.getAppId());

        // 回调
        if(appConfig.isAddFriendShipBlackAfterCallback()){
            AddFriendBlackAfterCallbackDto deleteFriendAfterCallbackDto = new AddFriendBlackAfterCallbackDto();
            deleteFriendAfterCallbackDto.setToId(req.getToId());
            deleteFriendAfterCallbackDto.setFromId(req.getFromId());
            callbackService.callback(req.getAppId(), Constants.CallbackCommand.AddBlackAfter,
                    JSONObject.toJSONString(deleteFriendAfterCallbackDto));
        }

        return ResponseVO.successResponse();
    }

    /**
     * 移除黑名单
     */
    @Override
    public ResponseVO deleteFriendShipBlack(DeleteBlackReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getFromId, req.getFromId());
        lqw.eq(ImFriendShipEntity::getAppId, req.getAppId());
        lqw.eq(ImFriendShipEntity::getToId, req.getToId());
        ImFriendShipEntity entity = imFriendShipMapper.selectOne(lqw);

        if(entity.getBlack() != null && entity.getBlack() == FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()){
            throw new ApplicationException(FriendShipErrorCode.FRIEND_IS_NOT_YOUR_BLACK);
        }

        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.Friendship);
        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setBlack(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
        update.setFriendSequence(seq);

        int update1 = imFriendShipMapper.update(update, lqw);

        if(update1 == 1){
            writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.Friendship, seq);
            // TCP通知
            DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
            deleteFriendPack.setFromId(req.getFromId());
            deleteFriendPack.setToId(req.getToId());
            deleteFriendPack.setSequence(seq);
            messageProducer.sendToUser(req.getFromId(), req.getClientType(), req.getImei(),
                    FriendshipEventCommand.FRIEND_BLACK_DELETE, deleteFriendPack, req.getAppId());

            // 回调
            if(appConfig.isDeleteFriendShipBlackAfterCallback()){
                AddFriendBlackAfterCallbackDto deleteFriendAfterCallbackDto = new AddFriendBlackAfterCallbackDto();
                deleteFriendAfterCallbackDto.setFromId(req.getFromId());
                deleteFriendAfterCallbackDto.setToId(req.getToId());
                callbackService.callback(req.getAppId(), Constants.CallbackCommand.DeleteBlack,
                        JSONObject.toJSONString(deleteFriendAfterCallbackDto));
            }
            return ResponseVO.successResponse();
        }

        return ResponseVO.errorResponse();
    }

    /**
     * 校验黑名单
     */
    @Override
    public ResponseVO checkFriendBlack(CheckFriendShipReq req) {
        Map<String, Integer> toIdMap
                = req.getToIds().stream().collect(Collectors.toMap(Function.identity(),s -> 0));

        List<CheckFriendShipResp> resp = new ArrayList<>();

        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()){
            resp = imFriendShipMapper.checkFriendShipBlack(req);
        }else {
            resp = imFriendShipMapper.checkFriendShipBlackBoth(req);
        }
        Map<String, Integer> collect
                = resp.stream().collect(Collectors.toMap(CheckFriendShipResp::getToId, CheckFriendShipResp::getStatus));

        for (String toId : toIdMap.keySet()) {
            if(!collect.containsKey(toId)){
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setStatus(toIdMap.get(toId));
                resp.add(checkFriendShipResp);
            }
        }
        return ResponseVO.successResponse(resp);
    }

    /**
     * 同步好友列表信息,增量拉取
     */
    @Override
    public ResponseVO syncFriendShipList(SyncReq req) {
        // 单次最大拉取数量
        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }

        // 返回体
        SyncResp<ImFriendShipEntity> resp = new SyncResp<>();
        // seq > req.getseq limit maxlimit
        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getFromId, req.getOperater());
        lqw.gt(ImFriendShipEntity::getFriendSequence, req.getLastSequence());
        lqw.eq(ImFriendShipEntity::getAppId, req.getAppId());
        lqw.last("limit " + req.getMaxLimit());
        lqw.orderByAsc(ImFriendShipEntity::getFriendSequence);
        List<ImFriendShipEntity> dataList = imFriendShipMapper.selectList(lqw);

        if(!CollectionUtils.isEmpty(dataList)){
            ImFriendShipEntity maxSeqEntity = dataList.get(dataList.size() - 1);
            resp.setDataList(dataList);
            // 设置最大seq
            Long friendShipMaxSeq = imFriendShipMapper.getFriendShipMaxSeq(req.getAppId(), req.getOperater());
            resp.setMaxSequence(friendShipMaxSeq);
            // 设置是否拉取完毕
            resp.setCompleted(maxSeqEntity.getFriendSequence() >= friendShipMaxSeq);
            return ResponseVO.successResponse(resp);
        }

        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

    /**
     * 获取指定用户的所有好友ID
     */
    @Override
    public List<String> getAllFriendId(String userId, Integer appId) {
        return imFriendShipMapper.getAllFriendId(userId, appId);
    }
}
