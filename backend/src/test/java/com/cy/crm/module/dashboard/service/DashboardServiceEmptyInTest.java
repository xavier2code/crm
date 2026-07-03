package com.cy.crm.module.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.contract.mapper.ContractMapper;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.dashboard.vo.DashboardVO;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.project.mapper.ProjectMilestoneMapper;
import com.cy.crm.module.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DashboardService.getDashboard 边界场景：用户名下无项目 / 30 天内无生效中商机
 * 修复前：抛 PSQLException "syntax error at or near ')'"，因为空 List 拼成 IN ()
 * 修复后：短路返回 0，不调用 contractMapper / customerMapper 的对应查询
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceEmptyInTest {

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
    void getDashboard_shouldReturnZeroContractAmountWhenUserHasNoProjects() {
        // 用户无渠道
        when(userChannelMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        // 用户名下无项目 -> 空 List -> 不能拼 IN ()
        when(projectMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        // 其余计数 query 全部返回 0
        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(opportunityMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(projectMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        DashboardVO vo = dashboardService.getDashboard(100L, List.of(4L));

        assertNotNull(vo);
        assertEquals(BigDecimal.ZERO, vo.getTotalContractAmount());
        // 关键：contractMapper 绝不能被调用，因为没有 projectIds 就不该跑那个 IN 查询
        verify(contractMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void getDashboard_shouldReturnZeroFollowUpCountWhenNoRecentActiveOpportunities() {
        when(userChannelMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(projectMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(opportunityMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(projectMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        // 30 天内的生效中商机为空 -> 空 List -> 不能拼 IN ()
        when(opportunityMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        DashboardVO vo = dashboardService.getDashboard(100L, List.of(4L));

        assertNotNull(vo);
        assertEquals(0, vo.getTodayFollowUpCount());
    }
}
