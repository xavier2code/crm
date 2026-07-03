package com.cy.crm.module.rebate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.module.admin.entity.Channel;
import com.cy.crm.module.admin.entity.UserChannel;
import com.cy.crm.module.admin.mapper.ChannelMapper;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.contract.entity.Contract;
import com.cy.crm.module.contract.mapper.ContractMapper;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.entity.PaymentNode;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.PaymentNodeMapper;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.rebate.converter.RebateConverter;
import com.cy.crm.module.rebate.entity.Rebate;
import com.cy.crm.module.rebate.mapper.RebateMapper;
import com.cy.crm.security.DataScopeValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RebateServiceTest {

    @Mock private RebateMapper rebateMapper;
    @Mock private ChannelMapper channelMapper;
    @Mock private DictionaryService dictionaryService;
    @Mock private RebateRateService rebateRateService;
    @Mock private CurrentUserService currentUserService;
    @Mock private RebateConverter rebateConverter;
    @Mock private DataScopeValidator dataScopeValidator;

    @Mock private ProjectMapper projectMapper;
    @Mock private OpportunityMapper opportunityMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private UserChannelMapper userChannelMapper;
    @Mock private ContractMapper contractMapper;
    @Mock private PaymentNodeMapper paymentNodeMapper;

    @InjectMocks private RebateService rebateService;

    private static final Long CHANNEL_ID = 10L;
    private static final String PRODUCT_CATEGORY = "TAN_ZHAO_DENG";
    private static final BigDecimal RATE = new BigDecimal("0.10");

    private Project projectWithChannel() {
        Project project = new Project();
        project.setId(1L);
        project.setOpportunityId(2L);
        project.setProductCategory(PRODUCT_CATEGORY);
        project.setStatus(1);
        return project;
    }

    private void stubChannelResolution() {
        when(projectMapper.selectById(1L)).thenReturn(projectWithChannel());
        Opportunity opportunity = new Opportunity();
        opportunity.setId(2L);
        opportunity.setCustomerId(3L);
        when(opportunityMapper.selectById(2L)).thenReturn(opportunity);
        Customer customer = new Customer();
        customer.setId(3L);
        customer.setOwnerUserId(4L);
        when(customerMapper.selectById(3L)).thenReturn(customer);
        UserChannel assignment = new UserChannel();
        assignment.setChannelId(CHANNEL_ID);
        assignment.setAssignedAt(LocalDateTime.now());
        when(userChannelMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(assignment));
        when(rebateRateService.getRateForChannelAndProduct(PRODUCT_CATEGORY, CHANNEL_ID, LocalDate.now()))
                .thenReturn(RATE);
    }

    @Test
    void generateContractRebate_shouldCreatePerformanceRebate() {
        when(rebateRateService.getRateForChannelAndProduct(PRODUCT_CATEGORY, CHANNEL_ID, LocalDate.now()))
                .thenReturn(RATE);

        rebateService.generateContractRebate(100L, CHANNEL_ID, new BigDecimal("100000"), PRODUCT_CATEGORY);

        ArgumentCaptor<Rebate> captor = ArgumentCaptor.forClass(Rebate.class);
        verify(rebateMapper).insert(captor.capture());
        Rebate rebate = captor.getValue();
        assertEquals(CHANNEL_ID, rebate.getChannelId());
        assertEquals(100L, rebate.getContractId());
        assertEquals(PRODUCT_CATEGORY, rebate.getProductCategory());
        assertEquals(RebateService.TYPE_PERFORMANCE, rebate.getRebateType());
        assertEquals(0, new BigDecimal("10000").compareTo(rebate.getTotalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(rebate.getActualAmount()));
        assertEquals(RebateService.CONFIRM_PENDING, rebate.getConfirmStatus());
        assertEquals(RebateService.PAYMENT_UNPAID, rebate.getPaymentStatus());
    }

    @Test
    void generateMissingPerformanceRebates_shouldCreateWhenNotExists() {
        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);
        contract.setAmount(new BigDecimal("100000"));
        contract.setStatus(RebateService.CONTRACT_STATUS_SIGNED);

        when(contractMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(contract));
        when(rebateMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        stubChannelResolution();

        int count = rebateService.generateMissingPerformanceRebates();

        assertEquals(1, count);
        verify(rebateMapper).insert(any(Rebate.class));
    }

    @Test
    void generateMissingPerformanceRebates_shouldSkipWhenExists() {
        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);
        contract.setAmount(new BigDecimal("100000"));
        contract.setStatus(RebateService.CONTRACT_STATUS_SIGNED);

        when(contractMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(contract));
        when(rebateMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        int count = rebateService.generateMissingPerformanceRebates();

        assertEquals(0, count);
        verify(rebateMapper, never()).insert(any(Rebate.class));
    }

    @Test
    void generatePaymentRebates_shouldCreateWhenPaymentReceivedAndNoRebate() {
        PaymentNode paymentNode = new PaymentNode();
        paymentNode.setId(200L);
        paymentNode.setProjectId(1L);
        paymentNode.setAmount(new BigDecimal("50000"));
        paymentNode.setStatus(RebateService.PAYMENT_STATUS_RECEIVED);
        paymentNode.setReceivedDate(LocalDate.now());

        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);
        contract.setAmount(new BigDecimal("100000"));

        when(paymentNodeMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(paymentNode));
        when(rebateMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(contractMapper.selectOne(any(QueryWrapper.class))).thenReturn(contract);
        stubChannelResolution();

        int count = rebateService.generatePaymentRebates();

        assertEquals(1, count);
        ArgumentCaptor<Rebate> captor = ArgumentCaptor.forClass(Rebate.class);
        verify(rebateMapper).insert(captor.capture());
        Rebate rebate = captor.getValue();
        assertEquals(RebateService.TYPE_PAYMENT, rebate.getRebateType());
        assertEquals(200L, rebate.getPaymentNodeId());
        assertEquals(0, new BigDecimal("5000").compareTo(rebate.getTotalAmount()));
        assertEquals(0, new BigDecimal("5000").compareTo(rebate.getActualAmount()));
    }

    @Test
    void generatePaymentRebates_shouldSkipWhenRebateExists() {
        PaymentNode paymentNode = new PaymentNode();
        paymentNode.setId(200L);
        paymentNode.setProjectId(1L);
        paymentNode.setAmount(new BigDecimal("50000"));
        paymentNode.setStatus(RebateService.PAYMENT_STATUS_RECEIVED);

        when(paymentNodeMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(paymentNode));
        when(rebateMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        int count = rebateService.generatePaymentRebates();

        assertEquals(0, count);
        verify(rebateMapper, never()).insert(any(Rebate.class));
    }

    @Test
    void generateServiceRebates_shouldCreateWhenServiceMatured() {
        Project project = projectWithChannel();
        project.setFormalAt(LocalDate.now().minusMonths(10).atStartOfDay());

        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);
        contract.setAmount(new BigDecimal("100000"));

        when(projectMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(project));
        when(contractMapper.selectOne(any(QueryWrapper.class))).thenReturn(contract);
        when(rebateMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        stubChannelResolution();

        int count = rebateService.generateServiceRebates(LocalDate.now());

        assertEquals(1, count);
        ArgumentCaptor<Rebate> captor = ArgumentCaptor.forClass(Rebate.class);
        verify(rebateMapper).insert(captor.capture());
        assertEquals(RebateService.TYPE_SERVICE, captor.getValue().getRebateType());
    }

    @Test
    void generateServiceRebates_shouldSkipWhenExists() {
        Project project = projectWithChannel();
        project.setFormalAt(LocalDate.now().minusMonths(10).atStartOfDay());

        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);

        when(projectMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(project));
        when(contractMapper.selectOne(any(QueryWrapper.class))).thenReturn(contract);
        when(rebateMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        int count = rebateService.generateServiceRebates(LocalDate.now());

        assertEquals(0, count);
        verify(rebateMapper, never()).insert(any(Rebate.class));
    }

    @Test
    void updateActualPerformanceAmounts_shouldSetFullAmountWhenProjectCompleted() {
        Rebate rebate = new Rebate();
        rebate.setId(1L);
        rebate.setContractId(100L);
        rebate.setRebateType(RebateService.TYPE_PERFORMANCE);
        rebate.setTotalAmount(new BigDecimal("10000"));
        rebate.setActualAmount(BigDecimal.ZERO);

        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);

        Project project = projectWithChannel();
        project.setStatus(RebateService.PROJECT_STATUS_COMPLETED);

        when(rebateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(rebate));
        when(contractMapper.selectById(100L)).thenReturn(contract);
        when(projectMapper.selectById(1L)).thenReturn(project);

        int updated = rebateService.updateActualPerformanceAmounts(LocalDate.now());

        assertEquals(1, updated);
        assertEquals(0, new BigDecimal("10000").compareTo(rebate.getActualAmount()));
        verify(rebateMapper).updateById(rebate);
    }

    @Test
    void updateActualPerformanceAmounts_shouldProrateByServiceMonths() {
        Rebate rebate = new Rebate();
        rebate.setId(1L);
        rebate.setContractId(100L);
        rebate.setRebateType(RebateService.TYPE_PERFORMANCE);
        rebate.setTotalAmount(new BigDecimal("9000"));
        rebate.setActualAmount(BigDecimal.ZERO);

        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);

        Project project = projectWithChannel();
        project.setStatus(1);
        project.setFormalAt(LocalDate.now().minusMonths(3).atStartOfDay());

        when(rebateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(rebate));
        when(contractMapper.selectById(100L)).thenReturn(contract);
        when(projectMapper.selectById(1L)).thenReturn(project);

        int updated = rebateService.updateActualPerformanceAmounts(LocalDate.now());

        assertEquals(1, updated);
        // 服务第 4 个月：4/9
        assertEquals(0, new BigDecimal("4000.00").compareTo(rebate.getActualAmount()));
    }

    @Test
    void updateActualPerformanceAmounts_shouldSkipWhenUnchanged() {
        Rebate rebate = new Rebate();
        rebate.setId(1L);
        rebate.setContractId(100L);
        rebate.setRebateType(RebateService.TYPE_PERFORMANCE);
        rebate.setTotalAmount(new BigDecimal("9000"));
        rebate.setActualAmount(new BigDecimal("4000.00"));

        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);

        Project project = projectWithChannel();
        project.setStatus(1);
        project.setFormalAt(LocalDate.now().minusMonths(3).atStartOfDay());

        when(rebateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(rebate));
        when(contractMapper.selectById(100L)).thenReturn(contract);
        when(projectMapper.selectById(1L)).thenReturn(project);

        int updated = rebateService.updateActualPerformanceAmounts(LocalDate.now());

        assertEquals(0, updated);
        verify(rebateMapper, never()).updateById(any(Rebate.class));
    }

    @Test
    void updateActualPerformanceAmounts_shouldReturnZeroWhenNoFormalAtAndInProgress() {
        Rebate rebate = new Rebate();
        rebate.setId(1L);
        rebate.setContractId(100L);
        rebate.setRebateType(RebateService.TYPE_PERFORMANCE);
        rebate.setTotalAmount(new BigDecimal("9000"));
        rebate.setActualAmount(BigDecimal.ZERO);

        Contract contract = new Contract();
        contract.setId(100L);
        contract.setProjectId(1L);

        Project project = projectWithChannel();
        project.setStatus(1);

        when(rebateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(rebate));
        when(contractMapper.selectById(100L)).thenReturn(contract);
        when(projectMapper.selectById(1L)).thenReturn(project);

        int updated = rebateService.updateActualPerformanceAmounts(LocalDate.now());

        assertEquals(0, updated);
        verify(rebateMapper, never()).updateById(any(Rebate.class));
    }
}
