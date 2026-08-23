package com.example.seckill.common.core;

import lombok.Data;

/**
 * 统一响应结果体。
 *
 * <p>所有接口统一返回该结构，约定 code=200 表示成功，其余为业务失败，
 * 便于前端与跨服务 Feign 调用方统一处理成功/失败分支。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class Result<T> {

    /** 状态码：200 成功，500 业务失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T data;

    /**
     * 成功响应（携带数据）。
     */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    /**
     * 成功响应（无数据）。
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 失败响应。
     */
    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }

    /**
     * 失败响应（自定义状态码）。
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
