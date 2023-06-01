package com.hao14293.im.service.user.model.resp;

import lombok.Data;

import java.util.List;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@Data
public class ImportUserResp {

    private List<String> successId;

    private List<String> errorId;
}
