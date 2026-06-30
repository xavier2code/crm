package com.cy.crm.module.rebate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.util.FieldMaskUtil;
import com.cy.crm.module.admin.mapper.ChannelMapper;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.rebate.converter.RebateConverter;
import com.cy.crm.module.rebate.dto.RebateRequest;
import com.cy.crm.module.rebate.entity.Rebate;
import com.cy.crm.module.rebate.mapper.RebateMapper;
import com.cy.crm.module.rebate.vo.RebateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RebateService extends ServiceImpl<RebateMapper, Rebate> {

    private final RebateMapper rebateMapper;
    private final ChannelMapper channelMapper;
    private final DictionaryService dictionaryService;
    private final RebateRateService rebateRateService;
    private final CurrentUserService currentUserService;
    private final RebateConverter rebateConverter;

    public Page<RebateVO> pageRebates(Long current, Long size, Long channelId, Integer confirmStatus, Integer paymentStatus) {
        QueryWrapper<Rebate> wrapper = new QueryWrapper<Rebate>()
                .eq(channelId != null, "channel_id", channelId)
                .eq(confirmStatus != null, "confirm_status", confirmStatus)
                .eq(paymentStatus != null, "payment_status", paymentStatus)
                .orderByDesc("created_at");
        Page<Rebate> page = rebateMapper.selectPage(new Page<>(current, size), wrapper);
        Page<RebateVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    public List<RebateVO> listByChannelId(Long channelId) {
        List<Rebate> rebates = rebateMapper.selectList(
                new QueryWrapper<Rebate>().eq("channel_id", channelId).orderByDesc("created_at")
        );
        return rebates.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createRebate(RebateRequest request) {
        Rebate rebate = rebateConverter.requestToEntity(request);
        rebateMapper.insert(rebate);

        return rebate.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRebate(Long id, RebateRequest request) {
        Rebate rebate = rebateMapper.selectById(id);
        if (rebate == null) {
            throw BusinessException.resourceNotFound("返利记录");
        }

        rebateConverter.updateEntityFromRequest(request, rebate);
        rebateMapper.updateById(rebate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateConfirmStatus(Long id, Integer confirmStatus) {
        Rebate rebate = rebateMapper.selectById(id);
        if (rebate == null) {
            throw BusinessException.resourceNotFound("返利记录");
        }
        rebate.setConfirmStatus(confirmStatus);
        rebateMapper.updateById(rebate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentStatus(Long id, Integer paymentStatus) {
        Rebate rebate = rebateMapper.selectById(id);
        if (rebate == null) {
            throw BusinessException.resourceNotFound("返利记录");
        }
        rebate.setPaymentStatus(paymentStatus);
        rebateMapper.updateById(rebate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void generateContractRebate(Long contractId, Long channelId, BigDecimal amount, String productCategory) {
        BigDecimal rebateRate = getRebateRate(channelId, productCategory);

        Rebate rebate = new Rebate();
        rebate.setChannelId(channelId);
        rebate.setContractId(contractId);
        rebate.setProductCategory(productCategory);
        rebate.setRebateRate(rebateRate);
        rebate.setTotalAmount(amount.multiply(rebateRate));
        rebate.setActualAmount(BigDecimal.ZERO);
        rebate.setRebateType(1);
        rebate.setConfirmStatus(1);
        rebate.setPaymentStatus(1);
        rebateMapper.insert(rebate);
    }

    private BigDecimal getRebateRate(Long channelId, String productCategory) {
        // 优先使用 RebateRateService 获取配置的返利率
        BigDecimal rate = rebateRateService.getRateForChannelAndProduct(productCategory, channelId, LocalDate.now());
        if (rate.compareTo(BigDecimal.ZERO) > 0) {
            return rate;
        }
        // 回退到字典配置
        String remark = dictionaryService.getDictionaryRemark("rebate_rate", productCategory);
        if (remark != null) {
            return new BigDecimal(remark);
        }
        return new BigDecimal("0.05");
    }

    private RebateVO toVO(Rebate rebate) {
        RebateVO vo = rebateConverter.entityToVO(rebate);

        com.cy.crm.module.admin.entity.Channel channel = channelMapper.selectById(rebate.getChannelId());
        if (channel != null) {
            vo.setChannelName(channel.getName());
        }

        // 字段级权限：返利金额仅渠道负责人、CYBD、管理员可见
        if (!FieldMaskUtil.canViewRebateAmount(currentUserService)) {
            vo.setTotalAmount(FieldMaskUtil.maskAmount(vo.getTotalAmount()));
            vo.setActualAmount(FieldMaskUtil.maskAmount(vo.getActualAmount()));
        }

        vo.setConfirmStatusName(getConfirmStatusName(rebate.getConfirmStatus()));
        vo.setPaymentStatusName(getPaymentStatusName(rebate.getPaymentStatus()));
        vo.setRebateTypeName(getRebateTypeName(rebate.getRebateType()));

        return vo;
    }

    private String getConfirmStatusName(Integer status) {
        return switch (status) {
            case 1 -> "未确认";
            case 2 -> "已确认";
            default -> "未知";
        };
    }

    private String getPaymentStatusName(Integer status) {
        return switch (status) {
            case 1 -> "未付款";
            case 2 -> "已付款";
            default -> "未知";
        };
    }

    private String getRebateTypeName(Integer type) {
        return switch (type) {
            case 1 -> "业绩完成返利";
            case 2 -> "回款返利";
            case 3 -> "服务返利";
            default -> "未知";
        };
    }
}
