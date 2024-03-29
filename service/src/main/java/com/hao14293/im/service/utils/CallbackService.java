package com.hao14293.im.service.utils;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.utils.HttpRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Component
public class CallbackService {

    private Logger logger = LoggerFactory.getLogger(CallbackService.class);

    @Resource
    private HttpRequestUtils httpRequestUtils;

    @Resource
    private AppConfig appConfig;

    @Resource
    private ShareThreadPool shareThreadPool;

    public void callback(Integer appId, String callbackCommand, String jsonBody){
        shareThreadPool.submit(()->{
            try {
                httpRequestUtils.doPost(appConfig.getCallbackUrl(), Object.class, builderUrlParams(appId, callbackCommand),
                        jsonBody, null);
            } catch (Exception e) {
                logger.error("callback 回调{} : {}出现异常 ： {} ",callbackCommand , appId, e.getMessage());
            }
        });
    }

    public ResponseVO beforecallback(Integer appId, String callbackCommand, String jsonBody){
        try {
            ResponseVO responseVO
                    = httpRequestUtils.doPost("", ResponseVO.class, builderUrlParams(appId, callbackCommand)
                    , jsonBody, null);
            return responseVO;
        } catch (Exception e) {
            logger.error("callback 之前 回调{} : {}出现异常 ： {} ",callbackCommand , appId, e.getMessage());
            return ResponseVO.successResponse();
        }
    }

    public Map builderUrlParams(Integer appId, String command){
        Map map = new HashMap();
        map.put("appId", appId);
        map.put("command", command);
        return map;
    }
}
