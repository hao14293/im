package com.hao14293.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.hao14293.im.common.BaseErrorCode;
import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.common.enums.GateWayErrorCode;
import com.hao14293.im.common.exception.ApplicationExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * @author hao14293
 * @data 2023/4/24
 * @time 11:28
 */
@Component
public class GateWayInterceptor implements HandlerInterceptor {

    @Resource
    private IdentityCheck identityCheck;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 方便测试
        if(1 == 1){
            return true;
        }

        // 获取appid 操作人 usersign
        String appIdStr = request.getParameter("appId");
        if(StringUtils.isBlank(appIdStr)){
            resp(ResponseVO.errorResponse(GateWayErrorCode.APPID_NOT_EXIST), response);
            return false;
        }

        String identifier = request.getParameter("identifier");
        if(StringUtils.isBlank(identifier)){
            resp(ResponseVO.errorResponse(GateWayErrorCode.OPERATER_NOT_EXIST), response);
            return false;
        }

        String userSign = request.getParameter("userSign");
        if(StringUtils.isBlank(userSign)){
            resp(ResponseVO.errorResponse(GateWayErrorCode.USERSIGN_IS_ERROR), response);
            return false;
        }

        // 校验签名和操作人和appId是否匹配
        ApplicationExceptionEnum applicationExceptionEnum
                = identityCheck.checkUserSign(identifier, appIdStr, userSign);

        if(applicationExceptionEnum != BaseErrorCode.SUCCESS){
            resp(ResponseVO.errorResponse(applicationExceptionEnum), response);
            return false;
        }
        return true;
    }

    private void resp(ResponseVO responseVO, HttpServletResponse response){
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");

        try {
            String resp = JSONObject.toJSONString(responseVO);
            writer = response.getWriter();
            writer.write(resp);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(writer != null){
                writer.close();
            }
        }
    }
}
