
# 开发规范指南
为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目环境信息

- **工作目录**：`E:\items\natherItems\monomer_seckill`
- **代码作者**：`haiy`
- **操作系统**：Windows 10
- **构建工具**：Maven
- **JDK 版本**：Java 21 (JDK 21.0.12)
- **核心框架**：Spring Boot 3.3.5

## 二、项目目录结构

```text
monomer_seckill
├── .dsh
│   └── skills                  # 技能相关配置
├── docs                        # 文档目录
├── monomer_seckill_backend
│   └── monomer_seckill_backend
│       └── src
│           ├── main
│           │   ├── java
│           │   │   └── org
│           │   │       └── example
│           │   │           └── monomer_seckill_backend
│           │   │               ├── common        # 通用组件与工具类
│           │   │               ├── config        # 配置类
│           │   │               ├── controller    # 控制层
│           │   │               ├── entity        # 实体类
│           │   │               ├── mapper        # 数据访问层
│           │   │               └── service       # 业务逻辑层
│           │   └── resources     # 资源文件
│           └── test             # 测试目录
└── monomer_seckill_fromend     # 前端项目
```

## 三、技术栈要求

- **主框架**：Spring Boot 3.3.5
- **语言版本**：Java 21
- **核心依赖**：
  - `spring-boot-starter-web`
  - `mybatis-plus-spring-boot3-starter` (版本 3.5.7)
  - `spring-boot-starter-data-redis`
  - `mysql-connector-j`
  - `lombok`

## 四、分层架构规范

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|----------------------------------------------------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 不得直接访问数据库，必须通过 Service 层调用                  |
| **Service**    | 实现业务逻辑、事务管理与数据校验   | 必须通过 Mapper 层访问数据库；返回 DTO 而非 Entity（除非必要） |
| **Mapper**     | 数据库访问与持久化操作             | 继承 `BaseMapper`；使用 MyBatis-Plus 注解进行映射             |
| **Entity**     | 映射数据库表结构                   | 不得直接返回给前端（需转换为 DTO）；包名统一为 `entity`         |
| **Common**     | 通用响应、常量、工具类             | 统一封装 Result/Response 对象，全局异常处理                   |

### 接口与实现分离

- 所有接口实现类需放在接口所在包下的 `impl` 子包中。

## 五、MyBatis-Plus 与数据库规范

### ORM 使用

- 使用 MyBatis-Plus 作为 ORM 框架，Mapper 接口继承 `BaseMapper`。
- 实体类主键策略配置为 `id-type: auto`（数据库自增）。
- 开启驼峰命名映射：`map-underscore-to-camel-case: true`。

### 数据库初始化

- 项目配置了自动初始化脚本（`schema.sql` 和 `data.sql`），开发时请保证 SQL 脚本的幂等性。
- 数据库名称：`monomer_seckill`，连接端口 `3307`（请注意非标准端口）。

## 六、安全与性能规范

### 输入校验

- 使用 `@Valid` 与 JSR-303 校验注解（如 `@NotBlank`, `@Size` 等）
  - 注意：Spring Boot 3.x 中校验注解位于 `jakarta.validation.constraints.*`

- 禁止手动拼接 SQL 字符串，防止 SQL 注入攻击。利用 MyBatis-Plus 的 Wrapper 机制进行条件构造。

### 事务管理

- `@Transactional` 注解仅用于 **Service 层**方法。
- 避免在循环中频繁提交事务，影响性能。

### 缓存使用

- 项目集成 `spring-boot-starter-data-redis`。
- 秒杀场景下，关键数据（如库存、令牌）应考虑使用 Redis 进行预热与扣减，防止数据库宕机。

## 七、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |

### 注释规范

- 所有类、方法、字段需添加 **Javadoc** 注释。
- **注释语言**：请使用中文（用户第一语言）进行注释描述，便于团队理解。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         |
|------|------------------------------|--------------|
| DTO  | 数据传输对象                 | `UserDTO`    |
| Entity/DO   | 数据库实体对象               | `UserDO`     |
| BO   | 业务逻辑封装对象             | `UserBO`     |
| VO   | 视图展示对象                 | `UserVO`     |
| Query| 查询参数封装对象             | `UserQuery`  |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`

## 八、扩展性与日志规范

### 接口优先原则

- 所有业务逻辑通过接口定义（如 `IUserService`），具体实现放在 `impl` 包中（如 `UserServiceImpl`）。

### 日志记录

- 使用 `@Slf4j` 注解代替 `System.out.println`。
- 日志级别配置：`org.example.monomer_seckill_backend: debug`。
- MyBatis-Plus 开启了 SQL 日志打印 (`StdOutImpl`)，生产环境建议关闭或调整为适当级别。

## 九、编码原则总结

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS 等      |
