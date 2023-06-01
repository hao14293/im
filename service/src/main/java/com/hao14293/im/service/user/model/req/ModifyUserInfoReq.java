package com.hao14293.im.service.user.model.req;

import com.hao14293.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class ModifyUserInfoReq extends RequestBase {

    // 用户id
    @NotEmpty(message = "用户id不能为空")
    private String userId;

    // 用户名称
    private String nickName;

    //位置
    private String location;

    //生日
    private String birthDay;

    private String password;

    // 头像
    private String photo;

    // 性别
    private String userSex;

    // 个性签名
    private String selfSignature;

    // 加好友验证类型（Friend_AllowType） 1需要验证
    private Integer friendAllowType;

    private String extra;
}
