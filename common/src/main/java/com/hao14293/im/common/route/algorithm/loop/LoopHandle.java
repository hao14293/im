package com.hao14293.im.common.route.algorithm.loop;

import com.hao14293.im.common.enums.UserErrorCode;
import com.hao14293.im.common.exception.ApplicationException;
import com.hao14293.im.common.route.RouteHandle;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public class LoopHandle implements RouteHandle {

    private AtomicLong index = new AtomicLong();

    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if (size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVALIABLE);
        }
        Long l = index.incrementAndGet() % size;
        if (l < 0) {
            l = 0L;
        }
        return values.get(l.intValue());
    }
}
