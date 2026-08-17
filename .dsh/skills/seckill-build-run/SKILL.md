---
name: seckill-build-run
description: "编译、打包、启动、运行后端或前端、联调验证、排查构建或启动报错时使用。含 JDK21/Maven3.9.10 切换、沙箱构建、前端启动、易踩坑清单。"
---

# 构建与运行

## 后端

```powershell
# 关键：切换 JDK 21 和 Maven 3.9.10（系统默认是 Java 8 + Maven 3.6.3）
$env:JAVA_HOME = 'E:\app\config\java21'
$env:Path = 'E:\app\config\java21\bin;E:\app\config\Maven\Maven\apache-maven-3.9.10\bin;' + $env:Path

cd E:\items\natherItems\monomer_seckill\monomer_seckill_backend\monomer_seckill_backend

# 编译打包（跳过测试）
mvn -Dmaven.test.skip=true clean package

# 或直接运行
mvn spring-boot:run
```

启动后后端监听 **`http://localhost:8080`**。

- 启动时自动建库、建表、写入 3 条种子商品数据（幂等 SQL）。
- 建表/种子 SQL 见 `src/main/resources/schema.sql` 与 `data.sql`。

### PowerShell 参数拆分坑
PowerShell 下若 `-Dmaven.test.skip=true` 被拆成两段、报 `Unknown lifecycle phase ".test.skip=true"`，改用 `-DskipTests` 或用 `cmd /c "mvn ..."` 包裹。

### AI 沙箱（DSH）构建
沙箱里全局 Maven 仓库 `E:\app\config\Maven\Maven\repository` 不可写，需改用工作区本地仓库：
```powershell
mvn -s E:\items\natherItems\monomer_seckill\.m2\settings.xml -DskipTests clean package
```
该 `settings.xml` 把 localRepository 重定向到 `.m2/repository`（已加入 .gitignore），仅沙箱使用；用户在正常终端无需 `-s`。

## 前端

前端是纯静态页面，无构建。二选一：

```bash
cd E:\items\natherItems\monomer_seckill\monomer_seckill_fromend
npx serve . -l 3000          # 浏览器打开 http://localhost:3000
# 或
python -m http.server 3000
```

后端已配置全局 CORS，前端任意端口均可访问。

## 联调验证

```bash
curl http://localhost:8080/api/goods
curl -X POST "http://localhost:8080/api/seckill/1?userId=1001"
curl http://localhost:8080/api/redis/ping
```

## 易踩坑

- **JDK/Maven 版本**：系统默认是 Java 8 + Maven 3.6.3，务必先切换（见上），否则编译失败。
- **MySQL 端口是 3307**，不是 3306；数据库名 `monomer_seckill` 靠 `createDatabaseIfNotExist=true` 自动建。
- **中文编码**：`application.yml` 里 `spring.sql.init.encoding=UTF-8` 不能删，否则 Windows GBK 环境下 `schema.sql`/`data.sql` 里的中文注释会乱码报错。
- **两层目录**：后端 `pom.xml` 在 `monomer_seckill_backend/monomer_seckill_backend/` 第二层。
- **幂等初始化**：`schema.sql` 用 `CREATE TABLE IF NOT EXISTS`，`data.sql` 用 `INSERT IGNORE`，重复启动不报错，但改表结构需手动 `DROP TABLE` 或改 SQL。
