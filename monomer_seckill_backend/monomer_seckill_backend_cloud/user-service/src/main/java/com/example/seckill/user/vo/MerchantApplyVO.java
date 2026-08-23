package com.example.seckill.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家申请视图对象（含申请人信息）。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class MerchantApplyVO {

    private Long id;

    private Long userId;

    /** 申请人用户名 */
    private String username;

    /** 申请人昵称 */
    private String nickname;

    private String reason;

    private Integer status;

    private LocalDateTime applyTime;

    private LocalDateTime reviewTime;

    private String reviewRemark;
}
