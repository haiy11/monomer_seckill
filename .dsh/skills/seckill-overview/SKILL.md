---
name: seckill-overview
description: "了解秒杀项目的整体定位、目录结构、技术栈、阶段路线图和 API 接口清单。首次接触项目、需要整体概览或确认接口时使用。"
---

# 秒杀项目概览

## 项目定位

秒杀（seckill）学习项目，目的是通过亲手实现高并发秒杀系统，巩固面试三大高频知识点：**高并发、Redis、微服务组件**。

当前阶段：**单体最简版本**，只求把秒杀业务流程跑通，不做任何优化。

- 后端：Spring Boot 3 + MyBatis-Plus + Redis（单体，单模块）
- 前端：纯静态 HTML + 原生 JS（无框架，作者不熟悉前端）
- 完整规划见根目录 [`monomer_seckill_plan.md`](../monomer_seckill_plan.md)（P1 ~ P9）

## 目录结构

```
monomer_seckill/
├── monomer_seckill_plan.md          # 项目计划（P1~P9）
├── docker-compose.yml               # MySQL + Redis 一键启动
├── AGENTS.md                        # 入口索引（指向本目录下的 Skills）
├── .dsh/skills/                     # 本项目 Agent Skills（按需加载）
├── docs/                            # P2-wrk-压测教程.md、P2-check-data.sql
├── monomer_seckill_backend/
│   └── monomer_seckill_backend/     # 后端真正的项目根（含 pom.xml，两层目录）
│       └── src/main/java/org/example/monomer_seckill_backend/
│           ├── common/Result.java              # 统一响应体
│           ├── config/RedisConfig.java         # RedisTemplate(JSON 序列化)
│           ├── config/WebConfig.java           # 全局 CORS
│           ├── entity/Goods.java               # 秒杀商品
│           ├── entity/SeckillOrder.java        # 秒杀订单
│           ├── mapper/GoodsMapper.java         # 含 deductStock 原子扣库存 SQL
│           ├── mapper/SeckillOrderMapper.java
│           ├── service/GoodsService.java
│           ├── service/SeckillService.java     # 秒杀核心逻辑
│           └── controller/                     # Goods/Seckill/Redis 控制器
└── monomer_seckill_fromend/
    └── index.html                   # 秒杀前端页面
```

注意：后端目录是**两层嵌套** `monomer_seckill_backend/monomer_seckill_backend/`，`pom.xml` 在第二层，运行 Maven 命令时以第二层为工作目录。

## 技术栈与版本

| 技术 | 版本 / 说明 |
|------|-------------|
| JDK | **21**（本机路径 `E:\app\config\java21`，系统默认 `java -version` 是 1.8，需手动切 JAVA_HOME） |
| Spring Boot | 3.3.5 |
| MyBatis-Plus | 3.5.7（`mybatis-plus-spring-boot3-starter`） |
| Redis 客户端 | Spring Data Redis（Lettuce） |
| 数据库 | MySQL 8.0（端口 **3307**，非默认 3306） |
| Maven | 3.9.10（本机路径 `E:\app\config\Maven\Maven\apache-maven-3.9.10`，默认 PATH 里是 3.6.3） |
| Node.js | v25.2.1 |
| 前端 | 纯 HTML + 原生 JS（无构建步骤） |

## API 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/goods` | 商品列表 |
| GET | `/api/goods/{id}` | 商品详情 |
| POST | `/api/seckill/{goodsId}?userId=xxx` | 秒杀下单 |
| GET | `/api/redis/ping` | Redis 连通性测试 |

统一响应体 `Result<T>`：`{ code: 200|500, msg: string, data: T }`，`code=200` 表示成功。

## 路线图与当前进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| P1 | 环境基石（WSL+Docker+中间件） | ✅ 已完成（用户自建） |
| P2 | 单体秒杀：Redis 预扣库存 + Lua | ⏳ 当前：最简 DB 版已跑通，Redis 优化待做 |
| P3~P9 | 微服务拆分 / 网关 / Sentinel / 缓存 / 配置中心 / MQ / 分布式事务 | 🔜 未开始 |

详见根目录 `monomer_seckill_plan.md`。
