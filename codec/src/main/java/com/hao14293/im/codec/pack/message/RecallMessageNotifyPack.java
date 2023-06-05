package com.hao14293.im.codec.pack.message;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Data
@NoArgsConstructor
public class RecallMessageNotifyPack {

    private String fromId;

    private String toId;

    private Long messageKey;

    private Long messageSequence;
}
