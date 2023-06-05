package com.hao14293.im.tcp.reciver;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.codec.proto.MessagePack;
import com.hao14293.im.common.ClientType;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.Command.SystemCommand;
import com.hao14293.im.common.enums.DeviceMultiLoginEnum;
import com.hao14293.im.common.model.UserClientDto;
import com.hao14293.im.tcp.redis.RedisManager;
import com.hao14293.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 * 多端同步： 1、单端登录：一端在线，踢掉除了本clientType + imei 的设备
 *           2、双端登录： 允许pc 和 手机 其中一段登录 + web端
 *           3、三端登录：允许手机 + pc + web同时登录，踢掉同端的其他的imei
 *           4、多端登录：不做任何处理
 */
public class UserLoginMessageListener {

    private final static Logger logger = LoggerFactory.getLogger(UserLoginMessageListener.class);

    private Integer loginModel;

    public UserLoginMessageListener(Integer loginModel){
        this.loginModel = loginModel;
    }

    // 监听用户登录
    public void listenerUserLogin(){
        RTopic topic = RedisManager.getRedissonClient().getTopic(Constants.RedisConstants.UserLoginChannel);

        // 使用Redisson的订阅模式做  监听  当有用户的某个端登录就会
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence charSequence, String message) {
                logger.info("收到用户上线：" + message);

                UserClientDto userClientDto = JSONObject.parseObject(message, UserClientDto.class);

                // 获取所有的CHANNELS
                List<NioSocketChannel> nioSocketChannels
                        = SessionSocketHolder.get(userClientDto.getAppId(), userClientDto.getUserId());

                for (NioSocketChannel nioSocketChannel : nioSocketChannels) {
                    // 单端登录
                    if(loginModel == DeviceMultiLoginEnum.ONE.getLoginMode()){

                        // 获取clietType
                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();

                        // 获取imei号
                        String imei = (String)nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

                        if(!(clientType + ":" + imei).equals(userClientDto.getClientType() + ":" + userClientDto.getImei())){
                            // TODO 踢掉客户端
                            // 告诉客户端 其他端登录
                            MessagePack<Object> messagePack = new MessagePack<>();
                            messagePack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            messagePack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            messagePack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            nioSocketChannel.writeAndFlush(messagePack);
                        }
                    }else if(loginModel == DeviceMultiLoginEnum.TWO.getLoginMode()){

                        if(userClientDto.getClientType() == ClientType.WEB.getCode()){
                            continue;
                        }

                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();

                        if(clientType == ClientType.WEB.getCode()){
                            continue;
                        }

                        // 获取imei号
                        String imei = (String)nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

                        if(!(clientType + ":" + imei).equals(userClientDto.getClientType() + ":" + userClientDto.getImei())){
                            // TODO 踢掉客户端
                            MessagePack<Object> messagePack = new MessagePack<>();
                            messagePack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            messagePack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            messagePack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            nioSocketChannel.writeAndFlush(messagePack);

                        }
                    }else if(loginModel == DeviceMultiLoginEnum.THREE.getLoginMode()){

                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();

                        String imei = (String)nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

                        if(clientType == ClientType.WEB.getCode()){
                            continue;
                        }

                        Boolean isSameClient = false;

                        // 如果新登录的端和旧的端都是手机端，做处理
                        if((clientType == ClientType.IOS.getCode()
                                || clientType == ClientType.ANDROID.getCode()) &&
                                (userClientDto.getClientType() == ClientType.IOS.getCode()
                                        || userClientDto.getClientType() == ClientType.ANDROID.getCode())){
                            isSameClient = true;
                        }

                        // 如果新登录的端和旧的端都是电脑端，做处理
                        if((clientType == ClientType.MAC.getCode()
                                || clientType == ClientType.WINDOWS.getCode()) &&
                                (userClientDto.getClientType() == ClientType.MAC.getCode()
                                        || userClientDto.getClientType() == ClientType.WINDOWS.getCode())){
                            isSameClient = true;
                        }

                        if(isSameClient && !(clientType + ":" + imei).equals(userClientDto.getClientType() + ":" + userClientDto.getImei())){
                            // TODO 踢掉客户端
                            MessagePack<Object> messagePack = new MessagePack<>();
                            messagePack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            messagePack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            messagePack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
                            nioSocketChannel.writeAndFlush(messagePack);
                        }
                    }
                }
            }
        });
    }
}
