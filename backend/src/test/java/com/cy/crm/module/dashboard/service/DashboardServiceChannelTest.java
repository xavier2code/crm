package com.cy.crm.module.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.entity.UserChannel;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.contract.entity.Contract;
import com.cy.crm.module.contract.mapper.ContractMapper;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.dashboard.vo.ChannelDashboardVO;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.project.mapper.ProjectMilestoneMapper;
import com.cy.crm.module.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DashboardService.getChannelDashboard 单测
 *
 * 关键场景（业务依据 §9.1 渠道工作台）：
 *   - 无成员渠道：返回空 VO（不抛 NPE）
 *   - 有成员渠道：聚合 customer/opportunity/project/contract 数字
 *   - 成员业绩按合同金额倒序
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceChannelTest {

    @Mock private UserMapper userMapper;
    @Mock private UserChannelMapper userChannelMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private OpportunityMapper opportunityMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMilestoneMapper projectMilestoneMapper;
    @Mock private ContractMapper contractMapper;
    @Mock private TaskMapper taskMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldReturnEmptyDashboardWhenChannelHasNoMembers() {
        when(userChannelMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        ChannelDashboardVO vo = dashboardService.getChannelDashboard(99L);

        assertNotNull(vo);
        assertEquals(99L, vo.getChannelId());
        assertNull(vo.getMemberCount());
        assertNull(vo.getTotalCustomers());
        // 无成员时不应再调用任何 mapper
    }

    @Test
    void shouldAggregateChannelOverviewWhenChannelHasMembers() {
        when(userChannelMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                uc(10L), uc(20L)
        ));
        // opportunity 计数
        when(opportunityMapper.selectCount(any(Wrapper.class)))
                .thenReturn(5L)   // total
                .thenReturn(3L)   // active
                .thenReturn(1L);  // expired
        // project 计数
        when(projectMapper.selectCount(any(Wrapper.class)))
                .thenReturn(4L)   // total projects
                .thenReturn(2L)   // in progress (status=1)
                .thenReturn(1L)   // completed (status=2)
                .thenReturn(0L)   // status=3 (中断)
                .thenReturn(0L);  // status=4 (终止)
        when(customerMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(projectMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(contractMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        // 每个成员查 user 名字
        when(userMapper.selectById(10L)).thenReturn(user(10L, "张三"));
        when(userMapper.selectById(20L)).thenReturn(user(20L, "李四"));

        ChannelDashboardVO vo = dashboardService.getChannelDashboard(1L);

        assertNotNull(vo);
        assertEquals(2, vo.getMemberCount());
        assertEquals(5, vo.getTotalOpportunities());
        assertEquals(3, vo.getActiveOpportunities());
        assertEquals(1, vo.getExpiredOpportunities());
        assertEquals(4, vo.getTotalProjects());
        assertEquals(2, vo.getInProgressProjects());
        assertEquals(1, vo.getCompletedProjects());
        assertNotNull(vo.getProjectStatusDistribution());
        assertEquals(2, vo.getProjectStatusDistribution().get("项目中"));
        assertEquals(1, vo.getProjectStatusDistribution().get("项目完成"));
        assertNotNull(vo.getMemberPerformances());
        assertEquals(2, vo.getMemberPerformances().size());
    }

    @Test
    void shouldSumContractAmountsAndKeepYearAmount() {
        when(userChannelMapper.selectList(any(Wrapper.class))).thenReturn(List.of(uc(10L)));
        when(opportunityMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(projectMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(customerMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(projectMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(project(100L, 10L), project(101L, 10L)));
        Contract thisYear = contract(900L, 1L, new BigDecimal("100.00"), LocalDateTime.now());
        Contract lastYear = contract(901L, 2L, new BigDecimal("50.00"),
                LocalDateTime.now().minusYears(2));
        when(contractMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(thisYear, lastYear));
        when(userMapper.selectById(10L)).thenReturn(user(10L, "王五"));

        ChannelDashboardVO vo = dashboardService.getChannelDashboard(1L);

        assertNotNull(vo);
        assertEquals(0, new BigDecimal("150.00").compareTo(vo.getTotalContractAmount()),
                "总合同金额应为所有合同之和");
        assertEquals(0, new BigDecimal("100.00").compareTo(vo.getYearContractAmount()),
                "本年度合同金额应只算本年");
    }

    @Test
    void shouldSortMemberPerformanceByContractAmountDesc() {
        when(userChannelMapper.selectList(any(Wrapper.class))).thenReturn(List.of(uc(10L), uc(20L), uc(30L)));
        when(opportunityMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(projectMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(customerMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        // member 10 → 1 个 project (id=100)
        // member 20 → 2 个 projects (id=200, 201)
        // member 30 → 0 projects
        // 每个 member 调一次 projectMapper.selectList：member 10 → 1 个，member 20 → 2 个，member 30 → 0 个
        when(projectMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(project(100L, 10L), project(200L, 20L), project(201L, 20L)))  // 整渠道合同查询
                .thenReturn(List.of(project(100L, 10L)))   // member 10
                .thenReturn(List.of(project(200L, 20L), project(201L, 20L)))  // member 20
                .thenReturn(Collections.emptyList());      // member 30
        // 后面 getChannelDashboard 还会再调一次 (for 整渠道的 contract 查询) —— 用空 list 兜底
        when(contractMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(
                        contract(900L, 100L, new BigDecimal("10.00"), LocalDateTime.now()),
                        contract(901L, 200L, new BigDecimal("30.00"), LocalDateTime.now()),
                        contract(902L, 201L, new BigDecimal("20.00"), LocalDateTime.now())
                ));
        when(userMapper.selectById(10L)).thenReturn(user(10L, "甲"));
        when(userMapper.selectById(20L)).thenReturn(user(20L, "乙"));
        when(userMapper.selectById(30L)).thenReturn(user(30L, "丙"));

        ChannelDashboardVO vo = dashboardService.getChannelDashboard(1L);

        List<ChannelDashboardVO.MemberPerformanceVO> ms = vo.getMemberPerformances();
        assertEquals(3, ms.size());
        // 排序：第一名应该是合同金额最高的
        assertEquals("乙", ms.get(0).getUserName());
        assertEquals(0, new BigDecimal("50.00").compareTo(ms.get(0).getContractAmount()));
        assertEquals("甲", ms.get(1).getUserName());
        assertEquals(0, new BigDecimal("10.00").compareTo(ms.get(1).getContractAmount()));
        assertEquals("丙", ms.get(2).getUserName());
        assertEquals(0, BigDecimal.ZERO.compareTo(ms.get(2).getContractAmount()));
    }

    // ---------- helpers ----------

    private static UserChannel uc(Long userId) {
        UserChannel uc = new UserChannel();
        uc.setUserId(userId);
        uc.setChannelId(1L);
        uc.setAssignType(2);
        return uc;
    }

    private static User user(Long id, String realName) {
        User u = new User();
        u.setId(id);
        u.setRealName(realName);
        return u;
    }

    private static Project project(Long id, Long ownerBdId) {
        Project p = new Project();
        p.setId(id);
        p.setOwnerBdId(ownerBdId);
        return p;
    }

    private static Contract contract(Long id, Long projectId, BigDecimal amount, LocalDateTime createdAt) {
        Contract c = new Contract();
        c.setId(id);
        c.setAmount(amount);
        c.setCreatedAt(createdAt);
        c.setProjectId(projectId);
        return c;
    }
}
