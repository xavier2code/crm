package com.cy.crm.module.rebate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.util.FieldMaskUtil;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.DataScopeValidator;
import com.cy.crm.security.SecurityContext;
import com.cy.crm.module.admin.entity.UserChannel;
import com.cy.crm.module.admin.mapper.ChannelMapper;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.contract.entity.Contract;
import com.cy.crm.module.contract.mapper.ContractMapper;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.entity.PaymentNode;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.PaymentNodeMapper;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.rebate.converter.RebateConverter;
import com.cy.crm.module.rebate.dto.RebateRequest;
import com.cy.crm.module.rebate.entity.Rebate;
import com.cy.crm.module.rebate.mapper.RebateMapper;
import com.cy.crm.module.rebate.vo.RebateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final DataScopeValidator dataScopeValidator;

    private final ProjectMapper projectMapper;
    private final OpportunityMapper opportunityMapper;
    private final CustomerMapper customerMapper;
    private final UserChannelMapper userChannelMapper;
    private final ContractMapper contractMapper;
    private final PaymentNodeMapper paymentNodeMapper;

    public static final int TYPE_PERFORMANCE = 1;
    public static final int TYPE_PAYMENT = 2;
    public static final int TYPE_SERVICE = 3;

    public static final int CONFIRM_PENDING = 1;
    public static final int CONFIRM_CONFIRMED = 2;

    public static final int PAYMENT_UNPAID = 1;
    public static final int PAYMENT_PAID = 2;

    public static final int CONTRACT_STATUS_SIGNED = 2;
    public static final int PROJECT_STATUS_COMPLETED = 2;
    public static final int PAYMENT_STATUS_RECEIVED = 2;

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

        // IDOR protection: validate access to this rebate
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateChannelAccess(currentUserId, rebate.getChannelId(), currentDataScope);

        rebateConverter.updateEntityFromRequest(request, rebate);
        rebateMapper.updateById(rebate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateConfirmStatus(Long id, Integer confirmStatus) {
        Rebate rebate = rebateMapper.selectById(id);
        if (rebate == null) {
            throw BusinessException.resourceNotFound("返利记录");
        }

        // IDOR protection: validate access to this rebate
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateChannelAccess(currentUserId, rebate.getChannelId(), currentDataScope);

        rebate.setConfirmStatus(confirmStatus);
        rebateMapper.updateById(rebate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentStatus(Long id, Integer paymentStatus) {
        Rebate rebate = rebateMapper.selectById(id);
        if (rebate == null) {
            throw BusinessException.resourceNotFound("返利记录");
        }

        // IDOR protection: validate access to this rebate
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateChannelAccess(currentUserId, rebate.getChannelId(), currentDataScope);

        rebate.setPaymentStatus(paymentStatus);
        rebateMapper.updateById(rebate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void generateContractRebate(Long contractId, Long channelId, BigDecimal amount, String productCategory) {
        generatePerformanceRebate(contractId, channelId, amount, productCategory);
    }

    /**
     * 扫描已签合同，为尚未生成业绩完成返利的合同补录返利记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public int generateMissingPerformanceRebates() {
        List<Contract> signedContracts = contractMapper.selectList(
                new QueryWrapper<Contract>().eq("status", CONTRACT_STATUS_SIGNED)
        );

        int generated = 0;
        for (Contract contract : signedContracts) {
            if (performanceRebateExists(contract.getId())) {
                continue;
            }

            RebateContext ctx = buildRebateContext(contract);
            if (ctx == null || ctx.channelId == null) {
                continue;
            }

            BigDecimal amount = contract.getAmount() != null ? contract.getAmount() : BigDecimal.ZERO;
            generatePerformanceRebate(contract.getId(), ctx.channelId, amount, ctx.productCategory);
            generated++;
        }
        return generated;
    }

    /**
     * 扫描已到账回款节点，生成回款返利记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public int generatePaymentRebates() {
        List<PaymentNode> receivedPayments = paymentNodeMapper.selectList(
                new QueryWrapper<PaymentNode>()
                        .eq("status", PAYMENT_STATUS_RECEIVED)
                        .isNotNull("received_date")
        );

        int generated = 0;
        for (PaymentNode paymentNode : receivedPayments) {
            if (paymentNode.getProjectId() == null || paymentRebateExists(paymentNode.getId())) {
                continue;
            }

            Contract contract = contractMapper.selectOne(
                    new QueryWrapper<Contract>().eq("project_id", paymentNode.getProjectId())
            );
            if (contract == null) {
                continue;
            }

            RebateContext ctx = buildRebateContext(contract);
            if (ctx == null || ctx.channelId == null) {
                continue;
            }

            BigDecimal amount = paymentNode.getAmount() != null ? paymentNode.getAmount() : BigDecimal.ZERO;
            generatePaymentRebate(contract.getId(), ctx.channelId, amount, ctx.productCategory, paymentNode.getId());
            generated++;
        }
        return generated;
    }

    /**
     * 扫描服务期满 9 个月的项目，生成服务返利记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public int generateServiceRebates(LocalDate asOf) {
        LocalDateTime cutoff = asOf.minusMonths(9).atStartOfDay();
        List<Project> maturedProjects = projectMapper.selectList(
                new QueryWrapper<Project>()
                        .isNotNull("formal_at")
                        .le("formal_at", cutoff)
        );

        int generated = 0;
        for (Project project : maturedProjects) {
            Contract contract = contractMapper.selectOne(
                    new QueryWrapper<Contract>().eq("project_id", project.getId())
            );
            if (contract == null) {
                continue;
            }

            if (serviceRebateExists(contract.getId())) {
                continue;
            }

            RebateContext ctx = buildRebateContext(contract);
            if (ctx == null || ctx.channelId == null) {
                continue;
            }

            BigDecimal amount = contract.getAmount() != null ? contract.getAmount() : BigDecimal.ZERO;
            generateServiceRebate(contract.getId(), ctx.channelId, amount, ctx.productCategory);
            generated++;
        }
        return generated;
    }

    /**
     * 每月扫描实际业绩完成情况，按比例更新业绩完成返利的实发金额。
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateActualPerformanceAmounts(LocalDate asOf) {
        List<Rebate> performanceRebates = rebateMapper.selectList(
                new QueryWrapper<Rebate>().eq("rebate_type", TYPE_PERFORMANCE)
        );

        int updated = 0;
        for (Rebate rebate : performanceRebates) {
            if (rebate.getContractId() == null) {
                continue;
            }

            Contract contract = contractMapper.selectById(rebate.getContractId());
            if (contract == null) {
                continue;
            }

            Project project = projectMapper.selectById(contract.getProjectId());
            if (project == null) {
                continue;
            }

            BigDecimal newActual = calculateActualAmount(rebate.getTotalAmount(), project, asOf);
            BigDecimal currentActual = rebate.getActualAmount() != null ? rebate.getActualAmount() : BigDecimal.ZERO;
            if (newActual.compareTo(currentActual) != 0) {
                rebate.setActualAmount(newActual);
                rebateMapper.updateById(rebate);
                updated++;
            }
        }
        return updated;
    }

    private void generatePerformanceRebate(Long contractId, Long channelId, BigDecimal amount, String productCategory) {
        BigDecimal rebateRate = getRebateRate(channelId, productCategory);

        Rebate rebate = new Rebate();
        rebate.setChannelId(channelId);
        rebate.setContractId(contractId);
        rebate.setProductCategory(productCategory);
        rebate.setRebateRate(rebateRate);
        rebate.setTotalAmount(amount.multiply(rebateRate));
        rebate.setActualAmount(BigDecimal.ZERO);
        rebate.setRebateType(TYPE_PERFORMANCE);
        rebate.setConfirmStatus(CONFIRM_PENDING);
        rebate.setPaymentStatus(PAYMENT_UNPAID);
        rebateMapper.insert(rebate);
    }

    private void generatePaymentRebate(Long contractId, Long channelId, BigDecimal amount, String productCategory, Long paymentNodeId) {
        BigDecimal rebateRate = getRebateRate(channelId, productCategory);

        Rebate rebate = new Rebate();
        rebate.setChannelId(channelId);
        rebate.setContractId(contractId);
        rebate.setProductCategory(productCategory);
        rebate.setRebateRate(rebateRate);
        rebate.setTotalAmount(amount.multiply(rebateRate));
        rebate.setActualAmount(amount.multiply(rebateRate));
        rebate.setRebateType(TYPE_PAYMENT);
        rebate.setConfirmStatus(CONFIRM_PENDING);
        rebate.setPaymentStatus(PAYMENT_UNPAID);
        rebate.setPaymentNodeId(paymentNodeId);
        rebateMapper.insert(rebate);
    }

    private void generateServiceRebate(Long contractId, Long channelId, BigDecimal amount, String productCategory) {
        BigDecimal rebateRate = getRebateRate(channelId, productCategory);

        Rebate rebate = new Rebate();
        rebate.setChannelId(channelId);
        rebate.setContractId(contractId);
        rebate.setProductCategory(productCategory);
        rebate.setRebateRate(rebateRate);
        rebate.setTotalAmount(amount.multiply(rebateRate));
        rebate.setActualAmount(amount.multiply(rebateRate));
        rebate.setRebateType(TYPE_SERVICE);
        rebate.setConfirmStatus(CONFIRM_PENDING);
        rebate.setPaymentStatus(PAYMENT_UNPAID);
        rebateMapper.insert(rebate);
    }

    private boolean performanceRebateExists(Long contractId) {
        return rebateMapper.selectCount(
                new QueryWrapper<Rebate>()
                        .eq("contract_id", contractId)
                        .eq("rebate_type", TYPE_PERFORMANCE)
        ) > 0;
    }

    private boolean paymentRebateExists(Long paymentNodeId) {
        return rebateMapper.selectCount(
                new QueryWrapper<Rebate>()
                        .eq("payment_node_id", paymentNodeId)
                        .eq("rebate_type", TYPE_PAYMENT)
        ) > 0;
    }

    private boolean serviceRebateExists(Long contractId) {
        return rebateMapper.selectCount(
                new QueryWrapper<Rebate>()
                        .eq("contract_id", contractId)
                        .eq("rebate_type", TYPE_SERVICE)
        ) > 0;
    }

    private RebateContext buildRebateContext(Contract contract) {
        Project project = projectMapper.selectById(contract.getProjectId());
        if (project == null) {
            return null;
        }

        Long channelId = resolveChannelId(project);
        if (channelId == null) {
            return null;
        }

        return new RebateContext(channelId, project.getProductCategory());
    }

    private Long resolveChannelId(Project project) {
        if (project.getOpportunityId() == null) {
            return null;
        }
        Opportunity opportunity = opportunityMapper.selectById(project.getOpportunityId());
        if (opportunity == null) {
            return null;
        }
        Customer customer = customerMapper.selectById(opportunity.getCustomerId());
        if (customer == null || customer.getOwnerUserId() == null) {
            return null;
        }
        List<UserChannel> assignments = userChannelMapper.selectList(
                new QueryWrapper<UserChannel>()
                        .eq("user_id", customer.getOwnerUserId())
                        .orderByAsc("assigned_at")
        );
        return assignments.isEmpty() ? null : assignments.get(0).getChannelId();
    }

    private BigDecimal calculateActualAmount(BigDecimal totalAmount, Project project, LocalDate asOf) {
        if (totalAmount == null || BigDecimal.ZERO.compareTo(totalAmount) == 0) {
            return BigDecimal.ZERO;
        }

        if (project.getStatus() != null && project.getStatus() == PROJECT_STATUS_COMPLETED) {
            return totalAmount;
        }

        if (project.getFormalAt() == null) {
            return BigDecimal.ZERO;
        }

        long months = ChronoUnit.MONTHS.between(project.getFormalAt().toLocalDate(), asOf) + 1;
        if (months < 1) {
            return BigDecimal.ZERO;
        }
        if (months >= 9) {
            return totalAmount;
        }
        return totalAmount.multiply(BigDecimal.valueOf(months))
                .divide(BigDecimal.valueOf(9), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getRebateRate(Long channelId, String productCategory) {
        // 优先使用 RebateRateService 获取配置的返利率
        BigDecimal rate = rebateRateService.getRateForChannelAndProduct(productCategory, channelId, LocalDate.now());
        if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
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

    private record RebateContext(Long channelId, String productCategory) {
    }
}
