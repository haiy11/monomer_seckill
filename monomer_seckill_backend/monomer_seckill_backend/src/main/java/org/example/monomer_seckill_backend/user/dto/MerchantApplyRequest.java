package org.example.monomer_seckill_backend.user.dto;

import lombok.Data;

/**
 * 申请成为商家请求。
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
public class MerchantApplyRequest {

    /** 申请理由 */
    private String reason;
}
