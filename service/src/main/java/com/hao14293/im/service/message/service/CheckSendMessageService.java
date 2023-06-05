package com.hao14293.im.service.message.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.enums.*;
import com.hao14293.im.service.friendship.entity.ImFriendShipEntity;
import com.hao14293.im.service.friendship.model.req.GetRelationReq;
import com.hao14293.im.service.friendship.service.ImFriendShipService;
import com.hao14293.im.service.group.entity.ImGroupEntity;
import com.hao14293.im.service.group.model.resp.GetRoleInGroupResp;
import com.hao14293.im.service.group.service.ImGroupMemberService;
import com.hao14293.im.service.group.service.ImGroupService;
import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import com.hao14293.im.service.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: hao14293
 * @data 2023/4/24
 * @time 20:05
 */
@Service
public class CheckSendMessageService {

    @Autowired
    private ImUserService imUserService;

    @Autowired
    private ImFriendShipService imFriendShipService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ImGroupService imGroupService;

    @Autowired
    private ImGroupMemberService imGroupMemberService;

    // 判断发送发是否被禁用或者禁言
    public ResponseVO checkSenderForvidAndMute(String fromId, Integer appId){
        // 获取单个用户
        ResponseVO<ImUserDataEntity> singleUserInfo
                = imUserService.getSingleUserInfo(fromId, appId);
        if(!singleUserInfo.isOk()){
            return singleUserInfo;
        }

        // 取出用户
        ImUserDataEntity user = singleUserInfo.getData();
        // 是否被禁用
        if(user.getForbiddenFlag() == UserForbiddenFlagEnum.FORBIBBEN.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIBBEN);
        }
        // 是否被禁言
        else if(user.getSilentFlag() == UserSilentFlagEnum.MUTE.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }
        return ResponseVO.successResponse();
    }

    // 判断好友关系
    public ResponseVO checkFriendShip(String fromId, String toId, Integer appId){
        if(appConfig.isSendMessageCheckFriend()){
            // 判断双方好友记录是否存在
            GetRelationReq fromReq = new GetRelationReq();
            fromReq.setFromId(fromId);
            fromReq.setToId(toId);
            fromReq.setAppId(appId);
            ResponseVO<ImFriendShipEntity> fromRelation = imFriendShipService.getRelation(fromReq);
            if(!fromRelation.isOk()){
                return fromRelation;
            }
            GetRelationReq toReq = new GetRelationReq();
            toReq.setFromId(toId);
            toReq.setToId(fromId);
            toReq.setAppId(appId);
            ResponseVO<ImFriendShipEntity> toRelation = imFriendShipService.getRelation(toReq);
            if(!toRelation.isOk()){
                return toRelation;
            }

            // 判断好友关系记录是否正常
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != fromRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != toRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }

            // 判断是否在黑名单里面
            if(appConfig.isSendMessageCheckBlack()){
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != fromRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
                }
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != toRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.TARGET_IS_BLACK_YOU);
                }
            }
        }
        return ResponseVO.successResponse();
    }

    // 前置校验群组消息
    public ResponseVO checkGroupMessage(String fromId, String groupId, Integer appId){
        // 发送方是否被禁言
        ResponseVO responseVO = checkSenderForvidAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }

        // 判断群逻辑
        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(groupId, appId);
        if(!group.isOk()){
            return group;
        }

        // 判断群成员是否在群内
        ResponseVO<GetRoleInGroupResp> roleInGroupOne
                = imGroupMemberService.getRoleInGroupOne(groupId, fromId, appId);
        if(!roleInGroupOne.isOk()){
            return roleInGroupOne;
        }
        GetRoleInGroupResp data = roleInGroupOne.getData();


        // 判断群是否被禁言
        //如果禁言，只有群管理和群主可以发言
        ImGroupEntity groupdata = group.getData();
        // 如果群组已经禁言并且 发言人不是群管理或者群主
        if(groupdata.getMute() == GroupMuteTypeEnum.MUTE.getCode()
                && (data.getRole() != GroupMemberRoleEnum.MAMAGER.getCode() ||
                data.getRole() != GroupMemberRoleEnum.OWNER.getCode())){
            return ResponseVO.errorResponse(GroupErrorCode.THIS_GROUP_IS_MUTE);
        }
        // 如果是个人禁言，并且还在禁言时长中
        if(data.getSpeakDate() != null && data.getSpeakDate() > System.currentTimeMillis()){
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_MEMBER_IS_SPEAK);
        }
        return ResponseVO.successResponse();
    }
}
