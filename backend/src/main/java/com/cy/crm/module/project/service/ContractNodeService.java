package com.cy.crm.module.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.project.converter.ContractNodeConverter;
import com.cy.crm.module.project.dto.ContractNodeRequest;
import com.cy.crm.module.project.entity.ContractNode;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ContractNodeMapper;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.notification.service.NotificationService;
import com.cy.crm.module.rebate.service.RebateService;
import com.cy.crm.module.admin.entity.UserChannel;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractNodeService extends ServiceImpl<ContractNodeMapper, ContractNode> {

    private final ContractNodeMapper contractNodeMapper;
    private final ProjectMapper projectMapper;
    private final NotificationService notificationService;
    private final RebateService rebateService;
    private final ContractNodeConverter contractNodeConverter;
    private final UserChannelMapper userChannelMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityMapper opportunityMapper;

    /**
     * 获取项目合同节点
     */
    public ContractNode getByProjectId(Long projectId) {
        return contractNodeMapper.selectOne(
                new QueryWrapper<ContractNode>().eq("project_id", projectId)
        );
    }

    /**
     * 创建或更新合同节点
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveContractNode(Long projectId, ContractNodeRequest request) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }

        ContractNode node = contractNodeMapper.selectOne(
                new QueryWrapper<ContractNode>().eq("project_id", projectId)
        );

        ContractNode oldNode = null;
        if (node != null) {
            oldNode = cloneNode(node);
            contractNodeConverter.updateEntityFromRequest(request, node);
            node.setUpdatedAt(java.time.LocalDateTime.now());
            contractNodeMapper.updateById(node);
        } else {
            node = contractNodeConverter.requestToEntity(request);
            node.setProjectId(projectId);
            node.setCreatedAt(java.time.LocalDateTime.now());
            node.setUpdatedAt(java.time.LocalDateTime.now());
            contractNodeMapper.insert(node);
        }

        // 检测关键事件并发送通知
        detectAndNotifyKeyEvents(project, oldNode, node);

        // 如果是新创建的合同节点，且项目有商机信息，触发返利生成
        if (oldNode == null && project.getOpportunityId() != null) {
            triggerRebateGeneration(project, node);
        }

        return node.getId();
    }

    /**
     * 检测关键事件并发送通知
     */
    private void detectAndNotifyKeyEvents(Project project, ContractNode oldNode, ContractNode newNode) {
        if (oldNode == null) return;

        // 检测合同审批通过
        if (isDateChanged(oldNode.getApproveDate(), newNode.getApproveDate())) {
            notifyContractApproved(project, newNode);
        }

        // 检测发票开具
        if (isDateChanged(oldNode.getInvoiceDate(), newNode.getInvoiceDate())) {
            notifyInvoiceIssued(project, newNode);
        }

        // 检测款项收到
        if (isDateChanged(oldNode.getReceivedDate(), newNode.getReceivedDate())) {
            notifyPaymentReceived(project, newNode);
        }
    }

    private boolean isDateChanged(LocalDate oldValue, LocalDate newValue) {
        return (oldValue == null) && (newValue != null);
    }

    private void notifyContractApproved(Project project, ContractNode node) {
        if (project.getOwnerBdId() != null) {
            notificationService.createNotification(
                    project.getOwnerBdId(),
                    "合同审批通过",
                    String.format("项目 %s 的合同已于 %s 审批通过。", project.getName(), node.getApproveDate()),
                    "CONTRACT_APPROVED",
                    project.getId()
            );
        }
    }

    private void notifyInvoiceIssued(Project project, ContractNode node) {
        if (project.getOwnerBdId() != null) {
            notificationService.createNotification(
                    project.getOwnerBdId(),
                    "发票已开具",
                    String.format("项目 %s 的发票已于 %s 开具。", project.getName(), node.getInvoiceDate()),
                    "INVOICE_ISSUED",
                    project.getId()
            );
        }
    }

    private void notifyPaymentReceived(Project project, ContractNode node) {
        if (project.getOwnerBdId() != null) {
            notificationService.createNotification(
                    project.getOwnerBdId(),
                    "款项已到账",
                    String.format("项目 %s 的款项已于 %s 到账。", project.getName(), node.getReceivedDate()),
                    "PAYMENT_RECEIVED",
                    project.getId()
            );
        }
    }

    /**
     * 触发返利生成
     * 合同签订后自动生成业绩完成返利记录
     */
    private void triggerRebateGeneration(Project project, ContractNode node) {
        if (node.getApproveDate() == null) return;

        // 获取项目所属渠道（从商机获取）
        if (project.getOpportunityId() == null) return;

        try {
            // 假设从Project或Opportunity获取channelId和productCategory
            // 这里使用默认值，实际应从项目或商机中获取
            Long channelId = getChannelIdFromProject(project);
            String productCategory = getProductCategoryFromProject(project);
            BigDecimal amount = project.getAmount() != null ? project.getAmount() : BigDecimal.ZERO;

            rebateService.generateContractRebate(
                    project.getId(),  // 使用projectId作为contractId
                    channelId,
                    amount,
                    productCategory
            );

            log.info("项目 {} 触发返利生成，金额：{}", project.getId(), amount);
        } catch (Exception e) {
            log.error("生成返利失败：{}", e.getMessage());
            throw new BusinessException(6005, "合同节点保存失败：返利生成失败", e);
        }
    }

    private Long getChannelIdFromProject(Project project) {
        if (project.getOpportunityId() == null) {
            return null;
        }
        Opportunity opportunity = opportunityMapper.selectById(project.getOpportunityId());
        if (opportunity == null) {
            return null;
        }
        Customer customer = customerMapper.selectById(opportunity.getCustomerId());
        if (customer == null) {
            return null;
        }
        Long ownerUserId = customer.getOwnerUserId();
        if (ownerUserId == null) {
            return null;
        }
        // 取客户跟进人作为渠道 BD 时的归属渠道（按创建时间最早的）
        List<UserChannel> assignments = userChannelMapper.selectList(
                new QueryWrapper<UserChannel>()
                        .eq("user_id", ownerUserId)
                        .orderByAsc("assigned_at")
        );
        return assignments.isEmpty() ? null : assignments.get(0).getChannelId();
    }

    private String getProductCategoryFromProject(Project project) {
        return project.getProductCategory();
    }

    private ContractNode cloneNode(ContractNode source) {
        ContractNode target = new ContractNode();
        target.setId(source.getId());
        target.setProjectId(source.getProjectId());
        target.setDraftDate(source.getDraftDate());
        target.setReviewDept(source.getReviewDept());
        target.setApproveDate(source.getApproveDate());
        target.setOriginalArchived(source.getOriginalArchived());
        target.setPaymentMethod(source.getPaymentMethod());
        target.setPaymentRatio(source.getPaymentRatio());
        target.setPaymentTerms(source.getPaymentTerms());
        target.setPaymentNodes(source.getPaymentNodes());
        target.setHasWarranty(source.getHasWarranty());
        target.setWarrantyAmount(source.getWarrantyAmount());
        target.setAcceptanceDept(source.getAcceptanceDept());
        target.setHasSettlementAudit(source.getHasSettlementAudit());
        target.setInvoiceDate(source.getInvoiceDate());
        target.setPaymentVoucherDept(source.getPaymentVoucherDept());
        target.setReceivedDate(source.getReceivedDate());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
