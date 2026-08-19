# 秒杀商城（单体 · P2 完成版 · 三角色）

秒杀学习项目第二阶段（P2）成果：**Redis 预扣库存 + Lua 脚本** 秒杀，并扩展为**三角色商城系统**
（普通用户 / 商家 / 管理员），按业务模块分包，为 P3 微服务拆分做准备。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | 需手动切换（本机默认 1.8） |
| Spring Boot | 3.3.5 | 单体单模块 |
| MyBatis-Plus | 3.5.7 | `mybatis-plus-spring-boot3-starter` |
| Redis | Spring Data Redis (Lettuce) | 预扣库存 + Lua + 登录 token |
| MySQL | 8.0（端口 3307） | 自动建库 `monomer_seckill` |
| 前端 | 纯 HTML + 原生 JS | 无构建 |

## 三种角色

| 角色 | role | 能力 |
|------|------|------|
| 普通用户 | 0 | 登录、浏览商品/秒杀商品、购物车多商品下单、限时秒杀、申请成为商家 |
| 商家 | 1 | 我的商品、新增商品、上下架、申请秒杀（自定义秒杀价/库存/时间段） |
| 管理员 | 2 | 审核商家申请、审核商品上架、审核秒杀商品上架、订单与库存管理 |

> 下单（正常商品 + 秒杀商品）均需登录，用户身份由 token 解析，不能自行传 userId。

## 核心业务流程

1. **普通用户 → 商家**：用户申请 → 管理员审核通过 → role 变为 1。
2. **商品上架**：商家新增商品（待审核）→ 管理员审核通过 → 展示到正常商品页。
3. **秒杀商品上架**：商家直接填写秒杀商品（名称/秒杀价/库存/起止时间，与正常商品相互独立）→ 管理员审核通过 → 展示到秒杀商品页，仅在时间窗口内可购买。
4. **正常购物**：加购物车（设数量）→ 结算（多商品一起下单，扣库存 + 生成订单 + 明细 + 清购物车）→ 支付 / 取消 / 超时回滚。
5. **秒杀**：登录后对「进行中」的秒杀商品下单 → Redis Lua 原子预扣库存 → DB 落单；取消 / 超时回滚 Redis + DB 库存。

## 数据模型（8 表）

| 表 | 说明 |
|----|------|
| `mall_user` | 用户（三角色） |
| `merchant_apply` | 商家申请（待审核/通过/拒绝） |
| `goods` | 正常商品（待审核/已上架/已下架/已拒绝） |
| `seckill_goods` | 秒杀商品（秒杀价/库存/起止时间/状态） |
| `cart_item` | 购物车 |
| `mall_order` | 订单主表 |
| `order_item` | 订单明细（一个订单多商品） |
| `seckill_order` | 秒杀订单（`uk_seckill_user` 防重复） |

## Redis 预扣库存 + Lua

- 库存 key：`seckill:stock:{seckillGoodsId}`；已购集合：`seckill:users:{seckillGoodsId}`
- Lua：`src/main/resources/lua/seckill_deduct.lua`、`seckill_rollback.lua`
- 超时回滚：`OrderTimeoutTask` 扫描超时未支付订单（正常 + 秒杀）自动关闭并回补库存。

## 目录结构（对应 P3 微服务）

```
org.example.monomer_seckill_backend/
├── common/     Result/BizException/全局异常/Constants/RedisUtil/TokenService/UserContext
├── config/     RedisConfig/WebConfig/认证拦截器(用户·商家·管理员)/DataInitializer/MyMetaObjectHandler
├── user/       用户 + 商家申请        → P3 user-service
├── goods/      商品 + 秒杀商品 + StockService(预扣)  → P3 stock-service
├── cart/       购物车                → P3 order-service
├── order/      订单/明细/秒杀订单/超时回滚 → P3 order-service
├── seckill/    秒杀下单编排          → P3 order-service
├── merchant/   商家中心              → P3 stock-service
└── admin/      管理员审核/管理       → P3 admin-service
```

## API 一览

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

## 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 管理员 |
| merchant1 | 123456 | 商家 |
| user1 | 123456 | 普通用户 |

## 运行

```powershell
$env:JAVA_HOME = 'E:\app\config\java21'
$env:Path = 'E:\app\config\java21\bin;E:\app\config\Maven\Maven\apache-maven-3.9.10\bin;' + $env:Path
cd monomer_seckill_backend\monomer_seckill_backend
mvn spring-boot:run            # http://localhost:7099

cd monomer_seckill_fromend
npx serve . -l 3000            # http://localhost:3000
```

## P2 任务完成清单

- [x] RedisConfig + RedisUtil + Lua（扣库存/回滚）
- [x] `/api/seckill` Redis 预扣库存，登录后限时秒杀
- [x] 库存不足 / 重复抢购 / 超时回滚
- [x] 三角色：普通用户 / 商家 / 管理员
- [x] 购物车 + 多商品一起下单 + 订单明细
- [x] 商家：申请入驻 / 我的商品 / 申请秒杀
- [x] 管理员：审核商家 / 审核商品 / 审核秒杀商品
- [x] wrk 压测与数据校验（`docs/`）
