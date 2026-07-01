package com.cy.crm.module.rebate.controller;
import com.cy.crm.common.constant.RoleConstants;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.rebate.dto.RebateRequest;
import com.cy.crm.module.rebate.service.RebateService;
import com.cy.crm.module.rebate.vo.RebateVO;
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

@Tag(name = "返利管理")
@Validated
@RestController
@RequestMapping("/api/rebates")
@RequiredArgsConstructor
public class RebateController {

    private final RebateService rebateService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "分页查询返利（仅渠道负责人和CYBD）")
    @GetMapping
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Page<RebateVO>> pageRebates(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Integer confirmStatus,
            @RequestParam(required = false) Integer paymentStatus
    ) {
        if (currentUserService.hasRole("CHANNEL_HEAD") && !currentUserService.hasRole("CYBD")) {
            channelId = currentUserService.getCurrentChannelId();
        }
        return ApiResult.success(rebateService.pageRebates(current, size, channelId, confirmStatus, paymentStatus));
    }

    @Operation(summary = "查询我的渠道返利（渠道负责人）")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD)")
    public ApiResult<List<RebateVO>> listMyRebates() {
        Long channelId = currentUserService.getCurrentChannelId();
        return ApiResult.success(rebateService.listByChannelId(channelId));
    }

    @Operation(summary = "创建返利记录")
    @PostMapping
    @PreAuthorize("hasAuthority(T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> createRebate(@Valid @RequestBody RebateRequest request) {
        return ApiResult.success(rebateService.createRebate(request));
    }

    @Operation(summary = "更新返利记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateRebate(
            @PathVariable Long id,
            @Valid @RequestBody RebateRequest request) {
        rebateService.updateRebate(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "更新确认状态")
    @PutMapping("/{id}/confirm-status")
    @PreAuthorize("hasAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD)")
    public ApiResult<Void> updateConfirmStatus(
            @PathVariable Long id,
            @RequestParam Integer confirmStatus) {
        rebateService.updateConfirmStatus(id, confirmStatus);
        return ApiResult.success();
    }

    @Operation(summary = "更新付款状态")
    @PutMapping("/{id}/payment-status")
    @PreAuthorize("hasAuthority(T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam Integer paymentStatus) {
        rebateService.updatePaymentStatus(id, paymentStatus);
        return ApiResult.success();
    }
}
