package org.example.monomer_seckill_backend.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家申请实体，对应表 merchant_apply。
 *
 * <p>普通用户申请成为商家，管理员审核；审核通过后用户角色由 0 变为 1。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
@TableName("merchant_apply")
public class MerchantApply {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人用户ID */
    private Long userId;

    /** 申请理由 */
    private String reason;

    /** 状态：0-待审核 1-通过 2-拒绝 */
    private Integer status;

    /** 申请时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 审核备注 */
    private String reviewRemark;
}
