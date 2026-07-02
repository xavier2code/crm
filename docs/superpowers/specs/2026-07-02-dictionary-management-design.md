# 系统管理 - 字典管理功能设计

## 1. 概述

### 1.1 目标
完成 CRM 系统「系统管理」模块下的「字典维护」功能：
- 前端实现可交互的字典管理页面
- 后端补齐单元测试
- 对系统预置字典项进行删除保护

### 1.2 当前状态
- 后端 API 已存在：`DictionaryController`、`DictionaryService`、`DictionaryMapper`、`Entity`、`DTO`、`VO`、`Converter`
- 数据库表 `t_dictionary` 已存在
- 前端路由 `/system/dictionary`、菜单「字典维护」、API 文件 `frontend/src/api/admin/dictionary.ts`、`dict store` 均已注册
- 前端页面 `frontend/src/pages/system/dictionary/index.tsx` 当前为占位页
- 缺少后端单元测试

### 1.3 范围
- **包含**：
  - 前端字典管理页面实现
  - 后端 `DictionaryService` 和 `DictionaryController` 单元测试
  - 预置字典项删除保护（数据库 `is_builtin` 标记）
- **不包含**：
  - 字典类型管理（类型固定由系统初始化决定）
  - 前端自动化测试（项目尚未配置前端测试框架）
  - 批量导入/导出

## 2. 架构

```
用户访问 /system/dictionary
        ↓
前端 DictionaryPage
  ├─ 左侧 DictionaryTypeMenu（类型列表）
  └─ 右侧 DictionaryItemTable（字典项表格）
        ↓
调用 /api/admin/dictionaries/{type} 等 API
        ↓
后端 DictionaryController → DictionaryService → DictionaryMapper → t_dictionary
        ↓
预置项删除保护（is_builtin = 1 不可删）
```

### 2.1 数据流
1. 页面加载时请求 `GET /api/admin/dictionaries` 获取所有类型分组数据
2. 默认选中第一个类型，请求 `GET /api/admin/dictionaries/{type}` 获取该类型字典项
3. 新增/编辑/删除后刷新当前类型字典项
4. 删除预置项时后端返回业务错误，前端提示

## 3. 数据库变更

### 3.1 迁移脚本
新增 `backend/src/main/resources/db/migration/V11__add_is_builtin_to_dictionary.sql`：

```sql
-- 为字典表增加预置项标记
ALTER TABLE t_dictionary ADD COLUMN is_builtin SMALLINT NOT NULL DEFAULT 0;

-- 将系统初始化时写入的字典项标记为预置
UPDATE t_dictionary SET is_builtin = 1 WHERE type IN (
    'police_type', 'business_domain', 'project_type',
    'opportunity_status', 'project_status', 'stage_6'
);

-- V5 中写入的 stage / opportunity_stage 也标记为预置
UPDATE t_dictionary SET is_builtin = 1 WHERE type IN ('stage', 'opportunity_stage');
```

### 3.2 说明
- 新增 `is_builtin` 字段，默认值 0（非预置）
- 通过 `UPDATE` 将系统初始化时写入的字典项标记为预置
- 不修改已执行的历史迁移脚本，符合 Flyway 规范

## 4. 后端变更

### 4.1 Entity
`backend/src/main/java/com/cy/crm/module/admin/entity/Dictionary.java`：

```java
private Integer isBuiltin;
```

### 4.2 VO
`backend/src/main/java/com/cy/crm/module/admin/vo/DictionaryVO.java`：

```java
@Schema(description = "是否预置")
private Integer isBuiltin;
```

### 4.3 Service
`backend/src/main/java/com/cy/crm/module/admin/service/DictionaryService.java` 修改 `delete` 方法：

```java
public void delete(Long id) {
    Dictionary dict = dictionaryMapper.selectById(id);
    if (dict == null) {
        throw BusinessException.resourceNotFound("字典");
    }
    if (dict.getIsBuiltin() != null && dict.getIsBuiltin() == 1) {
        throw BusinessException.dictionaryBuiltinNotDeletable();
    }
    dictionaryMapper.deleteById(id);
}
```

### 4.4 异常码
`backend/src/main/java/com/cy/crm/common/exception/BusinessException.java` 新增：

```java
public static BusinessException dictionaryBuiltinNotDeletable() {
    return new BusinessException(3009, "预置字典项不可删除");
}
```

> 错误码 3006 已被 `unitAssigned` 占用，3008 已被 `contactNotFound` 占用，因此使用 3009。

### 4.5 现有 API 不变
- `GET /api/admin/dictionaries`：按类型分组返回所有字典
- `GET /api/admin/dictionaries/{type}`：按类型返回字典列表
- `POST /api/admin/dictionaries`：创建字典项
- `PUT /api/admin/dictionaries/{id}`：编辑字典项
- `DELETE /api/admin/dictionaries/{id}`：删除字典项

## 5. 前端变更

### 5.1 页面文件
`frontend/src/pages/system/dictionary/index.tsx`

### 5.2 布局
- 左侧：`Card + Menu` 展示所有字典类型
- 右侧：`Card + Table` 展示选中类型的字典项列表
- 顶部操作：新增按钮

### 5.3 表格字段
| 字段 | 说明 |
|------|------|
| code | 编码 |
| name | 名称 |
| sort | 排序 |
| remark | 备注 |
| 操作 | 编辑 / 删除 |

### 5.4 新增/编辑弹窗
使用 `Modal + Form`，字段：
- code（编码）
- name（名称）
- sort（排序，数字）
- remark（备注，可选）

编辑时 `code` 字段只读。

### 5.5 删除保护
- 表格中 `isBuiltin === 1` 的行禁用删除按钮
- 后端再次校验，防止绕过前端

### 5.6 状态管理
- 使用 React `useState` 管理选中类型、字典项列表、弹窗状态
- 调用 `frontend/src/api/admin/dictionary.ts` 中的 API 函数
- 使用 `message.success` / `message.error` 反馈操作结果

## 6. 测试计划

### 6.1 后端单元测试

#### `DictionaryServiceTest`
- `listByType`：按类型返回字典项列表
- `create`：正常创建、重复 code 抛异常
- `update`：正常更新、更新不存在字典抛异常
- `delete`：正常删除、删除预置项抛异常、删除不存在字典抛异常
- `allByTypes`：返回按类型分组数据

#### `DictionaryControllerTest`
- `GET /api/admin/dictionaries`：返回分组数据
- `GET /api/admin/dictionaries/{type}`：返回指定类型列表
- `POST /api/admin/dictionaries`：创建成功
- `PUT /api/admin/dictionaries/{id}`：更新成功
- `DELETE /api/admin/dictionaries/{id}`：删除成功、删除预置项返回错误码

### 6.2 测试数据
在 `backend/src/test/resources/sql/dictionary-test-data.sql` 中准备：
- 预置字典项：`is_builtin = 1`
- 普通字典项：`is_builtin = 0`

### 6.3 前端验证
手动验证：
- 类型列表加载
- 表格数据展示
- 新增/编辑/删除流程
- 预置项删除按钮禁用
- 删除预置项后端报错提示

## 7. 验收标准

- [ ] 访问 `/system/dictionary` 可看到左侧类型列表和右侧字典项表格
- [ ] 切换类型时右侧表格刷新
- [ ] 可新增普通字典项
- [ ] 可编辑普通字典项和预置字典项的名称/排序/备注
- [ ] 编辑时 `code` 不可修改
- [ ] 可删除普通字典项
- [ ] 预置字典项删除按钮禁用，后端删除返回业务错误
- [ ] `DictionaryServiceTest` 全部通过
- [ ] `DictionaryControllerTest` 全部通过
- [ ] Flyway 迁移脚本可正常执行

## 8. 风险与注意事项

1. **错误码冲突**：3009 需确认未被占用（当前 3008 已被 `contactNotFound` 占用）
2. **现有数据**：生产环境已有 `t_dictionary` 数据，新增字段默认值 0 不会破坏现有数据
3. **预置项识别**：V11 迁移中的 `UPDATE` 需要覆盖所有历史迁移中写入的预置字典项
4. **前端类型**：`DictionaryVO` 新增 `isBuiltin` 后，可运行 `npm run gen:api`（需后端服务启动）重新生成 `api.d.ts`，或暂时手动更新
