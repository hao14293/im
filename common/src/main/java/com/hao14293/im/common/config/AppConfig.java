package com.hao14293.im.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class AppConfig {
    /** zk连接地址*/
    private String zkAddr;

    /** zk连接超时时间*/
    private Integer zkConnectTimeOut;

    /** im管道地址路由策略*/
    private Integer imRouteWay;

    /** 如果选用一致性hash的话具体hash算法*/
    private Integer consistentHashWay;

    // 回调地址
    private String callbackUrl;

    // 秘钥
    private String privatekey;

    //发送消息是否校验关系链
    private boolean sendMessageCheckFriend;

    //发送消息是否校验黑名单
    private boolean sendMessageCheckBlack;

    /**
     * 回调开关
     */
    //用户资料变更之后回调开关
    private boolean modifyUserAfterCallback;

    //添加好友之后回调开关
    private boolean addFriendAfterCallback;

    //添加好友之前回调开关
    private boolean addFriendBeforeCallback;

    //修改好友之后回调开关
    private boolean modifyFriendAfterCallback;

    //删除好友之后回调开关
    private boolean deleteFriendAfterCallback;

    //添加黑名单之后回调开关
    private boolean addFriendShipBlackAfterCallback;

    //删除黑名单之后回调开关
    private boolean deleteFriendShipBlackAfterCallback;

    //创建群聊之后回调开关
    private boolean createGroupAfterCallback;

    //修改群聊之后回调开关
    private boolean modifyGroupAfterCallback;

    //解散群聊之后回调开关
    private boolean destroyGroupAfterCallback;

    //删除群成员之后回调
    private boolean deleteGroupMemberAfterCallback;

    //拉人入群之前回调
    private boolean addGroupMemberBeforeCallback;

    //拉人入群之后回调
    private boolean addGroupMemberAfterCallback;

    //发送单聊消息之后
    private boolean sendMessageAfterCallback;

    //发送单聊消息之前
    private boolean sendMessageBeforeCallback;

    private Integer deleteConversationSyncMode;

    //离线消息最大条数
    private Integer offlineMessageCount;
}
