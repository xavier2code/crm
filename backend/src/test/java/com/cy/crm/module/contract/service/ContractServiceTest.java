package com.cy.crm.module.contract.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.contract.dto.ContractRequest;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.JwtAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 合同服务测试 - 验证唯一约束
 *
 * 注意：此测试为顺序测试，足以验证业务逻辑正确性。
 * 真正的竞态条件保护由数据库层面的 UNIQUE 约束提供
 * (参见 V8__add_unique_constraints.sql)，该约束会在并发场景下
 * 阻止重复数据的插入，抛出 DataIntegrityViolationException。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ContractServiceTest {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private OpportunityMapper opportunityMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private UnitMapper unitMapper;

    @Autowired
    private UserMapper userMapper;

    private Long currentUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("test_user_unique");
        user.setPasswordHash("password");
        user.setRealName("测试用户");
        user.setCreatedBy(1L);
        userMapper.insert(user);
        currentUserId = user.getId();

        DataScope allScope = new DataScope();
        allScope.setAll(true);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                user.getUsername(),
                Collections.emptyList(),
                currentUserId,
                Collections.emptyList(),
                Collections.emptyList(),
                allScope
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testCreateContract_duplicateProjectId_throwsException() {
        // 创建必要的测试数据
        Unit unit = new Unit();
        unit.setName("测试单位");
        unit.setRegion("华北");
        unit.setAdminLevel(1);
        unitMapper.insert(unit);

        Customer customer = new Customer();
        customer.setUnitId(unit.getId());
        customer.setPoliceType("公安");
        customer.setName("测试客户");
        customerMapper.insert(customer);

        Opportunity opportunity = new Opportunity();
        opportunity.setCustomerId(customer.getId());
        opportunity.setBusinessDomain("视频监控");
        opportunity.setProjectType(1);
        opportunity.setAmount(BigDecimal.valueOf(100000));
        opportunity.setStatus(1);
        opportunity.setSubmittedBy(currentUserId);
        opportunityMapper.insert(opportunity);

        Project project = new Project();
        project.setOpportunityId(opportunity.getId());
        project.setName("测试项目");
        project.setAmount(BigDecimal.valueOf(100000));
        project.setStatus(1);
        projectMapper.insert(project);

        ContractRequest request = new ContractRequest();
        request.setProjectId(project.getId());
        request.setAmount(BigDecimal.valueOf(100000));

        // 第一次创建应该成功
        Long contractId = contractService.createContract(request);
        assertNotNull(contractId);

        // 第二次创建应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            contractService.createContract(request);
        });

        assertEquals(6001, exception.getCode());
        assertTrue(exception.getMessage().contains("该项目已有合同"));
    }
}
