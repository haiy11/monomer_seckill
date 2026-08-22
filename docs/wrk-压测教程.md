# 用 wrk 对秒杀接口做高并发压测 + 数据正确性校验

> 目标：用 wrk 模拟大量用户同时抢购，验证系统在高并发下**不超卖、不多卖、不重复下单**。

---

## 〇、整体流程速览（先看这条）

P2 秒杀是「**Redis 预扣库存 + token 鉴权**」：每个压测请求必须带**不同用户**的 token，否则全被「重复抢购」拦掉，测不出高并发。所以一次完整压测分四步：

1. **批量新建用户** —— 跑单测 `LoadTestUserGeneratorTest`（一次建 20000 个 `loaduser_*` 用户）；
2. **生成 token + tokens.txt** —— 跑单测 `LoadTestTokenGeneratorTest`（给每个用户写 `mall:token:{token}→userId` 到 Redis，并生成 `docs/tokens.txt`）；
3. **重置数据** —— 执行 `docs/check-data.sql`【0】重置 DB，再调管理接口重置 Redis（SQL 动不了 Redis）；
4. **跑 wrk** → 压测后回数据库**校验** → 需要下一轮再执行【压测后重置】。

前置：后端以 **7099** 端口运行、MySQL(3307)/Redis(6379) 已启动。

---

## 一、先想清楚：压测到底在验证什么

秒杀接口最怕三个数据错误：

| 错误类型 | 含义 | 现象 |
|---|---|---|
| **超卖** | 卖出的数量 > 库存 | 库存被扣成负数，或订单数 > 初始库存 |
| **多卖** | 订单数 > 实际扣减的库存数 | 扣库存和建订单没有原子性，两边对不上 |
| **重复下单** | 同一用户对同一商品下多单 | 唯一索引没兜住，出现重复记录 |

所以压测后不是只看「接口能不能通」，而是**回到数据库里核对这三件事**（见第十节）。

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

## 三、准备压测用户与 token（自动化）

之前要「手动注册一堆用户、手动往 Redis 写一堆 token」，太麻烦。现在用**两个单元测试**一键完成。

> 前置：MySQL(3307)/Redis(6379) 已启动。这两个测试是 `@SpringBootTest`，自带 Spring 上下文，**不需要**后端进程在跑。
> ⚠️ 注意：它们是 `@SpringBootTest`，运行时会触发 `schema.sql`（重建 `goods`/`seckill_goods`/`seckill_order` 三张表）与数据播种、并把库存预载到 Redis——与正常启动后端行为一致，对 `mall_user`（用户表）无影响。所以请在「准备阶段」跑它们，不要在压测数据就绪后再跑。

### 1. 批量新建用户

在 IntelliJ 右键运行测试类：`src/test/java/org/example/monomer_seckill_backend/loadtest/LoadTestUserGeneratorTest`

- 按 `LoadTestConfig.USER_COUNT`（默认 **20000**）批量注册普通用户，用户名 `loaduser_00001`…`loaduser_20000`，统一密码 `123456`；
- **幂等**：已存在的用户名自动跳过，可重复运行；
- 想调整数量：改 `LoadTestConfig.java` 里的 `USER_COUNT` 常量。

### 2. 生成 token 并写出 tokens.txt

右键运行同一目录下的 `LoadTestTokenGeneratorTest`：

- 查出所有 `loaduser_*` 用户，为每个生成一个 UUID token，写入 Redis：`mall:token:{token} → userId`（有效期 **24 小时**，避免压测中途过期）；
- 把所有 token 逐行写入 `docs/tokens.txt`（UTF-8、`\n` 换行，WSL 里 Lua 的 `io.lines` 读得干净，不会带上 `\r`）；
- **可重复运行**：会先清掉上一次的旧 token，避免 Redis 堆积。

> 命令行等价做法（想手动看过程也可）：
> - 批量注册并登录取 token：`POST /api/user/register` 建用户 → `POST /api/user/login` 拿 `data.token`；
> - 或直接往 Redis 写 token→userId：`SET mall:token:{uuid} {userId}`（token 是去掉连字符的 UUID，value 是 userId）。

---

## 四、重置数据（每次压测前必做）

把秒杀商品1 的库存设成一个好算的数（**1000**）并清空秒杀订单。

**关键：P2 是 Redis 预扣库存，只重置 DB 不够，必须连 Redis 一起重置**（SQL 无法操作 Redis，需调用管理接口）。顺序：先重置 DB，再重置 Redis（接口会按 DB 当前 `seckill_stock` 预载 Redis，并清空已购集合 `seckill:users:1`）。

### 1. 重置 DB

执行 `docs/check-data.sql` 里的【0】段（Navicat/DBeaver 或命令行都行）：

```sql
DELETE FROM seckill_order;
UPDATE seckill_goods SET seckill_stock = 1000 WHERE id = 1;
```

### 2. 重置 Redis（二选一，推荐方式一）

- **方式一（无需重启）**：用管理员 token 调用管理接口
  ```bash
  # ① 登录拿管理员 token（默认账号 admin/admin123）
  curl -X POST http://localhost:7099/api/admin/login \
       -H "Content-Type: application/json" \
       -d '{"username":"admin","password":"admin123"}'
  # 返回体里的 data.token 即管理员 token

  # ② 重置秒杀商品1 的 Redis 库存并清空已购集合
  curl -X POST http://localhost:7099/api/admin/stock/reset/1 \
       -H "Authorization: Bearer <管理员token>"
  ```
- **方式二**：重启后端（启动时自动把 DB 库存预载到 Redis）。

> 说明：秒杀商品1 = 应用启动时 `DataInitializer` 播种的示例秒杀商品（`id=1`）。虽然种子库存是 50，但【0】会把 DB 设为 1000，再调 reset 让 Redis 同步成 1000，前后一致。

---

## 五、确认接口

秒杀下单需登录，用户身份从 token 解析（不再传 userId）：

```
POST http://localhost:7099/api/seckill/1
Authorization: Bearer <token>
```

- 路径参数 `1` = 秒杀商品ID（`seckill_goods.id`）；
- 从 `docs/tokens.txt` 里任取一个 token，带上 header 单发一次，确认返回 `{"code":200,...,"data":"SO...订单号"}`；
- 确认完**重跑一次【五】重置数据**，把刚才那笔测试订单清掉。

---

## 六、tokens.txt 与 post.lua

`docs/post.lua` 用 `io.lines("tokens.txt")` 读取一批 token（每行一个），在 `request()` 里循环取用并加到 `Authorization: Bearer <token>` 头。

⚠️ `tokens.txt` 必须与 `post.lua` 放在同一目录（都在 `docs/`，token 测试已自动写到 `docs/tokens.txt`）。运行 wrk 时进入 `docs` 目录再执行，或用 `-s /绝对路径/post.lua` 并保证 `io.lines` 能找到文件。

然后运行（IP 换成你的主机 IP）：

```bash
wrk -t10 -c200 -d30s --latency -s post.lua http://172.17.48.1:7099
```

---

## 七、压测命令怎么设参数

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

**秒杀测试套路**：初始库存 1000，`-c` 设 **20000**（远超库存），就是要让「成功」和「库存不足」的边界暴露出来。

---

## 八、报告怎么看

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

> 秒杀场景的正确预期：并发 20000、库存 1000 时，**大约 1000 个成功、19000 个「库存不足」失败**，这才是正常的（不是 1000 个全成功）。wrk 默认不显示非 2xx 状态码统计，要看失败类型需在 Lua 里 `wrk.headers`/`response()` 钩子里累计，或用 `--latency` + 结合后端日志判断。

---

## 九、高并发下如何确认数据对错（重点）

压测完，**别只看报告，去数据库核对**。打开 `docs/check-data.sql`，跑「压测后校验」那几段（并配合 Redis CLI 核对剩余库存）。

### 校验 1：Redis 剩余库存不为负（超卖）
DB 层 `seckill_goods.seckill_stock` 是原子扣减（`UPDATE ... WHERE seckill_stock > 0`），到 0 就停、**永远不会为负**；真正的超卖风险在 Redis 层。
```bash
redis-cli GET seckill:stock:1      # 期望 >= 0；更建议配合校验 2 看订单数
```

### 校验 2：订单数 + 剩余库存 = 初始库存（不多卖不少卖）
```sql
SELECT
  (SELECT COUNT(*) FROM seckill_order WHERE seckill_goods_id = 1)
  + (SELECT seckill_stock FROM seckill_goods WHERE id = 1) AS total;
```
✅ 期望：**= 1000**（前提：DB 与 Redis 都已重置为 1000）。
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
✅ 期望：秒杀商品1 卖出数 = 1000（和校验 2 对应）。可再核对 `SCARD seckill:users:1` 是否一致。

### 一句话总结
> **Redis/DB 库存不为负** + **订单数 = 初始库存 − 剩余库存** + **无重复下单** = 数据正确。

---

## 十、预期结果 vs 你可能遇到的问题

### 正常预期（P2：Redis 预扣库存 + Lua 实现）
并发 20000、库存 1000 → 1000 成功、19000「库存不足」失败；数据库校验 1/2/3 全通过。
秒杀请求先在 Redis 层用 Lua 脚本原子完成「判库存 → 判重复 → 扣库存 → 记用户」，
只有抢到库存的少数请求（约等于库存数）才会落到数据库建订单，DB 压力显著下降。

### 你大概率会看到的「问题」——这是好事
正常情况下不会再看到 `DuplicateKeyException`（重复下单已在 Redis Lua 层被幂等拦截）。
若 Redis 被重启导致已购用户集合丢失，极少数重复请求会穿透到数据库，最终靠 `(seckill_goods_id, user_id)`
唯一索引兜底抛出 `DuplicateKeyException`，此时后端会回滚 Redis 库存并返回「重复抢购」——这正是
「Redis 预扣 + DB 唯一索引兜底」双层防重复的设计。

### 准备/重置阶段常见坑
- **token 过期**：登录 token 只有 10 分钟，压测前用 `LoadTestTokenGeneratorTest` 生成的是 24 小时，够用；若手动写 token 请给足 TTL。
- **只重置了 DB 没重置 Redis**：Redis 里上轮扣剩的库存/已购集合不清，这轮一上来就「库存不足」或「重复抢购」。务必按【五】连 Redis 一起重置。
- **tokens.txt 带 `\r`**：WSL 里 Lua `io.lines` 不会剥离行尾 `\r`，token 会被污染导致鉴权失败；用测试生成的文件已按 `\n` 写好，若手改注意用 Unix 换行。

### wrk 相比 Apifox / JMeter 的优劣
- **优势**：极轻量、QPS 上限高、命令式、适合快速看吞吐与延迟。
- **局限**：
  - 默认不渲染失败状态码分布，要看失败原因需额外写 Lua 的 `response()` 统计。
  - 不是全功能压测平台（无图形报告、无断言、无分布式），要更细的图表/断言，再用 JMeter。

---

## 十一、相关文件

- 压测数据正确性校验 SQL（含压测前重置 / 压测后校验 / 压测后重置）：`docs/check-data.sql`
- 压测用 Lua 脚本：`docs/post.lua`
- 批量新建用户单测：`src/test/java/org/example/monomer_seckill_backend/loadtest/LoadTestUserGeneratorTest.java`
- 生成 Redis token + tokens.txt 单测：`src/test/java/org/example/monomer_seckill_backend/loadtest/LoadTestTokenGeneratorTest.java`
- 压测共享配置（用户数量 / token 有效期等）：`src/test/java/org/example/monomer_seckill_backend/loadtest/LoadTestConfig.java`
- 项目整体说明：`AGENTS.md`、`README.md`
- 路线图（P2 Redis 预扣库存 + Lua）：`monomer_seckill_plan.md`
