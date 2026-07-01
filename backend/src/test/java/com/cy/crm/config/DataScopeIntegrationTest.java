package com.cy.crm.config;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.DataPermission;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.DataPermissionMapper;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.contract.entity.Contract;
import com.cy.crm.module.contract.mapper.ContractMapper;
import com.cy.crm.module.contract.service.ContractService;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.customer.service.CustomerService;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.project.service.ProjectService;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.JwtAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据权限拦截器与服务层 IDOR 保护集成测试
 *
 * 测试覆盖：
 * 1. DataScopeInterceptor SQL 级过滤（列表查询）- 仅 t_customer 表完全支持
 * 2. DataScopeValidator 服务级资源访问验证（单条查询/更新/删除）- Customer, Contract, Project
 * 3. 三种数据权限范围：SELF_ONLY、UNIT、ALL
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataScopeIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UnitMapper unitMapper;

    @Autowired
    private DataPermissionMapper dataPermissionMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private OpportunityMapper opportunityMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private ProjectService projectService;

    private Long currentUserId;
    private Long otherUserId;
    private Long unitOneId;
    private Long unitTwoId;
    private Long projectForCurrent;
    private Long projectForOther;
    private Long customerForCurrent;
    private Long customerForOther;
    private Long contractForCurrent;
    private Long contractForOther;

    @BeforeEach
    void setUp() {
        currentUserId = createUser("current_bd");
        otherUserId = createUser("other_bd");
        unitOneId = createUnit("测试单位一", "湖北");
        unitTwoId = createUnit("测试单位二", "湖南");
        // 数据准备阶段使用 ALL 权限，避免 DataScopeInterceptor 过滤导致数据查询不到
        setAuthentication(currentUserId, DataScope.all());

        // 创建两个客户，分别归属当前用户和其他用户，且属于不同单位
        customerForCurrent = createCustomer(currentUserId, unitOneId);
        customerForOther = createCustomer(otherUserId, unitTwoId);

        // 创建两个商机，用于创建项目
        Long opportunityForCurrent = createOpportunity(customerForCurrent);
        Long opportunityForOther = createOpportunity(customerForOther);

        // 创建两个项目，分别归属当前用户和其他用户
        projectForCurrent = createProject(currentUserId, opportunityForCurrent);
        projectForOther = createProject(otherUserId, opportunityForOther);

        // 为每个项目创建合同
        contractForCurrent = createContract(projectForCurrent);
        contractForOther = createContract(projectForOther);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== SELF_ONLY 权限测试 ====================

    @Test
    void customerQuery_withSelfOnly_shouldReturnOnlyOwnData() {
        setAuthentication(currentUserId, selfOnlyScope());

        List<Customer> customers = customerMapper.selectList(null);
        assertEquals(1, customers.size(), "self-only 应只返回自己的客户");
        assertEquals(customerForCurrent, customers.get(0).getId());
    }

    @Test
    void customerService_getOwnCustomer_withSelfOnly_shouldSucceed() {
        setAuthentication(currentUserId, selfOnlyScope());

        var vo = customerService.getCustomerById(customerForCurrent);
        assertNotNull(vo, "self-only 用户应能访问自己的客户");
    }

    @Test
    void customerService_getOtherCustomer_withSelfOnly_shouldBeDenied() {
        setAuthentication(currentUserId, selfOnlyScope());

        var vo = customerService.getCustomerById(customerForOther);
        assertNull(vo, "self-only 用户不应能访问其他用户的客户");
    }

    @Test
    void contractService_getOwnContract_withSelfOnly_shouldSucceed() {
        setAuthentication(currentUserId, selfOnlyScope());

        var vo = contractService.getContractById(contractForCurrent);
        assertNotNull(vo, "self-only 用户应能访问自己的合同");
    }

    @Test
    void contractService_getOtherContract_withSelfOnly_shouldBeDenied() {
        setAuthentication(currentUserId, selfOnlyScope());

        var vo = contractService.getContractById(contractForOther);
        assertNull(vo, "self-only 用户不应能访问其他用户的合同");
    }

    @Test
    void projectService_getOwnProject_withSelfOnly_shouldSucceed() {
        setAuthentication(currentUserId, selfOnlyScope());

        var vo = projectService.getProjectById(projectForCurrent);
        assertNotNull(vo, "self-only 用户应能访问自己的项目");
    }

    @Test
    void projectService_getOtherProject_withSelfOnly_shouldBeDenied() {
        setAuthentication(currentUserId, selfOnlyScope());

        var vo = projectService.getProjectById(projectForOther);
        assertNull(vo, "self-only 用户不应能访问其他用户的项目");
    }

    @Test
    void customerService_deleteOtherCustomer_withSelfOnly_shouldBeDenied() {
        setAuthentication(currentUserId, selfOnlyScope());

        // 删除他人客户时，因数据已被拦截器过滤，应直接返回而不执行删除
        assertDoesNotThrow(() -> customerService.deleteCustomer(customerForOther));
        // 确认数据仍然存在
        setAuthentication(currentUserId, DataScope.all());
        assertNotNull(customerMapper.selectById(customerForOther), "他人客户不应被删除");
    }

    // ==================== UNIT 权限测试 ====================

    @Test
    void customerQuery_withUnitPermission_shouldReturnUnitData() {
        grantUnitPermission(currentUserId, unitOneId);
        setAuthentication(currentUserId, unitScope(unitOneId));

        List<Customer> customers = customerMapper.selectList(null);
        assertEquals(1, customers.size(), "单位权限应只返回该单位的客户");
        assertEquals(customerForCurrent, customers.get(0).getId());
    }

    @Test
    void customerService_getUnitCustomer_withUnitPermission_shouldSucceed() {
        grantUnitPermission(currentUserId, unitOneId);
        setAuthentication(currentUserId, unitScope(unitOneId));

        var vo = customerService.getCustomerById(customerForCurrent);
        assertNotNull(vo, "有单位权限的用户应能访问该单位客户");
    }

    @Test
    void customerService_getOtherUnitCustomer_withUnitPermission_shouldBeDenied() {
        grantUnitPermission(currentUserId, unitOneId);
        setAuthentication(currentUserId, unitScope(unitOneId));

        var vo = customerService.getCustomerById(customerForOther);
        assertNull(vo, "有单位权限的用户不应能访问其他单位客户");
    }

    @Test
    void contractService_getUnitContract_withUnitPermission_shouldSucceed() {
        grantUnitPermission(currentUserId, unitOneId);
        setAuthentication(currentUserId, unitScope(unitOneId));

        var vo = contractService.getContractById(contractForCurrent);
        assertNotNull(vo, "有单位权限的用户应能访问该单位合同");
    }

    @Test
    void contractService_getOtherUnitContract_withUnitPermission_shouldBeDenied() {
        grantUnitPermission(currentUserId, unitOneId);
        setAuthentication(currentUserId, unitScope(unitOneId));

        var vo = contractService.getContractById(contractForOther);
        assertNull(vo, "有单位权限的用户不应能访问其他单位合同");
    }

    @Test
    void projectService_getUnitProject_withUnitPermission_shouldSucceed() {
        grantUnitPermission(currentUserId, unitOneId);
        setAuthentication(currentUserId, unitScope(unitOneId));

        var vo = projectService.getProjectById(projectForCurrent);
        assertNotNull(vo, "有单位权限的用户应能访问该单位项目");
    }

    @Test
    void projectService_getOtherUnitProject_withUnitPermission_shouldBeDenied() {
        grantUnitPermission(currentUserId, unitOneId);
        setAuthentication(currentUserId, unitScope(unitOneId));

        var vo = projectService.getProjectById(projectForOther);
        assertNull(vo, "有单位权限的用户不应能访问其他单位项目");
    }

    // ==================== ALL 权限测试 ====================

    @Test
    void customerQuery_withAllPermission_shouldReturnAll() {
        setAuthentication(currentUserId, DataScope.all());

        List<Customer> customers = customerMapper.selectList(null);
        assertEquals(2, customers.size(), "全部权限应返回所有客户");
    }

    @Test
    void customerService_getAnyCustomer_withAllPermission_shouldSucceed() {
        setAuthentication(currentUserId, DataScope.all());

        var vo1 = customerService.getCustomerById(customerForCurrent);
        assertNotNull(vo1, "全部权限应能访问任意客户");

        var vo2 = customerService.getCustomerById(customerForOther);
        assertNotNull(vo2, "全部权限应能访问任意客户");
    }

    @Test
    void contractService_getAnyContract_withAllPermission_shouldSucceed() {
        setAuthentication(currentUserId, DataScope.all());

        var vo1 = contractService.getContractById(contractForCurrent);
        assertNotNull(vo1, "全部权限应能访问任意合同");

        var vo2 = contractService.getContractById(contractForOther);
        assertNotNull(vo2, "全部权限应能访问任意合同");
    }

    @Test
    void projectService_getAnyProject_withAllPermission_shouldSucceed() {
        setAuthentication(currentUserId, DataScope.all());

        var vo1 = projectService.getProjectById(projectForCurrent);
        assertNotNull(vo1, "全部权限应能访问任意项目");

        var vo2 = projectService.getProjectById(projectForOther);
        assertNotNull(vo2, "全部权限应能访问任意项目");
    }

    // ==================== 边界测试 ====================

    @Test
    void serviceMethod_withNullDataScope_shouldBeDenied() {
        setAuthentication(currentUserId, nullDataScope());

        Exception ex = assertThrows(Exception.class, () ->
                customerService.getCustomerById(customerForCurrent));
        Throwable cause = ex.getCause();
        while (cause != null && !(cause instanceof BusinessException)) {
            cause = cause.getCause();
        }
        assertNotNull(cause, "应抛出数据权限不足的业务异常");
        assertEquals(2006, ((BusinessException) cause).getCode(), "应为数据权限不足错误码");
    }

    @Test
    void otherUserSelfOnly_canAccessOwnData() {
        // 以 otherUser 身份登录，self-only 模式
        setAuthentication(otherUserId, selfOnlyScope());

        List<Customer> customers = customerMapper.selectList(null);
        assertEquals(1, customers.size(), "otherUser 的 self-only 应只返回自己的客户");
        assertEquals(customerForOther, customers.get(0).getId());

        var vo = customerService.getCustomerById(customerForOther);
        assertNotNull(vo, "otherUser 应能访问自己的客户");
    }

    @Test
    void otherUserSelfOnly_cannotAccessCurrentUserData() {
        setAuthentication(otherUserId, selfOnlyScope());

        var vo = customerService.getCustomerById(customerForCurrent);
        assertNull(vo, "otherUser 不应能访问 currentUser 的客户");
    }

    // ==================== 辅助方法 ====================

    private Long createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("$2b$10$dummy");
        user.setRealName(username);
        user.setPhone("13800000000");
        user.setEmail(username + "@crm.com");
        user.setStatus(1);
        user.setIsInitialPassword(0);
        user.setCreatedBy(1L);
        userMapper.insert(user);
        return user.getId();
    }

    private Long createUnit(String name, String region) {
        Unit unit = new Unit();
        unit.setName(name);
        unit.setRegion(region);
        unit.setAdminLevel(1);
        unit.setStatus(1);
        unitMapper.insert(unit);
        return unit.getId();
    }

    private Long createCustomer(Long ownerUserId, Long unitId) {
        Customer customer = new Customer();
        customer.setName("测试客户-" + ownerUserId);
        customer.setUnitId(unitId);
        customer.setPoliceType("CRIMINAL");
        customer.setOwnerUserId(ownerUserId);
        customer.setCreatedBy(ownerUserId);
        customer.setRegion("湖北");
        customer.setStatus(1);
        customerMapper.insert(customer);
        return customer.getId();
    }

    private Long createOpportunity(Long customerId) {
        Opportunity opportunity = new Opportunity();
        opportunity.setCustomerId(customerId);
        opportunity.setBusinessDomain("SECURITY");
        opportunity.setProjectType(1);
        opportunity.setAmount(BigDecimal.valueOf(100));
        opportunity.setSubmittedBy(currentUserId);
        opportunity.setStatus(1);
        opportunityMapper.insert(opportunity);
        return opportunity.getId();
    }

    private Long createProject(Long ownerBdId, Long opportunityId) {
        Project project = new Project();
        project.setOpportunityId(opportunityId);
        project.setName("测试项目-" + ownerBdId);
        project.setBusinessDomain("SECURITY");
        project.setOwnerBdId(ownerBdId);
        project.setStatus(1);
        project.setPNode(1);
        projectMapper.insert(project);
        return project.getId();
    }

    private Long createContract(Long projectId) {
        Contract contract = new Contract();
        contract.setProjectId(projectId);
        contract.setAmount(BigDecimal.valueOf(50000));
        contract.setStatus(1);
        contractMapper.insert(contract);
        return contract.getId();
    }

    private void grantUnitPermission(Long userId, Long unitId) {
        DataPermission dp = new DataPermission();
        dp.setUserId(userId);
        dp.setScopeType(4); // UNIT 类型
        dp.setScopeValue(String.valueOf(unitId));
        dataPermissionMapper.insert(dp);
    }

    private void setAuthentication(Long userId, DataScope dataScope) {
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                "test_user",
                Collections.emptyList(),
                userId,
                Collections.emptyList(),
                Collections.emptyList(),
                dataScope
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private DataScope selfOnlyScope() {
        DataScope scope = new DataScope();
        scope.setSelfOnly(true);
        return scope;
    }

    private DataScope unitScope(Long unitId) {
        DataScope scope = new DataScope();
        scope.setUnitIds(List.of(unitId));
        return scope;
    }

    private DataScope nullDataScope() {
        return null;
    }
}
