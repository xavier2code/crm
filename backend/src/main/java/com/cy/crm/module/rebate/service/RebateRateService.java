package com.cy.crm.module.rebate.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.module.rebate.entity.RebateRate;
import com.cy.crm.module.rebate.mapper.RebateRateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RebateRateService extends ServiceImpl<RebateRateMapper, RebateRate> {

    private final RebateRateMapper rebateRateMapper;

    /**
     * 获取指定渠道和产品的返利率
     * 优先使用渠道专属配置，否则使用默认配置
     */
    public BigDecimal getRateForChannelAndProduct(String productCategory, Long channelId, LocalDate effectiveDate) {
        BigDecimal rate = rebateRateMapper.findRateForChannelAndProduct(productCategory, channelId, effectiveDate);
        return rate != null ? rate : BigDecimal.ZERO;
    }

    /**
     * 获取渠道的所有返利率配置
     */
    public List<RebateRate> getRatesForChannel(Long channelId) {
        return rebateRateMapper.findByChannelId(channelId);
    }

    /**
     * 创建或更新返利率配置
     */
    public RebateRate saveRebateRate(RebateRate rebateRate) {
        rebateRate.setCreatedAt(java.time.LocalDateTime.now());
        rebateRate.setUpdatedAt(java.time.LocalDateTime.now());
        rebateRate.setIsDeleted(0);
        saveOrUpdate(rebateRate);
        return rebateRate;
    }

    /**
     * 删除返利率配置（软删除）
     */
    public void deleteRebateRate(Long id) {
        RebateRate rate = getById(id);
        if (rate != null) {
            rate.setIsDeleted(1);
            rate.setUpdatedAt(java.time.LocalDateTime.now());
            updateById(rate);
        }
    }
}
