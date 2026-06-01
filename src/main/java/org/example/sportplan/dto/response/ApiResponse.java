package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应包装类
 * 所有后端接口的返回值都使用此类包装，提供统一的响应格式。
 * 包含状态码、消息和数据三部分。
 * 成功时 code=200，失败时 code 为对应的 HTTP 状态码（如400、401、500）。
 *
 * @param <T> 响应数据的类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /** 响应状态码，200-成功，400-业务错误，401-未登录，500-服务器错误 */
    private int code;

    /** 响应消息，成功时为"success"，失败时为具体错误描述 */
    private String message;

    /** 响应数据，成功时携带业务数据，失败时为null */
    private T data;

    /**
     * 构建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 构建成功响应（无数据）
     * 用于删除、登出等不需要返回数据的操作。
     *
     * @param <T> 数据类型
     * @return 成功响应（data为null）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null);
    }

    /**
     * 构建错误响应
     *
     * @param code    错误状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误响应（data为null）
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
