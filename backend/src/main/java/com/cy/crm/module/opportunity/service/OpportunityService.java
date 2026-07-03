package com.cy.crm.module.opportunity.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.annotation.AuditLog;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.DataScopeValidator;
import com.cy.crm.security.SecurityContext;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.opportunity.converter.OpportunityConverter;
import com.cy.crm.module.opportunity.dto.OpportunityApproveRequest;
import com.cy.crm.module.opportunity.dto.OpportunityRequest;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.entity.OpportunityApprovalLog;
import com.cy.crm.module.opportunity.mapper.OpportunityApprovalLogMapper;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.opportunity.vo.OpportunityDetailVO;
import com.cy.crm.module.opportunity.vo.OpportunityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityService extends ServiceImpl<OpportunityMapper, Opportunity> {

    private final OpportunityMapper opportunityMapper;
    private final OpportunityApprovalLogMapper approvalLogMapper;
    private final CustomerMapper customerMapper;
    private final UserService userService;
    private final DictionaryService dictionaryService;
    private final OpportunityConverter opportunityConverter;
    private final DataScopeValidator dataScopeValidator;

    public static final int STATUS_DRAFT = 1;
    public static final int STATUS_PENDING = 2;
    public static final int STATUS_ACTIVE = 3;
    public static final int STATUS_FAILED = 4;
    public static final int STATUS_EXPIRED = 5;
    public static final int STATUS_CONVERTED = 6;

    // 商机阶段常量
    public static final String STAGE_DRAFT = "DRAFT";
    public static final String STAGE_IN_PROGRESS = "IN_PROGRESS";    // 商机中
    public static final String STAGE_IN_PROJECT = "IN_PROJECT";      // 项目中
    public static final String STAGE_SERVICE = "SERVICE";            // 服务中
    public static final String STAGE_COMPLETED = "COMPLETED";        // 完成

    private static final int ACTION_SUBMIT = 1;
    private static final int ACTION_APPROVE = 2;
    private static final int ACTION_REJECT = 3;

    public Page<OpportunityVO> pageOpportunities(Long current, Long size, Integer status, Long userId, List<Long> roleIds) {
        Page<Opportunity> page = opportunityMapper.selectPage(
                new Page<>(current, size),
                new QueryWrapper<Opportunity>()
                        .eq(status != null, "status", status)
                        .eq(userId != null && hasOnlyBDRole(roleIds), "submitted_by", userId)
                        .orderByDesc("created_at")
        );
        Page<OpportunityVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    public OpportunityDetailVO getOpportunityById(Long id) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            return null;
        }

        // IDOR protection: validate access based on submitted_by
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateCreatorAccess(currentUserId, opportunity.getSubmittedBy(), currentDataScope);

        OpportunityDetailVO vo = new OpportunityDetailVO();
        OpportunityVO baseVO = toVO(opportunity);
        vo.setId(baseVO.getId());
        vo.setCustomerId(baseVO.getCustomerId());
        vo.setCustomerName(baseVO.getCustomerName());
        vo.setBusinessDomain(baseVO.getBusinessDomain());
        vo.setBusinessDomainName(baseVO.getBusinessDomainName());
        vo.setProjectType(baseVO.getProjectType());
        vo.setProjectTypeName(baseVO.getProjectTypeName());
        vo.setAmount(baseVO.getAmount());
        vo.setStatus(baseVO.getStatus());
        vo.setStatusName(baseVO.getStatusName());
        vo.setSubmitCount(baseVO.getSubmitCount());
        vo.setLastFollowUpAt(baseVO.getLastFollowUpAt());
        vo.setEffectiveAt(baseVO.getEffectiveAt());
        vo.setExpiredAt(baseVO.getExpiredAt());
        vo.setCoolingUntil(baseVO.getCoolingUntil());
        vo.setSubmittedBy(baseVO.getSubmittedBy());
        vo.setSubmittedByName(baseVO.getSubmittedByName());
        vo.setApprovedBy(baseVO.getApprovedBy());
        vo.setApprovedByName(baseVO.getApprovedByName());
        vo.setApprovedAt(baseVO.getApprovedAt());
        vo.setRejectReason(baseVO.getRejectReason());
        vo.setCreatedAt(baseVO.getCreatedAt());
        vo.setEditable(baseVO.getEditable());
        vo.setSubmittable(baseVO.getSubmittable());
        vo.setApprovable(baseVO.getApprovable());

        Customer customer = customerMapper.selectById(opportunity.getCustomerId());
        if (customer != null) {
            vo.setUnitId(customer.getUnitId());
            vo.setUnitName(dictionaryService.getUnitName(customer.getUnitId()));
            vo.setPoliceType(customer.getPoliceType());
            vo.setPoliceTypeName(dictionaryService.getDictionaryName("police_type", customer.getPoliceType()));
        }

        List<OpportunityApprovalLog> logs = approvalLogMapper.selectList(
                new QueryWrapper<OpportunityApprovalLog>()
                        .eq("opportunity_id", id)
                        .orderByDesc("created_at")
        );
        vo.setApprovalLogs(logs.stream().map(this::toApprovalLogVO).collect(Collectors.toList()));

        return vo;
    }

    @AuditLog("创建报备")
    @Transactional(rollbackFor = Exception.class)
    public Long createOpportunity(OpportunityRequest request, Long userId) {
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw BusinessException.customerNotFound();
        }

        Opportunity opportunity = opportunityConverter.requestToEntity(request);
        opportunity.setStatus(STATUS_DRAFT);
        opportunity.setSubmitCount(0);
        opportunity.setSubmittedBy(userId);
        opportunityMapper.insert(opportunity);

        return opportunity.getId();
    }

    @AuditLog("更新报备")
    @Transactional(rollbackFor = Exception.class)
    public void updateOpportunity(Long id, OpportunityRequest request) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw BusinessException.resourceNotFound("报备");
        }

        // IDOR protection: validate access based on submitted_by
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateCreatorAccess(currentUserId, opportunity.getSubmittedBy(), currentDataScope);

        if (opportunity.getStatus() != STATUS_DRAFT && opportunity.getStatus() != STATUS_FAILED) {
            throw new BusinessException(4008, "只有草稿或报备失败的记录可以编辑");
        }

        opportunityConverter.updateEntityFromRequest(request, opportunity);
        opportunityMapper.updateById(opportunity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitOpportunity(Long id, Long userId) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw BusinessException.resourceNotFound("报备");
        }
        // submit 端点仅用于首次提交（草稿 -> 审批中）。
        // 报备失败/报备失效后的重提请走 resubmitOpportunity，便于冷却期校验和审计日志区分。
        if (opportunity.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(4009, "只有草稿状态的记录可以提交，报备失败/失效请使用重提");
        }

        checkProtection(opportunity.getCustomerId(), opportunity.getBusinessDomain(), id);

        opportunity.setStatus(STATUS_PENDING);
        opportunity.setSubmitCount(opportunity.getSubmitCount() + 1);
        opportunityMapper.updateById(opportunity);

        ApprovalLogBuilder logBuilder = new ApprovalLogBuilder()
                .opportunityId(id)
                .action(ACTION_SUBMIT)
                .operatorId(userId)
                .comment("提交审批");
        approvalLogMapper.insert(logBuilder.build());

        notifyApprovalDeadline(opportunity);
    }

    /**
     * 重提报备。
     * 支持两种来源：
     * - 报备失败 (FAILED, 4)：驳回后 BD 编辑后再次提交，submit_count 计入统计
     * - 报备失效 (EXPIRED, 5)：30 天未跟进导致失效，submit_count <= 1 时允许一次恢复机会
     *
     * 锁定 1 个月规则（与开发文档 3.4 / 18.1 一致）：
     * - submit_count >= 2 时不允许重提，提示"已用完恢复机会，需等待 1 个月冷却期"
     * - cooling_until 未到期时不允许重提，提示剩余天数
     */
    @Transactional(rollbackFor = Exception.class)
    public void resubmitOpportunity(Long id, Long userId) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw BusinessException.resourceNotFound("报备");
        }
        if (opportunity.getStatus() != STATUS_FAILED && opportunity.getStatus() != STATUS_EXPIRED) {
            throw new BusinessException(4009, "只有报备失败或报备失效的记录可以重提");
        }

        // IDOR 校验：仅原提交人可重提
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateCreatorAccess(currentUserId, opportunity.getSubmittedBy(), currentDataScope);

        // submit_count 上限：每个报备仅 1 次恢复机会
        int submitCount = opportunity.getSubmitCount() == null ? 0 : opportunity.getSubmitCount();
        if (submitCount >= 2) {
            throw new BusinessException(4002, "已用完恢复机会，需等待 1 个月冷却期");
        }

        // 冷却期校验：cooling_until 未到期则拒绝
        if (opportunity.getCoolingUntil() != null &&
            opportunity.getCoolingUntil().isAfter(LocalDateTime.now())) {
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now(), opportunity.getCoolingUntil()
            );
            throw new BusinessException(4004,
                String.format("该报备仍在冷却期内，请 %d 天后再试", daysRemaining)
            );
        }

        // 报备保护：同客户同业务域下不能有其它审批中/生效中的报备
        checkProtection(opportunity.getCustomerId(), opportunity.getBusinessDomain(), id);

        opportunity.setStatus(STATUS_PENDING);
        opportunity.setSubmitCount(submitCount + 1);
        opportunity.setCoolingUntil(null);
        opportunityMapper.updateById(opportunity);

        String comment = opportunity.getStatus() == STATUS_FAILED
            ? String.format("驳回后重提 (第 %d 次)", submitCount + 1)
            : String.format("失效后重提 (第 %d 次)", submitCount + 1);
        ApprovalLogBuilder logBuilder = new ApprovalLogBuilder()
                .opportunityId(id)
                .action(ACTION_SUBMIT)
                .operatorId(userId)
                .comment(comment);
        approvalLogMapper.insert(logBuilder.build());

        notifyApprovalDeadline(opportunity);
    }

    @AuditLog("审批报备")
    @Transactional(rollbackFor = Exception.class)
    public void approveOpportunity(Long id, OpportunityApproveRequest request, Long approverId) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw BusinessException.resourceNotFound("报备");
        }
        if (opportunity.getStatus() != STATUS_PENDING) {
            throw BusinessException.opportunityNotInApproval();
        }

        if (request.getAction() == ACTION_APPROVE) {
            opportunity.setStatus(STATUS_ACTIVE);
            opportunity.setStage(STAGE_IN_PROGRESS);  // 审批通过 → 商机中
            opportunity.setApprovedBy(approverId);
            opportunity.setApprovedAt(LocalDateTime.now());
            opportunity.setEffectiveAt(LocalDateTime.now());
        } else if (request.getAction() == ACTION_REJECT) {
            if (request.getComment() == null || request.getComment().length() < 5) {
                throw BusinessException.paramError("驳回原因不能少于5个字");
            }
            opportunity.setStatus(STATUS_FAILED);
            opportunity.setApprovedBy(approverId);
            opportunity.setApprovedAt(LocalDateTime.now());
            opportunity.setRejectReason(request.getComment());
        } else {
            throw BusinessException.paramError("无效的操作类型");
        }

        opportunityMapper.updateById(opportunity);

        ApprovalLogBuilder logBuilder = new ApprovalLogBuilder()
                .opportunityId(id)
                .action(request.getAction())
                .operatorId(approverId)
                .comment(request.getComment());
        approvalLogMapper.insert(logBuilder.build());
    }

    @AuditLog("删除报备")
    @Transactional(rollbackFor = Exception.class)
    public void deleteOpportunity(Long id) {
        Opportunity opportunity = opportunityMapper.selectById(id);
        if (opportunity == null) {
            throw BusinessException.resourceNotFound("报备");
        }

        // IDOR protection: validate access based on submitted_by
        Long currentUserId = SecurityContext.getCurrentUserId();
        DataScope currentDataScope = SecurityContext.getCurrentDataScope();
        dataScopeValidator.validateCreatorAccess(currentUserId, opportunity.getSubmittedBy(), currentDataScope);

        if (opportunity.getStatus() != STATUS_DRAFT) {
            throw new BusinessException(4011, "只有草稿状态的记录可以删除");
        }
        opportunityMapper.deleteById(id);
    }

    private void checkProtection(Long customerId, String businessDomain, Long excludeId) {
        QueryWrapper<Opportunity> wrapper = new QueryWrapper<Opportunity>()
                .eq("customer_id", customerId)
                .eq("business_domain", businessDomain)
                .in("status", STATUS_PENDING, STATUS_ACTIVE);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        Long count = opportunityMapper.selectCount(wrapper);
        if (count > 0) {
            throw BusinessException.opportunityExists();
        }
    }

    private void notifyApprovalDeadline(Opportunity opportunity) {
    }

    /**
     * 更新商机阶段
     * 由外部事件触发（项目创建、状态变更等）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateOpportunityStage(Long opportunityId, String newStage) {
        Opportunity opportunity = opportunityMapper.selectById(opportunityId);
        if (opportunity == null) {
            throw BusinessException.opportunityNotFound();
        }
        String oldStage = opportunity.getStage();
        opportunity.setStage(newStage);
        opportunityMapper.updateById(opportunity);
        log.info("商机 {} 阶段从 {} 更新为 {}", opportunityId, oldStage, newStage);
    }

    private boolean hasOnlyBDRole(List<Long> roleIds) {
        return roleIds != null && roleIds.size() == 1 && roleIds.get(0).equals(4L);
    }

    private OpportunityVO toVO(Opportunity opportunity) {
        OpportunityVO vo = opportunityConverter.entityToVO(opportunity);

        Customer customer = customerMapper.selectById(opportunity.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getName());
        }

        vo.setBusinessDomainName(dictionaryService.getDictionaryName("business_domain", opportunity.getBusinessDomain()));
        vo.setProjectTypeName(dictionaryService.getDictionaryName("project_type", String.valueOf(opportunity.getProjectType())));
        vo.setStatusName(getStatusName(opportunity.getStatus()));

        User submittedBy = userService.getUserEntityById(opportunity.getSubmittedBy());
        if (submittedBy != null) {
            vo.setSubmittedByName(submittedBy.getRealName());
        }
        if (opportunity.getApprovedBy() != null) {
            User approvedBy = userService.getUserEntityById(opportunity.getApprovedBy());
            if (approvedBy != null) {
                vo.setApprovedByName(approvedBy.getRealName());
            }
        }

        // editable/submittable：草稿 + 报备失败可编辑可提交
        boolean isDraftOrFailed = opportunity.getStatus() == STATUS_DRAFT
            || opportunity.getStatus() == STATUS_FAILED;
        vo.setEditable(isDraftOrFailed);
        vo.setSubmittable(opportunity.getStatus() == STATUS_DRAFT);
        vo.setApprovable(opportunity.getStatus() == STATUS_PENDING);

        // resubmittable：报备失败/报备失效 且 submit_count < 2 且 不在冷却期
        boolean isResubmittableSource = opportunity.getStatus() == STATUS_FAILED
            || opportunity.getStatus() == STATUS_EXPIRED;
        boolean underSubmitLimit = opportunity.getSubmitCount() == null
            || opportunity.getSubmitCount() < 2;
        boolean outOfCooling = opportunity.getCoolingUntil() == null
            || !opportunity.getCoolingUntil().isAfter(LocalDateTime.now());
        vo.setResubmittable(isResubmittableSource && underSubmitLimit && outOfCooling);

        return vo;
    }

    private OpportunityDetailVO.ApprovalLogVO toApprovalLogVO(OpportunityApprovalLog log) {
        OpportunityDetailVO.ApprovalLogVO vo = new OpportunityDetailVO.ApprovalLogVO();
        vo.setAction(log.getAction());
        vo.setActionName(getActionName(log.getAction()));
        vo.setOperatorId(log.getOperatorId());
        User operator = userService.getUserEntityById(log.getOperatorId());
        vo.setOperatorName(operator != null ? operator.getRealName() : null);
        vo.setComment(log.getComment());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PENDING -> "审批中";
            case STATUS_ACTIVE -> "生效中";
            case STATUS_FAILED -> "报备失败";
            case STATUS_EXPIRED -> "报备失效";
            case STATUS_CONVERTED -> "已转化";
            default -> "未知";
        };
    }

    private String getActionName(Integer action) {
        return switch (action) {
            case ACTION_SUBMIT -> "提交";
            case ACTION_APPROVE -> "通过";
            case ACTION_REJECT -> "驳回";
            default -> "未知";
        };
    }

    private static class ApprovalLogBuilder {
        private final OpportunityApprovalLog log = new OpportunityApprovalLog();

        public ApprovalLogBuilder opportunityId(Long opportunityId) {
            log.setOpportunityId(opportunityId);
            return this;
        }

        public ApprovalLogBuilder action(Integer action) {
            log.setAction(action);
            return this;
        }

        public ApprovalLogBuilder operatorId(Long operatorId) {
            log.setOperatorId(operatorId);
            return this;
        }

        public ApprovalLogBuilder comment(String comment) {
            log.setComment(comment);
            return this;
        }

        public OpportunityApprovalLog build() {
            log.setCreatedAt(LocalDateTime.now());
            return log;
        }
    }
}
