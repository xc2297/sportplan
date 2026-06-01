package org.example.sportplan.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 配置跨域访问（CORS）、登录认证拦截器和前端路由转发。
 * 前端使用 Vue Router 的 History 模式，需要将前端路由路径转发到 index.html。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 配置跨域访问规则
     * 允许所有来源的跨域请求，支持前后端分离开发。
     * 开发环境前端运行在5173端口，后端运行在8080端口，需要CORS支持。
     *
     * @param registry CORS注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")   // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的HTTP方法
                .allowedHeaders("*")           // 允许所有请求头
                .allowCredentials(true);       // 允许携带Cookie/Session
    }

    /**
     * 配置登录认证拦截器
     * 拦截所有API请求，排除不需要认证的路径（如登录、注册、静态资源）。
     * 排除路径说明：
     * - /auth/** — 登录登出相关接口
     * - /users — 用户注册接口（POST /users）
     * - /error — Spring Boot 默认错误页面
     * - /, /index.html, /assets/**, /favicon.svg, /icons.svg — 前端静态资源
     * - /login, /register, /record 等 — 前端路由路径（转发到 index.html）
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")       // 拦截所有路径
                .excludePathPatterns(          // 排除不需要登录认证的路径
                        "/auth/**",
                        "/users",
                        "/error",
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/favicon.svg",
                        "/icons.svg",
                        "/login",
                        "/register",
                        "/record",
                        "/rewards",
                        "/reward-admin",
                        "/exercise-admin",
                        "/profile",
                        "/team"
                );
    }

    /**
     * 配置前端路由转发
     * Vue Router 使用 History 模式，需要将所有前端路由路径转发到 index.html，
     * 由前端 JavaScript 处理路由解析。
     *
     * @param registry 视图控制器注册器
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将前端路由路径转发到 index.html，支持 Vue Router 的 History 模式
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/register").setViewName("forward:/index.html");
        registry.addViewController("/record").setViewName("forward:/index.html");
        registry.addViewController("/rewards").setViewName("forward:/index.html");
        registry.addViewController("/reward-admin").setViewName("forward:/index.html");
        registry.addViewController("/exercise-admin").setViewName("forward:/index.html");
        registry.addViewController("/profile").setViewName("forward:/index.html");
        registry.addViewController("/team").setViewName("forward:/index.html");
    }

    /**
     * 配置静态资源缓存策略
     * 对 index.html 设置 no-cache，确保每次都加载最新版本。
     * 对 assets 下的 JS/CSS 文件保留缓存（Vite 已自带 hash 文件名）。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(31536000);
    }
}
