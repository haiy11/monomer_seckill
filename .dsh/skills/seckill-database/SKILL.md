---
name: seckill-database
description: "建表、改表结构、写或改 SQL、查询数据、理解表设计时使用。含 seckill_goods 与 seckill_order 表结构、字段说明、唯一索引、幂等初始化 SQL。"
---

# 数据库设计

数据库名：`monomer_seckill`（后端启动时自动创建）。

## seckill_goods（秒杀商品）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| name | VARCHAR(128) | 商品名称 |
| description | VARCHAR(512) | 描述 |
| price | DECIMAL(10,2) | 价格 |
| stock | INT | 库存 |
| start_time / end_time | DATETIME | 秒杀起止时间（暂未做校验） |
| create_time | DATETIME | 创建时间 |

## seckill_order（秒杀订单）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| goods_id | BIGINT | 商品ID |
| user_id | BIGINT | 用户ID |
| status | TINYINT | 0-已下单 1-已支付 |
| create_time | DATETIME | 下单时间 |

- `uk_goods_user (goods_id, user_id)` 唯一索引：**重复抢购的最终兜底**。

## 幂等初始化 SQL

- 建表：`src/main/resources/schema.sql`（`CREATE TABLE IF NOT EXISTS`）
- 种子数据：`src/main/resources/data.sql`（`INSERT IGNORE`，3 条商品：iPhone 15 Pro 库存100、小米14 库存50、AirPods Pro 2 库存30）

由 `spring.sql.init.mode=always` 每次启动执行。

## 压测后数据校验

校验 SQL 见 `docs/check-data.sql`（校验不超卖、不多卖、不重复下单）。
