### 🗺️ 整体项目阶段总览（2 周）

| 阶段编号 | 阶段名称              | 核心任务                                 | 涉及技术                          |
| :------: | :-------------------- | :--------------------------------------- | :-------------------------------- |
|  **P1**  | 环境基石搭建          | WSL + Docker + AI CLI + 中间件容器化     | WSL、Docker Compose、sgpt(AI CLI) |
|  **P2**  | 高性能单体秒杀        | 秒杀核心逻辑 + Redis 预扣库存 + Lua 脚本 | Spring Boot、Redis、Lua、wrk      |
|  **P3**  | 微服务拆分 + 注册调用 | 拆分为 3 个服务，Feign 远程调用          | Nacos(注册中心)、OpenFeign        |
|  **P4**  | 统一流量入口          | Gateway 路由转发 + JWT 鉴权              | Spring Cloud Gateway              |
|  **P5**  | 高可用限流熔断        | Sentinel 限流、降级、热点参数规则        | Sentinel、JMeter 压测             |
|  **P6**  | 多级缓存优化          | Caffeine 本地缓存 + Redis 分布式缓存     | Caffeine、Redis（深度）           |
|  **P7**  | 配置中心 + 动态刷新   | 配置集中管理，修改配置不重启             | Nacos Config                      |
|  **P8**  | 异步削峰解耦          | MQ 下单 + 库存异步扣减 + 死信队列        | RabbitMQ、死信队列(DLX)           |
|  **P9**  | 分布式事务（选做）    | 最终一致性方案（TCC / 事务消息）         | Seata / RocketMQ 事务消息         |


### 📅 两周落地安排（按天拆解）

#### 🔥 第 1 周：地基 → 微服务骨架 → 高可用

| 日期     | 阶段   | 核心任务       | 具体动作                                                     | 耗时 |
| :------- | :----- | :------------- | :----------------------------------------------------------- | :--- |
| **周日** | **P1** | 环境基石搭建   | ① WSL 安装 + 迁移到 SSD<br>② Docker Desktop + docker-compose 安装<br>③ 用 docker-compose 一键启动 MySQL、Redis、Nacos、RabbitMQ<br>④ 安装 AI CLI（sgpt），配置 DeepSeek API 密钥，测试问一句 "docker 启动 Nacos 命令" | 4~6h |
| **周一** | **P2** | 单体秒杀（上） | ① 创建 Spring Boot 单体项目<br>② 用 AI 生成 RedisConfig + RedisUtil<br>③ 编写 Lua 脚本（扣库存 + 回滚）<br>④ 实现 `/seckill` 接口，Redis 预扣库存 | 1.5h |
| **周二** | **P2** | 单体秒杀（下） | ① 完善业务逻辑（库存不足、重复抢购、超时回滚）<br>②实现一个简易的后台管理功能<br>③ 用 wrk 简易压测，确保单体能抗 5000+ QPS<br>④排查 AI 辅助解决报错 | 1.5h |
| **周三** | **P3** | 微服务拆分     | ① 引入 Spring Cloud Alibaba 依赖<br>② 将单体拆分为 order-service、stock-service、user-service、admin-service<br>③ 四个服务成功注册到 Nacos | 1.5h |
| **周四** | **P3** | Feign 远程调用 | ① 在 order-service 引入 OpenFeign<br>② 编写 Feign 客户端调用 stock-service 扣库存<br>③ 调试调用链（order → stock），确保通顺 | 1.5h |
| **周五** | **P4** | 统一网关入口   | ① 创建 gateway-service，配置路由规则<br>② 编写全局过滤器（GlobalFilter），解析 JWT Token 鉴权<br>③ 端到端测试：Postman → Gateway → order → stock | 1.5h |
| **周六** | **P5** | 限流熔断       | ① 引入 Sentinel，配置网关流控规则（QPS=100）<br>② JMeter 压测验证限流生效<br>③ 配置 stock-service 熔断规则（慢调用比例 > 20% 触发）<br>④ 可选：配置热点参数限流（如商品 ID 维度） | 6h   |

> 至此第 1 周结束，你已完成 **微服务注册发现 + 远程调用 + 网关 + 限流熔断**，骨架完整。


#### 🚀 第 2 周（7.20 - 7.26）：缓存进阶 → 配置中心 → 消息队列 → 收尾

| 日期     | 阶段     | 核心任务           | 具体动作                                                     | 耗时 |
| :------- | :------- | :----------------- | :----------------------------------------------------------- | :--- |
| **周日** | **P6**   | 多级缓存           | ① 引入 Caffeine 本地缓存（热点商品信息）<br>② 构建 multi-level cache 链：Caffeine → Redis → DB<br>③ 实现缓存更新策略（更新 DB 后删除/刷新缓存）<br>④ JMeter 对比压测，观察响应时间下降 | 6h   |
| **周一** | **P7**   | 配置中心           | ① 将 order-service 和 stock-service 的 application.yml 搬到 Nacos Config<br>② 实现动态刷新（@RefreshScope），修改限流阈值不重启生效<br>③ 多环境配置（dev/test/prod） | 1.5h |
| **周二** | **P8**   | 消息队列（上）     | ① 学习 RabbitMQ 核心概念（Exchange、Queue、Binding）<br>② 在 order-service 中发送 "扣减库存" 消息<br>③ stock-service 监听消息，异步执行扣减 | 1.5h |
| **周三** | **P8**   | 消息队列（下）     | ① 引入死信队列（DLX），处理下单后 15 分钟未支付自动取消<br>② 测试：下单后不支付，15 分钟后库存自动回补<br>③ 用 AI 辅助排查 MQ 连接/序列化报错 | 1.5h |
| **周四** | **P9**   | 分布式事务（选做） | ① 引入 Seata AT 模式，保证下单+扣库存强一致性<br>② 或使用 RocketMQ 事务消息实现最终一致性<br>（若时间不够，跳过此阶段，面试只讲 MQ + 本地消息表方案即可） | 1.5h |
| **周五** | **收尾** | 全链路压测 & 调优  | ① JMeter 完整压测（登录 → 抢购 → 支付 → 回滚）<br>② 调整线程池、连接池、超时参数<br>③ 整理面试话术（见下方模板） | 1.5h |
| **周六** | **收官** | 项目总结 & 文档    | ① 用 AI CLI 生成 README 架构图描述<br>② 整理技术选型理由清单（为什么用 Nacos 而不是 Eureka？为什么用 Sentinel 而不是 Hystrix？）<br>③ 录制 3 分钟演示视频（Postman 调用 + 监控截图）<br>④ 休息半天，奖励自己！ | 4h   |
