package org.example.sportplan.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建用户（注册）请求 DTO
 * 用于接收前端用户注册的请求数据。
 * 账号全局唯一，密码使用 SHA-256 哈希后存储。
 */
@Data
public class CreateUserRequest {

    /** 登录账号，全局唯一 */
    @NotBlank(message = "账号不能为空")
    private String username;

    /** 登录密码（明文传输，服务端进行 SHA-256 哈希后存储） */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 用户真实姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 性别，值为 "male" 或 "female"（不区分大小写），影响运动积分计算系数 */
    @NotNull(message = "性别不能为空")
    private String gender;
}
