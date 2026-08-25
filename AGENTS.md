# AGENTS.md

秒杀学习项目（Spring Boot 3 + MyBatis-Plus + Redis + Spring Cloud Alibaba/Nacos/OpenFeign）。

后端有两套，互不影响：
- 单体版（P2）：`monomer_seckill_backend/monomer_seckill_backend/`（两层目录，端口 7099）
- 微服务版（P3/P4/P5）：`monomer_seckill_backend/monomer_seckill_backend_cloud/`（父 pom + common-service / user-service / goods-order-service / seckill-service / gateway-service，业务端口 7001/7002/7003，网关统一入口 8080）

前端在 `monomer_seckill_fromend/`：
- 单体版：`monomer_seckill_fromend/monomer/index.html`（调 7099）
- 微服务版：`monomer_seckill_fromend/cloud/index.html`（按 API 前缀路由到 7001/7002/7003）
- 网关版：`monomer_seckill_fromend/unified_gateway/index.html`（统一调 8080，经网关转发）

详细说明已拆分为 Agent Skills（`.dsh/skills/<name>/SKILL.md`），**按需加载，不要一次性全读**。先判断当前任务属于哪一类，只加载对应的那一个。

| Skill | 何时加载 |
| --- | --- |
| `seckill-overview` | 首次接触项目 / 看整体结构、技术栈、路线图、API 接口清单 |
| `seckill-environment` | 涉及 MySQL / Redis / WSL / 端口 / 中间件连接 |
| `seckill-build-run` | 编译 / 打包 / 启动 / 运行 / 联调 / 排查构建报错 |
| `seckill-database` | 建表 / 改表 / 写 SQL / 查数据 |
| `seckill-code` | 写 / 改 Java 代码、理解秒杀业务、遵循编码规范 |
| `java-coding-standards` | 写 / 改 Java 代码、代码审查（阿里编程规约，仅 Java） |
| `java-exception-logging` | 处理异常 / 写日志 / 设计错误码（全语言） |
| `java-unit-testing` | 写单元测试 / 测试用例（全语言） |
| `java-security-standards` | 实现安全功能 / 认证授权 / 输入校验（全语言） |
| `java-mysql-database` | 设计表结构 / 写 SQL / ORM 映射（全语言） |
| `java-project-structure` | 项目分层 / 依赖管理 / 架构组织（Java/Maven） |
| `java-design-standards` | 软件架构设计 / 领域建模 / 设计决策（全语言） |
| `error-code-catalog` | 设计 / 查询 A/B/C 三段式错误码（全语言） |

补充资料（非 skill，需要时再读）：
- 压测教程与数据校验：`docs/P2-wrk-压测教程.md`、`docs/P2-post.lua`、`docs/P2-check-data.sql`
- P5 限流熔断 JMeter 压测：`docs/P5-JMeter压测.md`、`docs/P5-seckill-gateway-limit.jmx`
- 完整路线图：`monomer_seckill_plan.md`
- 微服务知识点笔记：`knowledge/P3-微服务知识.md`、`knowledge/P4-网关微服务和JWT鉴权知识.md`、`knowledge/P5-限流熔断知识.md`
- 微服务工程说明：`monomer_seckill_backend/monomer_seckill_backend_cloud/README.md`
