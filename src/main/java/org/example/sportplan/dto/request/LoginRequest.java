package org.example.sportplan.dto.request;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 * 用于接收前端用户登录的请求数据，包含账号和密码。
 */
@Data
public class LoginRequest {

    /** 登录账号 */
    @NotBlank(message = "账号不能为空")
    private String username;

    /** 登录密码（明文，服务端进行哈希后与存储的哈希值比对） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
