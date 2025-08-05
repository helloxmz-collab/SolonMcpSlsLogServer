package com.anker.sls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;


@MapperScan("com.anker.sls.mapper")
@SpringBootApplication
public class SolonAIMcpSlsServer {
    public static void main(String[] args) {
        SpringApplication.run(SolonAIMcpSlsServer.class, args);
    }
}