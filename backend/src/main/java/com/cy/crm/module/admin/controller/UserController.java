package com.cy.crm.module.admin.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.security.SecurityContext;
import com.cy.crm.module.admin.dto.UserRequest;
import com.cy.crm.module.admin.dto.UserStatusRequest;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.module.admin.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@Validated
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:view')")
    public ApiResult<Page<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size) {
        return ApiResult.ok(userService.pageUsers(keyword, current, size));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:view')")
    public ApiResult<UserVO> detail(@PathVariable Long id) {
        return ApiResult.ok(userService.getUserById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:edit')")
    public ApiResult<Void> create(@Valid @RequestBody UserRequest request) {
        userService.createUser(request, SecurityContext.getCurrentUserId());
        return ApiResult.ok();
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:edit')")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        request.setId(id);
        userService.updateUser(request);
        return ApiResult.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:edit')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResult.ok();
    }

    @Operation(summary = "重置用户密码为初始密码 123456")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:edit')")
    public ApiResult<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return ApiResult.ok();
    }

    @Operation(summary = "启用/停用用户")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:edit')")
    public ApiResult<Void> updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return ApiResult.ok();
    }
}
