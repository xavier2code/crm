package com.cy.crm.module.opportunity;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.JwtAuthenticationToken;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.opportunity.dto.OpportunityRequest;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.opportunity.service.OpportunityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商机报备保护集成测试（使用 H2 内存数据库）
 * 验证同客户 + 同业务域不能同时存在生效中/审批中的报备
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OpportunityProtectionIntegrationTest {

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private OpportunityMapper opportunityMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private UnitMapper unitMapper;

    private Long unitId;

    @BeforeEach
    void setUp() {
        // 设置 SecurityContext，使用 JwtAuthenticationToken 以通过数据权限检查
        DataScope dataScope = DataScope.all(); // 给予全部权限以避免数据权限过滤干扰测试
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                "admin",
                java.util.Collections.emptyList(),
                1L,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                dataScope
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Unit unit = new Unit();
        unit.setName("测试单位");
        unit.setRegion("湖北");
        unit.setAdminLevel(1);
        unitMapper.insert(unit);
        this.unitId = unit.getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPreventDuplicateActiveOpportunity() {
        // given: 创建一个客户
        Customer customer = new Customer();
        customer.setName("测试公安局");
        customer.setUnitId(unitId);
        customer.setPoliceType("网络安全");
        customer.setOwnerUserId(1L);
        customer.setCreatedBy(1L);
        customer.setRegion("湖北");
        customer.setStatus(1);
        customerMapper.insert(customer);

        // given: 第一个报备（草稿）并提交到审批中
        OpportunityRequest request = new OpportunityRequest();
        request.setCustomerId(customer.getId());
        request.setBusinessDomain("SECURITY");
        request.setProjectType(1);
        request.setAmount(java.math.BigDecimal.valueOf(100));

        Long firstId = opportunityService.createOpportunity(request, 1L);
        opportunityService.submitOpportunity(firstId, 1L);

        // when / then: 同客户同业务域创建第二个报备并提交，应触发报备保护
        Long secondId = opportunityService.createOpportunity(request, 1L);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.submitOpportunity(secondId, 1L));
        assertEquals(4001, ex.getCode()); // opportunityExists
    }

    @Test
    void shouldAllowOpportunityAfterFirstExpired() {
        // given: 创建客户和第一个报备
        Customer customer = new Customer();
        customer.setName("测试公安局2");
        customer.setUnitId(unitId);
        customer.setPoliceType("刑事侦查");
        customer.setOwnerUserId(1L);
        customer.setCreatedBy(1L);
        customer.setRegion("湖北");
        customer.setStatus(1);
        customerMapper.insert(customer);

        OpportunityRequest request = new OpportunityRequest();
        request.setCustomerId(customer.getId());
        request.setBusinessDomain("CRIMINAL");
        request.setProjectType(1);
        request.setAmount(java.math.BigDecimal.valueOf(50));

        Long firstId = opportunityService.createOpportunity(request, 1L);
        opportunityService.submitOpportunity(firstId, 1L);

        // 将第一个报备置为失效
        Opportunity first = opportunityMapper.selectById(firstId);
        first.setStatus(OpportunityService.STATUS_EXPIRED);
        opportunityMapper.updateById(first);

        // when: 创建第二个报备并提交
        Long secondId = opportunityService.createOpportunity(request, 1L);
        assertDoesNotThrow(() -> opportunityService.submitOpportunity(secondId, 1L));
    }
}
