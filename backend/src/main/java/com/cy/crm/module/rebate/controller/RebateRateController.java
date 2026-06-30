package com.cy.crm.module.rebate.controller;

import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.rebate.dto.RebateRateRequest;
import com.cy.crm.module.rebate.entity.RebateRate;
import com.cy.crm.module.rebate.service.RebateRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rebate-rate")
@RequiredArgsConstructor
@Tag(name = "返利率配置", description = "返利率配置管理")
public class RebateRateController {

    private final RebateRateService rebateRateService;

    @GetMapping("/list")
    @Operation(summary = "获取返利率配置列表")
    public ApiResult<List<RebateRate>> listRates(@RequestParam(required = false) Long channelId) {
        if (channelId != null) {
            return ApiResult.ok(rebateRateService.getRatesForChannel(channelId));
        }
        return ApiResult.ok(rebateRateService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取返利率配置详情")
    public ApiResult<RebateRate> getRate(@PathVariable Long id) {
        return ApiResult.ok(rebateRateService.getById(id));
    }

    @PostMapping("/save")
    @Operation(summary = "保存返利率配置")
    public ApiResult<RebateRate> saveRate(@RequestBody RebateRateRequest request) {
        RebateRate rate = new RebateRate();
        BeanUtils.copyProperties(request, rate);
        return ApiResult.ok(rebateRateService.saveRebateRate(rate));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除返利率配置")
    public ApiResult<Void> deleteRate(@PathVariable Long id) {
        rebateRateService.deleteRebateRate(id);
        return ApiResult.ok();
    }

    @GetMapping("/query-rate")
    @Operation(summary = "查询指定渠道和产品的返利率")
    public ApiResult<BigDecimal> queryRate(
            @RequestParam String productCategory,
            @RequestParam Long channelId,
            @RequestParam(required = false) String effectiveDate) {
        LocalDate date = effectiveDate != null ? LocalDate.parse(effectiveDate) : LocalDate.now();
        BigDecimal rate = rebateRateService.getRateForChannelAndProduct(productCategory, channelId, date);
        return ApiResult.ok(rate);
    }
}
