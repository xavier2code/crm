package com.cy.crm.module.project.converter;

import com.cy.crm.module.project.dto.BiddingNodeRequest;
import com.cy.crm.module.project.dto.ContractNodeRequest;
import com.cy.crm.module.project.dto.ProjectRequest;
import com.cy.crm.module.project.entity.BiddingNode;
import com.cy.crm.module.project.entity.ContractNode;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.entity.ProjectMilestone;
import com.cy.crm.module.project.entity.ProjectScore;
import com.cy.crm.module.project.vo.ProjectDetailVO;
import com.cy.crm.module.project.vo.ProjectVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 项目对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface ProjectConverter {

    ProjectConverter INSTANCE = Mappers.getMapper(ProjectConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    Project requestToEntity(ProjectRequest request);

    /**
     * 更新实体
     * opportunityId/version/createdAt 不应由请求更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "opportunityId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "PNode", ignore = true)
    @Mapping(target = "stage6", ignore = true)
    @Mapping(target = "trialAt", ignore = true)
    @Mapping(target = "formalAt", ignore = true)
    @Mapping(target = "expireAt", ignore = true)
    void updateEntityFromRequest(ProjectRequest request, @MappingTarget Project project);

    /**
     * 实体 -> VO
     * businessDomainName/adminLevelName/statusName/pNodeName/stage6Name/ownerBdName/salesUserName/completionRate/currentScore 由 Service 层设置
     */
    @Mapping(target = "businessDomainName", ignore = true)
    @Mapping(target = "adminLevelName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    @Mapping(target = "PNodeName", ignore = true)
    @Mapping(target = "stage6Name", ignore = true)
    @Mapping(target = "ownerBdName", ignore = true)
    @Mapping(target = "salesUserName", ignore = true)
    @Mapping(target = "completionRate", ignore = true)
    @Mapping(target = "currentScore", ignore = true)
    ProjectVO entityToVO(Project project);

    /**
     * 实体列表 -> VO列表
     */
    List<ProjectVO> entitiesToVOs(List<Project> projects);

    /**
     * 招投标节点请求 -> 实体
     */
    BiddingNode biddingNodeRequestToEntity(BiddingNodeRequest request);

    /**
     * 合同节点请求 -> 实体
     */
    ContractNode contractNodeRequestToEntity(ContractNodeRequest request);

    /**
     * 里程碑实体 -> VO
     */
    ProjectDetailVO.MilestoneVO milestoneToVO(ProjectMilestone milestone);

    /**
     * 招投标节点实体 -> VO
     * purchaseMethodName 由 Service 层设置
     */
    @Mapping(target = "purchaseMethodName", ignore = true)
    ProjectDetailVO.BiddingNodeVO biddingNodeToVO(BiddingNode node);

    /**
     * 合同节点实体 -> VO
     */
    ProjectDetailVO.ContractNodeVO contractNodeToVO(ContractNode node);
}
