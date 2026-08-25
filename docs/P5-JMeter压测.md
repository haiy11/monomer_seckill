# 用 JMeter 对 P5 限流熔断做压测验证

> 目标：用 JMeter 模拟大量**不同用户**同时访问，验证 Sentinel 的网关限流（QPS=100）、服务熔断（慢调用比例）、热点参数限流（商品维度）是否按预期拦截流量。

---

## 〇、整体流程速览（先看这条）

P5 压测验证的是「限流/熔断是否按预期拦流量」，和 P2 秒杀压测（验证不超卖/不多卖）目标不同。

关键区别：P5 每个请求**必须是不同用户**——同一用户第二次下单会被「重复抢购」拦掉，测不出真实并发。所以一次完整压测分四步：

1. **批量新建用户** —— 跑单测 `LoadTestUserGeneratorTest`（一次建 2000 个 `jmeteruser_*` 用户）；
2. **生成 JWT + tokens.txt** —— 跑单测 `LoadTestTokenGeneratorTest`（给每个用户签发 JWT，逐行写 `docs/tokens.txt`）；
3. **复制 tokens.txt 到压测 WSL** —— 交给 JMeter 逐行取用；
4. **跑 JMeter** → 观察 429/503。

前置：后端微服务已启动（统一入口 **8080**）、MySQL(3307)/Redis(6379)/Nacos(8848) 已启动。

---

## 一、先想清楚：P5 压测到底在验证什么

三个场景，对应三种 Sentinel 规则，也对应三种不同的「被拦」响应：

| 场景 | 规则 | 触发条件 | 现象 |
|------|------|----------|------|
| 网关限流 | 网关 `GatewayFlowRule` QPS=100 | `/api/seckill/**` 每秒 > 100 个请求 | **HTTP 状态码 429**，响应体 `{"code":429,"msg":"请求过于频繁，已被限流…"}` |
| 服务熔断 | seckill-service `DegradeRule` 慢调用比例 | 1 秒内 ≥5 个请求、RT>200ms 的慢调用占比 > 20% | **HTTP 200** + 响应体 `{"code":503,"msg":"演示资源已熔断…"}` |
| 热点参数限流 | seckill-service `ParamFlowRule` 商品维度 | 商品 id=1 每秒 > 5 个请求 | **HTTP 200** + 响应体 `{"code":429,"msg":"该商品太火爆…"}` |

> 关键点：网关限流返回的是 **HTTP 状态码 429**；服务侧熔断/热点限流按本项目业务约定返回 **HTTP 200 + 响应体里的 code**。看结果时用「响应代码」和「响应数据」两个字段区分，别混。

---

## 二、JMeter 是什么 / 怎么装

JMeter 是 Apache 的纯 Java 压测工具，支持图形界面 + 非 GUI 命令行两种方式。压测时用**非 GUI 模式**（`-n`），不占图形资源、吞吐更高。

在压测 WSL 里用官方 tarball 安装（最新稳定版 5.6.3）：

```bash
# 1. 装 JRE（JMeter 依赖 Java）
sudo apt update && sudo apt install -y openjdk-21-jre-headless

# 2. 下载官方 tarball（Apache 最新稳定版 5.6.3）
wget https://dlcdn.apache.org/jmeter/binaries/apache-jmeter-5.6.3.tgz

# 3. 解压
tar -xzf apache-jmeter-5.6.3.tgz

# 4. 加入 PATH（bash 读 ~/.bashrc，zsh 读 ~/.zshrc；不确定就两个都写）
echo 'export PATH=$PATH:$HOME/apache-jmeter-5.6.3/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$HOME/apache-jmeter-5.6.3/bin' >> ~/.zshrc
source ~/.zshrc 2>/dev/null || source ~/.bashrc

# 5. 验证
jmeter --version
```

> 若 `dlcdn.apache.org` 下载慢或失效，去 [Apache JMeter 官方下载页](https://jmeter.apache.org/download_jmeter.cgi) 看最新地址，替换上面的 tarball 链接即可。

---

## 三、准备压测用户与 token（自动化）

之前要「手动注册一堆用户、手动登录取一堆 token」，太麻烦。用**两个单元测试**一键完成（在 `user-service` 模块里，与 P2 单体版同理）。

> 前置：MySQL(3307) 已启动，且 `mall_user` 表已建（启动过一次 `goods-order-service` 即可）。这两个测试是 `@SpringBootTest`，自带 Spring 上下文，**不需要**后端进程在跑；运行时会连 Nacos/MySQL。

### 1. 批量新建用户

在 IntelliJ 右键运行：`user-service/src/test/java/com/example/seckill/user/loadtest/LoadTestUserGeneratorTest`

- 按 `LoadTestConfig.USER_COUNT`（默认 **2000**）批量注册普通用户，用户名 `jmeteruser_00001`…`jmeteruser_02000`，统一密码 `123456`；
- **幂等**：已存在的用户名自动跳过，可重复运行；
- 想调整数量：改 `LoadTestConfig.java` 里的 `USER_COUNT` 常量。

### 2. 生成 JWT 并写出 tokens.txt

右键运行同目录下的 `LoadTestTokenGeneratorTest`：

- 查出所有 `jmeteruser_*` 用户，用 `TokenService` 给每个用户签发一个 **JWT**（P4 起登录态是无状态 JWT，token 即 JWT 本身，**不需要写 Redis**，比单体版更简单）；
- 把所有 JWT 逐行写入 `docs/tokens.txt`（UTF-8、`\n` 换行，JMeter 逐行读，行尾不能带 `\r`）；
- token 有效期由 `jwt.expire-minutes` 决定（默认 **120 分钟**），够一次压测，过期重跑本测试即可。

> 命令行等价做法（不用 IDE）：先切 JDK21 + Maven3.9.10（见 `seckill-build-run` skill），在 `user-service` 目录执行：
> ```bash
> mvn -Dtest=LoadTestUserGeneratorTest test
> mvn -Dtest=LoadTestTokenGeneratorTest test
> ```
> 两个要**按顺序**跑（先建用户、再生成 token）。

---

## 四、复制 tokens.txt 到压测 WSL + 确认网络

### 1. 复制 tokens.txt

在压测 WSL 里（Windows 磁盘挂在 `/mnt/` 下）：

```bash
mkdir -p ~/jmeter
cp /mnt/e/items/natherItems/monomer_seckill/docs/tokens.txt ~/jmeter/tokens.txt
# 检查：应有 2000 行，每行一个 JWT
wc -l ~/jmeter/tokens.txt
```

> 其实 JMeter 也能直接读 `/mnt/e/.../tokens.txt`，但复制到 WSL 本地读得更快、避免跨文件系统 I/O 影响压测。

### 2. 确认网络

JMeter 在 WSL 里访问 Windows 微服务，**不能用 `localhost`**（`localhost` 指向 WSL 自己），要用 Windows 主机在 WSL2 虚拟网里的地址（本项目是 `172.17.48.1`）：

```bash
# 确认主机地址（通常就是 172.17.48.1）
ip route show default | awk '{print $3}'

# 测通网关（应返回 JSON）
curl -i http://172.17.48.1:8080/api/seckill-goods
```

- 返回 `HTTP/1.1 200` + JSON 即通；
- 连不上多半是 Windows 防火墙拦了 WSL2 的入站流量，放行 8080（或 7001/7002/7003）即可。

---

## 五、确认接口

从 `~/jmeter/tokens.txt` 任取一个 token 单发一次，确认秒杀接口能通：

```bash
TOKEN=$(head -n 1 ~/jmeter/tokens.txt)
curl -i -X POST http://172.17.48.1:8080/api/seckill/1 \
     -H "Authorization: Bearer $TOKEN"
```

- 首次下单：`{"code":200,...,"data":"SO...订单号"}`；
- 同一 token 再打一次：`{"code":500,"msg":"您已抢购过该秒杀商品"}` —— 这正是「每个请求要换不同 token」的原因。

---

## 六、JMeter 测试计划怎么搭（tokens.txt 循环取 token）

三个场景骨架一样，差异只在「线程数 / 路径 / 断言」。元素树：

```
测试计划 (Test Plan)
├── CSV Data Set Config           # 读 tokens.txt，变量名 TOKEN
├── 线程组 (Thread Group)          # 并发数 / Ramp-Up / 循环 见第七节
│   ├── HTTP Header Manager       # Authorization = Bearer ${TOKEN}
│   └── HTTP Request              # 服务器 172.17.48.1、端口 8080、方法/路径见第七节
│       └── Response Assertion    # 见第七节
└── 监听器：查看结果树 / 汇总报告    # 看响应码、吞吐、错误占比
```

**CSV Data Set Config 关键项**（核心：让每个线程取不同 token）：

| 配置项 | 值 |
|--------|----|
| Filename | `~/jmeter/tokens.txt` |
| Variable Names | `TOKEN` |
| Sharing mode | All threads（所有线程共享文件，逐个取下一行，即每个虚拟用户拿到不同 token） |
| Recycle on EOF | True（请求数多于 token 数时循环回到开头） |

**HTTP Header Manager**：加一条 `Authorization` = `Bearer ${TOKEN}`。

### 现成的测试计划文件（省得手搭）

`docs/P5-seckill-gateway-limit.jmx` 就是上面这套骨架的「场景一：网关限流」，拷到 WSL 后可直接非 GUI 运行（见第八节）。它里面每个配置的用途：

| 元素（.jmx 里的名字） | 作用 |
|----------------------|------|
| 用户定义的变量 `HOST` / `PORT` | 被测网关地址/端口（默认 `172.17.48.1:8080`），环境变了改这里即可 |
| CSV Data Set Config（`读取 tokens.txt`） | 从 tokens.txt 逐行读 JWT 到变量 `TOKEN`；Sharing mode=All threads，每个虚拟用户取不同 token |
| 线程组（`秒杀接口 300 并发`） | 300 并发、Ramp-Up 1 秒、循环 1，模拟瞬时突发流量 |
| HTTP Header Manager（`JWT 请求头`） | 给每个请求加 `Authorization: Bearer ${TOKEN}` |
| HTTP 请求（`GET /api/seckill/demo/slow?delay=0`） | 打网关下的快接口，只触发网关限流、不掺服务侧热点限流 |
| 汇总报告（`Summary Report`） | 非 GUI 下由 `-l result.jtl` 收集结果；GUI 下直接看聚合统计 |

两个要注意的点：

- CSV 的 `filename` 写死为 `/root/jmeter/tokens.txt`；你不是 root 或放别的目录，就改成你的实际路径（或相对路径 `tokens.txt`，并把 tokens.txt 与 .jmx 放同一目录）。
- 本文件**故意不加断言**：网关限流结果是「200（通过）+ 429（被限流）」混合的，严格断言会误报。跑完看汇总报告/HTML 报告里 429 的数量即可；想自动标记异常（401/500）请求，再在 GUI 里给 HTTP 请求加「响应断言」，字段选「响应代码」、规则选「匹配」、填 `(200|429)`。

---

## 七、三个压测场景

### 场景一：网关限流（QPS=100 → HTTP 429）

| 项 | 值 |
|----|----|
| HTTP 请求 | `GET /api/seckill/demo/slow?delay=0`（快接口，隔离服务侧热点限流，只测网关） |
| 线程组 | 线程数 300、Ramp-Up 1 秒、循环 1 |
| 断言 | 响应代码（response code）Contains `429` |

预期：300 并发远超 100 QPS，超出部分被网关拦下返回 **HTTP 429**（响应体 `请求过于频繁，已被限流`）。

> 想更快肉眼看到 429，可临时把 `GatewaySentinelConfig` 的 `SECKILL_QPS_LIMIT` 改小（如 5）再测，测完改回。

### 场景二：服务熔断（慢调用比例 > 20% → 503）

| 项 | 值 |
|----|----|
| HTTP 请求 | `GET /api/seckill/demo/slow?delay=300`（300ms > 熔断阈值 200ms，即慢调用） |
| 线程组 | 线程数 30、Ramp-Up 1 秒、循环「永远」+ 持续时间 20 秒 |
| 断言 | 不加严格断言（前 1 秒是 200、之后才 503），直接看结果树观察 503 出现 |

预期时间线：`0~1s 全 200（慢调用累积）→ 1s 后熔断打开，请求秒回 503 → 约 10s 后半开试探 → 试探仍慢则再次熔断`。

### 场景三：热点参数限流（商品维度 → 429）

| 项 | 值 |
|----|----|
| HTTP 请求 | `POST /api/seckill/1`（商品 id=1 是热点，QPS 阈值 5） |
| 线程组 | 线程数 30、Ramp-Up 1 秒、循环 1（30 < 网关 100，不会混入网关 429） |
| 断言 | 响应数据（response data）Contains `该商品太火爆` |

预期：前约 5 个请求得到业务结果（成功/重复抢购），第 6 个起返回 `{"code":429,"msg":"该商品太火爆"}`。

---

## 八、非 GUI 运行 + 报告怎么看

```bash
# 先把 .jmx 也拷到 WSL（和 tokens.txt 放一起）
cp /mnt/e/items/natherItems/monomer_seckill/docs/P5-seckill-gateway-limit.jmx ~/jmeter/
cd ~/jmeter

# 单个场景：-n 无 GUI，-l 存结果
jmeter -n -t P5-seckill-gateway-limit.jmx -l result.jtl

# 生成 HTML 报告（更直观）
# 注意：-e 生成报告时要求 -l 的结果文件是空的，所以先删掉上次的 result.jtl，否则报 "Results file is not empty"
rm -f result.jtl && rm -rf report && jmeter -n -t P5-seckill-gateway-limit.jmx -l result.jtl -e -o report
```

看结果：

| 现象 | 含义 |
|------|------|
| HTTP 状态码 **429** | 网关限流生效 |
| HTTP 200 + 响应体 `code:503` | 服务熔断生效 |
| HTTP 200 + 响应体 `code:429` | 热点参数限流生效 |

重点看「查看结果树」里每个请求的**响应代码**（HTTP 状态码）和**响应数据**（body 里的 code/msg），以及「汇总报告」的吞吐（Req/s）和错误占比。

---

## 九、预期结果 vs 你可能遇到的问题

### 正常预期
- 场景一：300 并发下大量 HTTP 429，说明网关把超 100 QPS 的流量拦住了；
- 场景二：先 200、后大量 503（快速失败，响应时间骤降）、再半开恢复；
- 场景三：同一商品每秒第 6 个请求起被拦，返回 `该商品太火爆`。

### 大概率会遇到的坑
- **`localhost` 连不上**：JMeter 在 WSL 里必须用 `172.17.48.1`，不是 `localhost`。
- **全是 401 未登录**：Header Manager 没配 `Authorization: Bearer ${TOKEN}`，或 token 过期（120 分钟）——重跑 `LoadTestTokenGeneratorTest`。
- **全是「重复抢购」**：所有请求用了同一个 token（没配好 CSV Data Set Config，或 Sharing mode 没设 All threads）——每个虚拟用户必须取不同 token。
- **网关 429 与服务 429 分不清**：看 HTTP 状态码（网关=429、服务=200）+ 响应体 msg（见第一节表）。
- **熔断测不出**：并发太低，1 秒内不足 5 个慢调用（`minRequestAmount=5`）——提高线程数，保证 `delay>200` 且请求 > 5 QPS。
- **响应体中文乱码**：HTTP Request 里设「内容编码 UTF-8」，或直接看 code/msg 的英文字段。

---

## 十、相关文件

- 批量新建用户单测：`user-service/src/test/java/com/example/seckill/user/loadtest/LoadTestUserGeneratorTest.java`
- 生成 JWT + tokens.txt 单测：`user-service/src/test/java/com/example/seckill/user/loadtest/LoadTestTokenGeneratorTest.java`
- 压测共享配置（用户数量 / 前缀 / 密码）：`user-service/src/test/java/com/example/seckill/user/loadtest/LoadTestConfig.java`
- 生成的 token 文件：`docs/tokens.txt`
- JMeter 测试计划（网关限流场景）：`docs/P5-seckill-gateway-limit.jmx`
- Sentinel 规则（限流/熔断阈值）：`gateway-service/.../gateway/sentinel/GatewaySentinelConfig.java`、`seckill-service/.../seckill/config/SentinelConfig.java`
- 限流熔断原理笔记：`knowledge/P5-限流熔断知识.md`
- P2 秒杀压测（数据正确性校验，另一套目标）：`docs/P2-wrk-压测教程.md`
- 整体路线图：`monomer_seckill_plan.md`
