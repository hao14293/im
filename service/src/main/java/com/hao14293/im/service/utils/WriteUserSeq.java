package com.hao14293.im.service.utils;

import com.hao14293.im.common.constant.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Service
public class WriteUserSeq {

    // redis
    // uid friend group conversation

    @Autowired
    private RedisTemplate redisTemplate;

    // 修改消息的seq值
    public void writeUserSeq(Integer appId, String userId, String type, Long seq){
        String key = appId + ":" + Constants.RedisConstants.SeqPrefix + ":" + userId;
        redisTemplate.opsForHash().put(key, type, seq);
    }
}

