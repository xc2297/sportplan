package org.example.sportplan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 运动积分奖惩系统 - 主启动类
 * Spring Boot 应用的入口点，使用 @SpringBootApplication 注解启用自动配置和组件扫描。
 * 扫描范围：org.example.sportplan 包及其子包下的所有组件。
 * 支持两种运行方式：
 * 1. java -jar 方式运行（内嵌 Tomcat）
 * 2. WAR 包部署到外部 Servlet 容器（通过 ServletInitializer 配置）
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("org.example.sportplan.mapper")
public class SportplanApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SportplanApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SportplanApplication.class, args);
    }

}
