# monomer_seckill_backend_cloud

秒杀项目 **P3 阶段**：单体 → 微服务拆分 + Nacos 注册发现 + OpenFeign 远程调用。

原单体代码保留在 `../monomer_seckill_backend/`，未做任何改动；本目录是从单体拆分出的微服务工程。

## 一、模块划分与职责边界

| 模块 | 服务名 / 端口 | 职责 |
|------|--------------|------|
| `common-service` | （公共库，不注册） | **仅系统级基础设施**：统一响应体、异常、用户上下文、认证（Token/AuthInterceptor）、Redis 工具、CORS、MyBatis 字段填充。不含任何业务领域代码 |
| `user-service` | `user-service` / **7001** | 用户 + 管理员 + 商家（人员相关）。持有 `mall_user`、`merchant_apply`；商品/订单/秒杀商品的审核与管理经 **Feign** 调对应服务 |
| `goods-order-service` | `goods-order-service` / **7002** | 商品 + 购物车 + 正常订单（业务紧密）。持有 `goods`、`cart_item`、`mall_order`、`order_item`，并暴露内部审核/管理接口 |
| `seckill-service` | `seckill-service` / **7003** | 秒杀（高并发流量隔离）。持有 `seckill_goods`、`seckill_order`、Redis 预扣库存、Lua 脚本、秒杀商品查询、秒杀订单全生命周期，并暴露内部审核/管理接口 |

### 设计原则

- **公共模块只放跨服务复用的基础设施**，不承载任何实体/Mapper/业务 Service/VO/常量/Lua。
- **每个服务拥有自己的实体、Mapper、Service、VO、常量**，跨服务访问通过 **Feign（HTTP）**，不共享 Mapper。
- 修改某个服务的数据访问或业务逻辑，只需重新构建/部署该服务，不影响其它服务。

## 二、调用链（OpenFeign）

**秒杀下单链路（秒杀域内闭环，无跨服务调用）**

```
客户端 → seckill-service (7003)  POST /api/seckill/{goodsId}
            ① 校验可购买 → ② Redis 预扣 → ③ DB 扣减+建单 → ④ 失败回滚
            秒杀订单支付/取消/超时关闭也在本服务内完成
```

**审核/管理链路（user-service 通过 Feign 调两个服务）**

```
user-service (7001)
   │  GoodsOrderClient ──► goods-order-service：管理员审核商品/查看订单、商家管理商品
   │  SeckillClient    ──► seckill-service：管理员审核秒杀商品/查看秒杀订单/重置库存、商家管理秒杀商品
```

- `GoodsOrderClient`（`@FeignClient(name="goods-order-service")`）调用 `/internal/admin/**`、`/internal/merchant/**`。
- `SeckillClient`（`@FeignClient(name="seckill-service")`）调用 `/internal/admin/**`、`/internal/merchant/**`。
- 内部接口统一返回 `Result<T>`，避免把「商品不存在/已处理」等业务语义映射成 HTTP 5xx，导致 Feign 侧无法区分。

## 三、接口分布（P4 网关接入前，各服务端口直连）

| 方法 | 路径 | 所在服务 |
|------|------|---------|
| POST/GET | `/api/user/**` | user-service :7001 |
| POST/GET | `/api/admin/**` | user-service :7001 |
| GET/POST/PUT | `/api/merchant/**` | user-service :7001 |
| GET | `/api/goods` `/api/goods/{id}` | goods-order-service :7002 |
| GET/POST/PUT/DELETE | `/api/cart/**` | goods-order-service :7002 |
| GET/POST | `/api/order/**`（正常订单） | goods-order-service :7002 |
| GET | `/api/seckill-goods` `/api/seckill-goods/{id}` | seckill-service :7003 |
| POST | `/api/seckill/{goodsId}` | seckill-service :7003 |
| GET/POST | `/api/seckill/order/**`（秒杀订单） | seckill-service :7003 |

## 四、启动步骤

1. 保证中间件就绪：MySQL(`localhost:3307`)、Redis(`localhost:6379`)、Nacos(`localhost:8848`)。
2. 切换 JDK 21 + Maven 3.9.10（见 `seckill-build-run` skill）。
3. **先启动 `goods-order-service`**（负责建表 + 用户/商品种子数据），再启动 `seckill-service`（秒杀商品种子 + 库存预载）、`user-service`。

```powershell
# 根目录一键打包
mvn -DskipTests clean package

# 分别启动
cd goods-order-service ; mvn spring-boot:run
cd seckill-service     ; mvn spring-boot:run
cd user-service        ; mvn spring-boot:run
```

## 五、设计说明（P3 阶段取舍）

- **共享单个 MySQL 库**（用户选择）：`goods-order-service` 作为数据属主负责 `schema.sql`（建全部表）+「用户/正常商品」种子数据；`seckill-service` 负责「秒杀商品」种子数据；其余服务 `spring.sql.init.mode=never`。
- **跨服务访问走 Feign**：管理员/商家的审核与管理操作经 OpenFeign 调数据属主服务，user-service 不再持有商品/订单的 Mapper 副本，只在本地保留 Feign 响应所需的 DTO（少量契约重复）。
- **秒杀域完整下沉**：秒杀商品、Redis 预扣库存、秒杀订单的创建/支付/取消/超时全部归 seckill-service，保证高并发路径与其它服务隔离。

## 六、后续阶段衔接

- P4：新增 `gateway-service` 统一入口 + JWT 鉴权，收敛上面的多端口直连。
- P7：把 `application.yml` 迁移到 Nacos Config。
- P8：秒杀下单改 MQ 异步削峰。
