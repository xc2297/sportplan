package org.example.sportplan.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sportplan.dto.response.ApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录认证拦截器
 * 在请求到达 Controller 之前检查用户是否已登录。
 * 通过检查 Session 中是否存在 userId 来判断登录状态。
 * 未登录时直接返回 401 状态码和 JSON 错误信息，不转发到错误页面。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** JSON 序列化工具，用于将错误响应转为 JSON 字符串 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 请求预处理 —— 登录认证检查
     * 对于 OPTIONS 预检请求直接放行（支持 CORS）。
     * 对于其他请求，检查 Session 中是否存在 userId：
     * - 存在：已登录，放行请求
     * - 不存在：未登录，返回 401 状态码和错误信息
     *
     * @param request  当前HTTP请求
     * @param response HTTP响应
     * @param handler  目标处理器
     * @return true-放行请求，false-拦截请求
     * @throws Exception IO异常（写入响应时可能出现）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 放行 CORS 预检请求（浏览器跨域时会先发送 OPTIONS 请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 检查 Session 中是否有用户ID（登录成功时会设置）
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return true;  // 已登录，放行
        }

        // 未登录，返回 401 状态码和 JSON 格式的错误信息
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(401);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(401, "未登录")));
        return false;  // 拦截请求，不继续执行 Controller
    }
}
