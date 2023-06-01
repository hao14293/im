package com.hao14293.im.common.model.message;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class ImMessageBody {

    private Integer appId;

    /** messageBodyId*/
    private Long messageKey;

    /** messageBody*/
    private String messageBody;

    private String securityKey;

    private Long messageTime;

    private Long createTime;

    private String extra;

    private Integer delFlag;
}
