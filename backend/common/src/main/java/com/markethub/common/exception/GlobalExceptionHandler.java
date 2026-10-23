package com.markethub.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/** 全局异常处理：统一返回 R，并保留正确的 HTTP 状态码。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验失败返回 400，避免掉进兜底被报成 500。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return errorResponse(HttpStatus.BAD_REQUEST, 400, message);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<R<?>> handleUnreadableBody(org.springframework.http.converter.HttpMessageNotReadableException e) {
        // 请求体不是合法 JSON（或用错字符集传了中文）时，这属于客户端问题，不该报成 500
        log.warn("请求体解析失败: {}", e.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, 400, "请求体格式不正确");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<?>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        int code = e.getCode() == null ? HttpStatus.BAD_REQUEST.value() : e.getCode();
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null || !status.isError()) {
            status = HttpStatus.BAD_REQUEST;
        }
        return errorResponse(status, code, e.getMessage());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<R<?>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return errorResponse(HttpStatus.FORBIDDEN, 403, "权限不足");
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<R<?>> handleAuthenticationException(org.springframework.security.core.AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return errorResponse(HttpStatus.UNAUTHORIZED, 401, "认证失败，请重新登录");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<R<?>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 500, "服务器内部错误");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<?>> handleException(Exception e) {
        log.error("系统异常: ", e);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 500, "系统内部错误");
    }

    private ResponseEntity<R<?>> errorResponse(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status).body(R.error(code, message));
    }
}
