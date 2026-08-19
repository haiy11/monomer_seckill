# 用 wrk 对秒杀接口做高并发压测 + 数据正确性校验

> 目标：用 wrk 模拟大量用户同时抢购，验证系统在高并发下**不超卖、不多卖、不重复下单**。

---

## 一、先想清楚：压测到底在验证什么

秒杀接口最怕三个数据错误：

| 错误类型 | 含义 | 现象 |
|---|---|---|
| **超卖** | 卖出的数量 > 库存 | 库存被扣成负数，或订单数 > 初始库存 |
| **多卖** | 订单数 > 实际扣减的库存数 | 扣库存和建订单没有原子性，两边对不上 |
| **重复下单** | 同一用户对同一商品下多单 | 唯一索引没兜住，出现重复记录 |

所以压测后不是只看「接口能不能通」，而是**回到数据库里核对这三件事**（见第七节）。

---

## 二、wrk 是什么 / 怎么装

wrk 是专为 HTTP 高并发压测设计的轻量工具：多线程 + 异步 I/O，单机轻松打出几千甚至上万 QPS，比 Apifox/JMeter 更轻更快。

- **Windows 下推荐装进 WSL2**（你的 WSL 是 Ubuntu 24.04）：

```bash
sudo apt-get update
sudo apt-get install -y wrk
wrk --version   # 能看到版本号即安装成功
```

- Windows 原生版也可以（去 wrk 的 GitHub release 或官方提供的 win32 编译版下载 `wrk.exe`），但 WSL 里跑更稳定。

> 常见参数：
> - `-t` 线程数（压测线程）
> - `-c` 并发连接数
> - `-d` 持续时间（如 `30s`）
> - `--latency` 输出延迟分布
> - `-s` 指定 Lua 脚本（做 POST、动态参数等）

---

## 三、压测前准备

### 1. 启动后端
```powershell
$env:JAVA_HOME = 'E:\app\config\java21'
$env:Path = 'E:\app\config\java21\bin;E:\app\config\Maven\Maven\apache-maven-3.9.10\bin;' + $env:Path
cd E:\items\natherItems\monomer_seckill\monomer_seckill_backend\monomer_seckill_backend
mvn spring-boot:run
```

> 注意：后端当前配置端口是 **7099**（`application.yml` 里 `server.port`），下面示例都用 7099。如果你改回 8080，替换一下即可。

### 2. 重置数据（重要，每次压测前都做一次）
把某个秒杀商品的库存设成一个好算的数，比如秒杀商品1 = **100**，然后清空秒杀订单。
执行 `docs/check-data.sql` 里「【0】压测前重置」那一段（Navicat/DBeaver 或命令行都行）。

> P2 采用 Redis 预扣库存，重置 DB 库存后还要重置 Redis 库存，二选一：
> - 重启后端（启动时自动把 DB 库存预载到 Redis）；或
> - 调用管理接口 `POST /api/admin/stock/reset`（需管理员 token）。

### 3. 确认接口
秒杀下单需登录，用户身份从 token 解析（不再传 userId）：
```
POST http://localhost:7099/api/seckill/1
Authorization: Bearer <token>
```
- 路径参数 `1` = 秒杀商品ID（seckill_goods.id）
- 先用 `POST /api/user/login` 登录拿 token，再带上 header 单发一次，
  确认返回 `{"code":200,...,"data":"SO...订单号"}`，然后把数据重置回去。

---

## 四、关键：每个请求要用不同用户的 token（用 Lua 脚本）

如果所有请求都用同一个用户的 token，那只有第一个能成功，其余全被「重复抢购」拦截，**根本测不出高并发**。所以每个请求必须携带不同用户的 token。

`docs/post.lua` 已实现：从 `tokens.txt` 读取一批 token（每行一个），压测时循环使用。准备 token 的方式：

1. 批量注册 N 个测试用户并登录，把登录返回的 `data.token` 逐行写入 `tokens.txt`；
2. 或在 Redis 里直接批量写入 token→userId 映射（token 是 UUID 字符串，value 是 userId）。

然后运行（把 IP 换成你的主机 IP）：

```bash
wrk -t10 -c200 -d30s --latency -s post.lua http://172.17.48.1:7099
```

说明：
- `post.lua` 用 `io.lines("tokens.txt")` 读取 token 列表，在 `request()` 里循环取用并加到 `Authorization: Bearer <token>` 头。
- `wrk.format(method, path, headers)` 的第三个参数是自定义 header 表。

---

## 五、压测命令怎么设参数

| 参数 | 含义 | 秒杀测试建议 |
|---|---|---|
| `-t` 线程数 | 压测线程数 | 先 `4`，再 `10`，最后 `20` 阶梯加压 |
| `-c` 并发连接数 | 同时维持的连接数 | 先 `50`，再 `200`，最后 `500` |
| `-d` 持续时间 | 压测多久 | `10s`~`30s`（不是专业压测，别太久） |
| `--latency` | 输出延迟分布 | 建议始终带上 |

**只读接口（预热/验证工具）**：
```bash
wrk -t4 -c100 -d10s --latency http://172.17.48.1:7099/api/goods
```

**秒杀接口（核心）**：
```bash
wrk -t4 -c200 -d30s --latency -s post.lua http://172.17.48.1:7099
```

**秒杀测试套路**：初始库存 100，`-c` 设 **500**（远超库存），就是要让「成功」和「库存不足」的边界暴露出来。

---

## 六、报告怎么看

wrk 跑完会输出类似：

```
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     12.34ms    5.67ms  89.00ms   78.00%
    Req/Sec       1.85k   210.00     2.30k    70.00%
  55712 requests in 30.00s, 8.01MB read
Requests/sec:   1857.02
Transfer/sec:    273.48KB
```

重点看 4 个数字：

| 指标 | 位置 | 关注点 |
|---|---|---|
| `Requests/sec` | 最后一行 | **QPS，越高越好**（你关心的 500+ 就看它） |
| `Latency` 的 Avg / Max | 线程统计 | 平均/最大延迟，关注 Max 有没有异常尖峰 |
| `Req/Sec` | 线程统计 | 每秒请求吞吐 |
| `非 2xx/3xx` 响应 | 结尾（若开启） | 失败分类：库存不足 vs 报错(500) |

> 秒杀场景的正确预期：并发 500、库存 100 时，**大约 100 个成功、400 个「库存不足」失败**，这才是正常的（不是 500 个全成功）。wrk 默认不显示非 2xx 状态码统计，要看失败类型需在 Lua 里 `wrk.headers`/`response()` 钩子里累计，或用 `--latency` + 结合后端日志判断。

---

## 七、高并发下如何确认数据对错（重点）

压测完，**别只看报告，去数据库核对**。打开 `docs/check-data.sql`，跑「压测后校验」那几段：

### 校验 1：库存不为负（超卖）
```sql
SELECT id, name, seckill_stock FROM seckill_goods WHERE seckill_stock < 0;
```
✅ 期望：**空**。只要出现任何一行，就是超卖了。

### 校验 2：订单数 + 剩余库存 = 初始库存（不多卖不少卖）
```sql
SELECT
  (SELECT COUNT(*) FROM seckill_order WHERE seckill_goods_id = 1)
  + (SELECT seckill_stock FROM seckill_goods WHERE id = 1) AS total;
```
✅ 期望：**= 100**（以初始库存 100 为例）。
- 结果 > 100：多卖/超卖，扣库存和建订单没对齐。
- 结果 < 100：正常（说明有一部分请求正确失败了）。

### 校验 3：无重复下单
```sql
SELECT seckill_goods_id, user_id, COUNT(*) AS cnt
FROM seckill_order
GROUP BY seckill_goods_id, user_id
HAVING cnt > 1;
```
✅ 期望：**空**。有返回行就是有人重复下单。

### 校验 4：卖出数量分布
```sql
SELECT seckill_goods_id, COUNT(*) AS sold_count FROM seckill_order GROUP BY seckill_goods_id;
```
✅ 期望：秒杀商品1 卖出数 = 100（和校验 2 对应）。

### 一句话总结
> **库存不为负** + **订单数 = 初始库存 − 剩余库存** + **无重复下单** = 数据正确。

---

## 八、预期结果 vs 你可能遇到的问题

### 正常预期（P2：Redis 预扣库存 + Lua 实现）
并发 500、库存 100 → 100 成功、400「库存不足」失败；数据库校验 1/2/3 全通过。
秒杀请求先在 Redis 层用 Lua 脚本原子完成「判库存 → 判重复 → 扣库存 → 记用户」，
只有抢到库存的少数请求（约等于库存数）才会落到数据库建订单，DB 压力显著下降。

### 你大概率会看到的「问题」——这是好事
正常情况下不会再看到 `DuplicateKeyException`（重复下单已在 Redis Lua 层被幂等拦截）。
若 Redis 被重启导致已购用户集合丢失，极少数重复请求会穿透到数据库，最终靠 `(seckill_goods_id, user_id)`
唯一索引兜底抛出 `DuplicateKeyException`，此时后端会回滚 Redis 库存并返回「重复抢购」——这正是
「Redis 预扣 + DB 唯一索引兜底」双层防重复的设计。

### wrk 相比 Apifox / JMeter 的优劣
- **优势**：极轻量、QPS 上限高、命令式、适合快速看吞吐与延迟。
- **局限**：
  - 默认不渲染失败状态码分布，要看失败原因需额外写 Lua 的 `response()` 统计。
  - 不是全功能压测平台（无图形报告、无断言、无分布式），要更细的图表/断言，再用 JMeter。

---

## 九、相关文件

- 压测后校验 SQL：`docs/check-data.sql`
- 压测用 Lua 脚本：`docs/post.lua`（如已抽出；否则按上面第四节内容自行创建）
- 项目整体说明：`AGENTS.md`、`README.md`
- 路线图（P2 将引入 Redis 预扣库存 + Lua）：`monomer_seckill_plan.md`
