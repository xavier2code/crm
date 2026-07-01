package com.cy.crm.module.admin.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.admin.dto.UnitRequest;
import com.cy.crm.module.admin.service.UnitService;
import com.cy.crm.module.admin.vo.UnitVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "单位主数据")
@Validated
@RestController
@RequestMapping("/api/admin/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @Operation(summary = "单位分页列表")
    @GetMapping
    public ApiResult<Page<UnitVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size) {
        return ApiResult.ok(unitService.pageUnits(keyword, region, current, size));
    }

    @Operation(summary = "全部启用单位（下拉用）")
    @GetMapping("/all")
    public ApiResult<List<UnitVO>> listAll() {
        return ApiResult.ok(unitService.listAll());
    }

    @Operation(summary = "单位详情")
    @GetMapping("/{id}")
    public ApiResult<UnitVO> detail(@PathVariable Long id) {
        return ApiResult.ok(unitService.getById(id));
    }

    @Operation(summary = "创建单位")
    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody UnitRequest request) {
        unitService.create(request);
        return ApiResult.ok();
    }

    @Operation(summary = "编辑单位")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody UnitRequest request) {
        request.setId(id);
        unitService.update(request);
        return ApiResult.ok();
    }

    @Operation(summary = "删除单位")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        unitService.delete(id);
        return ApiResult.ok();
    }
}
