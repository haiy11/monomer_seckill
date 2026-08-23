# monomer_seckill_backend_cloud

秒杀项目 **P3 阶段**：单体 → 微服务拆分 + Nacos 注册发现 + OpenFeign 远程调用。

原单体代码保留在 `../monomer_seckill_backend/`，未做任何改动；本目录是从单体拆分出的微服务工程。

## 一、模块划分

| 模块 | 服务名 / 端口 | 职责 |
|------|--------------|------|
| `seckill-common` | （公共库，不注册） | 统一响应体、异常、常量、Redis 工具、认证拦截器、**共享实体与 Mapper**、共享秒杀商品/库存服务、Lua 脚本 |
| `user-service` | `user-service` / **7001** | 用户注册登录、商家申请，以及**管理员审核、商家商品管理**等人员相关业务 |
| `goods-order-service` | `goods-order-service` / **7002** | 商品、秒杀商品、购物车、订单（含秒杀订单）业务，并暴露内部扣库存/下单接口 |
| `seckill-service` | `seckill-service` / **7003** | 秒杀下单入口（高并发流量隔离），编排调用 goods-order-service |

拆分遵循用户约定：

- **公共类 / 配置类**抽取到 `seckill-common`，其余服务通过依赖引入复用，避免重复定义；
- **user / admin / merchant**（人员相关）合并为 `user-service`；
- **goods / cart / order**（业务紧密）合并为 `goods-order-service`；
- **seckill**（高并发，最易出问题）单独拆为 `seckill-service`，隔离流量。

## 二、调用链（OpenFeign）

```
客户端 → seckill-service (7003)
            │  POST /api/seckill/{goodsId}
            ▼
        seckill-service 编排：
            ① getPurchasable()      ─┐
            ② deductStock()          │  OpenFeign (Nacos 服务发现)
            ③ createSeckillOrder()   │
            ④ rollbackRedis() 失败时 ─┘
            ▼
        goods-order-service (7002)
            /internal/... 内部接口 → Redis 预扣(Lua) + DB 扣减 + 建单
```

秒杀下单主链路为 `seckill-service → goods-order-service`，是 P3 阶段演示的 Feign 远程调用链。

## 三、接口分布（P4 网关接入前，各服务端口直连）

| 方法 | 路径 | 所在服务 |
|------|------|---------|
| POST | `/api/user/register` `/api/user/login` `/api/user/info` `/api/user/apply-merchant` | user-service :7001 |
| POST/GET | `/api/admin/**` | user-service :7001 |
| GET/POST/PUT | `/api/merchant/**` | user-service :7001 |
| GET | `/api/goods` `/api/goods/{id}` `/api/seckill-goods` `/api/seckill-goods/{id}` | goods-order-service :7002 |
| GET/POST/PUT/DELETE | `/api/cart/**` | goods-order-service :7002 |
| GET/POST | `/api/order/**` | goods-order-service :7002 |
| POST | `/api/seckill/{goodsId}` | seckill-service :7003 |

## 四、启动步骤

1. 保证中间件就绪：MySQL(`localhost:3307`)、Redis(`localhost:6379`)、Nacos(`localhost:8848`)。
2. 切换 JDK 21 + Maven 3.9.10（见 `seckill-build-run` skill）。
3. 先启动 `goods-order-service`（负责建表 + 种子数据 + 库存预载），再启动 `user-service`、`seckill-service`（顺序无所谓）。

```powershell
# 根目录一键打包
mvn -DskipTests clean package

# 分别启动
cd goods-order-service ; mvn spring-boot:run
cd user-service        ; mvn spring-boot:run
cd seckill-service     ; mvn spring-boot:run
```

## 五、设计说明（P3 阶段取舍）

- **共享单个 MySQL 库**（用户选择）：四个服务连同一个 `monomer_seckill` 库；`goods-order-service` 作为数据属主负责 `schema.sql`/种子初始化，其余服务 `spring.sql.init.mode=never`。
- **共享数据模型下沉 common**：实体 + Mapper 放入 `seckill-common`（shared kernel），减少各服务重复定义；user-service 的管理员/商家直接复用这些 Mapper 访问商品/订单表。
- **seckill-service 无 DB 依赖**：只依赖 Redis（token 校验），商品/库存/订单全部经 Feign 调 goods-order-service，实现高并发入口与数据侧解耦。
- **内部接口返回 `Result<T>`**：避免把「库存不足/重复抢购」等业务语义映射成 HTTP 5xx，导致 Feign 侧无法区分。

## 六、后续阶段衔接

- P4：新增 `gateway-service` 统一入口 + JWT 鉴权，收敛上面的多端口直连。
- P7：把 `application.yml` 迁移到 Nacos Config。
- P8：秒杀下单改 MQ 异步削峰，进一步解耦 seckill-service 与 goods-order-service。
