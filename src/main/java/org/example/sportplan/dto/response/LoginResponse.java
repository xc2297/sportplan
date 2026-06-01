package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 * 登录成功后返回给前端的数据，包含当前登录用户的基本信息。
 * 前端收到后保存用户信息用于页面展示和权限判断。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** 登录成功的用户信息 */
    private UserResponse user;
}
