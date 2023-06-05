package com.hao14293.im.tcp.handler;

import com.hao14293.im.common.constant.Constants;
import com.hao14293.im.tcp.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    private Long heartBeatTime;

    public HeartBeatHandler(Long heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }


    /**
     * 触发心跳检测
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断evt是否是IdleStateEvent（用于触发用户事件，包含 读空闲/写空闲/读写空闲 ）
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state() == IdleState.READER_IDLE) {
                log.info("读空闲");
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.info("写空闲");
            } else if (event.state() == IdleState.ALL_IDLE) {
                // 最后一次写入
                Long lastReadTime
                        = (Long) ctx.attr(AttributeKey.valueOf(Constants.ReadTime)).get();
                // 现在的时间
                long now = System.currentTimeMillis();
                // 如果最后一次写入的时间不为空
                if (lastReadTime != null && now - lastReadTime > heartBeatTime) {
                    // 离线
                    SessionSocketHolder.offLineUserSession((NioSocketChannel) ctx.channel());
                }
            }
        }
    }
}
