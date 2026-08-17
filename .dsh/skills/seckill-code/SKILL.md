---
name: seckill-code
description: "编写或修改 Java 代码、理解秒杀核心业务流程、遵循编码约定时使用。含包结构分层、构造器注入、Lombok、MyBatis-Plus 用法、防超卖 SQL 与事务、Redis 预留。"
---

# 代码结构与编码约定

包根：`org.example.monomer_seckill_backend`。

## 包结构分层

按 `common / config / entity / mapper / service / controller` 分层：

- `common/Result.java` — 统一响应体 `{ code, msg, data }`
- `config/RedisConfig.java` — `RedisTemplate<String,Object>`（String key + JSON value 序列化）
- `config/WebConfig.java` — 全局 CORS
- `entity/Goods.java`、`entity/SeckillOrder.java` — 实体，`@TableName` + `@TableId(type = IdType.AUTO)`
- `mapper/GoodsMapper.java` — 含 `deductStock` 原子扣库存 SQL；`mapper/SeckillOrderMapper.java`
- `service/GoodsService.java`、`service/SeckillService.java` — 业务
- `controller/` — `GoodsController`、`SeckillController`、`RedisController`

## 编码约定

- 依赖注入：**构造器注入**（不用 `@Autowired` 字段注入）。
- 使用 Lombok `@Data` 生成实体 getter/setter。
- MyBatis-Plus：实体用 `@TableName`、`@TableId(type = IdType.AUTO)`；复杂条件用 `LambdaQueryWrapper`。
- 对外接口统一返回 `Result<T>`。
- 注释与文档使用中文，SQL / 代码标识符使用英文。

## 核心业务流程（当前最简版）

`SeckillService.seckill(goodsId, userId)`，`@Transactional`：

1. 校验商品存在；
2. 查订单表校验是否重复抢购（唯一索引 `uk_goods_user` 兜底）；
3. `UPDATE seckill_goods SET stock = stock - 1 WHERE id = ? AND stock > 0`，返回 0 即库存不足——**防止超卖的核心**；
4. 插入订单，返回订单ID。

> Redis 当前**只做连通性预留**（`RedisConfig` + `/api/redis/ping`），未参与秒杀流程。
> P2 阶段再引入「Redis 预扣库存 + Lua 脚本」。
