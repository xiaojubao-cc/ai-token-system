package com.ai.system.exception;

import cn.hutool.core.util.StrUtil;
import com.ai.system.exception.enums.GlobalErrorCodeConstants;
import com.ai.system.model.pojo.CommonResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，将异常统一转换为 CommonResult 格式返回给前端
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public CommonResult<?> handleServiceException(ServiceException ex, HttpServletRequest request) {
        logWarn(request, ex);
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 服务器异常
     */
    @ExceptionHandler(ServerException.class)
    public CommonResult<?> handleServerException(ServerException ex, HttpServletRequest request) {
        logError(request, ex);
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 参数校验异常（@Valid 注解）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logWarn(request, ex);
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public CommonResult<?> handleBindException(BindException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logWarn(request, ex);
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                StrUtil.isNotBlank(message) ? message : "参数校验失败");
    }

    /**
     * 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public CommonResult<?> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logWarn(request, ex);
        return CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN);
    }

    /**
     * 兜底处理未知异常
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception ex, HttpServletRequest request) {
        logError(request, ex);
        return CommonResult.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR);
    }

    private void logWarn(HttpServletRequest request, Exception ex) {
        log.warn("[{}] {} — {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
    }

    private void logError(HttpServletRequest request, Exception ex) {
        log.error("[{}] {} — {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
    }
}
