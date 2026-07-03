package com.cy.crm.module.reimbursement.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.constant.RoleConstants;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.reimbursement.dto.ReimbursementApproveRequest;
import com.cy.crm.module.reimbursement.dto.ReimbursementRequest;
import com.cy.crm.module.reimbursement.service.ReimbursementService;
import com.cy.crm.module.reimbursement.vo.ReimbursementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "报销管理")
@Validated
@RestController
@RequestMapping("/api/reimbursements")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "分页查询报销记录")
    @GetMapping
    public ApiResult<Page<ReimbursementVO>> pageReimbursements(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(defaultValue = "false") Boolean mine
    ) {
        Long userId = currentUserService.getCurrentUserId();
        List<String> roleCodes = currentUserService.getCurrentUserRoles();
        // 非审批/管理员：默认只看自己的
        boolean onlyMine = mine
                || !(roleCodes.contains(RoleConstants.ADMIN)
                  || roleCodes.contains(RoleConstants.CHANNEL_HEAD)
                  || roleCodes.contains(RoleConstants.FINANCE));
        return ApiResult.success(reimbursementService.pageReimbursements(
                current, size, status, type, projectId, applicantId, onlyMine, userId));
    }

    @Operation(summary = "查询报销详情")
    @GetMapping("/{id}")
    public ApiResult<ReimbursementVO> getReimbursement(@PathVariable Long id) {
        return ApiResult.success(reimbursementService.getReimbursement(id));
    }

    @Operation(summary = "创建报销申请（DRAFT）")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('reimbursement:create')")
    public ApiResult<Long> createReimbursement(@Valid @RequestBody ReimbursementRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResult.success(reimbursementService.createReimbursement(request, userId));
    }

    @Operation(summary = "编辑报销申请（DRAFT/REJECTED）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('reimbursement:create')")
    public ApiResult<Void> updateReimbursement(@PathVariable Long id,
                                               @Valid @RequestBody ReimbursementRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        reimbursementService.updateReimbursement(id, request, userId);
        return ApiResult.success();
    }

    @Operation(summary = "删除报销申请（DRAFT/REJECTED）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('reimbursement:create')")
    public ApiResult<Void> deleteReimbursement(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        reimbursementService.deleteReimbursement(id, userId);
        return ApiResult.success();
    }

    @Operation(summary = "提交报销（DRAFT/REJECTED -> PENDING）")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('reimbursement:create')")
    public ApiResult<Void> submitReimbursement(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        reimbursementService.submitReimbursement(id, userId);
        return ApiResult.success();
    }

    @Operation(summary = "审批报销（PENDING -> APPROVED/REJECTED）")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('reimbursement:approve')")
    public ApiResult<Void> approveReimbursement(@PathVariable Long id,
                                                @Valid @RequestBody ReimbursementApproveRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        com.cy.crm.module.admin.entity.User currentUser = currentUserService.getCurrentUser();
        String userName = currentUser != null ? currentUser.getRealName() : "";
        reimbursementService.approveReimbursement(id, request, userId, userName);
        return ApiResult.success();
    }

    @Operation(summary = "标记已付款（APPROVED -> PAID）")
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('reimbursement:pay')")
    public ApiResult<Void> markPaid(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        reimbursementService.markPaid(id, userId);
        return ApiResult.success();
    }
}
