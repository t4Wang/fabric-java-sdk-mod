package com.routz.fabric_java_sdk_integration;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.routz.fabric_java_sdk_integration.mapper")
@Slf4j
public class FabApplication {

    public static void main(String[] args) {
        SpringApplication.run(FabApplication.class, args);
    }

}
