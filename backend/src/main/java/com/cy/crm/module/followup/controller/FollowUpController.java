package com.cy.crm.module.followup.controller;
import com.cy.crm.common.constant.RoleConstants;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.followup.dto.FollowUpRequest;
import com.cy.crm.module.followup.service.FollowUpService;
import com.cy.crm.module.followup.vo.FollowUpVO;
import com.cy.crm.module.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "跟进记录管理")
@RestController
@RequestMapping("/api/follow-ups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "分页查询跟进记录")
    @GetMapping
    public ApiResult<Page<FollowUpVO>> pageFollowUps(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) Long customerId
    ) {
        // 手动限制分页大小，防止 DoS 攻击
        if (size != null && size > 100) {
            size = 100L;
        }
        Long userId = currentUserService.getCurrentUserId();
        List<Long> roleIds = currentUserService.getCurrentUserRoleIds();
        return ApiResult.success(followUpService.pageFollowUps(current, size, customerId, userId, roleIds));
    }

    @Operation(summary = "创建跟进记录")
    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> createFollowUp(@Valid @RequestBody FollowUpRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResult.success(followUpService.createFollowUp(request, userId));
    }

    @Operation(summary = "更新跟进记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateFollowUp(
            @PathVariable Long id,
            @Valid @RequestBody FollowUpRequest request) {
        followUpService.updateFollowUp(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "删除跟进记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> deleteFollowUp(@PathVariable Long id) {
        followUpService.deleteFollowUp(id);
        return ApiResult.success();
    }
}
