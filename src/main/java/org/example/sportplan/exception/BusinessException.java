package org.example.sportplan.exception;

/**
 * 业务异常类
 * 用于封装业务逻辑中出现的可预期异常（如"账号已存在"、"积分不足"等）。
 * 继承 RuntimeException（非受检异常），不需要在方法签名中声明。
 * 由 GlobalExceptionHandler 统一捕获并转换为 400 错误响应返回给前端。
 */
public class BusinessException extends RuntimeException {

    /**
     * 构造业务异常
     *
     * @param message 异常消息，会直接返回给前端展示
     */
    public BusinessException(String message) {
        super(message);
    }
}
