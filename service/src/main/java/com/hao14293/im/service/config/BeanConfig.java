package com.hao14293.im.service.config;

import com.hao14293.im.common.config.AppConfig;
import com.hao14293.im.common.enums.ImUrlRouteWayEnum;
import com.hao14293.im.common.enums.RouteHashMethodEnum;
import com.hao14293.im.common.route.RouteHandle;
import com.hao14293.im.common.route.algorithm.consistenthash.AbstractConsistentHash;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
//@Configuration
public class BeanConfig {

    @Resource
    private AppConfig appConfig;

    @Bean
    public RouteHandle routeHandle() throws Exception {
        // 获取配置文件中使用的哪个路由策略
        Integer imRouteWay = appConfig.getImRouteWay();
        // 使用的路由策略的具体的路径
        String routWay = "";
        // 通过配置文件中的路由策略的代表值去Enum获取到具体路径的类
        ImUrlRouteWayEnum handler = ImUrlRouteWayEnum.getHandler(imRouteWay);
        // 赋值给具体路径
        routWay = handler.getClazz();
        // 通过反射拿到路由策略的类
        RouteHandle routeHandle = (RouteHandle) Class.forName(routWay).newInstance();

        // 如果是hash策略的话，还要搞一个具体的hash算法
        if (handler == ImUrlRouteWayEnum.HASH){
            // 通过反射拿到ConsistentHashHandle中的方法
            Method method = Class.forName(routWay).getMethod("setAbstractConsistentHash", AbstractConsistentHash.class);
            // 从配置文件中拿到指定hash算法的代表值
            Integer consistentHashWay = appConfig.getConsistentHashWay();
            // 具体hash算法的类的路径
            String hashWay = "";
            // 通过Enue拿到对象
            RouteHashMethodEnum handler1 = RouteHashMethodEnum.getHandler(consistentHashWay);
            // 赋值
            hashWay = handler1.getClazz();
            // 通过反射拿到hash算法
            AbstractConsistentHash abstractConsistentHash = (AbstractConsistentHash) Class.forName(hashWay).newInstance();
            method.invoke(routeHandle, abstractConsistentHash);
        }

        return routeHandle;
    }
}
