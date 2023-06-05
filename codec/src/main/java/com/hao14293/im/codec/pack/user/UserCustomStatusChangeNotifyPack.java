package com.hao14293.im.codec.pack.user;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */

@Data
public class UserCustomStatusChangeNotifyPack {

    private String customText;

    private Integer customStatus;

    private String userId;

}
