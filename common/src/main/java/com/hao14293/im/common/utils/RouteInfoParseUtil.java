package com.hao14293.im.common.utils;

import com.hao14293.im.common.BaseErrorCode;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.common.route.RouteInfo;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public class RouteInfoParseUtil {

    public static RouteInfo parse(String info) {
        try {
            String[] serverInfo = info.split(":");
            RouteInfo routeInfo = new RouteInfo(serverInfo[0], Integer.parseInt(serverInfo[1]));
            return routeInfo;
        } catch (Exception e) {
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR);
        }
    }
}
