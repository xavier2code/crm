package com.cy.crm.module.admin.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.admin.dto.UserRequest;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.module.admin.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
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
    public ApiResult<Page<UserVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size) {
        return ApiResult.ok(userService.pageUsers(keyword, current, size));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public ApiResult<UserVO> detail(@PathVariable Long id) {
        return ApiResult.ok(userService.getUserById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody UserRequest request) {
        userService.createUser(request, 1L);
        return ApiResult.ok();
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        request.setId(id);
        userService.updateUser(request);
        return ApiResult.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResult.ok();
    }
}
