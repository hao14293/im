package com.hao14293.im.service.group.controller;

import com.hao14293.im.common.ResponseVO;
import com.hao14293.im.service.group.model.req.ImportGroupReq;
import com.hao14293.im.service.group.service.ImGroupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Api(tags = "Group")
@RestController
@RequestMapping("/v1/group")
public class ImGroupController {

    @Resource
    private ImGroupService groupService;

    @ApiOperation(value = "导入群组")
    @PostMapping("/importGroup")
    public ResponseVO importGroup(@RequestBody ImportGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperater(identifier);
        return groupService.importGroup(req);
    }
}
