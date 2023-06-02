package com.hao14293.im.common.model;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class SyncReq extends RequestBase{

    /**
     * 客户端最大seq
     */

    private Long lastSequence;

    /*
     * 一次拉取多少
     */
    private Integer maxLimit;
}
