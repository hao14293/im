package com.hao14293.im.service.group.service;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.group.model.req.CreateGroupReq;
import com.hao14293.im.service.group.model.req.ImportGroupReq;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
public interface ImGroupService {
    /**
     * 导入群
     */
    ResponseVO importGroup(ImportGroupReq req);

    /**
     * 新建群
     */
    ResponseVO createGroup(CreateGroupReq req);
}
