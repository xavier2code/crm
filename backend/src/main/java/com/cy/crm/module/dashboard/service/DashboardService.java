package com.cy.crm.module.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.contract.entity.Contract;
import com.cy.crm.module.contract.mapper.ContractMapper;
import com.cy.crm.module.customer.entity.Customer;
import com.cy.crm.module.customer.mapper.CustomerMapper;
import com.cy.crm.module.dashboard.vo.*;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.mapper.OpportunityMapper;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.entity.ProjectMilestone;
import com.cy.crm.module.project.mapper.ProjectMapper;
import com.cy.crm.module.project.mapper.ProjectMilestoneMapper;
import com.cy.crm.module.task.entity.Task;
import com.cy.crm.module.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserMapper userMapper;
    private final UserChannelMapper userChannelMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityMapper opportunityMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMilestoneMapper projectMilestoneMapper;
    private final ContractMapper contractMapper;
    private final TaskMapper taskMapper;

    public DashboardVO getDashboard(Long userId, List<Long> roleIds) {
        DashboardVO vo = new DashboardVO();

        Long channelId = getUserChannelId(userId);

        vo.setTotalCustomers(Math.toIntExact(customerMapper.selectCount(
                new QueryWrapper<Customer>().eq("owner_user_id", userId)
        )));

        vo.setTotalOpportunities(Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>().eq("submitted_by", userId)
        )));

        vo.setActiveOpportunities(Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>()
                        .eq("submitted_by", userId)
                        .eq("status", 3)
        )));

        vo.setTotalProjects(Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().eq("owner_bd_id", userId)
        )));

        vo.setInProgressProjects(Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>()
                        .eq("owner_bd_id", userId)
                        .eq("status", 1)
        )));

        // 兜底：若 BD 名下无任何项目，projectIds 为空，IN () 在 PostgreSQL 中是非法 SQL，
        // 需短路返回 0 而非继续拼装空 IN 子句。
        List<Long> projectIds = projectMapper.selectList(
                new QueryWrapper<Project>().eq("owner_bd_id", userId)
        ).stream().map(Project::getId).collect(Collectors.toList());
        BigDecimal totalAmount = projectIds.isEmpty()
                ? BigDecimal.ZERO
                : contractMapper.selectList(
                        new QueryWrapper<Contract>().in("project_id", projectIds)
                ).stream().map(Contract::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalContractAmount(totalAmount);

        vo.setPendingTasks(Math.toIntExact(taskMapper.selectCount(
                new QueryWrapper<Task>()
                        .eq("owner_user_id", userId)
                        .eq("status", 1)
                        .le("plan_date", LocalDate.now())
        )));

        LocalDate today = LocalDate.now();
        // 兜底：若 30 天内无生效中商机，customerIds 为空，IN () 同样会触发 PG 语法错误。
        List<Long> customerIds = opportunityMapper.selectList(
                new QueryWrapper<Opportunity>()
                        .eq("submitted_by", userId)
                        .eq("status", 3)
                        .ge("last_follow_up_at", today.minusDays(30))
        ).stream().map(Opportunity::getCustomerId).collect(Collectors.toList());
        int todayFollowUpCount = customerIds.isEmpty()
                ? 0
                : Math.toIntExact(customerMapper.selectCount(
                        new QueryWrapper<Customer>()
                                .eq("owner_user_id", userId)
                                .in("id", customerIds)
                ));
        vo.setTodayFollowUpCount(todayFollowUpCount);

        Map<String, Integer> opportunityStatusMap = new LinkedHashMap<>();
        opportunityStatusMap.put("草稿", Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>().eq("submitted_by", userId).eq("status", 1)
        )));
        opportunityStatusMap.put("审批中", Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>().eq("submitted_by", userId).eq("status", 2)
        )));
        opportunityStatusMap.put("生效中", Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>().eq("submitted_by", userId).eq("status", 3)
        )));
        opportunityStatusMap.put("报备失败", Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>().eq("submitted_by", userId).eq("status", 4)
        )));
        vo.setOpportunityStatusDistribution(opportunityStatusMap);

        Map<String, Integer> projectStageMap = new LinkedHashMap<>();
        for (int i = 1; i <= 8; i++) {
            String stageName = "P" + i;
            int count = Math.toIntExact(projectMapper.selectCount(
                    new QueryWrapper<Project>().eq("owner_bd_id", userId).eq("p_node", i)
            ));
            projectStageMap.put(stageName, count);
        }
        vo.setProjectStageDistribution(projectStageMap);

        return vo;
    }

    public ChannelDashboardVO getChannelDashboard(Long channelId) {
        ChannelDashboardVO vo = new ChannelDashboardVO();
        vo.setChannelId(channelId);

        List<Long> memberIds = userChannelMapper.selectList(
                new QueryWrapper<com.cy.crm.module.admin.entity.UserChannel>()
                        .eq("channel_id", channelId)
                        .eq("assign_type", 2)
        ).stream().map(com.cy.crm.module.admin.entity.UserChannel::getUserId).collect(Collectors.toList());

        if (memberIds.isEmpty()) {
            return vo;
        }

        vo.setMemberCount(memberIds.size());

        List<Long> customerIds = customerMapper.selectList(
                new QueryWrapper<Customer>().in("owner_user_id", memberIds)
        ).stream().map(Customer::getId).collect(Collectors.toList());

        vo.setTotalCustomers(customerIds.size());

        vo.setTotalOpportunities(Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>().in("submitted_by", memberIds)
        )));

        vo.setActiveOpportunities(Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>()
                        .in("submitted_by", memberIds)
                        .eq("status", 3)
        )));

        vo.setExpiredOpportunities(Math.toIntExact(opportunityMapper.selectCount(
                new QueryWrapper<Opportunity>()
                        .in("submitted_by", memberIds)
                        .eq("status", 5)
        )));

        vo.setTotalProjects(Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("owner_bd_id", memberIds)
        )));

        vo.setInProgressProjects(Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>()
                        .in("owner_bd_id", memberIds)
                        .eq("status", 1)
        )));

        vo.setCompletedProjects(Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>()
                        .in("owner_bd_id", memberIds)
                        .eq("status", 2)
        )));

        List<Contract> contracts = contractMapper.selectList(
                new QueryWrapper<Contract>()
                        .in("project_id",
                                projectMapper.selectList(
                                        new QueryWrapper<Project>().in("owner_bd_id", memberIds)
                                ).stream().map(Project::getId).collect(Collectors.toList())
                        )
        );

        BigDecimal totalAmount = contracts.stream().map(Contract::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalContractAmount(totalAmount);

        int currentYear = LocalDate.now().getYear();
        BigDecimal yearAmount = contracts.stream()
                .filter(c -> c.getCreatedAt().getYear() == currentYear)
                .map(Contract::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setYearContractAmount(yearAmount);

        List<ChannelDashboardVO.MemberPerformanceVO> memberPerformances = memberIds.stream()
                .map(memberId -> {
                    ChannelDashboardVO.MemberPerformanceVO memberVO = new ChannelDashboardVO.MemberPerformanceVO();
                    var user = userMapper.selectById(memberId);
                    memberVO.setUserId(memberId);
                    memberVO.setUserName(user != null ? user.getRealName() : "未知");

                    memberVO.setCustomerCount(Math.toIntExact(customerMapper.selectCount(
                            new QueryWrapper<Customer>().eq("owner_user_id", memberId)
                    )));

                    memberVO.setOpportunityCount(Math.toIntExact(opportunityMapper.selectCount(
                            new QueryWrapper<Opportunity>().eq("submitted_by", memberId)
                    )));

                    memberVO.setProjectCount(Math.toIntExact(projectMapper.selectCount(
                            new QueryWrapper<Project>().eq("owner_bd_id", memberId)
                    )));

                    List<Long> userProjectIds = projectMapper.selectList(
                            new QueryWrapper<Project>().eq("owner_bd_id", memberId)
                    ).stream().map(Project::getId).collect(Collectors.toList());

                    if (!userProjectIds.isEmpty()) {
                        BigDecimal userAmount = contracts.stream()
                                .filter(c -> userProjectIds.contains(c.getProjectId()))
                                .map(Contract::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        memberVO.setContractAmount(userAmount);
                    } else {
                        memberVO.setContractAmount(BigDecimal.ZERO);
                    }

                    return memberVO;
                })
                .sorted(Comparator.comparing(ChannelDashboardVO.MemberPerformanceVO::getContractAmount).reversed())
                .collect(Collectors.toList());

        vo.setMemberPerformances(memberPerformances);

        Map<String, Integer> projectStatusMap = new LinkedHashMap<>();
        projectStatusMap.put("项目中", vo.getInProgressProjects());
        projectStatusMap.put("项目完成", vo.getCompletedProjects());
        projectStatusMap.put("项目中断", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("owner_bd_id", memberIds).eq("status", 3)
        )));
        projectStatusMap.put("项目终止", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("owner_bd_id", memberIds).eq("status", 4)
        )));
        vo.setProjectStatusDistribution(projectStatusMap);

        return vo;
    }

    public ProjectStatisticsVO getProjectStatistics(Long userId, List<Long> roleIds) {
        ProjectStatisticsVO vo = new ProjectStatisticsVO();

        List<Long> projectIds = getUserProjectIds(userId, roleIds);

        // 兜底：若用户名下无项目 (BD 且尚无任何项目)，projectIds 为空，IN () 在 PG 非法，
        // 直接返回空统计 VO，避免对 t_project / t_project_milestone 跑空 IN 查询。
        if (projectIds.isEmpty()) {
            vo.setTotalProjects(0);
            vo.setPNodeDistribution(new LinkedHashMap<>());
            vo.setStage6Distribution(new LinkedHashMap<>());
            vo.setCustomerLayerDistribution(new LinkedHashMap<>());
            vo.setMilestoneStatistics(new ArrayList<>());
            return vo;
        }

        vo.setTotalProjects(projectIds.size());

        Map<String, Integer> pNodeDist = new LinkedHashMap<>();
        for (int i = 1; i <= 8; i++) {
            pNodeDist.put("P" + i, Math.toIntExact(projectMapper.selectCount(
                    new QueryWrapper<Project>().in("id", projectIds).eq("p_node", i)
            )));
        }
        vo.setPNodeDistribution(pNodeDist);

        Map<String, Integer> stage6Dist = new LinkedHashMap<>();
        stage6Dist.put("价值验证", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("stage_6", "VALUE_VERIFY")
        )));
        stage6Dist.put("立项", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("stage_6", "LIXIANG")
        )));
        stage6Dist.put("招投标", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("stage_6", "ZHAOBIAO")
        )));
        stage6Dist.put("合同", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("stage_6", "HETONG")
        )));
        stage6Dist.put("服务", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("stage_6", "FUWU")
        )));
        stage6Dist.put("续签", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("stage_6", "XUQIAN")
        )));
        vo.setStage6Distribution(stage6Dist);

        Map<String, Integer> layerDist = new LinkedHashMap<>();
        layerDist.put("A类", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("customer_layer", "A")
        )));
        layerDist.put("B类", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("customer_layer", "B")
        )));
        layerDist.put("C类", Math.toIntExact(projectMapper.selectCount(
                new QueryWrapper<Project>().in("id", projectIds).eq("customer_layer", "C")
        )));
        vo.setCustomerLayerDistribution(layerDist);

        List<ProjectMilestone> milestones = projectMilestoneMapper.selectList(
                new QueryWrapper<ProjectMilestone>().in("project_id", projectIds)
        );

        int total = milestones.size();
        List<ProjectStatisticsVO.MilestoneStatisticsVO> milestoneStats = new ArrayList<>();
        milestoneStats.add(createMilestoneStat("提前开通业务", milestones, ProjectMilestone::getPreOpenBusiness, total));
        milestoneStats.add(createMilestoneStat("招标挂网", milestones, ProjectMilestone::getBiddingPublished, total));
        milestoneStats.add(createMilestoneStat("项目投标", milestones, ProjectMilestone::getBidSubmitted, total));
        milestoneStats.add(createMilestoneStat("中标挂网", milestones, ProjectMilestone::getBidWonPublished, total));
        milestoneStats.add(createMilestoneStat("签订合同", milestones, ProjectMilestone::getContractSigned, total));
        milestoneStats.add(createMilestoneStat("正常开通", milestones, ProjectMilestone::getServiceOpened, total));
        milestoneStats.add(createMilestoneStat("项目验收", milestones, ProjectMilestone::getAcceptanceDone, total));
        milestoneStats.add(createMilestoneStat("开具发票", milestones, ProjectMilestone::getInvoiceIssued, total));
        milestoneStats.add(createMilestoneStat("支付手续", milestones, ProjectMilestone::getPaymentDone, total));
        milestoneStats.add(createMilestoneStat("支付服务款", milestones, ProjectMilestone::getServiceFeeReceived, total));
        vo.setMilestoneStatistics(milestoneStats);

        return vo;
    }

    private ProjectStatisticsVO.MilestoneStatisticsVO createMilestoneStat(
            String name, List<ProjectMilestone> milestones, java.util.function.Function<ProjectMilestone, Integer> getter, int total) {
        ProjectStatisticsVO.MilestoneStatisticsVO stat = new ProjectStatisticsVO.MilestoneStatisticsVO();
        stat.setName(name);
        int completed = (int) milestones.stream().filter(m -> getter.apply(m) != null && getter.apply(m) == 1).count();
        stat.setCompletedCount(completed);
        stat.setRate(total > 0 ? new BigDecimal(completed)
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return stat;
    }

    private Long getUserChannelId(Long userId) {
        com.cy.crm.module.admin.entity.UserChannel uc = userChannelMapper.selectOne(
                new QueryWrapper<com.cy.crm.module.admin.entity.UserChannel>()
                        .eq("user_id", userId)
                        .eq("assign_type", 2)
                        .last("LIMIT 1")
        );
        return uc != null ? uc.getChannelId() : null;
    }

    private List<Long> getUserProjectIds(Long userId, List<Long> roleIds) {
        if (roleIds != null && roleIds.contains(4L)) {
            return projectMapper.selectList(
                    new QueryWrapper<Project>().eq("owner_bd_id", userId)
            ).stream().map(Project::getId).collect(Collectors.toList());
        }
        return projectMapper.selectList(new QueryWrapper<>()).stream()
                .map(Project::getId).collect(Collectors.toList());
    }
}
