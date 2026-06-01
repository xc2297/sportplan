package org.example.sportplan.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.CreateUserRequest;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.dto.response.UserResponse;
import org.example.sportplan.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 * 处理用户的创建和查询请求。
 * 用户创建（注册）接口不需要登录认证，查询接口需要登录。
 * 所有接口路径前缀：/users
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 创建新用户（注册）
     * 此接口不需要登录认证（在 WebConfig 中排除了 /users 路径的拦截）。
     *
     * @param request 注册请求，包含账号、密码、姓名和性别
     * @return 新创建的用户信息
     */
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    /**
     * 查询所有用户列表
     * 用于用户选择器（如运动记录提交时选择用户）等场景。
     *
     * @return 所有用户信息列表
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.success(userService.getAllUsers());
    }

    /**
     * 根据ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.success(userService.getUser(id));
    }
}
