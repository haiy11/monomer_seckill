package com.example.seckill.common.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>把 Controller 层抛出的异常统一转换为 {@link Result} 结构返回，
 * 保证调用方始终拿到 {code, msg, data} 而非堆栈信息。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：直接透传错误码与提示信息。
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 唯一索引冲突（重复下单 / 用户名重复等）：给出友好提示。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("唯一索引冲突: {}", e.getMessage());
        return Result.fail("操作冲突：数据已存在（可能为重复下单或用户名已被占用）");
    }

    /**
     * 兜底异常：记录日志，避免堆栈直接暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
