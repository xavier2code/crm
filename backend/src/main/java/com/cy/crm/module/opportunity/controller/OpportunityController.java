package com.cy.crm.module.opportunity.controller;
import com.cy.crm.common.constant.RoleConstants;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.opportunity.dto.OpportunityApproveRequest;
import com.cy.crm.module.opportunity.dto.OpportunityRequest;
import com.cy.crm.module.opportunity.service.OpportunityService;
import com.cy.crm.module.opportunity.vo.OpportunityDetailVO;
import com.cy.crm.module.opportunity.vo.OpportunityVO;
import com.cy.crm.module.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商机报备管理")
@Validated
@RestController
@RequestMapping("/api/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "分页查询报备列表")
    @GetMapping
    public ApiResult<Page<OpportunityVO>> pageOpportunities(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) Integer status
    ) {
        Long userId = currentUserService.getCurrentUserId();
        List<Long> roleIds = currentUserService.getCurrentUserRoleIds();
        return ApiResult.success(opportunityService.pageOpportunities(current, size, status, userId, roleIds));
    }

    @Operation(summary = "获取报备详情")
    @GetMapping("/{id}")
    public ApiResult<OpportunityDetailVO> getOpportunity(@PathVariable Long id) {
        return ApiResult.success(opportunityService.getOpportunityById(id));
    }

    @Operation(summary = "创建报备")
    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> createOpportunity(@Valid @RequestBody OpportunityRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResult.success(opportunityService.createOpportunity(request, userId));
    }

    @Operation(summary = "更新报备")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateOpportunity(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityRequest request) {
        opportunityService.updateOpportunity(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> submitOpportunity(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        opportunityService.submitOpportunity(id, userId);
        return ApiResult.success();
    }

    @Operation(summary = "审批报备")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> approveOpportunity(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityApproveRequest request) {
        Long approverId = currentUserService.getCurrentUserId();
        opportunityService.approveOpportunity(id, request, approverId);
        return ApiResult.success();
    }

    @Operation(summary = "删除报备")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> deleteOpportunity(@PathVariable Long id) {
        opportunityService.deleteOpportunity(id);
        return ApiResult.success();
    }
}
