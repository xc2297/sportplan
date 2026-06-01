package org.example.sportplan.exception;

import org.example.sportplan.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一捕获 Controller 层抛出的各类异常，转换为标准的 ApiResponse 格式返回给前端。
 * 使用 @RestControllerAdvice 实现全局异常拦截，避免在每个 Controller 中重复编写异常处理代码。
 * 处理三类异常：
 * 1. BusinessException —— 业务异常，返回 400 状态码和具体错误信息
 * 2. MethodArgumentNotValidException —— 参数校验异常，返回 400 状态码和字段校验错误
 * 3. Exception —— 未知异常，返回 500 状态码和通用错误信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * 捕获 Service 层主动抛出的 BusinessException，返回 400 状态码。
     * 异常消息直接透传给前端展示（如"账号已存在"、"积分不足"等）。
     *
     * @param e 业务异常
     * @return 400 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage()));
    }

    /**
     * 处理参数校验异常
     * 捕获 @Valid 注解触发的参数校验失败异常，返回第一个字段的校验错误信息。
     * 例如：@NotBlank(message = "账号不能为空") 校验失败时返回 "username: 账号不能为空"。
     *
     * @param e 参数校验异常
     * @return 400 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        // 提取第一个校验失败的字段和错误信息
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, message));
    }

    /**
     * 处理未知异常
     * 捕获所有未被上述方法处理的异常，返回 500 状态码。
     * 用于兜底处理不可预期的系统异常（如空指针、数据库连接失败等）。
     *
     * @param e 未知异常
     * @return 500 服务器内部错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误: " + e.getMessage()));
    }
}
