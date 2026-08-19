package org.example.monomer_seckill_backend.common;

/**
 * 业务异常。
 *
 * <p>业务逻辑校验失败时抛出，由 {@link GlobalExceptionHandler} 统一转换为 {@link Result} 返回，
 * 避免 Service 层到处 return Result.fail(...)，也让异常与正常返回语义更清晰。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
public class BizException extends RuntimeException {

    /** 业务错误码，默认 500 */
    private final Integer code;

    public BizException(String message) {
        this(500, message);
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
