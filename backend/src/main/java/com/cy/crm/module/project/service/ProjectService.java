package com.cy.crm.module.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.notification.service.NotificationService;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.converter.ProjectConverter;
import com.cy.crm.module.project.dto.*;
import com.cy.crm.module.project.entity.*;
import com.cy.crm.module.project.mapper.*;
import com.cy.crm.common.annotation.AuditLog;
import com.cy.crm.module.project.vo.ProjectDetailVO;
import com.cy.crm.module.project.vo.ProjectVO;
import com.cy.crm.module.project.vo.ProjectDetailVO.PaymentNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService extends ServiceImpl<ProjectMapper, Project> {

    private final ProjectMapper projectMapper;
    private final ProjectScoreMapper scoreMapper;
    private final ProjectMilestoneMapper milestoneMapper;
    private final BiddingNodeMapper biddingNodeMapper;
    private final ContractNodeMapper contractNodeMapper;
    private final PaymentNodeMapper paymentNodeMapper;
    private final OpportunityMapper opportunityMapper;
    private final CustomerMapper customerMapper;
    private final UserService userService;
    private final DictionaryService dictionaryService;
    private final NotificationService notificationService;
    private final com.cy.crm.module.opportunity.service.OpportunityService opportunityService;
    private final ProjectConverter projectConverter;

    // 项目状态常量
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_INTERRUPTED = 3;
    public static final int STATUS_TERMINATED = 4;

    // 8维评分维度常量（与文档定义一致）
    public static final String DIMENSION_LIXIANG_RESULT = "lixiang_result";        // 立项结果 10%
    public static final String DIMENSION_BUDGET = "budget";                        // 资金预算 20%
    public static final String DIMENSION_GOV_FINANCE = "gov_finance";             // 政府财评 15%
    public static final String DIMENSION_PURCHASE_MEETING = "purchase_meeting";    // 采购方式上会 15%
    public static final String DIMENSION_KP_REACH = "kp_reach";                    // 职能KP触达 15%
    public static final String DIMENSION_PURCHASE_PROCESS = "purchase_process";    // 采购流程确认 10%
    public static final String DIMENSION_CREDIT = "credit";                        // 渠道与客户资信 10%
    public static final String DIMENSION_OPEN_INTENTION = "open_intention";        // 客户开通意愿 5%

    // 10项里程碑名称常量
    private static final String MILESTONE_PRE_OPEN_BUSINESS = "售前商务沟通";
    private static final String MILESTONE_BIDDING_PUBLISHED = "招标挂网";
    private static final String MILESTONE_BID_SUBMITTED = "投标提交";
    private static final String MILESTONE_BID_WON_PUBLISHED = "中标挂网";
    private static final String MILESTONE_CONTRACT_SIGNED = "签订合同";
    private static final String MILESTONE_SERVICE_OPENED = "服务开通";
    private static final String MILESTONE_ACCEPTANCE_DONE = "项目验收";
    private static final String MILESTONE_INVOICE_ISSUED = "开票";
    private static final String MILESTONE_PAYMENT_DONE = "支付手续";
    private static final String MILESTONE_SERVICE_FEE_RECEIVED = "服务款到账";

    public Page<ProjectVO> pageProjects(Long current, Long size, Integer status, Long userId, List<Long> roleIds) {
        QueryWrapper<Project> wrapper = new QueryWrapper<Project>()
                .eq(status != null, "status", status)
                .eq(hasOnlyBDRole(roleIds), "owner_bd_id", userId)
                .orderByDesc("created_at");
        Page<Project> page = projectMapper.selectPage(new Page<>(current, size), wrapper);
        Page<ProjectVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    public ProjectDetailVO getProjectById(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            return null;
        }

        ProjectDetailVO vo = new ProjectDetailVO();
        ProjectVO baseVO = toVO(project);
        vo.setId(baseVO.getId());
        vo.setOpportunityId(baseVO.getOpportunityId());
        vo.setName(baseVO.getName());
        vo.setBusinessDomain(baseVO.getBusinessDomain());
        vo.setBusinessDomainName(baseVO.getBusinessDomainName());
        vo.setProductCategory(baseVO.getProductCategory());
        vo.setAdminLevel(baseVO.getAdminLevel());
        vo.setAdminLevelName(baseVO.getAdminLevelName());
        vo.setAmount(baseVO.getAmount());
        vo.setPerformanceCount(baseVO.getPerformanceCount());
        vo.setSalesMethod(baseVO.getSalesMethod());
        vo.setOwnerBdId(baseVO.getOwnerBdId());
        vo.setOwnerBdName(baseVO.getOwnerBdName());
        vo.setSalesUserId(baseVO.getSalesUserId());
        vo.setSalesUserName(baseVO.getSalesUserName());
        vo.setExpectedSignDate(baseVO.getExpectedSignDate());
        vo.setStatus(baseVO.getStatus());
        vo.setStatusName(baseVO.getStatusName());
        vo.setPNode(baseVO.getPNode());
        vo.setPNodeName(baseVO.getPNodeName());
        vo.setStage6(baseVO.getStage6());
        vo.setStage6Name(baseVO.getStage6Name());
        vo.setCustomerLayer(baseVO.getCustomerLayer());
        vo.setTrialAt(baseVO.getTrialAt());
        vo.setFormalAt(baseVO.getFormalAt());
        vo.setExpireAt(baseVO.getExpireAt());
        vo.setCompletionRate(baseVO.getCompletionRate());
        vo.setCurrentScore(baseVO.getCurrentScore());
        vo.setCreatedAt(baseVO.getCreatedAt());

        Opportunity opportunity = opportunityMapper.selectById(project.getOpportunityId());
        if (opportunity != null) {
            Customer customer = customerMapper.selectById(opportunity.getCustomerId());
            if (customer != null) {
                vo.setCustomerId(customer.getId());
                vo.setCustomerName(customer.getName());
                vo.setUnitId(customer.getUnitId());
                vo.setUnitName(dictionaryService.getUnitName(customer.getUnitId()));
                vo.setPoliceType(customer.getPoliceType());
                vo.setPoliceTypeName(dictionaryService.getDictionaryName("police_type", customer.getPoliceType()));
            }
        }

        ProjectMilestone milestone = milestoneMapper.selectOne(
                new QueryWrapper<ProjectMilestone>().eq("project_id", id)
        );
        if (milestone != null) {
            vo.setMilestone(toMilestoneVO(milestone));
        }

        BiddingNode biddingNode = biddingNodeMapper.selectOne(
                new QueryWrapper<BiddingNode>().eq("project_id", id)
        );
        if (biddingNode != null) {
            vo.setBiddingNode(toBiddingNodeVO(biddingNode));
        }

        ContractNode contractNode = contractNodeMapper.selectOne(
                new QueryWrapper<ContractNode>().eq("project_id", id)
        );
        if (contractNode != null) {
            vo.setContractNode(toContractNodeVO(contractNode));
        }

        List<PaymentNode> paymentNodes = paymentNodeMapper.selectList(
                new QueryWrapper<PaymentNode>().eq("project_id", id).orderByAsc("payment_no")
        );
        vo.setPaymentNodes(paymentNodes.stream().map(this::toPaymentNodeVO).collect(Collectors.toList()));

        List<ProjectScore> scores = scoreMapper.selectList(
                new QueryWrapper<ProjectScore>().eq("project_id", id)
                        .orderByDesc("snapshot_week").last("LIMIT 8")
        );
        vo.setScoreHistory(groupScoresByWeek(scores));

        return vo;
    }

    @AuditLog("创建项目")
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(ProjectRequest request, Long userId) {
        Opportunity opportunity = opportunityMapper.selectById(request.getOpportunityId());
        if (opportunity == null) {
            throw BusinessException.opportunityNotFound();
        }

        QueryWrapper<Project> wrapper = new QueryWrapper<Project>().eq("opportunity_id", request.getOpportunityId());
        if (projectMapper.selectCount(wrapper) > 0) {
            throw BusinessException.projectExists();
        }

        Project project = projectConverter.requestToEntity(request);
        project.setBusinessDomain(opportunity.getBusinessDomain());
        project.setAmount(opportunity.getAmount());
        project.setOwnerBdId(opportunity.getSubmittedBy());
        project.setPNode(1);
        project.setStage6("VALUE_VERIFY");
        project.setStatus(STATUS_IN_PROGRESS);
        project.setVersion(0);
        projectMapper.insert(project);

        ProjectMilestone milestone = new ProjectMilestone();
        milestone.setProjectId(project.getId());
        milestoneMapper.insert(milestone);

        if (request.getBiddingNode() != null) {
            BiddingNode biddingNode = projectConverter.biddingNodeRequestToEntity(request.getBiddingNode());
            biddingNode.setProjectId(project.getId());
            biddingNodeMapper.insert(biddingNode);
        }

        if (request.getContractNode() != null) {
            ContractNode contractNode = projectConverter.contractNodeRequestToEntity(request.getContractNode());
            contractNode.setProjectId(project.getId());
            contractNodeMapper.insert(contractNode);
        }

        // 更新商机阶段为"项目中"
        opportunityService.updateOpportunityStage(
                request.getOpportunityId(),
                com.cy.crm.module.opportunity.service.OpportunityService.STAGE_IN_PROJECT
        );

        return project.getId();
    }

    @AuditLog("更新项目")
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(Long id, ProjectRequest request) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }

        projectConverter.updateEntityFromRequest(request, project);
        projectMapper.updateById(project);

        if (request.getBiddingNode() != null) {
            BiddingNode biddingNode = biddingNodeMapper.selectOne(
                    new QueryWrapper<BiddingNode>().eq("project_id", id)
            );
            if (biddingNode == null) {
                biddingNode = projectConverter.biddingNodeRequestToEntity(request.getBiddingNode());
                biddingNode.setProjectId(id);
            } else {
                BiddingNodeRequest bnReq = request.getBiddingNode();
                biddingNode.setBiddingAgency(bnReq.getBiddingAgency());
                biddingNode.setPurchaseMethod(bnReq.getPurchaseMethod());
                biddingNode.setAnnouncementDate(bnReq.getAnnouncementDate());
                biddingNode.setRegistrationStart(bnReq.getRegistrationStart());
                biddingNode.setRegistrationEnd(bnReq.getRegistrationEnd());
                biddingNode.setBidDate(bnReq.getBidDate());
                biddingNode.setBidResultStart(bnReq.getBidResultStart());
                biddingNode.setBidResultEnd(bnReq.getBidResultEnd());
                biddingNode.setNoticeReceivedDate(bnReq.getNoticeReceivedDate());
                biddingNode.setNoticeOriginalArchived(bnReq.getNoticeOriginalArchived());
            }
            saveOrUpdate(biddingNode, biddingNodeMapper);
        }

        if (request.getContractNode() != null) {
            ContractNode contractNode = contractNodeMapper.selectOne(
                    new QueryWrapper<ContractNode>().eq("project_id", id)
            );
            if (contractNode == null) {
                contractNode = projectConverter.contractNodeRequestToEntity(request.getContractNode());
                contractNode.setProjectId(id);
            } else {
                ContractNodeRequest cnReq = request.getContractNode();
                contractNode.setDraftDate(cnReq.getDraftDate());
                contractNode.setReviewDept(cnReq.getReviewDept());
                contractNode.setApproveDate(cnReq.getApproveDate());
                contractNode.setOriginalArchived(cnReq.getOriginalArchived());
                contractNode.setPaymentMethod(cnReq.getPaymentMethod());
                contractNode.setPaymentRatio(cnReq.getPaymentRatio());
                contractNode.setPaymentTerms(cnReq.getPaymentTerms());
                contractNode.setPaymentNodes(cnReq.getPaymentNodes());
                contractNode.setHasWarranty(cnReq.getHasWarranty());
                contractNode.setWarrantyAmount(cnReq.getWarrantyAmount());
                contractNode.setAcceptanceDept(cnReq.getAcceptanceDept());
                contractNode.setHasSettlementAudit(cnReq.getHasSettlementAudit());
                contractNode.setInvoiceDate(cnReq.getInvoiceDate());
                contractNode.setPaymentVoucherDept(cnReq.getPaymentVoucherDept());
                contractNode.setReceivedDate(cnReq.getReceivedDate());
            }
            saveOrUpdate(contractNode, contractNodeMapper);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePNode(Long id, Integer pNode) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }
        project.setPNode(pNode);
        projectMapper.updateById(project);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStage6(Long id, String stage6) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }
        project.setStage6(stage6);
        projectMapper.updateById(project);
    }

    /**
     * 项目状态转换
     * 状态机规则：
     * - 项目中 ↔ 项目中断（可逆）
     * - 项目中 → 项目完成（终态）
     * - 项目中 → 项目终止（终态）
     * - 项目完成/终止不可逆
     */
    @AuditLog("转换项目状态")
    @Transactional(rollbackFor = Exception.class)
    public void transitionProjectStatus(Long projectId, int newStatus, String reason, Long userId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }

        int currentStatus = project.getStatus();

        // 验证状态转换是否合法
        if (!isValidTransition(currentStatus, newStatus)) {
            throw BusinessException.projectStatusInvalid();
        }

        // 执行状态转换
        project.setStatus(newStatus);
        projectMapper.updateById(project);

        // 状态转换后的联动处理
        handleStatusTransitionSideEffects(project, currentStatus, newStatus, userId);

        // TODO: 写入审计日志
        // auditLogService.log("project", "status_change", projectId, userId,
        //     String.format("项目状态从 %s 变更为 %s。原因：%s", getStatusName(currentStatus), getStatusName(newStatus), reason));
    }

    /**
     * 验证状态转换是否合法
     */
    private boolean isValidTransition(int fromStatus, int toStatus) {
        // 相同状态，无需转换
        if (fromStatus == toStatus) return false;

        // 终态不可逆
        if (fromStatus == STATUS_COMPLETED || fromStatus == STATUS_TERMINATED) {
            return false;
        }

        // 项目中可以转到任何状态
        if (fromStatus == STATUS_IN_PROGRESS) {
            return true;
        }

        // 项目中断只能回到项目中
        if (fromStatus == STATUS_INTERRUPTED && toStatus == STATUS_IN_PROGRESS) {
            return true;
        }

        return false;
    }

    /**
     * 状态转换后的联动处理
     */
    private void handleStatusTransitionSideEffects(Project project, int oldStatus, int newStatus, Long userId) {
        Long opportunityId = project.getOpportunityId();
        if (opportunityId == null) return;

        Opportunity opportunity = opportunityMapper.selectById(opportunityId);
        if (opportunity == null) return;

        if (newStatus == STATUS_INTERRUPTED) {
            // 项目中断：报备保持激活（submit_count=0）
            log.info("项目 {} 进入中断状态，报备 {} 保持激活", project.getId(), opportunityId);
        } else if (newStatus == STATUS_TERMINATED) {
            // 项目终止：报备置为失效，submit_count=2，cooling_until=+1月
            opportunity.setStatus(5); // STATUS_EXPIRED
            opportunity.setSubmitCount(2);
            opportunity.setCoolingUntil(java.time.LocalDateTime.now().plusMonths(1));
            opportunity.setExpiredAt(java.time.LocalDateTime.now());
            opportunityMapper.updateById(opportunity);
            log.info("项目 {} 已终止，报备 {} 置为失效，冷却期1个月", project.getId(), opportunityId);
        } else if (newStatus == STATUS_COMPLETED) {
            // 项目完成：报备置为已转化
            // TODO: 需要确认报备的"已转化"状态值
            log.info("项目 {} 已完成，报备 {} 置为已转化", project.getId(), opportunityId);
        } else if (newStatus == STATUS_IN_PROGRESS && oldStatus == STATUS_INTERRUPTED) {
            // 项目中断恢复：报备自动激活（如果submit_count=0）
            if (opportunity.getSubmitCount() == null || opportunity.getSubmitCount() == 0) {
                opportunity.setStatus(3); // STATUS_ACTIVE
                opportunityMapper.updateById(opportunity);
                log.info("项目 {} 恢复进行，报备 {} 重新激活", project.getId(), opportunityId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMilestone(Long id, ProjectDetailVO.MilestoneVO request) {
        ProjectMilestone milestone = milestoneMapper.selectOne(
                new QueryWrapper<ProjectMilestone>().eq("project_id", id)
        );
        ProjectMilestone oldMilestone = null;
        if (milestone != null) {
            // 保存旧值用于变更检测
            oldMilestone = projectConverter.milestoneToVO(milestone) != null ? new ProjectMilestone() : null;
            if (oldMilestone != null) {
                oldMilestone.setPreOpenBusiness(milestone.getPreOpenBusiness());
                oldMilestone.setBiddingPublished(milestone.getBiddingPublished());
                oldMilestone.setBidSubmitted(milestone.getBidSubmitted());
                oldMilestone.setBidWonPublished(milestone.getBidWonPublished());
                oldMilestone.setContractSigned(milestone.getContractSigned());
                oldMilestone.setServiceOpened(milestone.getServiceOpened());
                oldMilestone.setAcceptanceDone(milestone.getAcceptanceDone());
                oldMilestone.setInvoiceIssued(milestone.getInvoiceIssued());
                oldMilestone.setPaymentDone(milestone.getPaymentDone());
                oldMilestone.setServiceFeeReceived(milestone.getServiceFeeReceived());
            }
        } else {
            milestone = new ProjectMilestone();
            milestone.setProjectId(id);
        }
        milestone.setPreOpenBusiness(request.getPreOpenBusiness());
        milestone.setBiddingPublished(request.getBiddingPublished());
        milestone.setBidSubmitted(request.getBidSubmitted());
        milestone.setBidWonPublished(request.getBidWonPublished());
        milestone.setContractSigned(request.getContractSigned());
        milestone.setServiceOpened(request.getServiceOpened());
        milestone.setAcceptanceDone(request.getAcceptanceDone());
        milestone.setInvoiceIssued(request.getInvoiceIssued());
        milestone.setPaymentDone(request.getPaymentDone());
        milestone.setServiceFeeReceived(request.getServiceFeeReceived());
        milestone.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(milestone, milestoneMapper);

        // 检测里程碑变更并发送通知
        if (oldMilestone != null) {
            detectAndNotifyMilestoneChanges(id, oldMilestone, milestone);
        }
    }

    /**
     * 检测里程碑变更并发送通知
     */
    private void detectAndNotifyMilestoneChanges(Long projectId, ProjectMilestone oldM, ProjectMilestone newM) {
        List<String> completedMilestones = new ArrayList<>();

        // 检测每个里程碑字段的变化
        if (isMilestoneCompleted(oldM.getPreOpenBusiness(), newM.getPreOpenBusiness())) {
            completedMilestones.add(MILESTONE_PRE_OPEN_BUSINESS);
        }
        if (isMilestoneCompleted(oldM.getBiddingPublished(), newM.getBiddingPublished())) {
            completedMilestones.add(MILESTONE_BIDDING_PUBLISHED);
        }
        if (isMilestoneCompleted(oldM.getBidSubmitted(), newM.getBidSubmitted())) {
            completedMilestones.add(MILESTONE_BID_SUBMITTED);
        }
        if (isMilestoneCompleted(oldM.getBidWonPublished(), newM.getBidWonPublished())) {
            completedMilestones.add(MILESTONE_BID_WON_PUBLISHED);
        }
        if (isMilestoneCompleted(oldM.getContractSigned(), newM.getContractSigned())) {
            completedMilestones.add(MILESTONE_CONTRACT_SIGNED);
        }
        if (isMilestoneCompleted(oldM.getServiceOpened(), newM.getServiceOpened())) {
            completedMilestones.add(MILESTONE_SERVICE_OPENED);
        }
        if (isMilestoneCompleted(oldM.getAcceptanceDone(), newM.getAcceptanceDone())) {
            completedMilestones.add(MILESTONE_ACCEPTANCE_DONE);
        }
        if (isMilestoneCompleted(oldM.getInvoiceIssued(), newM.getInvoiceIssued())) {
            completedMilestones.add(MILESTONE_INVOICE_ISSUED);
        }
        if (isMilestoneCompleted(oldM.getPaymentDone(), newM.getPaymentDone())) {
            completedMilestones.add(MILESTONE_PAYMENT_DONE);
        }
        if (isMilestoneCompleted(oldM.getServiceFeeReceived(), newM.getServiceFeeReceived())) {
            completedMilestones.add(MILESTONE_SERVICE_FEE_RECEIVED);
        }

        // 发送通知
        if (!completedMilestones.isEmpty()) {
            Project project = projectMapper.selectById(projectId);
            if (project != null && project.getOwnerBdId() != null) {
                String milestoneNames = String.join("、", completedMilestones);
                notificationService.createNotification(
                        project.getOwnerBdId(),
                        "项目里程碑更新",
                        String.format("项目 %s 完成里程碑：%s", project.getName(), milestoneNames),
                        "MILESTONE_COMPLETED",
                        projectId
                );
                log.info("项目 {} 完成里程碑：{}", projectId, milestoneNames);
            }
        }
    }

    /**
     * 判断里程碑是否从未完成变为完成
     */
    private boolean isMilestoneCompleted(Integer oldValue, Integer newValue) {
        return (oldValue == null || oldValue != 1) && (newValue != null && newValue == 1);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitScores(ProjectScoreRequest request) {
        String week = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));

        scoreMapper.delete(new QueryWrapper<ProjectScore>()
                .eq("project_id", request.getProjectId())
                .eq("snapshot_week", week));

        for (ProjectScoreRequest.ScoreItem item : request.getScores()) {
            ProjectScore score = new ProjectScore();
            score.setProjectId(request.getProjectId());
            score.setSnapshotWeek(week);
            score.setScoreDimension(item.getDimension());
            score.setScore(item.getScore());
            score.setWeight(getWeight(item.getDimension()));
            scoreMapper.insert(score);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addPaymentNode(Long projectId, PaymentNodeVO request) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.resourceNotFound("项目");
        }

        PaymentNode node = new PaymentNode();
        node.setPaymentNo(request.getPaymentNo());
        node.setAmount(request.getAmount());
        node.setReceivedDate(request.getReceivedDate());
        node.setInvoiceNo(request.getInvoiceNo());
        node.setStatus(request.getStatus());
        node.setProjectId(projectId);
        paymentNodeMapper.insert(node);

        return node.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentNode(Long id, PaymentNodeVO request) {
        PaymentNode node = paymentNodeMapper.selectById(id);
        if (node == null) {
            throw BusinessException.resourceNotFound("回款节点");
        }
        node.setPaymentNo(request.getPaymentNo());
        node.setAmount(request.getAmount());
        node.setReceivedDate(request.getReceivedDate());
        node.setInvoiceNo(request.getInvoiceNo());
        node.setStatus(request.getStatus());
        paymentNodeMapper.updateById(node);
    }

    private boolean hasOnlyBDRole(List<Long> roleIds) {
        return roleIds != null && roleIds.size() == 1 && roleIds.contains(4L);
    }

    private void saveOrUpdate(BiddingNode node, BiddingNodeMapper mapper) {
        if (node.getId() == null) {
            mapper.insert(node);
        } else {
            mapper.updateById(node);
        }
    }

    private void saveOrUpdate(ContractNode node, ContractNodeMapper mapper) {
        if (node.getId() == null) {
            mapper.insert(node);
        } else {
            mapper.updateById(node);
        }
    }

    private void saveOrUpdate(ProjectMilestone milestone, ProjectMilestoneMapper mapper) {
        if (milestone.getId() == null) {
            mapper.insert(milestone);
        } else {
            mapper.updateById(milestone);
        }
    }

    private ProjectVO toVO(Project project) {
        ProjectVO vo = projectConverter.entityToVO(project);

        vo.setBusinessDomainName(dictionaryService.getDictionaryName("business_domain", project.getBusinessDomain()));
        vo.setAdminLevelName(dictionaryService.getDictionaryName("admin_level", String.valueOf(project.getAdminLevel())));
        vo.setStatusName(getStatusName(project.getStatus()));
        vo.setPNodeName(getPNodeName(project.getPNode()));
        vo.setStage6Name(dictionaryService.getDictionaryName("stage_6", project.getStage6()));

        if (project.getOwnerBdId() != null) {
            com.cy.crm.module.admin.entity.User ownerBd = userService.getUserEntityById(project.getOwnerBdId());
            vo.setOwnerBdName(ownerBd != null ? ownerBd.getRealName() : null);
        }
        if (project.getSalesUserId() != null) {
            com.cy.crm.module.admin.entity.User salesUser = userService.getUserEntityById(project.getSalesUserId());
            vo.setSalesUserName(salesUser != null ? salesUser.getRealName() : null);
        }

        ProjectMilestone milestone = milestoneMapper.selectOne(
                new QueryWrapper<ProjectMilestone>().eq("project_id", project.getId())
        );
        if (milestone != null) {
            vo.setCompletionRate(calculateCompletionRate(milestone));
        }

        BigDecimal currentScore = getCurrentScore(project.getId());
        vo.setCurrentScore(currentScore);

        return vo;
    }

    private ProjectDetailVO.MilestoneVO toMilestoneVO(ProjectMilestone milestone) {
        return projectConverter.milestoneToVO(milestone);
    }

    private ProjectDetailVO.BiddingNodeVO toBiddingNodeVO(BiddingNode node) {
        ProjectDetailVO.BiddingNodeVO vo = projectConverter.biddingNodeToVO(node);
        if (node.getPurchaseMethod() != null) {
            vo.setPurchaseMethodName(dictionaryService.getDictionaryName("purchase_method", String.valueOf(node.getPurchaseMethod())));
        }
        return vo;
    }

    private ProjectDetailVO.ContractNodeVO toContractNodeVO(ContractNode node) {
        return projectConverter.contractNodeToVO(node);
    }

    private ProjectDetailVO.PaymentNodeVO toPaymentNodeVO(PaymentNode node) {
        ProjectDetailVO.PaymentNodeVO vo = new ProjectDetailVO.PaymentNodeVO();
        vo.setId(node.getId());
        vo.setPaymentNo(node.getPaymentNo());
        vo.setAmount(node.getAmount());
        vo.setReceivedDate(node.getReceivedDate());
        vo.setInvoiceNo(node.getInvoiceNo());
        vo.setStatus(node.getStatus());
        vo.setStatusName(getPaymentStatusName(node.getStatus()));
        return vo;
    }

    private List<ProjectDetailVO.ScoreHistoryVO> groupScoresByWeek(List<ProjectScore> scores) {
        return scores.stream()
                .collect(Collectors.groupingBy(ProjectScore::getSnapshotWeek))
                .entrySet().stream()
                .map(entry -> {
                    ProjectDetailVO.ScoreHistoryVO vo = new ProjectDetailVO.ScoreHistoryVO();
                    vo.setSnapshotWeek(entry.getKey());
                    BigDecimal totalScore = entry.getValue().stream()
                            .map(s -> s.getScore().multiply(s.getWeight()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    vo.setTotalScore(totalScore);
                    vo.setScores(entry.getValue().stream().map(s -> {
                        ProjectDetailVO.ScoreHistoryVO.ScoreItemVO item = new ProjectDetailVO.ScoreHistoryVO.ScoreItemVO();
                        item.setDimension(s.getScoreDimension());
                        item.setDimensionName(getDimensionName(s.getScoreDimension()));
                        item.setScore(s.getScore());
                        item.setWeight(s.getWeight());
                        item.setWeightedScore(s.getScore().multiply(s.getWeight()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                        return item;
                    }).collect(Collectors.toList()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算10项里程碑完成度
     */
    private Integer calculateCompletionRate(ProjectMilestone milestone) {
        int count = 0;
        if (milestone.getPreOpenBusiness() != null && milestone.getPreOpenBusiness() == 1) count++;
        if (milestone.getBiddingPublished() != null && milestone.getBiddingPublished() == 1) count++;
        if (milestone.getBidSubmitted() != null && milestone.getBidSubmitted() == 1) count++;
        if (milestone.getBidWonPublished() != null && milestone.getBidWonPublished() == 1) count++;
        if (milestone.getContractSigned() != null && milestone.getContractSigned() == 1) count++;
        if (milestone.getServiceOpened() != null && milestone.getServiceOpened() == 1) count++;
        if (milestone.getAcceptanceDone() != null && milestone.getAcceptanceDone() == 1) count++;
        if (milestone.getInvoiceIssued() != null && milestone.getInvoiceIssued() == 1) count++;
        if (milestone.getPaymentDone() != null && milestone.getPaymentDone() == 1) count++;
        if (milestone.getServiceFeeReceived() != null && milestone.getServiceFeeReceived() == 1) count++;
        return count * 100 / 10;
    }

    private BigDecimal getCurrentScore(Long projectId) {
        List<ProjectScore> scores = scoreMapper.selectList(
                new QueryWrapper<ProjectScore>().eq("project_id", projectId)
                        .orderByDesc("snapshot_week").last("LIMIT 8")
        );
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return scores.stream()
                .map(s -> s.getScore().multiply(s.getWeight()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getWeight(String dimension) {
        return switch (dimension) {
            case DIMENSION_LIXIANG_RESULT -> new BigDecimal("10");
            case DIMENSION_BUDGET -> new BigDecimal("20");
            case DIMENSION_GOV_FINANCE -> new BigDecimal("15");
            case DIMENSION_PURCHASE_MEETING -> new BigDecimal("15");
            case DIMENSION_KP_REACH -> new BigDecimal("15");
            case DIMENSION_PURCHASE_PROCESS -> new BigDecimal("10");
            case DIMENSION_CREDIT -> new BigDecimal("10");
            case DIMENSION_OPEN_INTENTION -> new BigDecimal("5");
            default -> BigDecimal.ZERO;
        };
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case STATUS_IN_PROGRESS -> "项目中";
            case STATUS_COMPLETED -> "项目完成";
            case STATUS_INTERRUPTED -> "项目中断";
            case STATUS_TERMINATED -> "项目终止";
            default -> "未知";
        };
    }

    private String getPNodeName(Integer pNode) {
        if (pNode == null) return "未开始";
        return "P" + pNode;
    }

    private String getPaymentStatusName(Integer status) {
        return switch (status) {
            case 1 -> "待回款";
            case 2 -> "已到账";
            case 3 -> "逾期";
            default -> "未知";
        };
    }

    private String getDimensionName(String dimension) {
        return switch (dimension) {
            case DIMENSION_LIXIANG_RESULT -> "立项结果";
            case DIMENSION_BUDGET -> "资金预算";
            case DIMENSION_GOV_FINANCE -> "政府财评";
            case DIMENSION_PURCHASE_MEETING -> "采购方式上会";
            case DIMENSION_KP_REACH -> "职能KP触达";
            case DIMENSION_PURCHASE_PROCESS -> "采购流程确认";
            case DIMENSION_CREDIT -> "渠道与客户资信";
            case DIMENSION_OPEN_INTENTION -> "客户开通意愿";
            default -> dimension;
        };
    }

    /**
     * 获取所有评分维度及其权重（供前端使用）
     */
    public static java.util.Map<String, Object> getScoreDimensions() {
        java.util.Map<String, Object> dimensions = new java.util.LinkedHashMap<>();
        dimensions.put(DIMENSION_LIXIANG_RESULT, java.util.Map.of("name", "立项结果", "weight", 10));
        dimensions.put(DIMENSION_BUDGET, java.util.Map.of("name", "资金预算", "weight", 20));
        dimensions.put(DIMENSION_GOV_FINANCE, java.util.Map.of("name", "政府财评", "weight", 15));
        dimensions.put(DIMENSION_PURCHASE_MEETING, java.util.Map.of("name", "采购方式上会", "weight", 15));
        dimensions.put(DIMENSION_KP_REACH, java.util.Map.of("name", "职能KP触达", "weight", 15));
        dimensions.put(DIMENSION_PURCHASE_PROCESS, java.util.Map.of("name", "采购流程确认", "weight", 10));
        dimensions.put(DIMENSION_CREDIT, java.util.Map.of("name", "渠道与客户资信", "weight", 10));
        dimensions.put(DIMENSION_OPEN_INTENTION, java.util.Map.of("name", "客户开通意愿", "weight", 5));
        return dimensions;
    }
}
