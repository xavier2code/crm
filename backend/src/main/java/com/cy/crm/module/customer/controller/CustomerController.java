package com.cy.crm.module.customer.controller;
import com.cy.crm.common.constant.RoleConstants;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.customer.dto.ContactRequest;
import com.cy.crm.module.customer.dto.CustomerRequest;
import com.cy.crm.module.customer.service.CustomerService;
import com.cy.crm.module.customer.vo.ContactVO;
import com.cy.crm.module.customer.vo.CustomerVO;
import com.cy.crm.module.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "分页查询客户列表")
    @GetMapping
    public ApiResult<Page<CustomerVO>> pageCustomers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) String keyword
    ) {
        // 手动限制分页大小，防止 DoS 攻击
        if (size != null && size > 100) {
            size = 100L;
        }
        Long userId = currentUserService.getCurrentUserId();
        List<Long> roleIds = currentUserService.getCurrentUserRoleIds();
        return ApiResult.success(customerService.pageCustomers(current, size, keyword, userId, roleIds));
    }

    @Operation(summary = "获取客户详情")
    @GetMapping("/{id}")
    public ApiResult<CustomerVO> getCustomer(@PathVariable Long id) {
        return ApiResult.success(customerService.getCustomerById(id));
    }

    @Operation(summary = "创建客户")
    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).REGION_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> createCustomer(@Valid @RequestBody CustomerRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResult.success(customerService.createCustomer(request, userId));
    }

    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).REGION_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        customerService.updateCustomer(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).REGION_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ApiResult.success();
    }

    @Operation(summary = "分配客户")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).REGION_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> assignCustomer(
            @PathVariable Long id,
            @RequestParam Long userId) {
        customerService.assignCustomer(id, userId);
        return ApiResult.success();
    }

    @Operation(summary = "添加联系人")
    @PostMapping("/{id}/contacts")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).REGION_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> addContact(
            @PathVariable Long id,
            @Valid @RequestBody ContactRequest request) {
        return ApiResult.success(customerService.addContact(id, request));
    }

    @Operation(summary = "更新联系人")
    @PutMapping("/contacts/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).REGION_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody ContactRequest request) {
        customerService.updateContact(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "删除联系人")
    @DeleteMapping("/contacts/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).REGION_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> deleteContact(@PathVariable Long id) {
        customerService.deleteContact(id);
        return ApiResult.success();
    }
}
