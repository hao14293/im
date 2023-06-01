package com.hao14293.im.common.route.algorithm.consistenthash;

import com.hao14293.im.common.route.RouteHandle;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public class ConsistentHashHandle implements RouteHandle {

    private AbstractConsistentHash abstractConsistentHash;

    public void setAbstractConsistentHash(AbstractConsistentHash abstractConsistentHash) {
        this.abstractConsistentHash = abstractConsistentHash;
    }

    @Override
    public String routeServer(List<String> values, String key) {
        return abstractConsistentHash.process(values, key);
    }
}
