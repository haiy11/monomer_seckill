# 秒杀商城（单体 P2 + 微服务 P3~P8）

秒杀学习项目：按「单体 → 微服务 → 高可用 → 多级缓存 → 配置中心 → 异步削峰」逐步演进，
完整路线图见 `monomer_seckill_plan.md`（P1~P9）。

## 阶段进度

| 阶段 | 内容 | 涉及技术 | 状态 |
|:----:|------|---------|:----:|
| **P1** | 环境基石搭建 | WSL2 + Docker + 中间件容器化 | ✅ |
| **P2** | 高性能单体秒杀（Redis 预扣库存 + Lua + 三角色商城） | Spring Boot、Redis、Lua、wrk | ✅ |
| **P3** | 微服务拆分 + 注册调用 | Nacos（注册发现）、OpenFeign | ✅ |
| **P4** | 统一流量入口 | Spring Cloud Gateway、JWT | ✅ |
| **P5** | 高可用限流熔断 | Sentinel、JMeter | ✅ |
| **P6** | 多级缓存优化 | Caffeine + Redis、Pub/Sub 广播 | ✅ |
| **P7** | 配置中心 + 动态刷新 | Nacos Config、`@RefreshScope` | ✅ |
| **P8** | 异步削峰解耦 | RabbitMQ、死信队列（DLX） | ✅ |
| **P9** | 分布式事务 | Seata / 事务消息 | ⏭️ 跳过 |

> **P9 为何跳过**：本项目把「下单 + 扣库存」收敛在 `seckill-service` 自己的库、**同一个本地事务**里
> （天然强一致、防超卖），架构上不存在跨库分布式事务；P8 又用「Redis 预扣 → MQ 异步落库」实现了
> 削峰与最终一致（发布确认 + 幂等消费 + 死信回补）。对秒杀场景而言，引入 Seata 这类强一致方案
> 会带来全局锁开销、反而拖垮吞吐，属于负优化，因此不做。

---

## 技术栈总览

### 基础框架与数据访问

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | 全项目语言版本（本机默认是 1.8，需手动切 `JAVA_HOME`） |
| Spring Boot | 3.3.5 | 单体版与微服务版统一基座（`spring-boot-starter-parent`） |
| Maven | 3.9.10 | 构建；微服务父 pom 统一依赖版本仲裁 |
| MyBatis-Plus | 3.5.7 | ORM（`mybatis-plus-spring-boot3-starter`）：BaseMapper + Lambda 查询 + 字段自动填充 |
| MySQL | 8.0（端口 **3307**） | 主库 `monomer_seckill`，驱动 `mysql-connector-j` |
| Lombok | 随 Boot 管理 | 简化 POJO（`@Data` / `@RequiredArgsConstructor`） |

### 微服务与中间件（P3~P8）

| 技术 | 版本 | 用途 | 阶段 |
|------|------|------|:----:|
| Spring Cloud | 2023.0.3 | 微服务基座，BOM 统一版本 | P3 |
| Spring Cloud Alibaba | 2023.0.3.2 | Nacos / Sentinel 集成 BOM | P3 |
| Nacos（discovery） | — | **服务注册与发现**（5 个模块中 4 个注册） | P3 |
| OpenFeign + Spring Cloud LoadBalancer | — | 声明式远程调用 + 客户端负载均衡（user-service → 另两个服务） | P3 |
| Spring Cloud Gateway | — | **统一流量入口**：按前缀路由 + 全局过滤器鉴权 + 统一 CORS | P4 |
| JJWT | 0.12.6 | JWT 签发与校验（网关验签 + 各服务防御性校验共用） | P4 |
| Sentinel | — | 网关流控 + 服务熔断降级（慢调用比例）+ 热点参数限流 | P5 |
| Caffeine | 3.1.8 | **多级缓存 L1** 本地缓存 | P6 |
| Redis（Spring Data Redis / Lettuce） | — | **多级缓存 L2** + 预扣库存 + Lua 原子脚本 + Pub/Sub 失效广播 + 登录 token | P2 / P6 |
| Nacos Config | — | **配置中心** + `@RefreshScope` 动态刷新 + dev/prod 多环境 | P7 |
| RabbitMQ（`spring-boot-starter-amqp`） | — | **异步削峰** + 延迟队列（TTL）+ 死信队列（DLX） | P8 |

### 前端与工具链

| 技术 | 版本 | 用途 |
|------|------|------|
| 原生 HTML + JavaScript | — | 无构建步骤；三个静态页面分别对应单体 / 直连微服务 / 网关三种接入方式 |
| Node.js | v25.2.1 | 用 `npx serve` 起静态前端 |
| wrk / JMeter | — | 压测（P2 用 wrk；P5 / P6 用 JMeter） |
| WSL2 + Docker | — | 中间件容器化（MySQL / Redis / Nacos / RabbitMQ） |

### 本地中间件地址

| 中间件 | 地址 | 凭据 |
|--------|------|------|
| MySQL | `localhost:3307` | 见各服务 Nacos 配置 |
| Redis | `localhost:6379` | 无密码 |
| Nacos | `localhost:8848`（控制台 `/nacos`） | 见 Nacos 配置 |
| RabbitMQ | `localhost:5672`（管理台 `15672`） | `admin` / `admin123`，vhost `/` |

---

## 项目结构

```
monomer_seckill/
├── monomer_seckill_plan.md              # 完整路线图 P1~P9
├── AGENTS.md                            # 入口索引（指向 .dsh/skills 下的 Agent Skills）
├── knowledge/                           # 各阶段知识点笔记（面试复习向）
├── docs/                                # 各阶段实操指南 + 压测脚本 + Nacos 配置
├── monomer_seckill_backend/
│   ├── monomer_seckill_backend/         # 单体版（P2，端口 7099）
│   └── monomer_seckill_backend_cloud/   # 微服务版（P3~P8，网关统一入口 8080）
└── monomer_seckill_fromend/
    ├── monomer/index.html               # 单体版前端（调 7099）
    ├── cloud/index.html                 # 微服务版前端（按 API 前缀路由到 7001/7002/7003）
    └── unified_gateway/index.html       # 网关版前端（统一调 8080，经网关转发）
```

> 微服务工程内部结构、模块职责与设计取舍详见
> `monomer_seckill_backend/monomer_seckill_backend_cloud/README.md`。

---

## 文档导航

### 知识点笔记 `knowledge/`（原理）

| 文档 | 主题 |
|------|------|
| `P2-单体项目知识_1.md` | 单体架构 + Spring Boot 启动 + 配置加载 + 依赖注入 |
| `P2-单体项目知识_2.md` | 构造器注入原理 + `@RequiredArgsConstructor` + `final` + 单例无状态设计 |
| `P2-单体项目知识_3.md` | MyBatis 两种用法 + MyBatis-Plus 增强查询 + 代码生成器 + 注解与 XML 对比 |
| `P2-单体项目知识_4.md` | Spring Boot 自动装配 + 配置类 + Bean 生命周期 + 条件注解 + 循环依赖 |
| `P2-单体项目知识_5.md` | Redis 篇：依赖引入 + 序列化器原理 + Token 存储实战 |
| `P3-微服务知识.md` | 微服务拆分 + 注册发现 + 远程调用 |
| `P4-网关微服务和JWT鉴权知识.md` | Spring Cloud Gateway 路由转发 + JWT 鉴权 |
| `P5-限流熔断知识.md` | Sentinel 限流、降级、热点参数规则 |
| `P6-多级缓存知识.md` | Caffeine → Redis → DB 多级缓存 + 失效广播 |
| `P7-配置中心知识.md` | Nacos Config「发布 → 拉取 → 推送 → 刷新」全链路 |
| `P8-消息队列知识.md` | RabbitMQ 核心概念 + 削峰解耦 + 死信队列 |

### 实操指南 `docs/`

| 文档 | 内容 |
|------|------|
| `P2-wrk-压测教程.md`、`P2-post.lua`、`P2-check-data.sql` | P2 wrk 压测与数据校验 |
| `P5-JMeter压测.md`、`P5-seckill-gateway-limit.jmx` | P5 网关限流 JMeter 压测 |
| `P6-JMeter压测.md`、`P6-seckill-goods-cache.jmx` | P6 多级缓存对比压测 |
| `P7-配置中心.md`、`P7-nacos-config/` | P7 配置发布实操 + 各服务 dataId 配置 + 一键发布脚本 `publish-configs.ps1` |
| `P8-消息队列.md` | P8 RabbitMQ 异步削峰 + 死信队列实操与验证 |

---

## 微服务版（P3~P8）

后端在 `monomer_seckill_backend/monomer_seckill_backend_cloud/`，拆为 5 个模块（共享一个 MySQL 库）：

| 模块 | 服务名 / 端口 | 职责 |
|------|--------------|------|
| `common-service` | 公共库（不注册） | 统一响应体/异常/认证/Redis 工具/CORS 等基础设施；P6 起多级缓存通用组件（`MultiLevelCache` + Redis Pub/Sub 失效广播）；P7 起 `TokenService` 支持 `@RefreshScope`（jwt 密钥动态刷新） |
| `user-service` | user-service / 7001 | 用户 + 管理员 + 商家 |
| `goods-order-service` | goods-order-service / 7002 | 商品 + 购物车 + 正常订单；P6 起商品详情走多级缓存 |
| `seckill-service` | seckill-service / 7003 | 秒杀（秒杀商品/库存/秒杀订单全链路）；P5 起 Sentinel 熔断降级 + 热点参数限流；P6 起热点秒杀商品信息走 Caffeine → Redis → DB 多级缓存；P7 起 Sentinel 阈值配置化 + 动态刷新；P8 起 RabbitMQ 异步削峰（Redis 预扣 → MQ → 异步建单）+ 死信队列处理超时未支付取消 |
| `gateway-service` | gateway-service / 8080 | 统一入口：路由转发 + JWT 鉴权（P4）+ Sentinel 网关限流（P5，阈值 P7 起动态刷新） |

> P6 起，热点商品信息（秒杀商品 / 正常商品）走「Caffeine 本地缓存 → Redis 分布式缓存 → DB」多级缓存；写路径采用 Cache-Aside（更新 DB 后删缓存）+ Redis Pub/Sub 失效广播，保证多实例最终一致。
> P7 起，各服务业务配置（数据源/Redis/MyBatis/JWT/Sentinel 阈值/网关路由等）集中到 Nacos Config（本地只留端口/服务名/Nacos 连接），Sentinel 限流熔断阈值与 `jwt.secret` 经 `@RefreshScope` 动态刷新（改配置不重启），并按 profile 后缀 dataId 实现 dev/prod 多环境。
> P8 起，秒杀下单改「Redis 预扣 → RabbitMQ 消息 → 消费者异步落库」实现削峰解耦；订单超时未支付由「延迟队列 + 死信队列（DLX）」自动关闭回补库存（替代定时扫描），消息走 JSON 序列化 + 发布确认 + 消费幂等。详见 `knowledge/P8-消息队列知识.md`、`docs/P8-消息队列.md`。

---

## 单体版（P2）

代码保持在 `monomer_seckill_backend/monomer_seckill_backend/` 不动，技术栈同上（Spring Boot 3.3.5 + MyBatis-Plus 3.5.7 + Redis + MySQL），端口 7099。

### 三种角色

| 角色 | role | 能力 |
|------|------|------|
| 普通用户 | 0 | 登录、浏览商品/秒杀商品、购物车多商品下单、限时秒杀、申请成为商家 |
| 商家 | 1 | 我的商品、新增商品、上下架、申请秒杀（自定义秒杀价/库存/时间段） |
| 管理员 | 2 | 审核商家申请、审核商品上架、审核秒杀商品上架、订单与库存管理 |

> 下单（正常商品 + 秒杀商品）均需登录，用户身份由 token 解析，不能自行传 userId。

### 核心业务流程

1. **普通用户 → 商家**：用户申请 → 管理员审核通过 → role 变为 1。
2. **商品上架**：商家新增商品（待审核）→ 管理员审核通过 → 展示到正常商品页。
3. **秒杀商品上架**：商家直接填写秒杀商品（名称/秒杀价/库存/起止时间，与正常商品相互独立）→ 管理员审核通过 → 展示到秒杀商品页，仅在时间窗口内可购买。
4. **正常购物**：加购物车（设数量）→ 结算（多商品一起下单，扣库存 + 生成订单 + 明细 + 清购物车）→ 支付 / 取消 / 超时回滚。
5. **秒杀**：登录后对「进行中」的秒杀商品下单 → Redis Lua 原子预扣库存 → **发 MQ 消息并立即返回订单号** → 消费者异步「DB 扣库存 + 落订单」（P8）；取消 / 超时回滚 Redis + DB 库存。

### 数据模型（8 表）

| 表 | 说明 |
|----|------|
| `mall_user` | 用户（三角色） |
| `merchant_apply` | 商家申请（待审核/通过/拒绝） |
| `goods` | 正常商品（待审核/已上架/已下架/已拒绝） |
| `seckill_goods` | 秒杀商品（秒杀价/库存/起止时间/状态） |
| `cart_item` | 购物车 |
| `mall_order` | 订单主表 |
| `order_item` | 订单明细（一个订单多商品） |
| `seckill_order` | 秒杀订单（`uk_seckill_user_active` 防重复：仅对待支付/已支付生效，取消/超时关闭后可再次下单） |

### Redis 预扣库存 + Lua

- 库存 key：`seckill:stock:{seckillGoodsId}`；已购集合：`seckill:users:{seckillGoodsId}`
- Lua：`src/main/resources/lua/seckill_deduct.lua`、`seckill_rollback.lua`
- 超时回滚：
  - **正常订单**：`OrderTimeoutTask` 定时扫描超时未支付订单，自动关闭并回补库存。
  - **秒杀订单（P8 起）**：由「延迟队列 + 死信队列（DLX）」在 TTL 到期后触发关闭 + 回补库存，取代原先的定时扫库。

### 单体目录结构（对应 P3 拆分）

```
org.example.monomer_seckill_backend/
├── common/     Result/BizException/全局异常/Constants/RedisUtil/TokenService/UserContext → common-service
├── config/     RedisConfig/WebConfig/认证拦截器/DataInitializer/MyMetaObjectHandler        → common-service
├── user/       用户 + 商家申请                                  → user-service
├── admin/      管理员审核/管理（商品/订单/秒杀审核经 Feign）       → user-service
├── merchant/   商家中心（商品/秒杀商品管理经 Feign）              → user-service
├── goods/      商品 + 秒杀商品 + 库存                            → goods-order-service / seckill-service
├── cart/       购物车                                           → goods-order-service
├── order/      订单/明细（正常） + 秒杀订单                       → goods-order-service / seckill-service
└── seckill/    秒杀下单编排                                      → seckill-service
```

### API 一览

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/user/register` | 注册 | 无 |
| POST | `/api/user/login` | 登录 | 无 |
| GET | `/api/user/info` | 我的信息 | 用户 token |
| POST/GET | `/api/user/apply-merchant` | 申请商家 / 查申请 | 用户 token |
| GET | `/api/goods`、`/api/goods/{id}` | 正常商品 | 无 |
| GET | `/api/seckill-goods`、`/{id}` | 秒杀商品 | 无 |
| GET/POST/PUT/DELETE | `/api/cart...` | 购物车 | 用户 token |
| POST | `/api/order/checkout` | 购物车结算下单 | 用户 token |
| GET/POST | `/api/order/list`、`/{orderNo}`、`/{orderNo}/pay`、`/{orderNo}/cancel` | 正常订单 | 用户 token |
| GET/POST | `/api/order/seckill/list`、`/seckill/{orderNo}/pay`、`/cancel` | 秒杀订单 | 用户 token |
| POST | `/api/seckill/{seckillGoodsId}` | 秒杀下单 | 用户 token |
| GET/POST/PUT | `/api/merchant/goods...`、`/seckill-goods` | 商家中心 | 商家 token |
| POST | `/api/admin/login` | 管理员登录 | 无 |
| GET/POST | `/api/admin/merchant-applies`、`/goods`、`/seckill-goods`（含 approve/reject） | 审核 | 管理员 token |
| GET | `/api/admin/orders`、`/seckill-orders` | 订单管理 | 管理员 token |
| POST | `/api/admin/stock/reset` | 重置 Redis 库存 | 管理员 token |

统一响应体 `Result<T>`：`{ code: 200|500, msg, data }`。
（微服务版统一经网关 `:8080` 访问，接口分布详见微服务工程 README。）

### 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 管理员 |
| merchant1 | 123456 | 商家 |
| user1 | 123456 | 普通用户 |

---

## 运行

### 单体版（P2）

```powershell
$env:JAVA_HOME = 'E:\app\config\java21'
$env:Path = 'E:\app\config\java21\bin;E:\app\config\Maven\Maven\apache-maven-3.9.10\bin;' + $env:Path
cd monomer_seckill_backend\monomer_seckill_backend
mvn spring-boot:run            # http://localhost:7099

# 单体前端（调 7099）
cd monomer_seckill_fromend
npx serve monomer -l 3000      # http://localhost:3000
```

### 微服务版（P3~P8）

```powershell
# 1. 中间件就绪：MySQL 3307 / Redis 6379 / Nacos 8848 / RabbitMQ 5672
# 2. P7 起需先把 docs/P7-nacos-config/ 的最新配置发布到 Nacos：
cd docs\P7-nacos-config ; .\publish-configs.ps1

# 3. 打包 + 依次启动（goods-order-service 负责建表与种子数据，网关最后启动）
cd monomer_seckill_backend\monomer_seckill_backend_cloud
mvn -DskipTests clean package
cd goods-order-service ; mvn spring-boot:run
cd seckill-service     ; mvn spring-boot:run
cd user-service        ; mvn spring-boot:run
cd gateway-service     ; mvn spring-boot:run   # 统一入口 http://localhost:8080

# 前端
cd monomer_seckill_fromend
npx serve cloud -l 3001            # 微服务版（按前缀路由 7001/7002/7003）
npx serve unified_gateway -l 3002  # 网关版（统一调 8080）
```

> 编译/启动排错与 JDK/Maven 切换细节见 `.dsh/skills/seckill-build-run`；
> 中间件连接与环境详见 `.dsh/skills/seckill-environment`。

---

## P2 任务完成清单

- [x] RedisConfig + RedisUtil + Lua（扣库存/回滚）
- [x] `/api/seckill` Redis 预扣库存，登录后限时秒杀
- [x] 库存不足 / 重复抢购 / 超时回滚
- [x] 三角色：普通用户 / 商家 / 管理员
- [x] 购物车 + 多商品一起下单 + 订单明细
- [x] 商家：申请入驻 / 我的商品 / 申请秒杀
- [x] 管理员：审核商家 / 审核商品 / 审核秒杀商品
- [x] wrk 压测与数据校验（`docs/`）
