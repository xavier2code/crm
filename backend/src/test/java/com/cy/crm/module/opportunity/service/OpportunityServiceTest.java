package com.cy.crm.module.opportunity.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.opportunity.converter.OpportunityConverter;
import com.cy.crm.module.opportunity.dto.OpportunityApproveRequest;
import com.cy.crm.module.opportunity.dto.OpportunityRequest;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.entity.OpportunityApprovalLog;
import com.cy.crm.module.opportunity.mapper.OpportunityApprovalLogMapper;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 商机/报备 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock
    private OpportunityMapper opportunityMapper;
    @Mock
    private OpportunityApprovalLogMapper approvalLogMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private UserService userService;
    @Mock
    private DictionaryService dictionaryService;
    @Mock
    private OpportunityConverter opportunityConverter;

    @InjectMocks
    private OpportunityService opportunityService;

    @Test
    void createOpportunity_shouldCreateDraft() {
        // given
        OpportunityRequest request = new OpportunityRequest();
        request.setCustomerId(1L);

        Customer customer = new Customer();
        customer.setId(1L);
        when(customerMapper.selectById(1L)).thenReturn(customer);

        Opportunity entity = new Opportunity();
        entity.setId(100L);
        when(opportunityConverter.requestToEntity(request)).thenReturn(entity);
        when(opportunityMapper.insert(any(Opportunity.class))).thenReturn(1);

        // when
        Long id = opportunityService.createOpportunity(request, 100L);

        // then
        assertEquals(100L, id);
        assertEquals(OpportunityService.STATUS_DRAFT, entity.getStatus());
        assertEquals(0, entity.getSubmitCount());
        assertEquals(100L, entity.getSubmittedBy());
        verify(opportunityMapper).insert(entity);
    }

    @Test
    void createOpportunity_shouldThrowWhenCustomerNotFound() {
        // given
        OpportunityRequest request = new OpportunityRequest();
        request.setCustomerId(1L);
        when(customerMapper.selectById(1L)).thenReturn(null);

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.createOpportunity(request, 100L));
        assertEquals(3007, ex.getCode());
    }

    @Test
    void submitOpportunity_shouldSubmitDraft() {
        // given
        Long oppId = 1L;
        Opportunity opportunity = new Opportunity();
        opportunity.setId(oppId);
        opportunity.setStatus(OpportunityService.STATUS_DRAFT);
        opportunity.setSubmitCount(0);
        opportunity.setCustomerId(10L);
        opportunity.setBusinessDomain("SECURITY");

        when(opportunityMapper.selectById(oppId)).thenReturn(opportunity);
        when(opportunityMapper.selectCount(any())).thenReturn(0L);
        when(opportunityMapper.updateById(opportunity)).thenReturn(1);

        // when
        opportunityService.submitOpportunity(oppId, 100L);

        // then
        assertEquals(OpportunityService.STATUS_PENDING, opportunity.getStatus());
        assertEquals(1, opportunity.getSubmitCount());
        verify(opportunityMapper).updateById(opportunity);
        verify(approvalLogMapper).insert(any(OpportunityApprovalLog.class));
    }

    @Test
    void submitOpportunity_shouldThrowWhenInCoolingPeriod() {
        // given
        Long oppId = 1L;
        Opportunity opportunity = new Opportunity();
        opportunity.setId(oppId);
        opportunity.setStatus(OpportunityService.STATUS_FAILED);
        opportunity.setCoolingUntil(LocalDateTime.now().plusDays(15));

        when(opportunityMapper.selectById(oppId)).thenReturn(opportunity);

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.submitOpportunity(oppId, 100L));
        assertEquals(4010, ex.getCode());
    }

    @Test
    void submitOpportunity_shouldThrowWhenDuplicateActiveOpportunity() {
        // given
        Long oppId = 1L;
        Opportunity opportunity = new Opportunity();
        opportunity.setId(oppId);
        opportunity.setStatus(OpportunityService.STATUS_DRAFT);
        opportunity.setSubmitCount(0);
        opportunity.setCustomerId(10L);
        opportunity.setBusinessDomain("SECURITY");

        when(opportunityMapper.selectById(oppId)).thenReturn(opportunity);
        when(opportunityMapper.selectCount(any())).thenReturn(1L);

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.submitOpportunity(oppId, 100L));
        assertEquals(4001, ex.getCode());
    }

    @Test
    void approveOpportunity_shouldApprovePendingOpportunity() {
        // given
        Long oppId = 1L;
        Opportunity opportunity = new Opportunity();
        opportunity.setId(oppId);
        opportunity.setStatus(OpportunityService.STATUS_PENDING);

        OpportunityApproveRequest request = new OpportunityApproveRequest();
        request.setAction(2); // APPROVE

        when(opportunityMapper.selectById(oppId)).thenReturn(opportunity);

        // when
        opportunityService.approveOpportunity(oppId, request, 200L);

        // then
        assertEquals(OpportunityService.STATUS_ACTIVE, opportunity.getStatus());
        assertEquals(OpportunityService.STAGE_IN_PROGRESS, opportunity.getStage());
        assertEquals(200L, opportunity.getApprovedBy());
        assertNotNull(opportunity.getEffectiveAt());
    }

    @Test
    void approveOpportunity_shouldRejectPendingOpportunity() {
        // given
        Long oppId = 1L;
        Opportunity opportunity = new Opportunity();
        opportunity.setId(oppId);
        opportunity.setStatus(OpportunityService.STATUS_PENDING);

        OpportunityApproveRequest request = new OpportunityApproveRequest();
        request.setAction(3); // REJECT
        request.setComment("预算不足，无法推进");

        when(opportunityMapper.selectById(oppId)).thenReturn(opportunity);

        // when
        opportunityService.approveOpportunity(oppId, request, 200L);

        // then
        assertEquals(OpportunityService.STATUS_FAILED, opportunity.getStatus());
        assertEquals("预算不足，无法推进", opportunity.getRejectReason());
    }

    @Test
    void approveOpportunity_shouldThrowWhenCommentTooShort() {
        // given
        Long oppId = 1L;
        Opportunity opportunity = new Opportunity();
        opportunity.setId(oppId);
        opportunity.setStatus(OpportunityService.STATUS_PENDING);

        OpportunityApproveRequest request = new OpportunityApproveRequest();
        request.setAction(3); // REJECT
        request.setComment("太短");

        when(opportunityMapper.selectById(oppId)).thenReturn(opportunity);

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> opportunityService.approveOpportunity(oppId, request, 200L));
        assertEquals(1001, ex.getCode());
    }
}
