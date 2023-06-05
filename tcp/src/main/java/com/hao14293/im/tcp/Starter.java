package com.hao14293.im.tcp;

import com.hao14293.im.codec.config.BootstrapConfig;
import com.hao14293.im.tcp.reciver.MessageReciver;
import com.hao14293.im.tcp.redis.RedisManager;
import com.hao14293.im.tcp.register.RegistryZk;
import com.hao14293.im.tcp.register.ZKit;
import com.hao14293.im.tcp.server.LimServer;
import com.hao14293.im.tcp.server.LimWebSocketServer;
import com.hao14293.im.tcp.utils.MqFactory;
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @Author: hao14293
 * @Date: 2023/6/5
 */

public class Starter {
    public static void main(String[] args) {
        String path = "src/main/resources/config.yml";
        start(path);
    }

    private static void start(String path) {
        try {
            Yaml yaml = new Yaml();
            FileInputStream fileInputStream = new FileInputStream(path);
            BootstrapConfig bootstrapConfig = yaml.loadAs(fileInputStream, BootstrapConfig.class);
            new LimServer(bootstrapConfig.getLim()).start();
            new LimWebSocketServer(bootstrapConfig.getLim()).start();

            // 启动redis
            RedisManager.init(bootstrapConfig);
            // 启动rabbitmq
            MqFactory.init(bootstrapConfig.getLim().getRabbitmq());
            // 启动MQ的reciver端
            MessageReciver.init(bootstrapConfig.getLim().getBrokerId() + "");
            // 注册ZK
            registerZK(bootstrapConfig);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(500);
        }


    }

    public static void registerZK(BootstrapConfig config) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient
                = new ZkClient(config.getLim().getZkConfig().getZkAddr(),
                config.getLim().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegistryZk registryZk = new RegistryZk(zKit, hostAddress, config.getLim());
        // 这是一个实现了多线程的方法，启动线程
        new Thread(registryZk).start();
    }
}
