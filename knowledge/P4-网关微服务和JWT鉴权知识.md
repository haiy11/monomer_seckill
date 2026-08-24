# P4 网关微服务和 JWT 鉴权知识

> 本文件整理秒杀项目 P4 阶段「统一流量入口：Spring Cloud Gateway 路由转发 + JWT 鉴权」涉及的通用知识点，作为学习笔记与面试复习材料。
> 代码工程对应 `monomer_seckill_backend/monomer_seckill_backend_cloud/`，新增的网关模块是 `gateway-service`（端口 8080）。

---

## 1. P4 阶段做了什么（概览）

P4 的目标是**收敛多端口直连**：P3 之后客户端要自己记住 7001 / 7002 / 7003 三个端口分别打哪个服务，P4 用一个网关统一入口替代。

P4 其实是**两件独立的事**，凑在一起形成"统一入口"：

| 事情 | 谁做的 | 方式 |
|------|--------|------|
| 路由转发 | Spring Cloud Gateway 框架 | **配置驱动**，几乎不写代码 |
| JWT 鉴权 | 我们自己 | 分「签发」和「校验」两半 |

- **签发**：登录时由 `common-service` 的 `TokenService.createToken(...)` 生成 JWT（在 user-service）。
- **校验**：网关里的 `JwtUtil`（验签）+ `AuthGlobalFilter`（决定放行/拒绝）。

涉及的新文件（都在 `gateway-service`）：

```
gateway-service/
├── pom.xml                                     # Gateway + Nacos + LoadBalancer + JJWT
├── src/main/resources/application.yml          # 路由规则 + 跨域 + jwt 密钥
└── src/main/java/com/example/seckill/gateway/
    ├── GatewayServiceApplication.java          # 启动类
    ├── auth/JwtUtil.java                       # JWT 验签解析
    └── filter/AuthGlobalFilter.java            # 全局鉴权过滤器
```

---

## 2. 请求是怎么先到网关、再被转发的（完整链路）

客户端访问 `http://localhost:8080/api/xxx`，第一个收到请求的是 gateway-service，它鉴权后转发给真正的业务服务：

```
客户端
   │  ① 发请求 http://localhost:8080/api/seckill/1
   ▼
gateway-service (8080)
   │  ② AuthGlobalFilter 先鉴权（验 JWT、查角色）
   │  ③ 通过后按路径匹配路由 → 转发
   ▼
seckill-service (7003)   ← 真正干活的
```

> 注意：7001/7002/7003 端口**没有关闭**，直连仍可达（保留了各服务的防御性鉴权）。真正的"统一入口"在生产里要靠网络隔离（防火墙只放行 8080）；本项目为了学习方便保留直连，只是约定都走 8080。

---

## 3. 网关为什么知道该转发给谁：路径匹配 + Nacos 服务发现

看 `gateway-service/src/main/resources/application.yml` 里的路由：

```yaml
routes:
  - id: seckill-service
    uri: lb://seckill-service
    predicates:
      - Path=/api/seckill/**,/api/seckill-goods/**
```

两个词：

1. **`Path=/api/seckill/**`（谓词 predicate）**：路径以 `/api/seckill/` 开头就匹配这条路由。
2. **`uri: lb://seckill-service`**：`lb://` 表示"负载均衡"，后面跟的是**服务名**（不是 `localhost:7003`）。网关拿服务名去问 **Nacos**："`seckill-service` 有哪些活着的实例、地址多少？"，拿到 `127.0.0.1:7003` 后再转发。

所以能"转发到对的微服务"，靠的是：

- 三个业务服务启动时**自己注册到了 Nacos**（各自 pom 里的 `spring-cloud-starter-alibaba-nacos-discovery`）；
- 网关通过 **Nacos 服务发现**把"服务名"解析成"真实地址"。

这也是为什么 P3 要先做 Nacos 注册发现——P4 的网关是站在 P3 肩膀上的。

---

## 4. 为什么"转发全靠配置，框架背后干活"

路由转发一行转发逻辑的 Java 代码都没写，全靠：

1. 引入 `spring-cloud-starter-gateway`；
2. 在 `application.yml` 写好 `routes`（路径 → `lb://服务名`）和 `globalcors`（跨域）。

但**"配置"能被理解，是因为 Spring Cloud Gateway 框架本身已经实现了路由引擎、Netty 反向代理、和 Nacos 服务发现的对接**。`application.yml` 只是"填表"，框架负责"照着表执行"。

> 类比：写 `server.port=8080` 就能改端口——不是配置会变魔术，而是框架读了配置去干活。

---

## 5. 鉴权的两半：签发（TokenService）与校验（JwtUtil + AuthGlobalFilter）

P4 把 token 从「Redis 存的随机 UUID」换成了「无状态 JWT」，这是能让**网关集中鉴权**成立的关键：

| | P3（原来） | P4（现在） |
|---|---|---|
| token 是什么 | 随机 UUID 字符串 | 自带信息的一段签名（JWT） |
| 用户信息存哪 | Redis：`mall:token:{uuid} → userId` | 直接编进 token 里 |
| 校验方式 | 拿 token 去 **Redis 查** | 拿密钥**验签**即可 |
| 是否含角色 | 不含（还要查 DB） | 含 `role` |

原 UUID token 的问题是：网关想验它必须去连 Redis，且 Redis 里只有 userId 没有 role。JWT 把 `userId`、`role`、`exp` 全打包进 token 本体并签名，于是**任何持有同一密钥的进程都能离线验签**，网关不需要连 Redis/DB。

- `TokenService.createToken(userId, role)`：签发（user-service 登录时调用）。
- `JwtUtil.parse(token)`：验签（网关调用）。
- 两者用**同一个 `jwt.secret`** 串起来：user-service 用它签，网关用它验。

> 为什么网关里单独写了一份 `JwtUtil`，而不是复用 common-service 的 `TokenService`？因为网关是响应式 WebFlux 技术栈，不能依赖带 Servlet MVC 的 common-service（会冲突），所以 JWT 解析逻辑在网关侧单独写一份，与 `TokenService` 保持同一密钥、同一字段约定。

---

## 6. GlobalFilter 和 Ordered 两个接口是干嘛的

`AuthGlobalFilter` 实现 `GlobalFilter` 和 `Ordered`，这是网关给的"约定"：

**`GlobalFilter`**：告诉框架"每个请求都要先经过我这个方法"。

```java
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
```

- `exchange`：装着本次请求/响应的全部信息（路径、方法、请求头……）。
- `chain.filter(exchange)`：意思是"我放行，交给下一个过滤器"。
- 不调用 `chain.filter`、直接 `writeError(...)` 返回：意思是"**短路**，别往后走了，直接给客户端回错误"。

类上有 `@Component`，Spring 启动时自动识别为全局过滤器，无需手动注册。

**`Ordered`**：决定它在过滤器链里的**执行顺序**。`getOrder()` 返回的数字越小越先执行。网关内部路由转发发生在顺序很大的地方（上万），所以返回 `-100` 保证"**先鉴权、再转发**"。

> `filter` 返回的 `Mono<Void>` 是响应式编程的"未来结果"。先理解成 `filter` 只有两种结局：`chain.filter` 继续 / `writeError` 直接返回即可。

---

## 7. 两个接口必须同时实现吗？能不能拆到两个类？

**不是绑定的**，可以只实现 `GlobalFilter`、完全不实现 `Ordered`，程序照样能跑。

但鉴权这个场景"顺序"是致命的：

- 不实现 `Ordered`（也不用 `@Order` 注解）时，Spring 会给过滤器一个默认顺序——排在最后面（路由转发之后）。请求都转发到下游了才来鉴权，等于白写。
- 所以实现 `Ordered` 并返回 `-100`，是**故意插到路由之前**，确保先鉴权后转发。

**能不能拆到两个类里实现？不能，也没必要。** `getOrder()` 描述的是"这个过滤器 bean 自己在链里的位置"，它天然必须挂在过滤器对象上。Spring 的排序逻辑是"把过滤器挨个问：你的 order 是多少"，不存在"类 A 当过滤器、类 B 替 A 声明顺序"的机制。

顺带：连 `Ordered` 接口都不用实现，直接加注解效果一样：

```java
@Component
@Order(-100)                    // 等价于 getOrder() 返回 -100
public class AuthGlobalFilter implements GlobalFilter { ... }
```

Spring 的排序比较器同时认 `Ordered` 接口和 `@Order` 注解。

---

## 8. 为什么把两个接口分开写（接口隔离）

- `GlobalFilter` 是 **Spring Cloud Gateway 专属**：语义是"网关过滤器，方法叫 filter"。
- `Ordered` 是 **Spring 全框架通用**的（在 `org.springframework.core` 包）：拦截器、监听器、各种 Bean 都在用 `getOrder()` 表达执行顺序。

硬塞成一个接口有两个坏处：

1. `Ordered` 没法复用——排序跟网关无关，却锁死在网关接口里。
2. 想只用一个功能的组件被迫实现两个（只想要顺序的、或只想要过滤的，都得写个假方法）。

拆开后按需组合：

| 需求 | 实现 |
|---|---|
| 只当过滤器、不在乎顺序 | 只 `implements GlobalFilter` |
| 只表达顺序 | 只 `implements Ordered` |
| 既要过滤又要控制顺序 | 两个都 `implements`（本项目） |

---

## 9. 网关怎么知道哪些接口要过滤、哪些不用过滤

**没有魔法，是一份手写的白名单** —— `AuthGlobalFilter.isPublicPath()`：

```java
private boolean isPublicPath(HttpMethod method, String path) {
    // 商品 / 秒杀商品查询公开
    if (HttpMethod.GET.equals(method)) {
        return path.startsWith("/api/goods") || path.startsWith("/api/seckill-goods");
    }
    // 注册 / 用户登录 / 管理员登录公开
    if (HttpMethod.POST.equals(method)) {
        return path.equals("/api/user/register")
                || path.equals("/api/user/login")
                || path.equals("/api/admin/login");
    }
    return false;
}
```

这份名单是**照着下游三个服务自己的拦截器规则人工汇总**的：

| 下游服务 WebConfig 拦截了什么 | 所以哪些是公开的 |
|---|---|
| user-service 拦 `/api/user/info`、`/api/user/apply-merchant`、`/api/merchant/**`、`/api/admin/**`(除登录) | 注册、用户登录、管理员登录公开 |
| goods-order-service 拦 `/api/cart/**`、`/api/order/**` | `/api/goods` 公开 |
| seckill-service 拦 `/api/seckill/**` | `/api/seckill-goods` 公开 |

> 网关不会自动知道哪些接口要鉴权。代价是：以后新增一个公开接口，要**同时改**网关白名单和对应服务的 `WebConfig`，两边保持一致。后续 P7（Nacos Config）可更优雅地集中管理。

---

## 10. 校验请求的完整流程（过滤器内部）

`AuthGlobalFilter.filter()` 是一串"早退判断"，命中哪个就在哪结束：

```
/internal/**        → 404 接口不存在（内部接口不对外）
不是 /api/**        → 放行（交给路由，配了路由才通，否则自然 404）
OPTIONS 预检请求     → 放行（跨域预检不需要 token）
公开接口(isPublicPath)→ 放行
────────────────────────────── 以上都不需要登录
剩下的都是受保护接口：
  没带/验签不过 token → 401
  管理员路径角色不对   → 403
  商家路径角色不对     → 403
  通过 → 加身份头 → 放行转发
```

校验四步（第 71-83 行）：

```java
// ① 从 Authorization 头取出 token
JwtUtil.JwtInfo info = jwtUtil.parse(extractToken(request));
// ② 验签失败/过期/没带 → 401
if (info == null || info.userId() == null) {
    return writeError(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
}
// ③ 管理员接口但角色不对 → 403
if (path.startsWith("/api/admin/") && !hasRole(info, ROLE_ADMIN)) { ... }
// ④ 商家接口但角色不对 → 403
if (path.startsWith("/api/merchant/") && !hasRole(info, ROLE_MERCHANT)) { ... }
```

1. `extractToken(request)`：从 `Authorization` 头取字符串，掐掉 `"Bearer "` 前缀，返回 JWT（或 null）。
2. `jwtUtil.parse(token)`：验签 + 查过期，返回 `JwtInfo{userId, role}` 或 null。
3. 判空 → 401。
4. 判角色（`hasRole`：`info.role() != null && info.role() == 期望值`）→ 403。

通过后把 `userId`/`role` 塞进 `X-User-Id`、`X-User-Role` 头，再 `chain.filter(...)` 交给路由转发。

---

## 11. 网关为什么用响应式（Reactive/WebFlux）

先纠正一个常见直觉：**响应式不是"为了早点校验"**。我的鉴权只读路径和请求头，这些在 HTTP 里本来就先于 body 到达，无论响应式还是 Servlet 都是这样。响应式改变的是"线程怎么等 I/O"，不是"校验时机"。

真正价值是两点：

1. **非阻塞 I/O：用少量线程扛海量并发**。传统 Servlet 一个请求占一个线程，线程要等下游响应就干等卡住。网关是所有流量总闸门，并发极高，若用 Servlet 则 1 万并发要 1 万线程。响应式是事件驱动：请求来了登记回调，线程不等、转去处理别的，响应回来再回调继续。
2. **流式转发 + 背压：不用把整个 body 读进内存**。网关是代理角色，本质是"边收边转发"，响应式按块流式转发，上传大文件更省内存。

**为什么"必须"响应式？** Spring Cloud Gateway 框架**从设计之初就是基于 WebFlux/Reactor**，不是可选风格。所以 pom 里注释"不可引入 spring-boot-starter-web"——引入传统 Servlet MVC 会和响应式栈冲突，启动直接报错。

---

## 12. 负载均衡原理：Nacos 给名单，LoadBalancer 选一个

关键认知：**Nacos 不做负载均衡，做负载均衡的是 `spring-cloud-starter-loadbalancer`，而且它跑在网关自己进程里。**

```
网关要转发 lb://user-service
  │  ① 问 Nacos：「user-service 有哪些健康实例？」
  │  ② Nacos 答：「有 A(7001) 和 B(7001)」        ← 服务发现，只给全名单
  │  ③ Spring Cloud LoadBalancer（网关进程内）按规则挑一个，比如 B
  │  ④ 网关连 B 转发
```

| 组件 | 职责 | 位置 |
|---|---|---|
| Nacos | **服务发现**：维护"服务名 → 实例名单"，只告诉你有哪些 | 注册中心（独立进程） |
| Spring Cloud LoadBalancer | **负载均衡**：从名单里挑一个，默认**轮询**（RoundRobin） | 调用方进程内 |

> 纠正一个误解：不是"引入依赖告诉 Nacos 按我的规则返回地址"，而是"引入依赖让**网关自己**在拿到名单后按我的规则挑一个"。想改规则，就在网关里注册自定义的 `ReactorServiceInstanceLoadBalancer` Bean 覆盖默认实现。

---

## 13. P3 就已经用过负载均衡（Feign）

这套"服务发现 + 客户端负载均衡"机制在 P3 里已经用过，只是当时没注意：

- P3 里 user-service 用 Feign 写 `@FeignClient(name = "goods-order-service")`，内部也是"`name` → 问 Nacos 拿名单 → LoadBalancer 挑一个 → 发 HTTP"，和网关的 `lb://goods-order-service` 是**同一套机制**。
- 所以 user-service 的 pom 在 P3 时就已经有 `spring-cloud-starter-loadbalancer`。

网关的 `lb://` 只是把这个已有能力用到了"转发"场景上。

---

## 14. 多实例下登录态共享问题（无状态 JWT 解决）

**问题场景**：登录命中实例 A，下个请求被负载均衡到实例 B，B 不认识我怎么办？

**会不会发生，取决于登录态存在哪**：只有"登录态存在某个实例自己的内存里"（如 `HttpSession`、JVM 里的 Map）才会出问题——A 记得你、B 的内存是空的。

| 方案 | 登录态存哪 | 多实例下会出问题吗 |
|---|---|---|
| JWT（P4） | 存在 token 里，客户端携带 | ❌ 不会 |
| Redis 共享会话（P3 之前） | 共享 Redis | ❌ 不会 |
| 实例内存 / HttpSession | 某个 JVM 内存里 | ✅ **会** |
| 粘性会话（sticky session） | 实例内存，但固定打到同一实例 | 勉强不会（不推荐） |

**P4 为什么不会出问题**：JWT 无状态，登录态在 token 里、客户端每次带上。命中 B 时，B 用同一个 `jwt.secret` 验签，直接解出 userId + role，不需要"记得"你。

**P3 也不会出问题**：token 是 UUID 存在共享 Redis，B 也查同一个 Redis，照样能查到你是谁。

**JWT 无状态的代价**（面试加分点）：

- P3 用 Redis 存 token 时，想踢人下线直接 `deleteToken` 删掉 Redis 那条即可，**即时生效**。
- JWT 无状态，服务端没有"这张票还活着吗"的记录，**发了就收不回**，只能等 `exp` 过期。想即时封禁需额外引入"黑名单"机制。

> 选型心法：JWT 无状态、天然支持水平扩展、无需共享存储，但**难以即时吊销**；Redis Session 可即时吊销，但**依赖共享中间件、每次要查 Redis**。P4 选 JWT 是因为网关要集中鉴权、要横向扩展，无状态最合适。

---

## 15. 文件下载场景：共享存储 vs 粘性会话

**问题**：两服务轮询，文件生成在 A、下载链接却命中 B → 404。

两种常见解法（都有名字）：

| 方案 | 名称 |
|---|---|
| 文件放共享路径/对象存储 | **共享存储**（NFS / OSS / MinIO / S3） |
| 同一人的请求固定到同一实例 | **粘性会话 / 会话保持（sticky session / session affinity）** |

**粘性会话网关能实现吗？能。** 机制是"基于 Cookie 的粘性会话"：

```
第一次请求（无粘性 cookie）→ LoadBalancer 按轮询挑 A → 响应里塞 Set-Cookie: sc-lb-instance-id=<A>
之后的请求 → 浏览器带上该 cookie → LoadBalancer 读到就固定转发给 A
```

Spring Cloud LoadBalancer 原生支持 request-based sticky session，配置项 `spring.cloud.loadbalancer.sticky-session.*`（如 `add-service-instance-cookie`、`instance-id-cookie-name`）。参考：[Request based sticky session (spring-cloud-commons PR #860)](https://github.com/spring-cloud/spring-cloud-commons/pull/860)、[配置示例](https://stackoverflow.com/questions/68026243/request-based-sticky-session-configuration-with-spring-cloud-loadbalancer)。

> 小纠正：粘性会话通常挂在**会话 Cookie**（如 `JSESSIONID`）上，而不是"登录态"本身，本质都是给客户端贴标签固定去某实例。

**但工程上，文件下载场景共享存储才是正解**：

1. 文件放实例本地磁盘 + 粘性会话是脆的——实例重启/宕机/被替换，本地文件可能丢失，粘性只能保证"你去找 A"，不能保证"A 手里还有文件"。
2. 粘性破坏负载均衡的均匀性。
3. 它是"有状态"的退路，把复杂性转移而非消除。

> 核心心法（贯穿本项目）：**状态（登录态、文件、库存）要么无状态（JWT），要么放共享中间件（Redis/DB/对象存储），不要放在单个实例自己的内存或本地磁盘里**。这样负载均衡怎么甩都不会出问题。

---

## 16. JWT 不是"加密"，是"签名"

JWT 分三段 `header.payload.signature`：

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyMDAwNyIsInJvbGUiOjB9.签名
      ↑ header            ↑ payload(载荷)              ↑ signature
```

- **前两段只是 Base64Url 编码，不是加密**。任何人解码就能看到内容（如 `{"sub":"20007","role":0}`）。所以**不要把密码、手机号等敏感信息塞进 JWT**，它本来就是"明文"。
- **第三段签名**的作用只有两个：① 证明内容没被篡改；② 证明这是持密钥的人签发的。**它不负责保密**。

> 真正会加密的是另一个标准 JWE（JSON Web Encryption），本项目没用。日常说的 JWT 其实是 JWS（签名、可读、防篡改）。

---

## 17. 为什么 JJWT 拆成三个依赖

JWT 的 payload 是一段 JSON，而"Java 对象 ↔ JSON"的转换有 Jackson、Gson、org.json 等多套库。JJWT 把"JWT 核心逻辑"和"用哪个 JSON 库"解耦：

| jar | 是什么 | 类比 |
|---|---|---|
| `jjwt-api` | 接口 + 入口类（`Jwts`、`Claims`、`JwtBuilder`、`JwtParser`），代码只 import 它 | 插座标准 |
| `jjwt-impl` | 默认实现引擎：真正做签名/验签/解析 | 电器内部电路 |
| `jjwt-jackson` | "用 **Jackson** 处理 payload"的具体实现 | 插头（Jackson 款） |

- `jjwt-jackson` 只是"Jackson 款插头"，官方还提供 `jjwt-gson`、`jjwt-orgjson` 等"别的款插头"。项目若已用 Gson，就把第三个依赖换成 `jjwt-gson`，JWT 逻辑和业务代码**一行不改**。这就是"自由组合"。
- 类比 JDBC：`jjwt-api` ≈ JDBC 接口，`jjwt-impl`+`jjwt-jackson` ≈ MySQL 驱动。代码只面向接口写，运行时换实现。

**为什么不合并成一个 jar？** 完全可以，别的库（`java-jwt`、`nimbus-jose-jwt`）就是单 jar。"拆三个"不是技术必需，而是 JJWT 的取舍：

1. 解耦：代码依赖接口而非实现。
2. 可插拔：不被强制用 Jackson，避免与项目已有 JSON 库版本冲突。
3. 不强加依赖：合并成单 jar 等于逼所有使用者带上 Jackson。

代价是使用者要自己记住"三个都要引"，少一个运行时报错。

**`scope=runtime` 的含义**：

- `jjwt-api`：代码 `import` 它的类，**编译期**需要 → 默认 compile。
- `jjwt-impl` / `jjwt-jackson`：代码从不直接 import，只是运行时幕后干活 → 标 `runtime`（编译期不需要，运行期才需要），同时向读 pom 的人传达"这是引擎，不是给业务代码调的"。

---

## 附：本文对应代码文件

- `gateway-service/pom.xml`
- `gateway-service/src/main/resources/application.yml`
- `gateway-service/.../GatewayServiceApplication.java`
- `gateway-service/.../auth/JwtUtil.java`
- `gateway-service/.../filter/AuthGlobalFilter.java`
- `common-service/.../auth/TokenService.java`（JWT 签发/解析）
- `common-service/.../auth/AuthInterceptor.java`（下游防御性校验）
- `user-service/.../service/UserService.java`（登录签发 JWT）
