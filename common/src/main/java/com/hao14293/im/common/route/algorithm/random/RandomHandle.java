package com.hao14293.im.common.route.algorithm.random;

import com.hao14293.im.common.enums.UserErrorCode;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.common.route.RouteHandle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public class RandomHandle implements RouteHandle {
    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();

        if (size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVALIABLE);
        }
        int i = ThreadLocalRandom.current().nextInt(size);
        return values.get(i);
    }
}
