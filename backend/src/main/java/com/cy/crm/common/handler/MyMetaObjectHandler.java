package com.cy.crm.common.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 自动填充创建时间、更新时间、创建人、更新人、逻辑删除默认值
 *
 * 符合开发文档 §37 MyBatis-Plus 配置要求
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        // 创建时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        // 更新时间（与创建时间相同，避免首次更新前为空）
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // 创建人（仅当实体包含 createdBy 字段时生效）
        this.strictInsertFill(metaObject, "createdBy", Long.class, null);
        // 逻辑删除默认值
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);

        log.debug("insertFill: createdAt={}, updatedAt={}", now, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());

        log.debug("updateFill: updatedAt={}", LocalDateTime.now());
    }
}
