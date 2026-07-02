# 安全修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 修复 CRM 系统中已识别的安全漏洞，包括权限配置错误、IDOR 漏洞、验证码绕过、Redis 反序列化风险等关键安全问题。

**架构:** 本计划按优先级分为两个阶段：
1. **快速修复** - 10个低风险的简单修复，可快速完成且影响范围清晰
2. **深度修复** - 需要更复杂变更的关键安全问题

**技术栈:** Spring Boot 3.2.5, Spring Security, Redis, MyBatis-Plus, JWT

## 全局约束

- Java 版本: 21
- Spring Boot 版本: 3.2.5
- 保持现有代码风格和项目结构
- 所有修改需要兼容现有的 JWT 认证机制
- 不得破坏现有功能（除非明确为安全修复需要）

---

# 第一阶段：快速修复

### Task 1: 修复 SecurityConfig 权限字符串不匹配

**问题:** `hasAuthority("ADMIN")` 永远不会匹配 JWT 中派生的 `"ROLE_ADMIN"` 权限，导致 `/api/admin/**` 路径完全无法访问。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/config/SecurityConfig.java:47`

**代码变更:**

将第47行从:
```java
.requestMatchers("/api/admin/**").hasAuthority(RoleConstants.ADMIN)
```
改为:
```java
.requestMatchers("/api/admin/**").hasRole(RoleConstants.ADMIN)
```

**说明:** `hasRole()` 会自动添加 `ROLE_` 前缀，而 `hasAuthority()` 不会。当前 JWT 中的权限包含 `ROLE_` 前缀，所以必须使用 `hasRole()` 或将常量改为 `ROLE_ADMIN`。

- [ ] **步骤 1: 修改 SecurityConfig.java**

使用 Edit 工具将第47行的 `hasAuthority(RoleConstants.ADMIN)` 改为 `hasRole(RoleConstants.ADMIN)`

- [ ] **步骤 2: 验证修改**

确认修改后的代码如下:
```java
.requestMatchers("/api/admin/**").hasRole(RoleConstants.ADMIN)
```

- [ ] **步骤 3: 提交**

```bash
git add backend/src/main/java/com/cy/crm/config/SecurityConfig.java
git commit -m "fix(security): 修复 admin 权限配置，使用 hasRole 替代 hasAuthority"
```

---

### Task 2: 添加 @ToString.Exclude 到敏感字段

**问题:** LoginRequest, TokenResponse, User 等类使用 `@Data` 注解，自动生成的 `toString()` 会包含密码、令牌等敏感信息，可能导致日志泄露。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/module/auth/dto/LoginRequest.java`
- 修改: `backend/src/main/java/com/cy/crm/module/auth/dto/TokenResponse.java`
- 修改: `backend/src/main/java/com/cy/crm/module/admin/entity/User.java`

**代码变更:**

- [ ] **步骤 1: 修改 LoginRequest.java**

在 `LoginRequest.java` 中添加导入和注解:

在 `package` 声明后添加:
```java
import lombok.ToString;
```

然后修改字段注解:
```java
@NotBlank(message = "用户名不能为空")
@Schema(description = "用户名")
@ToStringExclude  // 添加此行
private String username;

@NotBlank(message = "密码不能为空")
@Schema(description = "密码")
@ToStringExclude  // 添加此行
private String password;

@Schema(description = "验证码唯一标识")
@ToStringExclude  // 添加此行
private String captchaUuid;

@Schema(description = "验证码")
@ToStringExclude  // 添加此行
private String captchaCode;
```

- [ ] **步骤 2: 修改 TokenResponse.java**

在 `TokenResponse.java` 中:
```java
package com.cy.crm.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;  // 添加此行

@Data
@Schema(description = "令牌响应")
@ToString(exclude = {"accessToken", "refreshToken"})  // 添加此行
public class TokenResponse {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型")
    private String tokenType = "Bearer";
}
```

- [ ] **步骤 3: 修改 User.java**

首先检查 User.java 文件中的 passwordHash 字段，添加:
```java
import lombok.ToString;

// 在 passwordHash 字段上添加
@ToStringExclude
private String passwordHash;
```

- [ ] **步骤 4: 提交**

```bash
git add backend/src/main/java/com/cy/crm/module/auth/dto/LoginRequest.java
git add backend/src/main/java/com/cy/crm/module/auth/dto/TokenResponse.java
git add backend/src/main/java/com/cy/crm/module/admin/entity/User.java
git commit -m "fix(security): 添加 @ToString.Exclude 防止敏感信息泄露到日志"
```

---

### Task 3: 使验证码字段必填

**问题:** LoginRequest 中的 `captchaUuid` 和 `captchaCode` 没有 `@NotBlank` 注解，AuthService.login 中的验证码验证可以简单通过省略这些字段来绕过。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/module/auth/dto/LoginRequest.java:18-22`

**代码变更:**

- [ ] **步骤 1: 修改 LoginRequest.java**

将验证码字段添加验证注解:
```java
@NotBlank(message = "验证码唯一标识不能为空")
@Schema(description = "验证码唯一标识")
private String captchaUuid;

@NotBlank(message = "验证码不能为空")
@Schema(description = "验证码")
private String captchaCode;
```

- [ ] **步骤 2: 同时移除 AuthService.login 中的可选检查**

在 `AuthService.java` 第48-52行，将条件检查改为直接验证:
```java
// 删除 if (request.getCaptchaUuid() != null && request.getCaptchaCode() != null) 条件
// 直接执行验证
if (!captchaService.validateCaptcha(request.getCaptchaUuid(), request.getCaptchaCode())) {
    throw BusinessException.paramError("验证码错误");
}
```

- [ ] **步骤 3: 提交**

```bash
git add backend/src/main/java/com/cy/crm/module/auth/dto/LoginRequest.java
git add backend/src/main/java/com/cy/crm/module/auth/service/AuthService.java
git commit -m "fix(security): 使验证码字段必填，防止验证码绕过"
```

---

### Task 4: 添加分页大小最大限制

**问题:** PageQuery.size 没有最大值限制，可能导致 DoS 攻击。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/common/query/PageQuery.java:12`

**代码变更:**

- [ ] **步骤 1: 修改 PageQuery.java**

添加 `@Max` 注解:
```java
package com.cy.crm.common.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;  // 添加导入
import lombok.Data;

@Data
@Schema(description = "分页查询参数")
public class PageQuery {
    @Schema(description = "页码", example = "1")
    private Long current = 1L;

    @Max(100)  // 添加此行
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;
}
```

- [ ] **步骤 2: 确保控制器使用 @Valid**

检查以下控制器是否在 `@RequestBody` 和 `@RequestParam` 参数上添加了 `@Valid` 或 `@Validated`:
- `UnitController`
- `UserController`
- `AuditLogController`
- `NotificationController`
- `TaskController`

如果缺少，需要添加 `@Valid` 注解。

- [ ] **步骤 3: 提交**

```bash
git add backend/src/main/java/com/cy/crm/common/query/PageQuery.java
git commit -m "fix(security): 添加分页大小最大限制防止 DoS 攻击"
```

---

### Task 5: 移除生产环境的 H2 数据库依赖

**问题:** H2 数据库配置为 `runtimeOnly` 会在所有环境（包括生产环境）可用，且 SecurityConfig 允许无认证访问 H2 console。

**文件:**
- 修改: `backend/build.gradle:36`

**代码变更:**

- [ ] **步骤 1: 修改 build.gradle**

将第36行从:
```gradle
runtimeOnly 'com.h2database:h2'
```
改为:
```gradle
testRuntimeOnly 'com.h2database:h2'
```

- [ ] **步骤 2: 提交**

```bash
git add backend/build.gradle
git commit -m "fix(security): 将 H2 数据库限制为仅测试环境使用"
```

---

### Task 6: 移除 H2 console 的 permitAll 配置

**问题:** SecurityConfig 中 H2 console 路径设置为 `permitAll()`，即使数据库已从生产移除，这也是不必要的风险。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/config/SecurityConfig.java:44-45`

**代码变更:**

- [ ] **步骤 1: 修改 SecurityConfig.java**

从 `requestMatchers` 列表中移除 `/h2-console/**`:
```java
.requestMatchers("/api/auth/**", "/doc.html", "/webjars/**", "/favicon.ico",
               "/v3/api-docs/**", "/swagger-ui/**").permitAll()
```

- [ ] **步骤 2: 提交**

```bash
git add backend/src/main/java/com/cy/crm/config/SecurityConfig.java
git commit -m "fix(security): 移除 H2 console 的公开访问配置"
```

---

### Task 7: 添加缺失的 @Valid 注解

**问题:** 多个控制器的 `@RequestBody` 参数缺少 `@Valid` 注解，导致 Bean Validation 约束不会生效。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/module/auth/controller/AuthController.java`
- 修改: `backend/src/main/java/com/cy/crm/module/rebate/controller/RebateController.java`
- 修改: `backend/src/main/java/com/cy/crm/module/project/controller/ProjectController.java`

**代码变更:**

- [ ] **步骤 1: 检查并修复 AuthController**

在 `refreshToken` 方法参数上添加 `@Valid`:
```java
public TokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request)
```

- [ ] **步骤 2: 检查并修复 RebateController**

在 `createRebate` 和 `updateRebate` 方法参数上添加 `@Valid`

- [ ] **步骤 3: 检查并修复 ProjectController**

在 `saveBiddingNode`, `saveContractNode`, `updateMilestone`, `addPaymentNode` 方法参数上添加 `@Valid`

- [ ] **步骤 4: 提交**

```bash
git add backend/src/main/java/com/cy/crm/module/auth/controller/AuthController.java
git add backend/src/main/java/com/cy/crm/module/rebate/controller/RebateController.java
git add backend/src/main/java/com/cy/crm/module/project/controller/ProjectController.java
git commit -m "fix(security): 添加缺失的 @Valid 注解启用 Bean Validation"
```

---

### Task 8: 移除冗余的 MapStruct INSTANCE 字段

**问题:** 9 个转换器类声明了从未使用的 `INSTANCE = Mappers.getMapper(...)` 字段（Spring 注入是实际的访问路径）。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/module/admin/converter/DictionaryConverter.java`
- 修改: `backend/src/main/java/com/cy/crm/module/admin/converter/UserConverter.java`
- 修改: `backend/src/main/java/com/cy/crm/module/admin/converter/UnitConverter.java`
- 修改: `backend/src/main/java/com/cy/crm/module/admin/converter/RoleConverter.java`
- 修改: `backend/src/main/java/com/cy/crm/module/contract/converter/ContractConverter.java`
- 修改: `backend/src/main/java/com/cy/crm/module/customer/converter/CustomerConverter.java`
- 修改: `backend/src/main/java/com/cy/crm/module/followup/converter/FollowUpConverter.java`
- 修改: `backend/src/main/java/com/cy/crm/module/rebate/converter/RebateConverter.java`
- 修改: `backend/src/main/java/com/cy/crm/module/task/converter/TaskConverter.java`

**代码变更:**

- [ ] **步骤 1: 删除 DictionaryConverter.java 中的 INSTANCE 字段**

删除以下行:
```java
public static final DictionaryConverter INSTANCE = Mappers.getMapper(DictionaryConverter.class);
```

- [ ] **步骤 2: 对其他 8 个转换器重复步骤 1**

- [ ] **步骤 3: 提交**

```bash
git add backend/src/main/java/com/cy/crm/module/admin/converter/
git add backend/src/main/java/com/cy/crm/module/contract/converter/
git add backend/src/main/java/com/cy/crm/module/customer/converter/
git add backend/src/main/java/com/cy/crm/module/followup/converter/
git add backend/src/main/java/com/cy/crm/module/rebate/converter/
git add backend/src/main/java/com/cy/crm/module/task/converter/
git commit -m "refactor: 移除冗余的 MapStruct INSTANCE 字段"
```

---

### Task 9: 修复 JwtUtil 中的令牌类型验证

**问题:** `validateToken` 方法不检查令牌的 `type` claim，导致 refresh token 可以被用作 access token。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/security/JwtUtil.java:148-159`

**代码变更:**

- [ ] **步骤 1: 添加令牌类型检查方法**

在 `JwtUtil.java` 中添加新方法:
```java
/**
 * 验证访问令牌
 */
public boolean validateAccessToken(String token) {
    try {
        Claims claims = parseClaims(token);
        // 检查签发者
        if (!issuer.equals(claims.getIssuer())) {
            return false;
        }
        // 访问令牌不应有 type claim 或 type 不为 "refresh"
        String type = claims.get("type", String.class);
        if ("refresh".equals(type)) {
            return false;
        }
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;
    }
}
```

- [ ] **步骤 2: 在 JwtAuthenticationFilter 中使用新方法**

检查 `JwtAuthenticationFilter.java` 并将 `validateToken` 改为 `validateAccessToken`

- [ ] **步骤 3: 提交**

```bash
git add backend/src/main/java/com/cy/crm/security/JwtUtil.java
git add backend/src/main/java/com/cy/crm/security/JwtAuthenticationFilter.java
git commit -m "fix(security): 添加令牌类型检查防止 refresh token 被用作 access token"
```

---

### Task 10: 修复 AuthService 中的 getClientIp() 实现

**问题:** `getClientIp()` 返回硬编码的 "unknown"，导致 IP 地址的暴力保护和审计日志功能失效。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/module/auth/service/AuthService.java:138-140`

**代码变更:**

- [ ] **步骤 1: 实现 getClientIp() 方法**

替换硬编码实现:
```java
/**
 * 获取客户端IP地址
 */
private String getClientIp() {
    HttpServletRequest request = 
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getRemoteAddr();
    }
    
    // 处理多个 IP 的情况（X-Forwarded-For 可能包含多个 IP）
    if (ip != null && ip.contains(",")) {
        ip = ip.split(",")[0].trim();
    }
    
    return ip;
}
```

同时添加必要的导入:
```java
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
```

- [ ] **步骤 2: 提交**

```bash
git add backend/src/main/java/com/cy/crm/module/auth/service/AuthService.java
git commit -m "fix(security): 实现 getClientIp() 方法恢复 IP 地址审计功能"
```

---

# 第二阶段：深度修复

### Task 11: 修复 Redis 反序列化安全风险

**问题:** RedisConfig 使用 `LaissezFaireSubTypeValidator` 和 `activateDefaultTyping(NON_FINAL)`，允许任意类反序列化，可能导致 RCE。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/config/RedisConfig.java:36-44`

**代码变更:**

- [ ] **步骤 1: 修改 RedisConfig.java**

替换不安全的 ObjectMapper 配置:
```java
ObjectMapper mapper = new ObjectMapper();
mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

// 使用白名单类型验证器替代 LaissezFaireSubTypeValidator
PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Object.class)
        .allowIfSubType("java.")  // 允许标准库类型
        .allowIfSubType("java.util.")
        .allowIfSubType("java.lang.")
        .allowIfSubType("com.cy.crm.")  // 仅允许项目自己的类
        .build();

mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
serializer.setObjectMapper(mapper);
```

添加必要的导入:
```java
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
```

- [ ] **步骤 2: 提交**

```bash
git add backend/src/main/java/com/cy/crm/config/RedisConfig.java
git commit -m "fix(security): 使用白名单类型验证器修复 Redis 反序列化风险"
```

---

### Task 12: 修复 DataScopeInterceptor 中的静默绕过问题

**问题:** DataScopeInterceptor 在多种情况下会静默绕过数据权限过滤：UNION/CTE 查询、无认证用户、表名匹配误判。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/config/DataScopeInterceptor.java`

**代码变更:**

- [ ] **步骤 1: 读取并分析 DataScopeInterceptor**

首先检查 `DataScopeInterceptor.java` 的完整内容

- [ ] **步骤 2: 修复表名匹配逻辑**

将 `String.contains` 改为精确匹配或使用正则表达式
```java
// 修改前
if (tableName.contains("t_customer")) { ... }

// 修改后
if (tableName.equals("t_customer") || tableName.startsWith("t_customer ")) { ... }
```

- [ ] **步骤 3: 添加无认证用户时的处理**

在 `getCurrentDataScope()` 返回 null 时，抛出异常或拒绝查询
```java
DataScope dataScope = getCurrentDataScope();
if (dataScope == null) {
    throw new BusinessException(403, "未授权的查询操作：缺少数据权限范围");
}
```

- [ ] **步骤 4: 处理 UNION/CTE 查询**

识别 UNION/CTE 查询并应用适当的过滤逻辑

- [ ] **步骤 5: 提交**

```bash
git add backend/src/main/java/com/cy/crm/config/DataScopeInterceptor.java
git commit -m "fix(security): 修复 DataScopeInterceptor 静默绕过问题"
```

---

### Task 13: 添加 IDOR 保护机制

**问题:** 多个控制器的资源级端点（get/update/delete）仅接受路径 `id` 参数，不验证所有权或数据权限。

**影响范围:**
- `CustomerController`
- `OpportunityController`
- `ProjectController`
- `TaskController`
- `FollowUpController`
- `RebateController`
- `ContractController`

**解决方案:**

- [ ] **步骤 1: 创建 DataScopeValidator 工具类**

创建 `backend/src/main/java/com/cy/crm/security/DataScopeValidator.java`:
```java
package com.cy.crm.security;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.DataPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataScopeValidator {

    private final DataPermissionMapper dataPermissionMapper;

    /**
     * 验证用户是否有访问指定单位数据的权限
     * @param userId 用户ID
     * @param unitId 单位ID
     * @param requireOwn 是否要求必须是自己单位的数据
     */
    public void validateUnitAccess(Long userId, Long unitId, boolean requireOwn) {
        DataScope scope = getUserDataScope(userId);

        if (scope.isAll()) {
            return; // 全部权限
        }

        if (requireOwn && scope.isSelfOnly()) {
            // 仅允许访问自己的数据
            if (!isUserOwnUnit(userId, unitId)) {
                throw new BusinessException(403, "无权访问此数据");
            }
        }

        if (scope.getUnitIds() != null && !scope.getUnitIds().isEmpty()) {
            if (!scope.getUnitIds().contains(unitId)) {
                throw new BusinessException(403, "无权访问此单位数据");
            }
        }

        // 默认拒绝
        throw new BusinessException(403, "无权访问此数据");
    }

    private DataScope getUserDataScope(Long userId) {
        List<DataPermission> permissions = dataPermissionMapper.selectList(
            new QueryWrapper<DataPermission>().eq("user_id", userId)
        );
        // ... 构建并返回 DataScope
    }

    private boolean isUserOwnUnit(Long userId, Long unitId) {
        // 检查用户是否属于指定单位
    }
}
```

- [ ] **步骤 2: 在 CustomerService 中应用验证**

修改 `CustomerService.getCustomerById` 等方法，添加权限验证

- [ ] **步骤 3: 在其他 Service 中应用验证**

对 OpportunityService, ProjectService, TaskService, FollowUpService, RebateService, ContractService 重复步骤 2

- [ ] **步骤 4: 提交**

```bash
git add backend/src/main/java/com/cy/crm/security/DataScopeValidator.java
git add backend/src/main/java/com/cy/crm/module/customer/service/CustomerService.java
# ... 添加其他修改的 service 文件
git commit -m "fix(security): 添加 IDOR 保护机制验证资源访问权限"
```

---

### Task 14: 修复 AuditLogAspect 中的敏感参数记录问题

**问题:** AuditLogAspect 将所有方法参数序列化到数据库，可能记录密码、令牌等敏感信息，且同步插入增加延迟。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/config/AuditLogAspect.java`

**代码变更:**

- [ ] **步骤 1: 读取 AuditLogAspect.java**

检查完整实现

- [ ] **步骤 2: 添加敏感字段过滤**

创建参数过滤方法:
```java
private String sanitizeParams(Object[] args) {
    // 创建参数副本并过滤敏感字段
    // 过滤包含 password, token, secret 等关键词的字段
}
```

- [ ] **步骤 3: 将同步插入改为异步**

使用 `@Async` 或线程池执行审计日志写入

- [ ] **步骤 4: 提交**

```bash
git add backend/src/main/java/com/cy/crm/config/AuditLogAspect.java
git commit -m "fix(security): 过滤审计日志敏感参数并改为异步写入"
```

---

### Task 15: 修复竞态条件（添加数据库唯一约束）

**问题:** 多个 Service 执行 selectCount → insert/update 操作存在竞态条件，且数据库缺少对应唯一约束作为安全网。

**影响范围:**
- RoleService.createRole
- UnitService.create
- UserService.createUser
- ContractService.createContract
- CustomerService.create
- OpportunityService.submitOpportunity/approveOpportunity
- ProjectService.createProject/transitionProjectStatus
- BiddingNodeService.saveBiddingNode
- ContractNodeService.saveContractNode

**解决方案:**

- [ ] **步骤 1: 创建数据库迁移文件**

创建 `backend/src/main/resources/db/migration/V8__add_unique_constraints.sql`:
```sql
-- 角色编码唯一约束
ALTER TABLE t_role ADD CONSTRAINT uk_role_code UNIQUE (code);

-- 单位名称+地区唯一约束（考虑软删除）
-- 注意：需要先处理现有重复数据

-- 用户名唯一约束（已有，但需确认）
-- ALTER TABLE t_user ADD CONSTRAINT uk_username UNIQUE (username);

-- 客户单位ID+警务类型唯一约束（考虑软删除）

-- ... 其他约束
```

- [ ] **步骤 2: 在 Service 中添加唯一约束异常处理**

```java
try {
    roleMapper.insert(role);
} catch (DuplicateKeyException e) {
    throw BusinessException.paramError("角色编码已存在");
}
```

- [ ] **步骤 3: 提交**

```bash
git add backend/src/main/resources/db/migration/V8__add_unique_constraints.sql
git add backend/src/main/java/com/cy/crm/module/admin/service/RoleService.java
# ... 添加其他修改的 service 文件
git commit -m "fix(security): 添加数据库唯一约束防止竞态条件"
```

---

### Task 16: 修复异常吞没问题

**问题:** ContractService 和 ContractNodeService 在 @Transactional 方法中吞没异常，导致事务提交不正确的状态。

**文件:**
- 修改: `backend/src/main/java/com/cy/crm/module/contract/service/ContractService.java`
- 修改: `backend/src/main/java/com/cy/crm/module/contract/service/ContractNodeService.java`

**代码变更:**

- [ ] **步骤 1: 读取并分析相关代码**

找到 `triggerRebateGeneration` 和 `saveContractNode` 方法

- [ ] **步骤 2: 移除 try-catch 或正确抛出异常**

```java
// 修改前
try {
    rebateService.generateContractRebate(contractId);
} catch (Exception e) {
    log.error("生成回扣失败", e);
    // 不抛出异常，事务继续
}

// 修改后
try {
    rebateService.generateContractRebate(contractId);
} catch (Exception e) {
    log.error("生成回扣失败", e);
    throw new BusinessException("合同保存失败：回扣生成失败", e);
}
```

- [ ] **步骤 3: 提交**

```bash
git add backend/src/main/java/com/cy/crm/module/contract/service/ContractService.java
git add backend/src/main/java/com/cy/crm/module/contract/service/ContractNodeService.java
git commit -m "fix(security): 修复异常吞没问题确保事务完整性"
```

---

## 总结

本计划包含 16 个任务，分为两个阶段：
- **第一阶段（Task 1-10）**: 快速修复，预计 2-4 小时完成
- **第二阶段（Task 11-16）**: 深度修复，预计 8-16 小时完成

每个任务完成后应立即提交，以便进行代码审查和回滚（如有必要）。
