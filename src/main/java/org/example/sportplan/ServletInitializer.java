package org.example.sportplan;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Servlet 初始化器
 * 用于将 Spring Boot 应用打包为 WAR 部署到外部 Servlet 容器（如 Tomcat）时使用。
 * 继承 SpringBootServletInitializer 并指定主应用类，使外部容器能够正确启动 Spring Boot 应用。
 * 当使用 java -jar 方式运行时，此类不会被使用。
 */
public class ServletInitializer extends SpringBootServletInitializer {

    /**
     * 配置 Spring Boot 应用的启动源
     * 指定 SportplanApplication 作为主配置类。
     *
     * @param application Spring Boot 应用构建器
     * @return 配置后的构建器
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SportplanApplication.class);
    }

}
