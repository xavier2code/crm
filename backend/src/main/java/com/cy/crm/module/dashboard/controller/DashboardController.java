package com.cy.crm.module.dashboard.controller;
import com.cy.crm.common.constant.RoleConstants;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.dashboard.service.DashboardService;
import com.cy.crm.module.dashboard.vo.*;
import com.cy.crm.module.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "工作台和统计")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "获取我的工作台")
    @GetMapping("/my")
    public ApiResult<DashboardVO> getMyDashboard() {
        Long userId = currentUserService.getCurrentUserId();
        List<Long> roleIds = currentUserService.getCurrentUserRoleIds();
        return ApiResult.success(dashboardService.getDashboard(userId, roleIds));
    }

    @Operation(summary = "获取渠道工作台（仅渠道负责人和CYBD）")
    @GetMapping("/channel/{channelId}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<ChannelDashboardVO> getChannelDashboard(@PathVariable Long channelId) {
        return ApiResult.success(dashboardService.getChannelDashboard(channelId));
    }

    @Operation(summary = "获取项目统计")
    @GetMapping("/statistics/project")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).REGION_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<ProjectStatisticsVO> getProjectStatistics() {
        Long userId = currentUserService.getCurrentUserId();
        List<Long> roleIds = currentUserService.getCurrentUserRoleIds();
        return ApiResult.success(dashboardService.getProjectStatistics(userId, roleIds));
    }
}
