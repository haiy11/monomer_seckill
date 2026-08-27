-- ============================================================
-- 秒杀商城 建表脚本（P3 微服务拆分：共享单体数据库）
--
-- 由 goods-order-service（数据属主）负责执行，其余服务 mode=never。
-- 结构有变更的表（goods、seckill_goods、seckill_order）开发期每次启动重建，
-- 其余表用 CREATE TABLE IF NOT EXISTS 幂等创建。
-- ============================================================

DROP TABLE IF EXISTS `goods`;
DROP TABLE IF EXISTS `seckill_goods`;
DROP TABLE IF EXISTS `seckill_order`;

-- ------------------------------------------------------------
-- 用户表（三种角色：0-普通用户 1-商家 2-管理员）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(64)  NOT NULL                COMMENT '用户名（登录账号）',
  `password`    VARCHAR(128) NOT NULL                COMMENT '密码（MD5 摘要）',
  `nickname`    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `role`        TINYINT      NOT NULL DEFAULT 0      COMMENT '角色：0-普通用户 1-商家 2-管理员',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城用户表';

-- ------------------------------------------------------------
-- 商家申请表（普通用户申请成为商家，管理员审核）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `merchant_apply` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT       NOT NULL                COMMENT '申请人用户ID',
  `reason`        VARCHAR(256) DEFAULT NULL            COMMENT '申请理由',
  `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-待审核 1-通过 2-拒绝',
  `apply_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `review_time`   DATETIME     DEFAULT NULL            COMMENT '审核时间',
  `review_remark` VARCHAR(256) DEFAULT NULL            COMMENT '审核备注',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家申请表';

-- ------------------------------------------------------------
-- 商品表（正常商品，归属商家；管理员审核上架）
-- ------------------------------------------------------------
CREATE TABLE `goods` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` BIGINT        NOT NULL                COMMENT '商家用户ID',
  `name`        VARCHAR(128)  NOT NULL                COMMENT '商品名称',
  `description` VARCHAR(512)  DEFAULT NULL            COMMENT '商品描述',
  `price`       DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '价格',
  `stock`       INT           NOT NULL DEFAULT 0      COMMENT '库存',
  `image_url`   VARCHAR(512)  DEFAULT NULL            COMMENT '商品图片地址',
  `status`      TINYINT       NOT NULL DEFAULT 0      COMMENT '状态：0-待审核 1-已上架 2-已下架 3-已拒绝',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ------------------------------------------------------------
-- 秒杀商品表（与正常商品相互独立，商家直接填写，管理员审核上架）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `seckill_goods` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id`   BIGINT        NOT NULL                COMMENT '商家用户ID',
  `name`          VARCHAR(128)  NOT NULL                COMMENT '秒杀商品名称',
  `seckill_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '秒杀价',
  `seckill_stock` INT           NOT NULL DEFAULT 0      COMMENT '秒杀库存',
  `start_time`    DATETIME      NOT NULL                COMMENT '秒杀开始时间',
  `end_time`      DATETIME      NOT NULL                COMMENT '秒杀结束时间',
  `status`        TINYINT       NOT NULL DEFAULT 0      COMMENT '状态：0-待审核 1-已上架 2-已拒绝 3-已下架/已结束',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- ------------------------------------------------------------
-- 购物车表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cart_item` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
  `goods_id`    BIGINT   NOT NULL                COMMENT '商品ID',
  `quantity`    INT      NOT NULL DEFAULT 1      COMMENT '购买数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ------------------------------------------------------------
-- 订单主表（正常商品下单）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mall_order` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`     VARCHAR(64)   NOT NULL                COMMENT '订单号（全局唯一）',
  `user_id`      BIGINT        NOT NULL                COMMENT '下单用户ID',
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '订单总金额',
  `status`       TINYINT       NOT NULL DEFAULT 0      COMMENT '状态：0-待支付 1-已支付 2-已取消 3-超时关闭',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time`     DATETIME      DEFAULT NULL            COMMENT '支付时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- ------------------------------------------------------------
-- 订单明细表（一个订单多件商品）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT        NOT NULL                COMMENT '订单ID',
  `goods_id`    BIGINT        NOT NULL                COMMENT '商品ID',
  `goods_name`  VARCHAR(128)  NOT NULL                COMMENT '商品名称（下单快照）',
  `price`       DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '成交单价',
  `quantity`    INT           NOT NULL DEFAULT 1      COMMENT '购买数量',
  `amount`      DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '小计金额',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ------------------------------------------------------------
-- 秒杀订单表（每个用户对每个秒杀活动仅一单）
-- ------------------------------------------------------------
CREATE TABLE `seckill_order` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`         VARCHAR(64)   NOT NULL                COMMENT '订单号（全局唯一）',
  `user_id`          BIGINT        NOT NULL                COMMENT '用户ID',
  `seckill_goods_id` BIGINT        NOT NULL                COMMENT '秒杀商品ID',
  `seckill_price`    DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '成交价（秒杀价快照）',
  `status`           TINYINT       NOT NULL DEFAULT 0      COMMENT '状态：0-待支付 1-已支付 2-已取消 3-超时关闭',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time`         DATETIME      DEFAULT NULL            COMMENT '支付时间',
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  -- 有效订单去重键：仅「待支付(0)/已支付(1)」生成值，已取消(2)/超时关闭(3)为 NULL
  -- （MySQL 唯一索引允许多个 NULL）：取消/超时关闭后旧订单不再占用名额，用户可再次下单；
  -- 已支付订单仍受约束，保持每用户每秒杀活动限购一单。
  `dedup_key`        VARCHAR(64)   GENERATED ALWAYS AS (
      CASE WHEN `status` IN (0, 1) THEN CONCAT(`seckill_goods_id`, '#', `user_id`) ELSE NULL END
  ) STORED COMMENT '有效订单去重键（待支付/已支付唯一，取消/超时关闭为 NULL）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_seckill_user_active` (`dedup_key`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';
