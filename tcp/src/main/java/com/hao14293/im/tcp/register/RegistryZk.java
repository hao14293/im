package com.hao14293.im.tcp.register;

import com.hao14293.im.codec.config.BootstrapConfig;
import com.hao14293.im.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */
@Slf4j
public class RegistryZk implements Runnable{

    private ZKit zKit;

    private String ip;

    private BootstrapConfig.TcpConfig tcpConfig;

    public RegistryZk(ZKit zKit, String ip, BootstrapConfig.TcpConfig tcpConfig) {
        this.zKit = zKit;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }

    @Override
    public void run() {
        // 注册Zookeeper
        // 先注册1级目录
        zKit.createRootNode();

        // 再注册2级目录
        String tcpPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootTcp
                + "/" + ip + ":" + this.tcpConfig.getTcpPort();
        zKit.createNode(tcpPath);
        log.info("Registry zookeeper tcpPath success, msg=[{}]", tcpPath);

        String webPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootWeb
                + "/" + ip + ":" + this.tcpConfig.getWebSocketPort();
        zKit.createNode(webPath);
        log.info("Registry zookeeper webPath success, msg=[{}]", webPath);
    }
}
