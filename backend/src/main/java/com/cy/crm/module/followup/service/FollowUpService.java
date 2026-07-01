package com.cy.crm.module.followup.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.DataScopeValidator;
import com.cy.crm.security.SecurityContext;
import com.cy.crm.module.admin.service.UserService;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.followup.converter.FollowUpConverter;
import com.cy.crm.module.followup.dto.FollowUpRequest;
import com.cy.crm.module.followup.entity.FollowUp;
import com.cy.crm.module.followup.mapper.FollowUpMapper;
import com.cy.crm.module.followup.vo.FollowUpVO;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.project.service.ProjectService;
import com.cy.crm.module.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpService extends ServiceImpl<FollowUpMapper, FollowUp> {

    private final FollowUpMapper followUpMapper;
    private final CustomerMapper customerMapper;
    private final ProjectMapper projectMapper;
    private final OpportunityMapper opportunityMapper;
    private final TaskMapper taskMapper;
    private final UserService userService;
    private final DictionaryService dictionaryService;
    private final ProjectService projectService;
    private final com.cy.crm.module.opportunity.service.OpportunityService opportunityService;
    private final FollowUpConverter followUpConverter;
    private final DataScopeValidator dataScopeValidator;

    // 跟进记录下一步阶段对应的项目状态
    private static final List<String> INTERRUPT_STAGES = Arrays.asList(
            "CUSTOMER_TRAFFIC", "CUSTOMER_EVANGELISM", "CUSTOMER_TRIAL"
    );
    private static final List<String> COMPLETION_STAGES = Arrays.asList(
            "SERVICE_STAGE"
    );

    // 阶段到 Stage6 的映射
    private static final java.util.Map<String, String> STAGE_TO_STAGE6 = new java.util.HashMap<>();
    static {
        // 价值验证阶段
        STAGE_TO_STAGE6.put("CUSTOMER_EVANGELISM", "VALUE_VERIFY");
        STAGE_TO_STAGE6.put("TRIAL_CONVERSION", "VALUE_VERIFY");

        // 立项阶段
        STAGE_TO_STAGE6.put("LIXIANG_PREPARE", "LIXIANG");
        STAGE_TO_STAGE6.put("LIXIANG_REPORT", "LIXIANG");
        STAGE_TO_STAGE6.put("LIXIANG_RESULT", "LIXIANG");

        // 招投标阶段
        STAGE_TO_STAGE6.put("PURCHASE_APPLY", "ZHAOBIAO");
        STAGE_TO_STAGE6.put("BIDDING", "ZHAOBIAO");
        STAGE_TO_STAGE6.put("BIDDING_RESULT", "ZHAOBIAO");

        // 合同阶段
        STAGE_TO_STAGE6.put("CONTRACT_ORDER", "HETONG");
        STAGE_TO_STAGE6.put("ACCOUNT_OPEN", "HETONG");

        // 服务阶段
        STAGE_TO_STAGE6.put("PRODUCT_USE", "FUWU");
        STAGE_TO_STAGE6.put("SERVICE_STAGE", "FUWU");

        // 续签阶段
        STAGE_TO_STAGE6.put("RENEW_COMMUNICATE", "XUQIAN");
    }

    public Page<FollowUpVO> pageFollowUps(Long current, Long size, Long customerId, Long userId, List<Long> roleIds) {
        QueryWrapper<FollowUp> wrapper = new QueryWrapper<FollowUp>()
                .eq(customerId != null, "customer_id", customerId)
                .eq(hasOnlyBDRole(roleIds), "created_by", userId)
                .orderByDesc("follow_up_date");
        Page<FollowUp> page = followUpMapper.selectPage(new Page<>(current, size), wrapper);
        Page<FollowUpVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createFollowUp(FollowUpRequest request, Long userId) {
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw BusinessException.customerNotFound();
        }

        if (request.getFollowUpDate().isAfter(LocalDate.now())) {
            throw BusinessException.paramError("跟进日期不能晚于今天");
        }

        FollowUp followUp = followUpConverter.requestToEntity(request);
        followUp.setCreatedBy(userId);
        followUpMapper.insert(followUp);

        if (request.getNextStage() != null && !request.getNextStage().isEmpty()) {
            createTaskFromFollowUp(followUp, userId);
            // 触发项目状态转换
            triggerProjectStatusTransition(followUp, userId);
            // 更新项目 Stage6
            updateProjectStage6(followUp);
        }

        if (request.getOpportunityId() != null) {
            updateOpportunityFollowUpTime(request.getOpportunityId());
        }

        return followUp.getId();
    }

    /**
     * 根据跟进记录的下一步阶段触发项目状态转换
     */
    private void triggerProjectStatusTransition(FollowUp followUp, Long userId) {
        if (followUp.getProjectId() == null) return;

        Project project = projectMapper.selectById(followUp.getProjectId());
        if (project == null) return;

        String nextStage = followUp.getNextStage();

        // 判断是否需要转换项目状态
        if (INTERRUPT_STAGES.contains(nextStage)) {
            // 转为项目中断
            projectService.transitionProjectStatus(
                    project.getId(),
                    ProjectService.STATUS_INTERRUPTED,
                    "跟进记录下一步阶段：" + nextStage,
                    userId
            );
            log.info("跟进记录 {} 触发项目 {} 进入中断状态", followUp.getId(), project.getId());
        } else if (COMPLETION_STAGES.contains(nextStage)) {
            // 转为项目完成
            projectService.transitionProjectStatus(
                    project.getId(),
                    ProjectService.STATUS_COMPLETED,
                    "跟进记录下一步阶段：" + nextStage,
                    userId
            );
            log.info("跟进记录 {} 触发项目 {} 进入完成状态", followUp.getId(), project.getId());
        } else if (project.getStatus().equals(ProjectService.STATUS_INTERRUPTED)) {
            // 项目中断恢复为进行中
            projectService.transitionProjectStatus(
                    project.getId(),
                    ProjectService.STATUS_IN_PROGRESS,
                    "跟进记录下一步阶段回到业务流程：" + nextStage,
                    userId
            );
            log.info("跟进记录 {} 触发项目 {} 恢复为进行中状态", followUp.getId(), project.getId());
        }
    }

    /**
     * 根据跟进记录的下一步阶段更新项目 Stage6
     */
    private void updateProjectStage6(FollowUp followUp) {
        if (followUp.getProjectId() == null) return;

        Project project = projectMapper.selectById(followUp.getProjectId());
        if (project == null) return;

        String nextStage = followUp.getNextStage();
        String newStage6 = STAGE_TO_STAGE6.get(nextStage);

        if (newStage6 != null && !newStage6.equals(project.getStage6())) {
            project.setStage6(newStage6);
            projectMapper.updateById(project);
            log.info("跟进记录 {} 触发项目 {} Stage6 更新为 {}", followUp.getId(), project.getId(), newStage6);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFollowUp(Long id, FollowUpRequest request) {
        FollowUp followUp = followUpMapper.selectById(id);
        if (followUp == null) {
            throw BusinessException.resourceNotFound("跟进记录");
        }

        // IDOR protection: validate access to the customer associated with this follow-up
        Customer customer = customerMapper.selectById(followUp.getCustomerId());
        if (customer != null) {
            Long currentUserId = SecurityContext.getCurrentUserId();
            DataScope currentDataScope = SecurityContext.getCurrentDataScope();
            dataScopeValidator.validateUnitAccess(currentUserId, customer.getUnitId(), currentDataScope);
        }

        followUpConverter.updateEntityFromRequest(request, followUp);
        followUpMapper.updateById(followUp);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFollowUp(Long id) {
        FollowUp followUp = followUpMapper.selectById(id);
        if (followUp == null) {
            return;
        }

        // IDOR protection: validate access to the customer associated with this follow-up
        Customer customer = customerMapper.selectById(followUp.getCustomerId());
        if (customer != null) {
            Long currentUserId = SecurityContext.getCurrentUserId();
            DataScope currentDataScope = SecurityContext.getCurrentDataScope();
            dataScopeValidator.validateUnitAccess(currentUserId, customer.getUnitId(), currentDataScope);
        }

        followUpMapper.deleteById(id);
    }

    /**
     * 判断用户是否仅拥有 BD 角色权限（用于数据范围过滤）
     * BD 角色只能查看自己创建的跟进记录
     */
    private boolean hasOnlyBDRole(List<Long> roleIds) {
        return roleIds != null && roleIds.size() == 1 && roleIds.contains(4L);
    }

    private void createTaskFromFollowUp(FollowUp followUp, Long userId) {
        com.cy.crm.module.task.entity.Task task = new com.cy.crm.module.task.entity.Task();
        task.setOwnerUserId(userId);
        task.setCustomerId(followUp.getCustomerId());
        task.setFollowUpId(followUp.getId());
        task.setPlanStage(followUp.getNextStage());
        task.setPlanDate(followUp.getFollowUpDate().plusDays(7));
        task.setStatus(1);
        taskMapper.insert(task);
    }

    private void updateOpportunityFollowUpTime(Long opportunityId) {
        Opportunity opportunity = opportunityMapper.selectById(opportunityId);
        if (opportunity != null) {
            opportunity.setLastFollowUpAt(java.time.LocalDateTime.now());
            opportunityMapper.updateById(opportunity);
        }
    }

    private FollowUpVO toVO(FollowUp followUp) {
        FollowUpVO vo = followUpConverter.entityToVO(followUp);

        Customer customer = customerMapper.selectById(followUp.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getName());
        }

        if (followUp.getProjectId() != null) {
            Project project = projectMapper.selectById(followUp.getProjectId());
            if (project != null) {
                vo.setProjectName(project.getName());
            }
        }

        vo.setCurrentStageName(dictionaryService.getDictionaryName("stage_6", followUp.getCurrentStage()));
        vo.setNextStageName(dictionaryService.getDictionaryName("stage_6", followUp.getNextStage()));
        vo.setFollowUpMethodName(dictionaryService.getDictionaryName("follow_up_method", followUp.getFollowUpMethod()));

        com.cy.crm.module.admin.entity.User creator = userService.getUserEntityById(followUp.getCreatedBy());
        vo.setCreatedByName(creator != null ? creator.getRealName() : null);

        return vo;
    }
}
