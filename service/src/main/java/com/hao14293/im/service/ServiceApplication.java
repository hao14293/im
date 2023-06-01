package com.hao14293.im.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @Author: hao14293
 * @Date: 2023/6/1
 */
@SpringBootApplication(scanBasePackages = {"com.hao14293.im.service"})
@EnableTransactionManagement
public class ServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }
}
