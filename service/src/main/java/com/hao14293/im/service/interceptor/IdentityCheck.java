package com.hao14293.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.common.BaseErrorCode;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.common.enums.GateWayErrorCode;
import com.hao14293.im.common.enums.ImUserTypeEnum;
import com.hao14293.im.common.exception.ApplicationExceptionEnum;
import com.hao14293.im.common.utils.SigAPI;
import com.hao14293.im.service.user.model.entity.ImUserDataEntity;
import com.hao14293.im.service.user.service.ImUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author hao14293
 * @data 2023/4/24
 * @time 11:42
 */
@Component
public class IdentityCheck {

    private static Logger logger = LoggerFactory.getLogger(IdentityCheck.class);

    @Resource
    private ImUserService imUserService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public ApplicationExceptionEnum checkUserSign(String identifier, String appId, String userSign){

        String cacheUserSig
                = stringRedisTemplate.opsForValue().get(appId + ":"
                + Constants.RedisConstants.userSign + ":" + identifier + userSign);
        if(!StringUtils.isBlank(cacheUserSig) && Long.valueOf(cacheUserSig) > System.currentTimeMillis() / 1000){
            return BaseErrorCode.SUCCESS;
        }

        // 获取秘钥
        String privatekey = appConfig.getPrivatekey();

        // 根据appId + 秘钥 创建 signApi
        SigAPI sigAPI = new SigAPI(Long.valueOf(appId), privatekey);

        // 调用 signApi 对 userSign解密
        JSONObject jsonObject = sigAPI.decodeUserSig(userSign);

        // 取出解密后的appId， 和操作人  和过期时间 做匹配，不通过则提示错误
        Long expireTime = 0L;
        Long expireSec = 0L;
        Long time = 0L;
        String decoderAppId = "";
        String decoderIdentifier = "";

        try {
            // 取出解密后的数据
            decoderAppId = jsonObject.getString("TLS.appId");
            decoderIdentifier = jsonObject.getString("TLS.identifier");
            String expireStr = jsonObject.get("TLS.expire").toString();
            String expireTimeStr = jsonObject.get("TLS.expireTime").toString();
            time =  Long.valueOf(expireTimeStr);
            expireSec = Long.valueOf(expireStr);
            expireTime = time + expireSec;
        }catch (Exception e){
            logger.error("checkUserSig-error: {}", e.getMessage());
            e.printStackTrace();
        }

        // 进行比对
        // 用户签名和操作人不匹配
            if(!decoderIdentifier.equals(identifier)){
            return GateWayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }

        // 用户签名不正确
        if(!decoderAppId.equals(appId)){
            return GateWayErrorCode.USERSIGN_IS_ERROR;
        }

        // 过期时间
        if(expireSec == 0){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        if(expireTime < System.currentTimeMillis() / 1000){
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        // 把userSign存储到redis中去
        // appid + "xxx" + "userId" + sign
        String genSig = sigAPI.genUserSig(identifier, expireSec,time, null);
        if (genSig.toLowerCase().equals(userSign.toLowerCase())) {
            String key = appId + ":" + Constants.RedisConstants.userSign + ":" + identifier + userSign;
            Long etime = expireTime - System.currentTimeMillis() / 1000;
            stringRedisTemplate.opsForValue().set(
                    key, expireTime.toString(), etime, TimeUnit.SECONDS);
            this.setIsAdmin(identifier,Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }
        return BaseErrorCode.SUCCESS;
    }

    private void setIsAdmin(String identifier, Integer appId) {
        //去DB或Redis中查找, 后面写
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(identifier, appId);
        if(singleUserInfo.isOk()){
            RequestHolder.set(singleUserInfo.getData().getUserType() == ImUserTypeEnum.APP_ADMIN.getCode());
        }else{
            RequestHolder.set(false);
        }
    }
}
