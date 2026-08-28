# monomer_seckill_backend_cloud

秒杀项目 **P8 阶段**：微服务拆分 + Nacos 注册发现 + OpenFeign 远程调用 + 统一流量入口（Spring Cloud Gateway + JWT 鉴权）+ 高可用限流熔断（Sentinel）+ 多级缓存（Caffeine 本地缓存 + Redis 分布式缓存）+ 配置中心（Nacos Config 动态刷新）+ 异步削峰解耦（RabbitMQ + 死信队列）。

原单体代码保留在 `../monomer_seckill_backend/`，未做任何改动；本目录是从单体拆分出的微服务工程。

## 一、模块划分与职责边界

| 模块 | 服务名 / 端口 | 职责 |
|------|--------------|------|
| `common-service` | （公共库，不注册） | **仅系统级基础设施**：统一响应体、异常、用户上下文、认证（Token/AuthInterceptor）、Redis 工具、MyBatis 字段填充（CORS 已上移至网关统一处理）；P6 起增加多级缓存通用组件（`MultiLevelCache` + Redis Pub/Sub 失效广播）。不含任何业务领域代码 |
| `user-service` | `user-service` / **7001** | 用户 + 管理员 + 商家（人员相关）。持有 `mall_user`、`merchant_apply`；商品/订单/秒杀商品的审核与管理经 **Feign** 调对应服务 |
| `goods-order-service` | `goods-order-service` / **7002** | 商品 + 购物车 + 正常订单（业务紧密）。持有 `goods`、`cart_item`、`mall_order`、`order_item`，并暴露内部审核/管理接口；P6 起正常商品详情走多级缓存，审核/上下架/库存变化后删除缓存 |
| `seckill-service` | `seckill-service` / **7003** | 秒杀（高并发流量隔离）。持有 `seckill_goods`、`seckill_order`、Redis 预扣库存、Lua 脚本、秒杀商品查询、秒杀订单全生命周期，并暴露内部审核/管理接口；P5 起增加 Sentinel 熔断降级（慢调用比例）+ 热点参数限流（商品维度）；P6 起热点秒杀商品信息走 Caffeine → Redis → DB 多级缓存；P8 起秒杀下单异步削峰（Redis 预扣 → RabbitMQ → 异步建单）+ 死信队列处理超时未支付自动取消 |
| `gateway-service` | `gateway-service` / **8080** | 统一流量入口（P4）。Spring Cloud Gateway 按 API 前缀路由到上述三个服务，统一处理跨域（CORS）与 JWT 鉴权（全局过滤器验签 + 角色校验）；`/internal/**` 不对外暴露；P5 起增加 Sentinel 网关流控（`/api/seckill/**` QPS=100） |

### 设计原则

- **公共模块只放跨服务复用的基础设施**，不承载任何实体/Mapper/业务 Service/VO/常量/Lua。
- **每个服务拥有自己的实体、Mapper、Service、VO、常量**，跨服务访问通过 **Feign（HTTP）**，不共享 Mapper。
- 修改某个服务的数据访问或业务逻辑，只需重新构建/部署该服务，不影响其它服务。

## 二、调用链（OpenFeign）

**秒杀下单链路（秒杀域内闭环，无跨服务调用；P8 起 DB 落库异步化）**

```
客户端 → seckill-service (7003)  POST /api/seckill/{goodsId}
            ① 校验可购买 → ② Redis 预扣 → ③ 发 MQ 消息 → 立即返回 orderNo
            （消费者异步：DB 扣减 + 建单，失败回滚 Redis 预扣）
            秒杀订单支付/取消在请求内完成；超时未支付由死信队列（DLX）自动关闭
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

## 三、接口分布（P4 起统一经网关 :8080 访问）

所有对外的 `/api/**` 请求统一打到 **`gateway-service :8080`**，由网关按前缀转发：

| 方法 | 路径 | 转发目标 | 是否需登录 |
|------|------|---------|-----------|
| POST | `/api/user/register` `/api/user/login` | user-service | 否 |
| GET | `/api/user/info` `/api/user/apply-merchant` | user-service | 是 |
| POST | `/api/admin/login` | user-service | 否 |
| POST/GET | `/api/admin/**`（登录除外） | user-service | 是（管理员） |
| GET/POST/PUT | `/api/merchant/**` | user-service | 是（商家） |
| GET | `/api/goods` `/api/goods/{id}` | goods-order-service | 否 |
| GET/POST/PUT/DELETE | `/api/cart/**` | goods-order-service | 是 |
| GET/POST | `/api/order/**`（正常订单） | goods-order-service | 是 |
| GET | `/api/seckill-goods` `/api/seckill-goods/{id}` | seckill-service | 否 |
| POST | `/api/seckill/{goodsId}` | seckill-service | 是 |
| GET/POST | `/api/seckill/order/**`（秒杀订单） | seckill-service | 是 |

> `/internal/**` 为服务间 Feign 内部接口，网关不配置路由、过滤器直接拒绝，外部不可达。
> 各服务端口（7001/7002/7003）仍保留直连能力并各自做防御性 JWT 校验，但对外统一走 8080。

## 四、启动步骤

1. 保证中间件就绪：MySQL(`localhost:3307`)、Redis(`localhost:6379`)、Nacos(`localhost:8848`)、RabbitMQ(`localhost:5672` / 管理台 `15672`，账号 `admin`/`admin123`)。P8 起需把 `docs/P7-nacos-config/` 的最新配置重新发布到 Nacos（`publish-configs.ps1`，因 `seckill-service.yml` 新增了 `spring.rabbitmq.*`）。
2. 切换 JDK 21 + Maven 3.9.10（见 `seckill-build-run` skill）。
3. **先启动 `goods-order-service`**（负责建表 + 用户/商品种子数据），再启动 `seckill-service`（秒杀商品种子 + 库存预载）、`user-service`，最后启动 `gateway-service`（统一入口）。

```powershell
# 根目录一键打包
mvn -DskipTests clean package

# 分别启动（网关最后启动）
cd goods-order-service ; mvn spring-boot:run
cd seckill-service     ; mvn spring-boot:run
cd user-service        ; mvn spring-boot:run
cd gateway-service     ; mvn spring-boot:run
```

启动后统一入口为 `http://localhost:8080`，各服务直连端口仍为 7001/7002/7003。

## 五、设计说明（P3~P8 阶段取舍）

- **共享单个 MySQL 库**（用户选择）：`goods-order-service` 作为数据属主负责 `schema.sql`（建全部表）+「用户/正常商品」种子数据；`seckill-service` 负责「秒杀商品」种子数据；其余服务 `spring.sql.init.mode=never`。
- **跨服务访问走 Feign**：管理员/商家的审核与管理操作经 OpenFeign 调数据属主服务，user-service 不再持有商品/订单的 Mapper 副本，只在本地保留 Feign 响应所需的 DTO（少量契约重复）。
- **秒杀域完整下沉**：秒杀商品、Redis 预扣库存、秒杀订单的创建/支付/取消/超时全部归 seckill-service，保证高并发路径与其它服务隔离。
- **JWT 无状态鉴权（P4）**：登录时由 user-service 用 `common-service` 的 `TokenService` 签发 JWT（载荷含 `sub`=userId、`role`），网关全局过滤器验签 + 过期校验 + 角色校验；下游各服务仍用同一密钥做防御性校验（可独立直连不被绕过），密钥经 `jwt.secret` 统一配置，后续 P7 迁移到 Nacos Config 集中管理。
- **Sentinel 限流熔断（P5）**：网关层用 `spring-cloud-alibaba-sentinel-gateway` 对秒杀下单 API 限流（QPS=100）；seckill-service 用 `@SentinelResource` + 程序化规则做熔断降级（慢调用比例 > 20%）与热点参数限流（商品维度）。规则写死在代码里、不依赖 Dashboard 即可生效，Dashboard 仅作可选的可视化。
- **多级缓存（P6）**：`common-service` 抽出通用组件 `MultiLevelCache<K, V>`（L1 Caffeine → L2 Redis → L3 DB，逐级回填 + 空值占位防穿透）；写路径采用 Cache-Aside（先更新 DB 后删缓存），并通过 Redis Pub/Sub 广播让其它实例同步删各自 L1，保证多实例最终一致。seckill-service 的秒杀商品、goods-order-service 的正常商品均已接入。
- **配置中心（P7）**：各服务 `application.yml` 的业务配置（数据源/Redis/MyBatis/JWT/Sentinel 规则/网关路由等）迁到 Nacos Config，本地只保留端口、服务名与 Nacos 连接信息；`jwt.secret` 走共享配置 `seckill-common.yml` 全局一致；Sentinel 限流/熔断阈值与 `jwt.secret` 经 `@RefreshScope` 支持动态刷新（改配置不重启），并按 profile 后缀 dataId 实现 dev/prod 多环境。
- **异步削峰解耦（P8）**：秒杀下单从「同步 DB 写」改为「Redis 预扣 → RabbitMQ 消息 → 消费者异步落库」，请求线程秒回订单号；订单超时未支付由「延迟队列 + 死信队列（DLX）」自动触发关闭与库存回补，取代原先的定时扫描。消息采用 JSON 序列化 + 发布确认（publisher confirm）+ 消费幂等（按 orderNo 判重），保证「预扣了就不丢、重复投递不重」。

## 六、限流熔断（P5）

| 位置 | 资源 / 路径 | 规则 | 说明 |
|------|-------------|------|------|
| gateway-service | `/api/seckill/**`（自定义 API 分组 `seckill_api`） | 流控 QPS=100 | 秒杀下单接口每秒最多 100 个请求，超限返回 HTTP 429 |
| seckill-service | `seckill`（秒杀下单方法） | 熔断：慢调用比例 > 20% | 统计窗口内 RT > 200ms 的慢调用占比 > 20% 时熔断 10s |
| seckill-service | `seckill`（第 0 参数 = 商品ID） | 热点参数限流 | 默认单商品 QPS=10，热点商品 id=1 收紧到 QPS=5 |
| seckill-service | `seckill-demo`（演示端点） | 熔断：慢调用比例 > 20% | `GET /api/seckill/demo/slow`，用于人工观察熔断触发/恢复 |

规则采用「代码程序化加载」：网关见 `GatewaySentinelConfig`，秒杀服务见 `SentinelConfig`，不依赖 Sentinel Dashboard 也能生效。Dashboard 作为可选的监控可视化，启动后配置 `spring.cloud.sentinel.transport.dashboard` 即可接入。详细原理与验证步骤见 `knowledge/P5-限流熔断知识.md`；JMeter 压测实操见 `docs/P5-JMeter压测.md`。

## 七、多级缓存（P6）

| 位置 | 缓存对象 | 缓存名 | 说明 |
|------|----------|--------|------|
| seckill-service | `SeckillGoods` 秒杀商品 | `seckill-goods` | 热点商品详情 / 下单校验（`requirePurchasable`）走 Caffeine → Redis → DB |
| goods-order-service | `Goods` 正常商品 | `goods` | 商品详情走多级缓存；审核/上下架/库存变化后删除缓存 |

读路径：L1 Caffeine → L2 Redis → L3 DB，逐级回填；DB 查不到时写空值占位（防穿透）。写路径：Cache-Aside（先更新 DB 后删缓存），删本地 L1 + 删分布式 L2 + Redis Pub/Sub 广播通知其它实例删各自 L1；本地缓存短 TTL（5 分钟）作为广播丢失时的兜底。库存变化（下单扣减/取消回补）在 `@Transactional` 事务提交后再删缓存，避免脏读回填。

扩展与原理：`MultiLevelCache` 是普通类，一个服务的 `CacheConfig` 可定义多个 `@Bean` 缓存多种数据（要求 `name` 唯一 + Redis key 前缀不同）；失效广播是 Redis Pub/Sub **推送**模型（`PSUBSCRIBE cache:evict:*`，非轮询），各实例收到推送后由 `CacheEvictListener` → `MultiLevelCacheRegistry` 路由到对应缓存删本地。

关键类：`common-service/.../common/cache/`（`MultiLevelCache`、`MultiLevelCacheRegistry`、`CacheEvictListener`、`MultiLevelCacheConfig`）；接入见各服务的 `CacheConfig` 与 `SeckillGoodsService` / `GoodsService`。原理笔记见 `knowledge/P6-多级缓存知识.md`；JMeter 对比压测见 `docs/P6-JMeter压测.md`。

## 八、配置中心（P7）

| 项 | 说明 |
|----|------|
| 接入方式 | `spring.config.import`（Spring Boot 3 原生，无需 bootstrap） |
| 共享配置 | `seckill-common.yml`：`jwt.secret` / `jwt.expire-minutes`，所有服务 + 网关统一引入 |
| 可刷新阈值 | `sentinel.seckill.*`（seckill-service 熔断/热点限流）、`sentinel.gateway.seckill-qps`（网关限流） |
| 动态刷新 | `@RefreshScope` + `@ConfigurationProperties` 绑定阈值，监听 `RefreshScopeRefreshedEvent` 重载 Sentinel 规则 |
| JWT 刷新 | `TokenService`（common-service）/ `JwtUtil`（gateway-service）标记 `@RefreshScope`，改密钥即时生效 |
| 多环境 | `{服务}.yml`（基础）+ `{服务}-dev.yml` / `{服务}-prod.yml`（环境覆盖），`--spring.profiles.active=prod` 切换 |

- 本地 `application.yml` 只保留 `server.port` / `spring.application.name` / Nacos 连接信息，业务配置全部迁到 Nacos。
- 配置内容与一键发布脚本：`docs/P7-nacos-config/`；实操步骤：`docs/P7-配置中心.md`；原理笔记：`knowledge/P7-配置中心知识.md`。

## 九、异步削峰解耦（P8）

| 项 | 说明 |
|----|------|
| 消息队列 | RabbitMQ（`localhost:5672` / 管理台 `15672`，账号 `admin`/`admin123`，vhost `/`） |
| 依赖 | `seckill-service` 引入 `spring-boot-starter-amqp` |
| 交换机 | `seckill.order.exchange`（direct） |
| 下单队列 | `seckill.order.queue`（路由键 `seckill.order.create`）——消费者异步「DB 扣库存 + 落订单」 |
| 延迟队列 | `seckill.order.delay.queue`（路由键 `seckill.order.delay`，`x-dead-letter-*` 指向超时路由）——超时消息带 TTL=15min |
| 死信（超时）队列 | `seckill.order.timeout.queue`（路由键 `seckill.order.timeout`）——消费过期消息，超时关闭 + 回补库存 |
| 序列化 | `Jackson2JsonMessageConverter`（JSON，替代 Java 原生序列化） |
| 可靠性 | 发布确认（`publisher-confirm-type: correlated`）+ 交换机/队列/消息持久化 + 消费幂等（按 orderNo 判重） |
| 削峰参数 | `listener.simple.concurrency=4` / `max-concurrency=16` / `prefetch=50`，控制 DB 写入速率 |
| 超时配置 | `seckill.order-timeout-minutes: 15`（延迟消息 TTL） |

关键类：`constant/SeckillMqConstants`、`config/RabbitMqConfig`、`mq/`（`SeckillOrderMessage`、`SeckillOrderTimeoutMessage`、`SeckillMqProducer`、`SeckillOrderCreateListener`、`SeckillOrderTimeoutListener`）；服务改造见 `SeckillService` / `SeckillOrderService`。原理笔记见 `knowledge/P8-消息队列知识.md`；实操见 `docs/P8-消息队列.md`。

## 十、后续阶段衔接

- P9：分布式事务（Seata / RocketMQ 事务消息，选做）。
