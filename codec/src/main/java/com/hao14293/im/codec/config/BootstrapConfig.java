package com.hao14293.im.codec.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hao14293
 * @data 2023/4/16
 * @time 18:26
 */

@Data
public class BootstrapConfig {

    private TcpConfig lim;

    @Data
    public static class TcpConfig{
        // tcp 绑定的端口号
        private Integer tcpPort;

        // webSocket 绑定的端口号
        private Integer webSocketPort;

        // boss线程 默认=1
        private Integer bossThreadSize;

        //work线程
        private Integer workThreadSize;

        // 心跳超时时间  单位ms
        private Long heartBeatTime;

        // 登录模式
        private Integer loginModel;


        // redis配置文件
        private RedisConfig redis;

        /**
         * rabbitmq配置
         */
        private Rabbitmq rabbitmq;

        /**
         * zk配置
         */
        private ZkConfig zkConfig;

        /**
         * brokerId
         */
        private Integer brokerId;

        private String logicUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {

        /**
         * 单机模式：single 哨兵模式：sentinel 集群模式：cluster
         */
        private String mode;
        /**
         * 数据库
         */
        private Integer database;
        /**
         * 密码
         */
        private String password;
        /**
         * 超时时间
         */
        private Integer timeout;
        /**
         * 最小空闲数
         */
        private Integer poolMinIdle;
        /**
         * 连接超时时间(毫秒)
         */
        private Integer poolConnTimeout;
        /**
         * 连接池大小
         */
        private Integer poolSize;

        /**
         * redis单机配置
         */
        private RedisSingle single;
    }

    @Data
    public static class ZkConfig {
        /**
         * zk连接地址
         */
        private String zkAddr;

        /**
         * zk连接超时时间
         */
        private Integer zkConnectTimeOut;
    }

    /**
     * redis单机配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisSingle {
        /**
         * 地址
         */
        private String address;
    }

    /**
     * rabbitmq哨兵模式配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbitmq {
        private String host;

        private Integer port;

        private String virtualHost;

        private String userName;

        private String password;
    }
}