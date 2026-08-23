package com.example.seckill.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器（各使用 DB 的服务共享）。
 *
 * <p>统一由应用侧写入 createTime / updateTime，避免依赖数据库 CURRENT_TIMESTAMP
 * （容器内 MySQL 为 UTC，与 JVM 本地时区 +08:00 不一致，会导致超时判断等时间比较错乱）。</p>
 *
 * @author haiy
 * @date 2026/08/17
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
