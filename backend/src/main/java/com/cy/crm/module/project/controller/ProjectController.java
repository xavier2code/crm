package com.cy.crm.module.project.controller;
import com.cy.crm.common.constant.RoleConstants;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.project.dto.*;
import com.cy.crm.module.project.entity.BiddingNode;
import com.cy.crm.module.project.entity.ContractNode;
import com.cy.crm.module.project.service.*;
import com.cy.crm.module.project.vo.ProjectDetailVO;
import com.cy.crm.module.project.vo.ProjectVO;
import com.cy.crm.module.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "项目管理")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final BiddingNodeService biddingNodeService;
    private final ContractNodeService contractNodeService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "分页查询项目列表")
    @GetMapping
    public ApiResult<Page<ProjectVO>> pageProjects(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size,
            @RequestParam(required = false) Integer status
    ) {
        // 手动限制分页大小，防止 DoS 攻击
        if (size != null && size > 100) {
            size = 100L;
        }
        Long userId = currentUserService.getCurrentUserId();
        List<Long> roleIds = currentUserService.getCurrentUserRoleIds();
        return ApiResult.success(projectService.pageProjects(current, size, status, userId, roleIds));
    }

    @Operation(summary = "获取项目详情")
    @GetMapping("/{id}")
    public ApiResult<ProjectDetailVO> getProject(@PathVariable Long id) {
        return ApiResult.success(projectService.getProjectById(id));
    }

    @Operation(summary = "创建项目")
    @PostMapping
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> createProject(@Valid @RequestBody ProjectRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ApiResult.success(projectService.createProject(request, userId));
    }

    @Operation(summary = "更新项目")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        projectService.updateProject(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "更新P级节点")
    @PutMapping("/{id}/p-node")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updatePNode(
            @PathVariable Long id,
            @RequestParam Integer pNode) {
        projectService.updatePNode(id, pNode);
        return ApiResult.success();
    }

    @Operation(summary = "更新6大阶段")
    @PutMapping("/{id}/stage-6")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateStage6(
            @PathVariable Long id,
            @RequestParam String stage6) {
        projectService.updateStage6(id, stage6);
        return ApiResult.success();
    }

    @Operation(summary = "转换项目状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> transitionStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam String reason) {
        Long userId = currentUserService.getCurrentUserId();
        projectService.transitionProjectStatus(id, status, reason, userId);
        return ApiResult.success();
    }

    @Operation(summary = "更新里程碑")
    @PutMapping("/{id}/milestone")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updateMilestone(
            @PathVariable Long id,
            @RequestBody ProjectDetailVO.MilestoneVO request) {
        projectService.updateMilestone(id, request);
        return ApiResult.success();
    }

    @Operation(summary = "提交双精评分")
    @PostMapping("/scores")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> submitScores(@Valid @RequestBody ProjectScoreRequest request) {
        projectService.submitScores(request);
        return ApiResult.success();
    }

    @Operation(summary = "获取评分维度配置")
    @GetMapping("/score-dimensions")
    public ApiResult<Map<String, Object>> getScoreDimensions() {
        return ApiResult.success(ProjectService.getScoreDimensions());
    }

    @Operation(summary = "添加回款节点")
    @PostMapping("/{id}/payment-nodes")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> addPaymentNode(
            @PathVariable Long id,
            @RequestBody ProjectDetailVO.PaymentNodeVO request) {
        return ApiResult.success(projectService.addPaymentNode(id, request));
    }

    @Operation(summary = "更新回款节点")
    @PutMapping("/payment-nodes/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Void> updatePaymentNode(
            @PathVariable Long id,
            @RequestBody ProjectDetailVO.PaymentNodeVO request) {
        projectService.updatePaymentNode(id, request);
        return ApiResult.success();
    }

    // ========== 招投标节点管理 ==========

    @Operation(summary = "获取项目招投标节点")
    @GetMapping("/{id}/bidding-node")
    public ApiResult<BiddingNode> getBiddingNode(@PathVariable Long id) {
        return ApiResult.success(biddingNodeService.getByProjectId(id));
    }

    @Operation(summary = "保存项目招投标节点")
    @PutMapping("/{id}/bidding-node")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> saveBiddingNode(
            @PathVariable Long id,
            @RequestBody BiddingNodeRequest request) {
        return ApiResult.success(biddingNodeService.saveBiddingNode(id, request));
    }

    // ========== 合同节点管理 ==========

    @Operation(summary = "获取项目合同节点")
    @GetMapping("/{id}/contract-node")
    public ApiResult<ContractNode> getContractNode(@PathVariable Long id) {
        return ApiResult.success(contractNodeService.getByProjectId(id));
    }

    @Operation(summary = "保存项目合同节点")
    @PutMapping("/{id}/contract-node")
    @PreAuthorize("hasAnyAuthority(T(com.cy.crm.common.constant.RoleConstants).CHANNEL_BD, T(com.cy.crm.common.constant.RoleConstants).CHANNEL_HEAD, T(com.cy.crm.common.constant.RoleConstants).CYBD)")
    public ApiResult<Long> saveContractNode(
            @PathVariable Long id,
            @RequestBody ContractNodeRequest request) {
        return ApiResult.success(contractNodeService.saveContractNode(id, request));
    }
}
