package com.hao14293.im.service.group.model.req;

import lombok.Data;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class GroupMemberDto {

    private String memberId;

    private String alias;

    /**
     * 群成员类型，0 普通成员, 1 管理员, 2 群主， 3 已经移除的成员，
     * 当修改群成员信息时，只能取值0/1，其他值由其他接口实现，暂不支持3
     */
    private Integer role;

    private Long speakDate;

    private String joinType;

    private Long joinTime;
}
