package com.hao14293.im.common.model;

import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */

@Data
public class SyncResp<T> {

    private Long maxSequence;

    private boolean isCompleted;

    private List<T> dataList;
}
