package com.cy.crm.module.admin.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.admin.dto.RoleRequest;
import com.cy.crm.module.admin.service.RoleService;
import com.cy.crm.module.admin.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表")
    @GetMapping
    public ApiResult<List<RoleVO>> list() {
        return ApiResult.ok(roleService.listRoles());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    public ApiResult<RoleVO> detail(@PathVariable Long id) {
        return ApiResult.ok(roleService.getRoleById(id));
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody RoleRequest request) {
        roleService.createRole(request);
        return ApiResult.ok();
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        request.setId(id);
        roleService.updateRole(request);
        return ApiResult.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResult.ok();
    }
}
