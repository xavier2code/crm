package com.cy.crm.module.sales_team.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.sales_team.dto.SalesTeamConfigRequest;
import com.cy.crm.module.sales_team.service.SalesTeamConfigService;
import com.cy.crm.module.sales_team.vo.SalesTeamConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 销售梯队配置 Controller
 *
 * 管理端路径：/api/admin/sales-teams
 * 业务端路径：/api/sales-teams/region/{region}
 *
 * 符合开发文档 §8.2 管理接口
 */
@Tag(name = "销售梯队配置")
@Validated
@RestController
@RequiredArgsConstructor
public class SalesTeamConfigController {

    private final SalesTeamConfigService salesTeamConfigService;

    @Operation(summary = "分页查询销售梯队配置")
    @GetMapping("/api/admin/sales-teams")
    @PreAuthorize("hasAnyAuthority('sales-team:view', 'sales-team:manage')")
    public ApiResult<Page<SalesTeamConfigVO>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String teamCode) {
        return ApiResult.success(salesTeamConfigService.pageConfigs(current, size, regionCode, teamCode));
    }

    @Operation(summary = "查询销售梯队配置详情")
    @GetMapping("/api/admin/sales-teams/{id}")
    @PreAuthorize("hasAnyAuthority('sales-team:view', 'sales-team:manage')")
    public ApiResult<SalesTeamConfigVO> get(@PathVariable Long id) {
        return ApiResult.success(salesTeamConfigService.getConfig(id));
    }

    @Operation(summary = "新建销售梯队配置")
    @PostMapping("/api/admin/sales-teams")
    @PreAuthorize("hasAnyAuthority('sales-team:manage')")
    public ApiResult<Long> create(@Valid @RequestBody SalesTeamConfigRequest request) {
        return ApiResult.success(salesTeamConfigService.createConfig(request));
    }

    @Operation(summary = "编辑销售梯队配置")
    @PutMapping("/api/admin/sales-teams/{id}")
    @PreAuthorize("hasAnyAuthority('sales-team:manage')")
    public ApiResult<Void> update(@PathVariable Long id,
                                   @Valid @RequestBody SalesTeamConfigRequest request) {
        salesTeamConfigService.updateConfig(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "删除销售梯队配置")
    @DeleteMapping("/api/admin/sales-teams/{id}")
    @PreAuthorize("hasAnyAuthority('sales-team:manage')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        salesTeamConfigService.deleteConfig(id);
        return ApiResult.success();
    }

    @Operation(summary = "按区域查询有效销售梯队")
    @GetMapping("/api/sales-teams/region/{region}")
    @PreAuthorize("hasAnyAuthority('sales-team:view', 'sales-team:manage')")
    public ApiResult<List<SalesTeamConfigVO>> listByRegion(@PathVariable String region) {
        return ApiResult.success(salesTeamConfigService.listByRegion(region));
    }
}
