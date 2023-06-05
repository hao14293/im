package com.hao14293.im.service.seq;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author hao14293
 * @data 2023/4/30
 * @time 10:27
 */
@Service
public class RedisSeq {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    public long doGetSeq(String key){
        return stringRedisTemplate.opsForValue().increment(key);
    }
}
