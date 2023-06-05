package com.hao14293.im.service.user.model.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.pack.user.UserCustomStatusChangeNotifyPack;
import com.hao14293.im.codec.pack.user.UserStatusChangeNotifyPack;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.Command;
import com.hao14293.im.common.enums.Command.UserEventCommand;
import com.hao14293.im.common.model.ClientInfo;
import com.hao14293.im.common.model.UserSession;
import com.hao14293.im.service.friendship.service.ImFriendShipService;
import com.hao14293.im.service.user.model.UserStatusChangeNotifyContent;
import com.hao14293.im.service.user.model.req.PullFriendOnlineStatusReq;
import com.hao14293.im.service.user.model.req.PullUserOnlineStatusReq;
import com.hao14293.im.service.user.model.req.SetUserCustomerStatusReq;
import com.hao14293.im.service.user.model.req.SubscribeUserOnlineStatusReq;
import com.hao14293.im.service.user.model.resp.UserOnlineStatusResp;
import com.hao14293.im.service.user.service.ImUserStatusService;
import com.hao14293.im.service.utils.MessageProducer;
import com.hao14293.im.service.utils.UserSessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Service
public class ImUserStatusServiceImpl implements ImUserStatusService {

    @Resource
    private UserSessionUtils userSessionUtils;

    @Resource
    private MessageProducer messageProducer;

    @Resource
    private ImFriendShipService imFriendShipService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content) {
        // 1、获取到该用户的所有 session，并将其设置到pack中
        List<UserSession> userSessions
                = userSessionUtils.getUserSession(content.getAppId(), content.getUserId());
        UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
        BeanUtils.copyProperties(content, userStatusChangeNotifyPack);
        userStatusChangeNotifyPack.setClient(userSessions);

        // TODO 发送给自己的同步端
        syncSender(userStatusChangeNotifyPack, content.getUserId(),
                UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC, content);

        // TODO 同步给好友和订阅了自己的人
        dispatcher(userStatusChangeNotifyPack, content.getUserId(), UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY,
                content.getAppId());
    }


    // 同步自己端
    private void syncSender(Object pack, String userId, Command command, ClientInfo clientInfo){
        messageProducer.sendToUserExceptClient(userId, command,
                pack, clientInfo);
    }

    // 同步对方端口
    private void dispatcher(Object pack, String userId, Command command, Integer appId){

        // TODO 获取指定用户的所有好友id
        List<String> allFriendId = imFriendShipService.getAllFriendId(userId, appId);
        for (String fid : allFriendId) {
            messageProducer.sendToUser(fid, command,
                    pack, appId);
        }

        // TODO 发送给临时订阅的人
        String key = appId+ ":" + Constants.RedisConstants.subscribe + ":" + userId;
        // 取出key中的所有key
        Set<Object> keys = stringRedisTemplate.opsForHash().keys(key);
        // 遍历
        for (Object k : keys) {
            String filed = (String)k;
            // 取出其中的过期时间
            Long expired = Long.valueOf((String) Objects.requireNonNull(stringRedisTemplate.opsForHash().get(key, filed)));
            // 如果没有过期，就要给他发送
            if(expired > 0 && expired > System.currentTimeMillis()){
                messageProducer.sendToUser(filed, UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY,
                        pack, appId);
            }else{
                stringRedisTemplate.opsForHash().delete(key, filed);
            }
        }
    }

    // 订阅用户状态
    @Override
    public void subscribeUserOnlineStatus(SubscribeUserOnlineStatusReq req) {
        Long subExpireTime = 0L;
        if(req != null && req.getSubTime() > 0){
            subExpireTime = System.currentTimeMillis() + req.getSubTime();
        }
        // 使用redis的hash结构存储订阅用户的状态
        for (String subUserId : req.getSubUserId()) {
            String key = req.getAppId() + ":" + Constants.RedisConstants.subscribe + ":" + subUserId;
            stringRedisTemplate.opsForHash().put(key, req.getOperater(), subExpireTime.toString());
        }
    }

    // 设置客户端状态
    @Override
    public void setUserCustomerStatus(SetUserCustomerStatusReq req) {

        // 包
        UserCustomStatusChangeNotifyPack userCustomStatusChangeNotifyPack = new UserCustomStatusChangeNotifyPack();
        userCustomStatusChangeNotifyPack.setCustomStatus(req.getCustomStatus());
        userCustomStatusChangeNotifyPack.setCustomText(req.getCustomText());
        userCustomStatusChangeNotifyPack.setUserId(req.getUserId());

        // 将状态存储到redis中
        String key = req.getAppId() + ":" + Constants.RedisConstants.userCustomerStatus + ":" + req.getUserId();
        stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(userCustomStatusChangeNotifyPack));

        syncSender(userCustomStatusChangeNotifyPack, req.getUserId()
                , UserEventCommand.USER_ONLINE_STATUS__SET_CHANGE_NOTIFY_SYNC,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        dispatcher(userCustomStatusChangeNotifyPack, req.getUserId()
                , UserEventCommand.USER_ONLINE_STATUS__SET_CHANGE_NOTIFY, req.getAppId());
    }

    // 拉取指定用户的状态
    @Override
    public Map<String, UserOnlineStatusResp> queryUserOnlineStatus(PullUserOnlineStatusReq req) {
        return getUserOnlineStatus(req.getUserList(), req.getAppId());
    }

    // 拉取所有用户的状态
    @Override
    public Map<String, UserOnlineStatusResp> queryFriendOnlineStatus(PullFriendOnlineStatusReq req) {
        List<String> allFriendId = imFriendShipService.getAllFriendId(req.getOperater(), req.getAppId());
        return getUserOnlineStatus(allFriendId, req.getAppId());
    }

    // 拉取用户在线状态
    private Map<String, UserOnlineStatusResp> getUserOnlineStatus(List<String> userId,Integer appId){
        // 返回类
        Map<String, UserOnlineStatusResp> res = new HashMap<>(userId.size());

        for (String uid : userId) {
            UserOnlineStatusResp resp = new UserOnlineStatusResp();
            // 拉取服务端的状态
            List<UserSession> userSession = userSessionUtils.getUserSession(appId, uid);
            resp.setSession(userSession);
            // 拉取客户端的状态
            String key = appId + ":" + Constants.RedisConstants.userCustomerStatus + ":" + uid;
            String s = stringRedisTemplate.opsForValue().get(key);
            if(StringUtils.isNotBlank(s)){
                JSONObject parse = (JSONObject) JSON.parse(s);
                resp.setCustomText(parse.getString("customText"));
                resp.setCustomStatus(parse.getInteger("customStatus"));
            }
            res.put(uid, resp);
        }
        return res;
    }
}
