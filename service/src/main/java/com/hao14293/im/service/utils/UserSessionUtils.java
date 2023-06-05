package com.hao14293.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.ImConnectStatusEnum;
import com.hao14293.im.common.model.UserSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Component
public class UserSessionUtils {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 1、获取用户所有的session
    public List<UserSession> getUserSession(Integer appId, String userId){
        // 获取session的key
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        // 获取到这个map
        Map<Object, Object> entries =
                stringRedisTemplate.opsForHash().entries(userSessionKey);
        List<UserSession> list = new ArrayList<>();
        Collection<Object> values = entries.values();
        for (Object value : values) {
            String str = (String)value;
            UserSession userSession
                    = JSONObject.parseObject(str, UserSession.class);

            // 只获取在线的
            if(userSession.getConnectState() == ImConnectStatusEnum.ONLINE_STATUS.getCode()){
                list.add(userSession);
            }
        }
        return list;
    }

    // 获取指定端的session

    public UserSession getUserSession(Integer appId, String userId, Integer clientType, String imei){
        // 获取session的key
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        String hashKey = clientType + ":" + imei;
        Object o = stringRedisTemplate.opsForHash().get(userSessionKey, hashKey);
        UserSession userSession
                = JSONObject.parseObject(o.toString(), UserSession.class);

        return userSession;
    }
}
