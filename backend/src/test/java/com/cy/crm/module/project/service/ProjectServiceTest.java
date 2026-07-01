package com.cy.crm.module.project.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.security.DataScopeValidator;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.opportunity.service.OpportunityService;
import com.cy.crm.module.project.converter.ProjectConverter;
import com.cy.crm.module.project.dto.ProjectRequest;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.*;
import com.cy.crm.module.rebate.service.RebateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 项目 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private OpportunityMapper opportunityMapper;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private ProjectConverter projectConverter;
    @Mock
    private BiddingNodeMapper biddingNodeMapper;
    @Mock
    private ContractNodeMapper contractNodeMapper;
    @Mock
    private PaymentNodeMapper paymentNodeMapper;
    @Mock
    private ProjectMilestoneMapper milestoneMapper;
    @Mock
    private ProjectScoreMapper projectScoreMapper;
    @Mock
    private RebateService rebateService;
    @Mock
    private OpportunityService opportunityService;
    @Mock
    private DataScopeValidator dataScopeValidator;

    @InjectMocks
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        // Mock DataScopeValidator to allow all access in unit tests
        lenient().doNothing().when(dataScopeValidator).validateUnitAccess(anyLong(), anyLong(), any());
    }

    @Test
    void createProject_shouldCreateWhenOpportunityExists() {
        // given
        Long oppId = 1L;
        ProjectRequest request = new ProjectRequest();
        request.setOpportunityId(oppId);
        request.setName("测试项目");

        Opportunity opportunity = new Opportunity();
        opportunity.setId(oppId);

        when(opportunityMapper.selectById(oppId)).thenReturn(opportunity);
        when(projectMapper.selectCount(any())).thenReturn(0L);
        when(projectConverter.requestToEntity(request)).thenReturn(new Project());

        Project savedProject = new Project();
        savedProject.setId(100L);
        doAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(100L);
            return 1;
        }).when(projectMapper).insert(any(Project.class));

        // when
        Long projectId = projectService.createProject(request, 200L);

        // then
        assertEquals(100L, projectId);
        verify(projectMapper).insert(any(Project.class));
    }

    @Test
    void createProject_shouldThrowWhenOpportunityNotFound() {
        // given
        Long oppId = 1L;
        ProjectRequest request = new ProjectRequest();
        request.setOpportunityId(oppId);

        when(opportunityMapper.selectById(oppId)).thenReturn(null);

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> projectService.createProject(request, 200L));
        assertEquals(4005, ex.getCode()); // opportunityNotFound
    }

    @Test
    void createProject_shouldThrowWhenProjectAlreadyExists() {
        // given
        Long oppId = 1L;
        ProjectRequest request = new ProjectRequest();
        request.setOpportunityId(oppId);

        when(opportunityMapper.selectById(oppId)).thenReturn(new Opportunity());
        when(projectMapper.selectCount(any())).thenReturn(1L);

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> projectService.createProject(request, 200L));
        assertEquals(5001, ex.getCode()); // projectExists
    }

    @Test
    void transitionProjectStatus_shouldCompleteInProgressProject() {
        // given
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        project.setStatus(ProjectService.STATUS_IN_PROGRESS);
        project.setOpportunityId(10L);

        Opportunity opportunity = new Opportunity();
        opportunity.setId(10L);
        opportunity.setCustomerId(100L);

        Customer customer = new Customer();
        customer.setId(100L);
        customer.setUnitId(1L);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(opportunityMapper.selectById(10L)).thenReturn(opportunity);
        when(customerMapper.selectById(100L)).thenReturn(customer);

        // when
        projectService.transitionProjectStatus(projectId, ProjectService.STATUS_COMPLETED, "正常完成", 200L);

        // then
        assertEquals(ProjectService.STATUS_COMPLETED, project.getStatus());
        verify(projectMapper).updateById(project);
    }

    @Test
    void transitionProjectStatus_shouldNotReverseTerminalStatus() {
        // given
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        project.setStatus(ProjectService.STATUS_COMPLETED);

        when(projectMapper.selectById(projectId)).thenReturn(project);

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> projectService.transitionProjectStatus(projectId, ProjectService.STATUS_IN_PROGRESS, "尝试回退", 200L));
        assertEquals(5003, ex.getCode()); // projectStatusInvalid
    }

    @Test
    void transitionProjectStatus_shouldRecoverFromInterrupted() {
        // given
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        project.setStatus(ProjectService.STATUS_INTERRUPTED);
        project.setOpportunityId(10L);

        Opportunity opportunity = new Opportunity();
        opportunity.setId(10L);
        opportunity.setCustomerId(100L);
        opportunity.setSubmitCount(0);
        opportunity.setStatus(ProjectService.STATUS_INTERRUPTED); // 任意旧状态

        Customer customer = new Customer();
        customer.setId(100L);
        customer.setUnitId(1L);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(opportunityMapper.selectById(10L)).thenReturn(opportunity);
        when(customerMapper.selectById(100L)).thenReturn(customer);

        // when
        projectService.transitionProjectStatus(projectId, ProjectService.STATUS_IN_PROGRESS, "恢复项目", 200L);

        // then
        assertEquals(ProjectService.STATUS_IN_PROGRESS, project.getStatus());
        assertEquals(3, opportunity.getStatus()); // STATUS_ACTIVE
        verify(opportunityMapper).updateById(opportunity);
    }

    @Test
    void transitionProjectStatus_shouldExpireOpportunityWhenTerminated() {
        // given
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        project.setStatus(ProjectService.STATUS_IN_PROGRESS);
        project.setOpportunityId(10L);

        Opportunity opportunity = new Opportunity();
        opportunity.setId(10L);
        opportunity.setCustomerId(100L);

        Customer customer = new Customer();
        customer.setId(100L);
        customer.setUnitId(1L);

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(opportunityMapper.selectById(10L)).thenReturn(opportunity);
        when(customerMapper.selectById(100L)).thenReturn(customer);

        // when
        projectService.transitionProjectStatus(projectId, ProjectService.STATUS_TERMINATED, "项目终止", 200L);

        // then
        assertEquals(ProjectService.STATUS_TERMINATED, project.getStatus());
        assertEquals(5, opportunity.getStatus()); // STATUS_EXPIRED
        assertEquals(2, opportunity.getSubmitCount());
        assertNotNull(opportunity.getCoolingUntil());
        verify(opportunityMapper).updateById(opportunity);
    }
}
