-- 秒杀商品表
CREATE TABLE IF NOT EXISTS `seckill_goods` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(128)  NOT NULL                COMMENT '商品名称',
  `description` VARCHAR(512)  DEFAULT NULL            COMMENT '商品描述',
  `price`       DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '价格',
  `stock`       INT           NOT NULL DEFAULT 0      COMMENT '库存',
  `start_time`  DATETIME      DEFAULT NULL            COMMENT '秒杀开始时间',
  `end_time`    DATETIME      DEFAULT NULL            COMMENT '秒杀结束时间',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS `seckill_order` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_id`    BIGINT   NOT NULL                COMMENT '商品ID',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
  `status`      TINYINT  NOT NULL DEFAULT 0      COMMENT '状态：0-已下单 1-已支付',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_user` (`goods_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';
