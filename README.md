# 单体秒杀项目（monomer_seckill）

一个用于面试准备的学习项目，目标是亲手实现秒杀系统，巩固 **高并发、Redis、微服务组件** 三大知识点。

当前为**单体最简版本**：Spring Boot 3 + MyBatis-Plus + Redis，已跑通「查商品 → 抢购 → 扣库存 → 建订单」核心链路，未做任何优化。

## 技术栈

- 后端：JDK 21、Spring Boot 3.3.5、MyBatis-Plus 3.5.7、Spring Data Redis
- 数据库：MySQL 8.0（`localhost:3307`，root / 123456hy）
- 缓存：Redis 7（`localhost:6379`，无密码）
- 前端：纯 HTML + 原生 JS（无框架）

## 快速开始

### 1. 启动中间件（WSL2 中）

```bash
docker compose up -d
```

### 2. 启动后端

```powershell
# 切换 JDK21 + Maven 3.9.10（系统默认是 Java8 + Maven3.6.3）
$env:JAVA_HOME = 'E:\app\config\java21'
$env:Path = 'E:\app\config\java21\bin;E:\app\config\Maven\Maven\apache-maven-3.9.10\bin;' + $env:Path

cd monomer_seckill_backend\monomer_seckill_backend
mvn spring-boot:run
```

启动后监听 `http://localhost:8080`，会自动建库、建表、写入 3 条商品数据。

### 3. 打开前端

```bash
cd monomer_seckill_fromend
npx serve . -l 3000
```

浏览器打开 `http://localhost:3000`，输入用户ID，点击「立即抢购」。

### 4. 接口速览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/goods` | 商品列表 |
| POST | `/api/seckill/{goodsId}?userId=xxx` | 秒杀下单 |
| GET | `/api/redis/ping` | Redis 连通性测试 |

## 更多说明

- 完整规划见 [`monomer_seckill_plan.md`](./monomer_seckill_plan.md)（P1~P9 路线图）。
- AI Agent / 开发约定见 [`AGENTS.md`](./AGENTS.md)。
