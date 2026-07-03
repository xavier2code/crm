package com.cy.crm.module.unit.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.unit.dto.UnitAssignRequest;
import com.cy.crm.module.unit.service.UnitAssignmentService;
import com.cy.crm.module.unit.vo.UnitAssignmentPage;
import com.cy.crm.module.unit.vo.UnitAssignmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务侧单位分配 controller（4 级分配链路，详见 CRM-渠道版-开发文档.md §9.5）。
 *
 * 路径前缀：/api/units 与现有 /api/admin/units 区分；前端业务侧从 BasicLayout 菜单"单位分配"进入。
 */
@Tag(name = "单位分配（业务侧）")
@Validated
@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitAssignmentController {

    private final UnitAssignmentService unitAssignmentService;

    @Operation(summary = "查询某单位的分配记录")
    @GetMapping("/{id}/assignments")
    public ApiResult<List<UnitAssignmentVO>> listByUnit(@PathVariable Long id) {
        return ApiResult.ok(unitAssignmentService.listByUnit(id));
    }

    @Operation(summary = "分页查询分配记录（按单位/用户/渠道/范围过滤）")
    @GetMapping("/assignments")
    public ApiResult<UnitAssignmentPage> page(
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String assignScope,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size) {
        return ApiResult.ok(unitAssignmentService.pageAssignments(
                unitId, userId, channelId, assignScope, current, size));
    }

    @Operation(summary = "新增单位分配")
    @PostMapping("/{id}/assignments")
    public ApiResult<Long> assign(@PathVariable Long id, @Valid @RequestBody UnitAssignRequest request) {
        return ApiResult.ok(unitAssignmentService.assign(id, request));
    }

    @Operation(summary = "撤销单位分配")
    @DeleteMapping("/assignments/{assignmentId}")
    public ApiResult<Void> revoke(@PathVariable Long assignmentId) {
        unitAssignmentService.revoke(assignmentId);
        return ApiResult.ok();
    }
}
