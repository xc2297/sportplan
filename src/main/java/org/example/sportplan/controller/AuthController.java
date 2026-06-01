package org.example.sportplan.controller;

import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.LoginRequest;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.dto.response.LoginResponse;
import org.example.sportplan.dto.response.UserResponse;
import org.example.sportplan.entity.User;
import org.example.sportplan.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

/**
 * 认证控制器
 * 处理用户登录、登出和获取当前登录用户信息的请求。
 * 使用 Session 机制维持用户登录状态，登录成功后将用户ID存入 Session。
 * 所有接口路径前缀：/auth（不需要登录认证，在 WebConfig 中排除拦截）
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     * 验证账号密码，成功后将用户ID存入 Session，返回用户信息。
     *
     * @param request    登录请求，包含账号和密码
     * @param httpRequest HTTP请求对象，用于获取/创建 Session
     * @return 登录响应，包含用户信息
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        User user = userService.login(request.getUsername(), request.getPassword());
        // 将用户ID存入Session，标记为已登录状态
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", user.getId());
        UserResponse userResponse = userService.getUser(user.getId());
        return ApiResponse.success(new LoginResponse(userResponse));
    }

    /**
     * 用户登出
     * 使当前 Session 失效，清除登录状态。
     *
     * @param httpRequest HTTP请求对象，用于获取 Session
     * @return 空响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        // 销毁Session，清除登录态
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.success();
    }

    /**
     * 获取当前登录用户信息
     * 从 Session 中获取用户ID，查询并返回用户信息。
     * 前端在页面刷新时调用此接口恢复登录状态。
     *
     * @param httpRequest HTTP请求对象，用于获取 Session
     * @return 当前用户信息，未登录时返回401错误
     */
    @GetMapping("/current")
    public ApiResponse<UserResponse> current(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        // 检查Session中是否有用户ID（即是否已登录）
        if (session == null || session.getAttribute("userId") == null) {
            return ApiResponse.error(401, "未登录");
        }
        Long userId = (Long) session.getAttribute("userId");
        return ApiResponse.success(userService.getUser(userId));
    }
}
