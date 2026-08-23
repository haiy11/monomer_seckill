package com.example.seckill.goodsorder.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商城用户实体（数据属主服务用于启动时种子数据），对应表 mall_user。
 *
 * <p>用户完整业务由 user-service 负责；本服务仅在共享库初始化种子数据时使用。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Data
@TableName("mall_user")
public class User {

    /** 主键，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录账号） */
    private String username;

    /** 密码（MD5 摘要，不落明文） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 角色：0-普通用户 1-商家 2-管理员 */
    private Integer role;

    /** 创建时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（应用自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
