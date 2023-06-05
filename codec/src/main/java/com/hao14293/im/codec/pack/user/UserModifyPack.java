package com.hao14293.im.codec.pack.user;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
public class UserModifyPack {

    private String userId;

    // 用户名称
    private String nickName;

    private String password;

    // 头像
    private String photo;

    // 性别
    private String userSex;

    // 个性签名
    private String selfSignature;

    // 加好友验证类型（Friend_AllowType） 1需要验证
    private Integer friendAllowType;
}
