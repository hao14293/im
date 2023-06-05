package com.hao14293.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hao14293.im.codec.pack.LoginPack;
import com.hao14293.im.codec.pack.message.ChatMessageAck;
import com.hao14293.im.codec.pack.user.LoginAckPack;
import com.hao14293.im.codec.pack.user.UserStatusChangeNotifyPack;
import com.hao14293.im.codec.proto.Message;
import com.hao14293.im.codec.proto.MessagePack;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.GroupEventCommand;
import com.hao14293.im.common.enums.Command.MessageCommand;
import com.hao14293.im.common.enums.Command.SystemCommand;
import com.hao14293.im.common.enums.Command.UserEventCommand;
import com.hao14293.im.common.enums.ImConnectStatusEnum;
import com.hao14293.im.common.model.UserClientDto;
import com.hao14293.im.common.model.UserSession;
import com.hao14293.im.common.model.message.CheckSendMessageReq;
import com.hao14293.im.tcp.feign.FeignMessageService;
import com.hao14293.im.tcp.publish.MqMessageProducer;
import com.hao14293.im.tcp.redis.RedisManager;
import com.hao14293.im.tcp.utils.SessionSocketHolder;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.net.InetAddress;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private Integer brokerId;

    private String logicUrl;

    private FeignMessageService feignMessageService;

    public NettyServerHandler(Integer brokerId, String logicUrl) {
        this.brokerId = brokerId;
        feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                // 设置超时时间
                .options(new Request.Options(1000, 3500))
                // 代理对象
                .target(FeignMessageService.class, logicUrl);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {

        Integer command = msg.getMessageHeader().getCommand();

        // 登录command
        if (SystemCommand.LOGIN.getCommand() == command) {
            LoginPack loginPack = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()),
                    new TypeReference<LoginPack>() {}.getType());

            // 为channel设置属性
            ctx.channel().attr(AttributeKey.valueOf(Constants.UserId))
                    .set(loginPack.getUserId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.AppId))
                    .set(msg.getMessageHeader().getAppId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.ClientType))
                    .set(msg.getMessageHeader().getClientType());
            ctx.channel().attr(AttributeKey.valueOf(Constants.Imei))
                    .set(msg.getMessageHeader().getImei());

            // redis map
            UserSession userSession = new UserSession();
            userSession.setAppId(msg.getMessageHeader().getAppId());
            userSession.setClientType(msg.getMessageHeader().getClientType());
            userSession.setUserId(loginPack.getUserId());
            userSession.setConnectState(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setImei(msg.getMessageHeader().getImei());

            // 这里的brokerId 和 brokerHost是来区分 分布式的server
            userSession.setBrokerId(brokerId);
            try {
                String hostAddress = InetAddress.getLocalHost().getHostAddress();
                userSession.setBrokerHost(hostAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 存入 redis
            RedissonClient redissonClient = RedisManager.getRedissonClient();
            // 10000:userSession:user123
            RMap<String, String> map
                    = redissonClient.getMap(msg.getMessageHeader().getAppId()
                    + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());

            map.put(msg.getMessageHeader().getClientType() + ":"
                    + msg.getMessageHeader().getImei(), JSONObject.toJSONString(userSession));

            // 将channel 存储起来
            SessionSocketHolder.put(msg.getMessageHeader().getAppId(), loginPack.getUserId(),
                    msg.getMessageHeader().getClientType(), msg.getMessageHeader().getImei(),
                    (NioSocketChannel) ctx);

            // 登录成功后，向其他的netty服务器发送一条消息，使用的是Redis订阅服务
            UserClientDto userClientDto = new UserClientDto();
            userClientDto.setAppId(msg.getMessageHeader().getAppId());
            userClientDto.setImei(msg.getMessageHeader().getImei());
            userClientDto.setUserId(loginPack.getUserId());
            userClientDto.setClientType(msg.getMessageHeader().getClientType());

            // 发送消息 （用于多端登录）
            // "signal/channel/LOGIN_USER_INNER_QUEUE"
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSONObject.toJSONString(userClientDto));

            // 用户状态变更
            UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
            userStatusChangeNotifyPack.setAppId(msg.getMessageHeader().getAppId());
            userStatusChangeNotifyPack.setUserId(loginPack.getUserId());
            userStatusChangeNotifyPack.setStatus(ImConnectStatusEnum.ONLINE_STATUS.getCode());

            // 发送给mq
            MqMessageProducer.sendMessage(userStatusChangeNotifyPack,
                    msg.getMessageHeader(), UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());

            // 补充登录ack
            MessagePack<LoginAckPack> loginSuccess = new MessagePack<>();
            LoginAckPack loginAckPack = new LoginAckPack();
            loginAckPack.setUserId(loginPack.getUserId());
            loginSuccess.setCommand(SystemCommand.LOGINACK.getCommand());
            loginSuccess.setData(loginAckPack);
            loginSuccess.setImei(msg.getMessageHeader().getImei());
            loginSuccess.setAppId(msg.getMessageHeader().getAppId());
            ctx.channel().writeAndFlush(loginSuccess);
        }else if(SystemCommand.LOGOUT.getCommand() == command){
            // 登出
            SessionSocketHolder.removeUserSession((NioSocketChannel) ctx.channel());
        }else if(SystemCommand.PING.getCommand() == command){
            ctx.attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());
        }else if(MessageCommand.MSG_P2P.getCommand() == command ||
                GroupEventCommand.MSG_GROUP.getCommand() == command) {
            try {
                String toId = "";
                CheckSendMessageReq checkSendMessageReq = new CheckSendMessageReq();
                checkSendMessageReq.setAppId(msg.getMessageHeader().getAppId());
                checkSendMessageReq.setCommand(msg.getMessageHeader().getCommand());
                JSONObject jsonObject
                        = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()));
                String fromId = jsonObject.getString("fromId");

                if(command == MessageCommand.MSG_P2P.getCommand()){
                    toId = jsonObject.getString("toId");
                }else{
                    toId = jsonObject.getString("groupId");
                }

                checkSendMessageReq.setFromId(fromId);
                checkSendMessageReq.setToId(toId);

                // 1、调用校验消息发送方的接口
                ResponseVO responseVO
                        = feignMessageService.checkSendMessage(checkSendMessageReq);

                if(responseVO.isOk()){
                    MqMessageProducer.sendMessage(msg, command);
                } else {
                    Integer ackCommand = 0;

                    if(command == MessageCommand.MSG_P2P.getCommand()){
                        ackCommand = MessageCommand.MSG_ACK.getCommand();
                    }else{
                        ackCommand = GroupEventCommand.GROUP_MSG_ACK.getCommand();
                    }

                    // 失败 直接 ACK
                    ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString("messageId"));
                    responseVO.setData(chatMessageAck);
                    MessagePack<ResponseVO> ack = new MessagePack<>();
                    ack.setData(responseVO);
                    ack.setCommand(ackCommand);
                    ctx.channel().writeAndFlush(ack);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            MqMessageProducer.sendMessage(msg, command);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 设置离线
        ctx.close();
    }
}
