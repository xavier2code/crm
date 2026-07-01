package com.cy.crm.module.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.util.FieldMaskUtil;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.DataScopeValidator;
import com.cy.crm.security.SecurityContext;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.contract.converter.ContractConverter;
import com.cy.crm.module.contract.dto.ContractRequest;
import com.cy.crm.module.contract.entity.Contract;
import com.cy.crm.module.contract.mapper.ContractMapper;
import com.cy.crm.module.contract.vo.ContractVO;
import com.cy.crm.module.notification.service.NotificationService;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.rebate.service.RebateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService extends ServiceImpl<ContractMapper, Contract> {

    private final ContractMapper contractMapper;
    private final ProjectMapper projectMapper;
    private final OpportunityMapper opportunityMapper;
    private final CustomerMapper customerMapper;
    private final UserService userService;
    private final RebateService rebateService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final DataScopeValidator dataScopeValidator;

    private final ContractConverter contractConverter;

    // 合同状态常量
    public static final int STATUS_PENDING = 1;    // 待签
    public static final int STATUS_SIGNED = 2;    // 已签
    public static final int STATUS_OPENED = 3;    // 已开通
    public static final int STATUS_SERVICE = 4;   // 服务中
    public static final int STATUS_EXPIRED = 5;   // 已到期

    public Page<ContractVO> pageContracts(Long current, Long size, Integer status) {
        QueryWrapper<Contract> wrapper = new QueryWrapper<Contract>()
                .eq(status != null, "status", status)
                .orderByDesc("created_at");
        Page<Contract> page = contractMapper.selectPage(new Page<>(current, size), wrapper);
        Page<ContractVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    public ContractVO getContractById(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            return null;
        }

        // IDOR protection: validate access to this contract
        Project project = projectMapper.selectById(contract.getProjectId());
        if (project == null) {
            // 项目不存在或当前用户无该项目访问权限，拒绝访问合同
            return null;
        }

        Opportunity opportunity = opportunityMapper.selectById(project.getOpportunityId());
        if (opportunity != null) {
            Customer customer = customerMapper.selectById(opportunity.getCustomerId());
            if (customer != null) {
                Long currentUserId = SecurityContext.getCurrentUserId();
                DataScope currentDataScope = SecurityContext.getCurrentDataScope();
                dataScopeValidator.validateAccess(currentUserId, project.getOwnerBdId(), customer.getUnitId(), currentDataScope);
            }
        }

        return toVO(contract);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createContract(ContractRequest request) {
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }

        QueryWrapper<Contract> wrapper = new QueryWrapper<Contract>().eq("project_id", request.getProjectId());
        if (contractMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(6001, "该项目已有合同");
        }

        Contract contract = contractConverter.requestToEntity(request);
        contract.setStatus(STATUS_PENDING);
        try {
            contractMapper.insert(contract);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(6001, "该项目已有合同");
        }

        return contract.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateContract(Long id, ContractRequest request) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BusinessException.resourceNotFound("合同");
        }

        // IDOR protection: validate access to this contract
        Project project = projectMapper.selectById(contract.getProjectId());
        if (project == null) {
            // 项目不存在或当前用户无该项目访问权限，拒绝更新合同
            throw BusinessException.dataScopeDenied();
        }

        Opportunity opportunity = opportunityMapper.selectById(project.getOpportunityId());
        if (opportunity != null) {
            Customer customer = customerMapper.selectById(opportunity.getCustomerId());
            if (customer != null) {
                Long currentUserId = SecurityContext.getCurrentUserId();
                DataScope currentDataScope = SecurityContext.getCurrentDataScope();
                dataScopeValidator.validateAccess(currentUserId, project.getOwnerBdId(), customer.getUnitId(), currentDataScope);
            }
        }

        contractConverter.updateEntityFromRequest(request, contract);
        contractMapper.updateById(contract);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BusinessException.resourceNotFound("合同");
        }

        int oldStatus = contract.getStatus();
        contract.setStatus(status);
        contractMapper.updateById(contract);

        // 合同签订后触发返利生成
        if (oldStatus != STATUS_SIGNED && status == STATUS_SIGNED) {
            triggerRebateGeneration(contract);
        }

        // 状态变更通知
        notifyStatusChange(contract, oldStatus, status);
    }

    /**
     * 触发返利生成
     */
    private void triggerRebateGeneration(Contract contract) {
        try {
            Project project = projectMapper.selectById(contract.getProjectId());
            if (project == null) {
                log.warn("合同 {} 关联的项目不存在", contract.getId());
                return;
            }

            Long channelId = getChannelIdFromProject(project);
            String productCategory = getProductCategoryFromProject(project);
            BigDecimal amount = contract.getAmount() != null ? contract.getAmount() : BigDecimal.ZERO;

            rebateService.generateContractRebate(
                    contract.getId(),
                    channelId,
                    amount,
                    productCategory
            );

            log.info("合同 {} 签订，触发返利生成，金额：{}", contract.getId(), amount);

            // 通知相关人员
            if (project.getOwnerBdId() != null) {
                notificationService.createNotification(
                        project.getOwnerBdId(),
                        "合同已签订",
                        String.format("项目 %s 的合同已签订，金额：%s", project.getName(), amount),
                        "CONTRACT_SIGNED",
                        contract.getId()
                );
            }
        } catch (Exception e) {
            log.error("合同 {} 生成返利失败：{}", contract.getId(), e.getMessage());
            throw new BusinessException(6005, "合同状态更新失败：返利生成失败", e);
        }
    }

    private Long getChannelIdFromProject(Project project) {
        // TODO: 从项目关联的商机获取渠道ID
        return 1L;
    }

    private String getProductCategoryFromProject(Project project) {
        // TODO: 从项目获取产品类别
        return "DEFAULT";
    }

    private void notifyStatusChange(Contract contract, int oldStatus, int newStatus) {
        Project project = projectMapper.selectById(contract.getProjectId());
        if (project == null || project.getOwnerBdId() == null) return;

        String message = String.format("合同状态从 %s 变更为 %s",
                getStatusName(oldStatus), getStatusName(newStatus));

        notificationService.createNotification(
                project.getOwnerBdId(),
                "合同状态变更",
                message,
                "CONTRACT_STATUS_CHANGE",
                contract.getId()
        );
    }

    private ContractVO toVO(Contract contract) {
        ContractVO vo = contractConverter.entityToVO(contract);

        Project project = projectMapper.selectById(contract.getProjectId());
        if (project != null) {
            vo.setProjectName(project.getName());
        }

        // 字段级权限：BD 角色对合同金额脱敏
        if (FieldMaskUtil.isBdOnly(currentUserService)) {
            vo.setAmount(FieldMaskUtil.maskAmount(vo.getAmount()));
        }

        vo.setStatusName(getStatusName(contract.getStatus()));

        return vo;
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case STATUS_PENDING -> "待签";
            case STATUS_SIGNED -> "已签";
            case STATUS_OPENED -> "已开通";
            case STATUS_SERVICE -> "服务中";
            case STATUS_EXPIRED -> "已到期";
            default -> "未知";
        };
    }
}
