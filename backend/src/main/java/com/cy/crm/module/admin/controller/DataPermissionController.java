package com.cy.crm.module.admin.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.admin.dto.DataPermissionUpdateRequest;
import com.cy.crm.module.admin.service.DataPermissionService;
import com.cy.crm.module.admin.vo.DataPermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户数据权限管理。
 *
 * 维度由 DataScopeDimension 枚举定义（ALL / CHANNEL / REGION / UNIT /
 * BUSINESS_DOMAIN / POLICE_TYPE / SELF），存于 t_data_permission 表。
 */
@Tag(name = "用户数据权限")
@RestController
@RequestMapping("/api/admin/users/{userId}/data-permissions")
@RequiredArgsConstructor
public class DataPermissionController {

    private final DataPermissionService dataPermissionService;

    @Operation(summary = "查询用户数据权限（按 7 个维度）")
    @GetMapping
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:edit')")
    public ApiResult<List<DataPermissionVO>> list(@PathVariable Long userId) {
        return ApiResult.ok(dataPermissionService.listByUser(userId));
    }

    @Operation(summary = "按维度覆盖用户数据权限")
    @PutMapping
    @PreAuthorize("hasAnyRole('CYBD','SUPER_ADMIN') or hasAuthority('admin:user:edit')")
    public ApiResult<Void> update(@PathVariable Long userId,
                                  @Valid @RequestBody DataPermissionUpdateRequest request) {
        dataPermissionService.replaceScope(userId, request.getScopeType(), request.getScopeValues());
        return ApiResult.ok();
    }
}
