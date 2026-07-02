# 字典管理功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 CRM 系统「系统管理」模块下的「字典维护」功能，包括前端管理页面、后端预置字典项删除保护以及后端单元测试。

**Architecture:** 前端采用左侧类型列表 + 右侧字典项表格的布局；后端在现有 `DictionaryService` 基础上增加 `is_builtin` 字段标记预置项，删除时校验保护；测试采用 Mockito 对 Service 和 Controller 分层覆盖。

**Tech Stack:** Java 21, Spring Boot 3.2.5, MyBatis-Plus 3.5.7, Flyway, H2, React 18, TypeScript, Ant Design 5, Zustand, Axios

## Global Constraints

- 所有新增字段类型必须与现有代码一致（`is_builtin` 使用 `Integer` 映射 `SMALLINT`）
- 错误码使用 3000 段（客户/单位/字典），3009 当前可用
- 不修改已执行的 Flyway 迁移脚本，新增 V11 迁移
- 前端类型定义通过 `npm run gen:api` 生成或临时手动更新
- 预置字典项可编辑但不可删除
- 编辑字典项时 `code` 字段只读

---

## File Structure

### 新增文件
- `backend/src/main/resources/db/migration/V11__add_is_builtin_to_dictionary.sql`
- `backend/src/test/java/com/cy/crm/module/admin/service/DictionaryServiceTest.java`
- `backend/src/test/java/com/cy/crm/module/admin/controller/DictionaryControllerTest.java`
- `backend/src/test/resources/sql/dictionary-test-data.sql`

### 修改文件
- `backend/src/main/java/com/cy/crm/module/admin/entity/Dictionary.java`
- `backend/src/main/java/com/cy/crm/module/admin/vo/DictionaryVO.java`
- `backend/src/main/java/com/cy/crm/module/admin/service/DictionaryService.java`
- `backend/src/main/java/com/cy/crm/common/exception/BusinessException.java`
- `frontend/src/pages/system/dictionary/index.tsx`
- `frontend/src/types/api.d.ts`

---

### Task 1: 数据库迁移 - 增加 is_builtin 字段

**Files:**
- Create: `backend/src/main/resources/db/migration/V11__add_is_builtin_to_dictionary.sql`
- Modify: `backend/src/main/resources/db/migration/V5__add_stage_dictionary.sql`（仅补充 `is_builtin` 列，不删除或变更现有语句）

**Interfaces:**
- Consumes: 现有 `t_dictionary` 表结构
- Produces: 包含 `is_builtin` 列的字典表

- [ ] **Step 1: 创建 V11 迁移脚本**

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

- [ ] **Step 2: 修改 V5 迁移脚本**

在 `backend/src/main/resources/db/migration/V5__add_stage_dictionary.sql` 的所有 `INSERT INTO t_dictionary` 语句中补充 `is_builtin` 列。

原语句示例：
```sql
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('stage', 'CUSTOMER_OPERATION', '客户运营', 1, '客户引流-售前及市场引入客户进行免费运营');
```

修改为：
```sql
INSERT INTO t_dictionary (type, code, name, sort, remark, is_builtin) VALUES
('stage', 'CUSTOMER_OPERATION', '客户运营', 1, '客户引流-售前及市场引入客户进行免费运营', 1);
```

对所有 V5 中的 `INSERT` 批量替换。

> 注意：V5 已在开发环境执行过，修改 V5 是为了新环境（如其他开发者、CI、生产重跑）数据一致。已执行的环境通过 V11 的 `UPDATE` 补偿。

- [ ] **Step 3: 验证迁移脚本语法**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/backend
./gradlew flywayMigrate -Pflyway.url=jdbc:h2:mem:test -Pflyway.user=sa
```

Expected: 迁移成功（H2 本地验证，PostgreSQL 环境由 Spring Boot 测试自动验证）

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/migration/V11__add_is_builtin_to_dictionary.sql
# 如果修改了 V5
# git add backend/src/main/resources/db/migration/V5__add_stage_dictionary.sql
git commit -m "feat(dictionary): add is_builtin column and mark builtin entries"
```

---

### Task 2: 后端 Entity 和 VO 增加 isBuiltin 字段

**Files:**
- Modify: `backend/src/main/java/com/cy/crm/module/admin/entity/Dictionary.java`
- Modify: `backend/src/main/java/com/cy/crm/module/admin/vo/DictionaryVO.java`

**Interfaces:**
- Consumes: 数据库 `t_dictionary.is_builtin` 列
- Produces: `Dictionary.getIsBuiltin()` / `DictionaryVO.getIsBuiltin()`

- [ ] **Step 1: 修改 Dictionary Entity**

```java
package com.cy.crm.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cy.crm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_dictionary")
public class Dictionary extends BaseEntity {

    private String type;
    private String code;
    private String name;
    private Integer sort;
    private String remark;
    private Integer isBuiltin;
}
```

- [ ] **Step 2: 修改 DictionaryVO**

```java
package com.cy.crm.module.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "字典视图")
public class DictionaryVO {
    @Schema(description = "字典ID")
    private Long id;

    @Schema(description = "字典类型")
    private String type;

    @Schema(description = "字典编码")
    private String code;

    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否预置")
    private Integer isBuiltin;
}
```

- [ ] **Step 3: 编译验证**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/backend
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/cy/crm/module/admin/entity/Dictionary.java
# 注意：如果文件名为 DictionaryVO.java
# git add backend/src/main/java/com/cy/crm/module/admin/vo/DictionaryVO.java
git commit -m "feat(dictionary): add isBuiltin to entity and vo"
```

---

### Task 3: 后端删除保护与错误码

**Files:**
- Modify: `backend/src/main/java/com/cy/crm/module/admin/service/DictionaryService.java`
- Modify: `backend/src/main/java/com/cy/crm/common/exception/BusinessException.java`

**Interfaces:**
- Consumes: `Dictionary.isBuiltin`
- Produces: `BusinessException.dictionaryBuiltinNotDeletable()`

- [ ] **Step 1: 新增业务异常方法**

在 `BusinessException.java` 的「客户/单位/字典错误（3xxx）」区域新增：

```java
public static BusinessException dictionaryBuiltinNotDeletable() {
    return new BusinessException(3009, "预置字典项不可删除");
}
```

> 3009 当前未被占用。3006 是 `unitAssigned`，3008 是 `contactNotFound`。

- [ ] **Step 2: 修改 DictionaryService.delete**

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

- [ ] **Step 3: 编译验证**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/backend
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/cy/crm/common/exception/BusinessException.java
# 注意：如果文件名为 DictionaryService.java
# git add backend/src/main/java/com/cy/crm/module/admin/service/DictionaryService.java
git commit -m "feat(dictionary): protect builtin entries from deletion"
```

---

### Task 4: DictionaryService 单元测试

**Files:**
- Create: `backend/src/test/java/com/cy/crm/module/admin/service/DictionaryServiceTest.java`
- Create: `backend/src/test/resources/sql/dictionary-test-data.sql`

**Interfaces:**
- Consumes: `DictionaryMapper`, `DictionaryConverter`, `UnitMapper`
- Produces: 验证后的 Service 行为

- [ ] **Step 1: 创建测试数据 SQL**

```sql
-- 预置字典项
INSERT INTO t_dictionary (id, type, code, name, sort, remark, is_builtin, is_deleted) VALUES
(1001, 'test_type', 'BUILTIN_CODE', '预置项', 1, '预置字典项', 1, 0);

-- 普通字典项
INSERT INTO t_dictionary (id, type, code, name, sort, remark, is_builtin, is_deleted) VALUES
(1002, 'test_type', 'NORMAL_CODE', '普通项', 2, '普通字典项', 0, 0);
```

> 注意：如果表结构包含 `created_at`, `updated_at`, `version`，需要同时插入。请根据实际表结构补充。

- [ ] **Step 2: 编写 DictionaryServiceTest**

```java
package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.converter.DictionaryConverter;
import com.cy.crm.module.admin.dto.DictionaryRequest;
import com.cy.crm.module.admin.entity.Dictionary;
import com.cy.crm.module.admin.mapper.DictionaryMapper;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.vo.DictionaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock
    private DictionaryMapper dictionaryMapper;

    @Mock
    private UnitMapper unitMapper;

    @Mock
    private DictionaryConverter dictionaryConverter;

    @InjectMocks
    private DictionaryService dictionaryService;

    @Test
    void listByType_shouldReturnConvertedList() {
        Dictionary dict = new Dictionary();
        dict.setId(1L);
        dict.setType("test_type");

        when(dictionaryMapper.selectByType("test_type")).thenReturn(List.of(dict));
        when(dictionaryConverter.entityToVO(dict)).thenReturn(new DictionaryVO());

        List<DictionaryVO> result = dictionaryService.listByType("test_type");

        assertEquals(1, result.size());
        verify(dictionaryMapper).selectByType("test_type");
    }

    @Test
    void create_shouldThrowWhenCodeDuplicate() {
        DictionaryRequest request = new DictionaryRequest();
        request.setType("test_type");
        request.setCode("EXISTING");

        when(dictionaryMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.create(request));
        assertEquals(3005, ex.getCode());
    }

    @Test
    void create_shouldInsertWhenCodeUnique() {
        DictionaryRequest request = new DictionaryRequest();
        request.setType("test_type");
        request.setCode("NEW");

        when(dictionaryMapper.selectCount(any())).thenReturn(0L);
        when(dictionaryConverter.requestToEntity(request)).thenReturn(new Dictionary());
        when(dictionaryMapper.insert(any(Dictionary.class))).thenReturn(1);

        assertDoesNotThrow(() -> dictionaryService.create(request));
        verify(dictionaryMapper).insert(any(Dictionary.class));
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        DictionaryRequest request = new DictionaryRequest();
        request.setId(999L);

        when(dictionaryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.update(request));
        assertEquals(1002, ex.getCode());
    }

    @Test
    void update_shouldUpdateWhenExists() {
        DictionaryRequest request = new DictionaryRequest();
        request.setId(1L);
        request.setName("新名称");

        Dictionary dict = new Dictionary();
        dict.setId(1L);

        when(dictionaryMapper.selectById(1L)).thenReturn(dict);
        doNothing().when(dictionaryConverter).updateEntityFromRequest(request, dict);
        when(dictionaryMapper.updateById(dict)).thenReturn(1);

        assertDoesNotThrow(() -> dictionaryService.update(request));
        verify(dictionaryMapper).updateById(dict);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(dictionaryMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.delete(999L));
        assertEquals(1002, ex.getCode());
    }

    @Test
    void delete_shouldThrowWhenBuiltin() {
        Dictionary dict = new Dictionary();
        dict.setId(1L);
        dict.setIsBuiltin(1);

        when(dictionaryMapper.selectById(1L)).thenReturn(dict);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> dictionaryService.delete(1L));
        assertEquals(3009, ex.getCode());
    }

    @Test
    void delete_shouldSucceedWhenNormal() {
        Dictionary dict = new Dictionary();
        dict.setId(2L);
        dict.setIsBuiltin(0);

        when(dictionaryMapper.selectById(2L)).thenReturn(dict);
        when(dictionaryMapper.deleteById(2L)).thenReturn(1);

        assertDoesNotThrow(() -> dictionaryService.delete(2L));
        verify(dictionaryMapper).deleteById(2L);
    }

    @Test
    void allByTypes_shouldReturnGroupedMap() {
        Dictionary dict = new Dictionary();
        dict.setType("test_type");

        when(dictionaryMapper.selectTypes()).thenReturn(List.of("test_type"));
        when(dictionaryMapper.selectByType("test_type")).thenReturn(List.of(dict));
        when(dictionaryConverter.entityToVO(dict)).thenReturn(new DictionaryVO());

        Map<String, List<DictionaryVO>> result = dictionaryService.allByTypes();

        assertTrue(result.containsKey("test_type"));
        assertEquals(1, result.get("test_type").size());
    }
}
```

- [ ] **Step 3: 运行测试**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/backend
./gradlew test --tests "com.cy.crm.module.admin.service.DictionaryServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests passed

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/cy/crm/module/admin/service/DictionaryServiceTest.java
# 如果创建了测试数据文件
# git add backend/src/test/resources/sql/dictionary-test-data.sql
git commit -m "test(dictionary): add DictionaryService unit tests"
```

---

### Task 5: DictionaryController 单元测试

**Files:**
- Create: `backend/src/test/java/com/cy/crm/module/admin/controller/DictionaryControllerTest.java`

**Interfaces:**
- Consumes: `DictionaryService`
- Produces: 验证后的 Controller 行为

- [ ] **Step 1: 编写 DictionaryControllerTest**

```java
package com.cy.crm.module.admin.controller;

import com.cy.crm.module.admin.dto.DictionaryRequest;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.vo.DictionaryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DictionaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DictionaryService dictionaryService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void all_shouldReturnGroupedDictionaries() throws Exception {
        DictionaryVO vo = new DictionaryVO();
        vo.setId(1L);
        vo.setType("test_type");
        vo.setCode("CODE");
        vo.setName("名称");

        when(dictionaryService.allByTypes()).thenReturn(Map.of("test_type", List.of(vo)));

        mockMvc.perform(get("/api/admin/dictionaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.test_type[0].name").value("名称"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void listByType_shouldReturnDictionaries() throws Exception {
        DictionaryVO vo = new DictionaryVO();
        vo.setId(1L);
        vo.setType("test_type");

        when(dictionaryService.listByType("test_type")).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/admin/dictionaries/test_type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].type").value("test_type"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void create_shouldReturnSuccess() throws Exception {
        doNothing().when(dictionaryService).create(any(DictionaryRequest.class));

        mockMvc.perform(post("/api/admin/dictionaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"test_type\",\"code\":\"NEW\",\"name\":\"新字典\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_shouldReturnSuccess() throws Exception {
        doNothing().when(dictionaryService).update(any(DictionaryRequest.class));

        mockMvc.perform(put("/api/admin/dictionaries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"test_type\",\"code\":\"CODE\",\"name\":\"更新\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void delete_shouldReturnSuccess() throws Exception {
        doNothing().when(dictionaryService).delete(1L);

        mockMvc.perform(delete("/api/admin/dictionaries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
```

- [ ] **Step 2: 运行测试**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/backend
./gradlew test --tests "com.cy.crm.module.admin.controller.DictionaryControllerTest"
```

Expected: BUILD SUCCESSFUL, all tests passed

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/cy/crm/module/admin/controller/DictionaryControllerTest.java
git commit -m "test(dictionary): add DictionaryController unit tests"
```

---

### Task 6: 前端字典管理页面实现

**Files:**
- Modify: `frontend/src/pages/system/dictionary/index.tsx`

**Interfaces:**
- Consumes: `getDictionariesByType`, `createDictionary`, `updateDictionary`, `deleteDictionary` from `frontend/src/api/admin/dictionary.ts`
- Produces: 完整的字典管理 UI

- [ ] **Step 1: 实现页面组件**

```tsx
import { useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Menu,
  message,
  Modal,
  Popconfirm,
  Row,
  Space,
  Table,
} from 'antd'
import type { MenuProps, TableColumnsType } from 'antd'

import {
  createDictionary,
  deleteDictionary,
  getDictionariesByType,
  updateDictionary,
} from '@/api/admin/dictionary'
import { getDictionaries } from '@/api/auth'
import type { components } from '@/types/api'

type DictionaryVO = components['schemas']['DictionaryVO']
type DictionaryRequest = components['schemas']['DictionaryRequest']

interface DictType {
  type: string
  name: string
}

export default function DictionaryPage() {
  const [types, setTypes] = useState<DictType[]>([])
  const [selectedType, setSelectedType] = useState<string>('')
  const [items, setItems] = useState<DictionaryVO[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<DictionaryVO | null>(null)
  const [form] = Form.useForm()

  // 加载所有字典类型
  useEffect(() => {
    getDictionaries().then((data) => {
      const typeList = Object.keys(data).map((type) => ({
        type,
        // 用该类型第一个字典项的类型作为展示名，或直接使用 type
        name: type,
      }))
      setTypes(typeList)
      if (typeList.length > 0 && !selectedType) {
        setSelectedType(typeList[0].type)
      }
    })
  }, [])

  // 加载选中类型的字典项
  useEffect(() => {
    if (!selectedType) return
    setLoading(true)
    getDictionariesByType(selectedType)
      .then((data) => setItems(data))
      .finally(() => setLoading(false))
  }, [selectedType])

  const menuItems: MenuProps['items'] = useMemo(
    () =
      types.map((t) => ({
        key: t.type,
        label: t.name,
      })),
    [types]
  )

  const handleAdd = () => {
    setEditingItem(null)
    form.resetFields()
    form.setFieldsValue({ type: selectedType })
    setModalOpen(true)
  }

  const handleEdit = (record: DictionaryVO) => {
    setEditingItem(record)
    form.setFieldsValue({
      type: record.type,
      code: record.code,
      name: record.name,
      sort: record.sort,
      remark: record.remark,
    })
    setModalOpen(true)
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteDictionary(id)
      message.success('删除成功')
      refreshItems()
    } catch {
      // request client 已处理错误提示
    }
  }

  const refreshItems = () => {
    if (!selectedType) return
    setLoading(true)
    getDictionariesByType(selectedType)
      .then((data) => setItems(data))
      .finally(() => setLoading(false))
  }

  const handleModalOk = async () => {
    const values = await form.validateFields()
    try {
      if (editingItem) {
        await updateDictionary(editingItem.id as number, values as DictionaryRequest)
        message.success('更新成功')
      } else {
        await createDictionary(values as DictionaryRequest)
        message.success('创建成功')
      }
      setModalOpen(false)
      refreshItems()
    } catch {
      // request client 已处理错误提示
    }
  }

  const columns: TableColumnsType<DictionaryVO> = [
    { title: '编码', dataIndex: 'code' },
    { title: '名称', dataIndex: 'name' },
    { title: '排序', dataIndex: 'sort', width: 80 },
    { title: '备注', dataIndex: 'remark', ellipsis: true },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除？"
            disabled={record.isBuiltin === 1}
            onConfirm={() => handleDelete(record.id as number)}
          >
            <Button type="link" danger size="small" disabled={record.isBuiltin === 1}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Row gutter={16}>
      <Col span={6}>
        <Card title="字典类型">
          <Menu
            mode="inline"
            selectedKeys={[selectedType]}
            items={menuItems}
            onClick={({ key }) => setSelectedType(key)}
          />
        </Card>
      </Col>
      <Col span={18}>
        <Card
          title={`${selectedType || ''} 字典项`}
          extra={
            <Button type="primary" onClick={handleAdd}>
              新增字典项
            </Button>
          }
        >
          <Table
            rowKey="id"
            columns={columns}
            dataSource={items}
            loading={loading}
            pagination={false}
          />
        </Card>
      </Col>

      <Modal
        title={editingItem ? '编辑字典项' : '新增字典项'}
        open={modalOpen}
        onOk={handleModalOk}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="type" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            label="编码"
            name="code"
            rules={[{ required: true, message: '请输入编码' }]}
          >
            <Input disabled={!!editingItem} />
          </Form.Item>
          <Form.Item
            label="名称"
            name="name"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item label="排序" name="sort" initialValue={0}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="备注" name="remark">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Row>
  )
}
```

> 注意：示例中使用了 `getDictionaries()` from `@/api/auth`。请确认该函数是否存在；若不存在，应改为直接调用 `request({ url: '/admin/dictionaries', method: 'GET' })` 或新增 `getAllDictionaries()` 到 `frontend/src/api/admin/dictionary.ts`。

- [ ] **Step 2: 类型检查**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run type-check
```

Expected: 无类型错误

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/system/dictionary/index.tsx
git commit -m "feat(dictionary): implement dictionary management page"
```

---

### Task 7: 前端 API 类型更新

**Files:**
- Modify: `frontend/src/types/api.d.ts`（手动或自动生成）
- Modify: `frontend/src/api/admin/dictionary.ts`（如需新增 `getAllDictionaries`）

**Interfaces:**
- Consumes: 后端 OpenAPI 文档
- Produces: 包含 `isBuiltin` 的 `DictionaryVO` 类型

- [ ] **Step 1: 启动后端服务并重新生成 API 类型**

启动后端：
```bash
cd /Users/xavier/Projects/Github/crm/backend
./gradlew bootRun
```

另一个终端生成类型：
```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run gen:api
```

Expected: `frontend/src/types/api.d.ts` 中 `DictionaryVO` 包含 `isBuiltin` 字段

- [ ] **Step 2: 如果无法启动后端，手动更新 api.d.ts**

在 `frontend/src/types/api.d.ts` 中找到 `DictionaryVO` 定义，增加：

```ts
isBuiltin?: number;
```

- [ ] **Step 3: 确认 admin/dictionary.ts 是否需要新增获取所有类型的函数**

当前 `frontend/src/api/admin/dictionary.ts` 没有获取所有字典类型的函数。如果 Task 6 中使用了 `getDictionaries()` from `@/api/auth`，请确认该函数返回的是 `Record<string, DictionaryItem[]>` 类型。

若不存在，请在 `frontend/src/api/admin/dictionary.ts` 中新增：

```ts
export function getAllDictionaries() {
  return request<Record<string, components['schemas']['DictionaryVO'][]>>({
    url: '/admin/dictionaries',
    method: 'GET',
  })
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/api.d.ts
# 如果修改了 dictionary.ts
# git add frontend/src/api/admin/dictionary.ts
git commit -m "feat(dictionary): update frontend api types and add all-dictionaries helper"
```

---

### Task 8: 集成验证

**Files:**
- 无新增/修改文件

**Interfaces:**
- Consumes: 所有前述任务产出
- Produces: 可工作的字典管理功能

- [ ] **Step 1: 运行后端全部测试**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/backend
./gradlew test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 启动后端**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/backend
./gradlew bootRun
```

Expected: 服务启动成功，Flyway 迁移执行成功

- [ ] **Step 3: 启动前端**

Run:
```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run dev
```

Expected: 开发服务器启动成功

- [ ] **Step 4: 手动验证前端页面**

1. 登录系统
2. 进入「后台管理」→「字典维护」
3. 确认左侧类型列表加载正常
4. 切换类型，确认右侧表格刷新
5. 新增一个普通字典项，确认成功
6. 编辑该字典项，确认成功
7. 删除该字典项，确认成功
8. 尝试编辑预置字典项，确认成功
9. 尝试删除预置字典项，确认删除按钮禁用且后端返回错误

- [ ] **Step 5: 最终 Commit / PR**

```bash
git log --oneline -10
```

确认所有变更已提交。

---

## Self-Review

### Spec Coverage

| 设计文档要求 | 实现任务 |
|-------------|---------|
| 前端左侧类型列表 + 右侧表格 | Task 6 |
| 预置字典项删除保护 | Task 1, Task 2, Task 3 |
| 后端单元测试 | Task 4, Task 5 |
| 数据库迁移 | Task 1 |
| 编辑时 code 只读 | Task 6 |
| 前端类型更新 | Task 7 |
| 集成验证 | Task 8 |

### Placeholder Scan

- 无 "TBD", "TODO", "implement later"
- 所有代码步骤包含完整代码
- 所有命令包含预期输出

### Type Consistency

- `isBuiltin` 在后端统一使用 `Integer`
- 前端 `DictionaryVO.isBuiltin` 为 `number` 类型
- `DictionaryRequest` 不包含 `isBuiltin`（创建/编辑时不由前端传入）

### 待确认点

1. `frontend/src/api/auth.ts` 中是否存在 `getDictionaries()` 函数。若不存在，Task 6 和 Task 7 需要调整。
2. `t_dictionary` 实际列名是否包含 `created_at`, `updated_at`, `version`。测试数据 SQL 需根据实际结构补充。
3. 前端类型检查命令 `npm run type-check` 是否存在。若不存在，改为 `npx tsc --noEmit`。
