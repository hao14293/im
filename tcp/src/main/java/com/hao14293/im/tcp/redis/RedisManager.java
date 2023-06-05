package com.hao14293.im.tcp.redis;

import com.hao14293.im.codec.config.BootstrapConfig;
import org.redisson.api.RedissonClient;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public class RedisManager {

    private static RedissonClient redissonClient;

    private static Integer loginModel;

    public static void init(BootstrapConfig config) {
        loginModel = config.getLim().getLoginModel();
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redissonClient = singleClientStrategy.getRedissonClient(config.getLim().getRedis());

    }

    public static RedissonClient getRedissonClient() {
        return redissonClient;
    }
}
