# monomer_seckill_backend_cloud

秒杀项目 **P3 阶段**：单体 → 微服务拆分 + Nacos 注册发现 + OpenFeign 远程调用。

原单体代码保留在 `../monomer_seckill_backend/`，未做任何改动；本目录是从单体拆分出的微服务工程。

## 一、模块划分与职责边界

| 模块 | 服务名 / 端口 | 职责 |
|------|--------------|------|
| `common-service` | （公共库，不注册） | **仅系统级基础设施**：统一响应体、异常、用户上下文、认证（Token/AuthInterceptor）、Redis 工具、CORS、MyBatis 字段填充。不含任何业务领域代码 |
| `user-service` | `user-service` / **7001** | 用户 + 管理员 + 商家（人员相关）。持有 `mall_user`、`merchant_apply`，以及审核/管理所需的 `goods`、`seckill_goods`、`mall_order`、`seckill_order` 视图与 Mapper |
| `goods-order-service` | `goods-order-service` / **7002** | 商品 + 购物车 + 订单（业务紧密）。持有 `goods`、`cart_item`、`mall_order`、`order_item`、`seckill_order`，并负责秒杀订单落库与 DB 库存扣减/回补 |
| `seckill-service` | `seckill-service` / **7003** | 秒杀（高并发流量隔离）。持有 `seckill_goods`、Redis 预扣库存、Lua 脚本、秒杀商品查询 |

### 设计原则

- **公共模块只放跨服务复用的基础设施**，不承载任何实体/Mapper/业务 Service/VO/常量/Lua。
- **每个服务拥有自己用到的实体、Mapper、常量、Service、VO、Lua**（共享库下个别表被多个服务访问时，各自持有自己的访问代码副本，互不影响）。
- 修改某个服务的数据访问或业务逻辑，只需重新构建/部署该服务，不影响其它服务。

## 二、调用链（OpenFeign，双向）

```
客户端 → seckill-service (7003)
            │  POST /api/seckill/{goodsId}
            │  ① 本地校验可购买（SeckillGoodsService）
            │  ② 本地 Redis 预扣库存（StockService + Lua）
            ▼
        goods-order-service (7002)  ◄── OpenFeign createSeckillOrder
            │  DB 扣减 seckill_goods 库存 + 落库 seckill_order
            │
        （取消 / 超时关闭秒杀订单时）
        goods-order-service ── OpenFeign rollbackRedis ──► seckill-service (7003)
```

- `seckill-service → goods-order-service`：Redis 预扣成功后，建单落库（DB 扣减 + 插订单）。
- `goods-order-service → seckill-service`：取消/超时关闭秒杀订单时，回滚 Redis 预扣库存。

## 三、接口分布（P4 网关接入前，各服务端口直连）

| 方法 | 路径 | 所在服务 |
|------|------|---------|
| POST/GET | `/api/user/**` | user-service :7001 |
| POST/GET | `/api/admin/**` | user-service :7001 |
| GET/POST/PUT | `/api/merchant/**` | user-service :7001 |
| GET | `/api/goods` `/api/goods/{id}` | goods-order-service :7002 |
| GET/POST/PUT/DELETE | `/api/cart/**` | goods-order-service :7002 |
| GET/POST | `/api/order/**` | goods-order-service :7002 |
| GET | `/api/seckill-goods` `/api/seckill-goods/{id}` | seckill-service :7003 |
| POST | `/api/seckill/{goodsId}` | seckill-service :7003 |

## 四、启动步骤

1. 保证中间件就绪：MySQL(`localhost:3307`)、Redis(`localhost:6379`)、Nacos(`localhost:8848`)。
2. 切换 JDK 21 + Maven 3.9.10（见 `seckill-build-run` skill）。
3. **先启动 `goods-order-service`**（负责建表 + 种子数据），再启动 `seckill-service`（预载秒杀库存到 Redis）、`user-service`。

```powershell
# 根目录一键打包
mvn -DskipTests clean package

# 分别启动
cd goods-order-service ; mvn spring-boot:run
cd seckill-service     ; mvn spring-boot:run
cd user-service        ; mvn spring-boot:run
```

## 五、设计说明（P3 阶段取舍）

- **共享单个 MySQL 库**（用户选择）：`goods-order-service` 作为数据属主负责 `schema.sql` + 种子数据初始化，其余服务 `spring.sql.init.mode=never`。
- **共享库下的代码副本**：`goods`、`seckill_goods`、`mall_order`、`seckill_order` 等表被多个服务访问，各服务在自身模块内持有对应的实体/Mapper 副本，换取部署与演进的独立性（避免回到共享内核式耦合）。
- **秒杀订单生命周期归属**：`seckill_goods`/Redis 预扣在 seckill-service，`seckill_order` 落库与生命周期在 goods-order-service，两者经 Feign 协作；取消/超时的 DB 回补与 Redis 回滚分别落在两个服务。
- **内部接口返回 `Result<T>`**：避免把「库存不足/重复抢购」等业务语义映射成 HTTP 5xx，导致 Feign 侧无法区分。

## 六、后续阶段衔接

- P4：新增 `gateway-service` 统一入口 + JWT 鉴权，收敛上面的多端口直连。
- P7：把 `application.yml` 迁移到 Nacos Config。
- P8：秒杀下单改 MQ 异步削峰，进一步解耦 seckill-service 与 goods-order-service。
