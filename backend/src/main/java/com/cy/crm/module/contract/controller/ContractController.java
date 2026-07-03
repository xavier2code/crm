package com.cy.crm.module.contract.controller;
import com.cy.crm.common.constant.RoleConstants;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.contract.dto.ContractRequest;
import com.cy.crm.module.contract.service.ContractService;
import com.cy.crm.module.contract.vo.ContractVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "合同管理")
@Validated
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "分页查询合同")
    @GetMapping
    public ApiResult<Page<ContractVO>> pageContracts(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) Integer status
    ) {
        return ApiResult.success(contractService.pageContracts(current, size, status));
    }

    @Operation(summary = "获取合同详情")
    @GetMapping("/{id}")
    public ApiResult<ContractVO> getContract(@PathVariable Long id) {
        return ApiResult.success(contractService.getContractById(id));
    }

    @Operation(summary = "创建合同")
    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> createContract(@Valid @RequestBody ContractRequest request) {
        return ApiResult.success(contractService.createContract(request));
    }

    @Operation(summary = "删除合同")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ApiResult.success();
    }

    @Operation(summary = "更新合同")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody ContractRequest request) {
        contractService.updateContract(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "更新合同状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        contractService.updateStatus(id, status);
        return ApiResult.success();
    }
}
