# 用 JMeter 验证 P6 多级缓存（观察响应时间下降）

> 目标：对**热点秒杀商品详情**接口 `GET /api/seckill-goods/{id}` 做高并发读，对比「改造前直查 DB」与「改造后 Caffeine → Redis → DB 多级缓存」的响应时间与吞吐，并从后端日志确认缓存的命中层级。

---

## 〇、先想清楚：P6 压测验证什么

P6 的核心收益是**读路径更快、DB 压力更小**。验证维度：

| 维度 | 改造前（P5 及之前） | 改造后（P6） |
|------|---------------------|--------------|
| 商品详情 `getVO` | 每次 `selectById` 直查 DB | 走多级缓存 |
| 下单校验 `requirePurchasable` | 仅 Redis 单级缓存 | Caffeine + Redis 两级缓存 |
| 响应时间量级 | 几 ms ~ 几十 ms（DB） | 首次回源后稳定在亚毫秒 ~ 1ms（L1） |
| 命中层级 | 无 | 后端日志打印「命中 L1 / 命中 L2 / 回源 DB」 |

关键点：**第一个请求**会回源 DB（冷启动），**之后的请求**命中 L1 本地缓存。看 JMeter 汇总报告里的「平均响应时间」和「吞吐」，再看后端日志确认层级。

---

## 一、前置条件

1. 中间件已启动：MySQL(3307)、Redis(6379)、Nacos(8848)（见 `seckill-environment` skill）。
2. 后端微服务已启动（统一入口 **8080**），且 `seckill-service` 启动时已初始化 1 条示例秒杀商品（id=1，`iPhone 15 Pro 秒杀专场`）。
3. 确认接口可访问（网关已路由 `/api/seckill-goods/**` → seckill-service，公开无需登录）：

```bash
curl -i http://172.17.48.1:8080/api/seckill-goods/1
# 期望 HTTP 200 + JSON，data.name = "iPhone 15 Pro 秒杀专场"
```

> 在 WSL 里压测时，`localhost` 指向 WSL 自己，要访问 Windows 上的服务必须用 `172.17.48.1`（详见 `docs/P5-JMeter压测.md` 第四节）。

---

## 二、准备压测（现成 .jmx）

本阶段提供了现成的测试计划 `docs/P6-seckill-goods-cache.jmx`：200 并发、持续 30 秒，反复打 `GET /api/seckill-goods/1`（公开接口，无需 token，所以比 P5 简单）。

```bash
mkdir -p ~/jmeter
cp /mnt/e/items/natherItems/monomer_seckill/docs/P6-seckill-goods-cache.jmx ~/jmeter/
cd ~/jmeter

# 非 GUI 跑（-l 存结果）
rm -f result.jtl && jmeter -n -t P6-seckill-goods-cache.jmx -l result.jtl

# 生成 HTML 报告更直观（要求 -l 的结果文件先删掉，否则报 "Results file is not empty"）
rm -f result.jtl && rm -rf report && jmeter -n -t P6-seckill-goods-cache.jmx -l result.jtl -e -o report
```

`.jmx` 里两个变量可改：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `HOST` | `172.17.48.1` | 网关所在 Windows 主机地址（环境变了改这里） |
| `PORT` | `8080` | 网关端口 |

---

## 三、怎么看结果

### 1. JMeter 汇总报告

| 现象 | 含义 |
|------|------|
| 平均响应时间 / P90 很低（个位数 ms 甚至 <1ms） | 大多命中 L1 本地缓存，改造见效 |
| 吞吐（Req/s）明显高于「直查 DB」时的量级 | 缓存挡掉了 DB 读压力 |
| 仅**第一个**请求慢、之后都很快 | 冷启动回源 DB，之后命中缓存（正常） |

### 2. 后端日志（最有说服力）

`seckill-service` 的日志级别是 `debug`，`MultiLevelCache` 会在每次 `get` 时打印命中层级：

```
[多级缓存] name=seckill-goods key=seckill:goods:1 回源 DB，回填 L1+L2   ← 第一次请求
[多级缓存] name=seckill-goods key=seckill:goods:1 命中 L1（本地）        ← 之后大量命中
```

对照着看：**第一次请求前**日志出现「回源 DB」；压测进行中**几乎全是「命中 L1」**，说明多级缓存确实生效。

### 3. 直观对比：临时关掉缓存看 DB 基线（可选）

想亲眼看「直查 DB」的差异，可临时把 seckill-service 的 `CacheConfig` 里 `local(...)` 那段换成 `Caffeine.newBuilder().maximumSize(0).build()`（本地容量 0，每次必然回源），或临时让 `SeckillGoodsService.getVO` 直接调 `getById`，重启后再压一次对比响应时间。**测完记得改回来。**

---

## 四、预期结果 vs 常见坑

### 正常预期
- 压测进行中，绝大多数请求命中 L1，平均响应时间显著低于直查 DB；
- Redis（`redis-cli GET seckill:goods:1`）里能看到该商品被写入了缓存；本地缓存生效后，对 Redis 的访问也大幅下降。

### 常见坑
- **`localhost` 连不上**：WSL 里用 `172.17.48.1`，不是 `localhost`。
- **全是 404 / data=null**：`seckill-service` 还没初始化出 id=1 的秒杀商品，或网关没路由到 seckill-service；先 `curl` 确认。
- **看不到日志**：日志级别被改成 info 了；确认 `application.yml` 里 `logging.level.com.example.seckill: debug`。
- **响应时间没明显下降**：并发线程数太少（数据库本身也扛得住），把 `.jmx` 的线程数/持续时间调大，或用 `wrk` 补测（见 `docs/P2-wrk-压测教程.md`）。
- **中文乱码**：看响应体乱码时，HTTP Request 的「内容编码」已设为 UTF-8；若还乱码，看 `code/msg` 英文字段即可。

---

## 五、相关文件

- 压测计划：`docs/P6-seckill-goods-cache.jmx`
- 多级缓存原理笔记：`knowledge/P6-多级缓存知识.md`
- 通用组件：`common-service/.../common/cache/`
- 秒杀接入：`seckill-service/.../config/CacheConfig.java`、`service/SeckillGoodsService.java`
- P5 限流熔断压测（另一套目标/另一套前置）：`docs/P5-JMeter压测.md`
- 整体路线图：`monomer_seckill_plan.md`
