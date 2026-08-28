---
name: seckill-environment
description: "涉及 MySQL、Redis、RabbitMQ、WSL2 Docker 容器、端口、数据库/缓存/消息队列连接配置时使用。含中间件地址账号、容器清单、端口唤醒方法、后端连接配置要点。"
---

# 环境与外部依赖

中间件在 **WSL2 中用 Docker 部署**，从 Windows 侧以 `localhost` 访问。

## 中间件地址

| 服务 | 地址 | 账号/密码 | 说明 |
|------|------|-----------|------|
| MySQL | `localhost:3307` | `root` / `123456hy` | 数据库 `monomer_seckill` 由后端启动时通过 `createDatabaseIfNotExist=true` 自动创建 |
| Redis | `localhost:6379` | 无密码 | |
| RabbitMQ | `localhost:5672` / 管理台 `15672` | `admin` / `admin123` | P8 起秒杀下单异步削峰 + 死信队列；vhost 默认 `/`，管理台 `http://localhost:15672` |
| Nacos | `localhost:8848` / 9848 | 默认未开启鉴权 | 注册中心 + 配置中心（P7 起） |

## WSL2 容器与端口唤醒

已在 WSL2 中部署为容器：`mysql`(3307→3306)、`redis`(6379)、`rabbitmq`(5672/15672)、`nacos`(8848/9848)。

- 若 `localhost:3307/6379` 突然不通，多半是 WSL2 虚拟机休眠/未启动：在任意终端执行一次 `wsl -d Ubuntu_Docker` 命令即可唤醒，Docker 会自动拉起这些容器。
- 启动中间件（若容器被删需重建）：在 WSL2 中执行 `docker compose up -d`（compose 文件在项目根 `docker-compose.yml`）。
- DSH 沙箱默认模式下 `wsl` 命令会报 `E_ACCESSDENIED`（命名管道受限），需以更高权限（danger-full-access）运行才可访问 WSL。

## 后端连接配置要点（application.yml）

`src/main/resources/application.yml` 关键项：

- 数据源 URL 含 `createDatabaseIfNotExist=true`，首次启动自动建库 `monomer_seckill`。
- `username: root`、`password: 123456hy`、端口 `3307`。
- Redis：`spring.data.redis.host=localhost`、`port=6379`、无密码。
- `spring.sql.init.mode=always` + `encoding=UTF-8`：每次启动幂等执行 `schema.sql`/`data.sql`（建表 + 种子数据），中文编码必须保留 UTF-8。
