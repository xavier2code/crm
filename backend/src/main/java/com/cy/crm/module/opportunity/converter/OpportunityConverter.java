package com.cy.crm.module.opportunity.converter;

import com.cy.crm.module.opportunity.dto.OpportunityRequest;
import com.cy.crm.module.opportunity.entity.Opportunity;
import com.cy.crm.module.opportunity.vo.OpportunityVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 商机报备对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface OpportunityConverter {

    OpportunityConverter INSTANCE = Mappers.getMapper(OpportunityConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    Opportunity requestToEntity(OpportunityRequest request);

    /**
     * 更新实体
     * submittedBy/status/submitCount 不应由请求更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "submitCount", ignore = true)
    @Mapping(target = "stage", ignore = true)
    @Mapping(target = "lastFollowUpAt", ignore = true)
    @Mapping(target = "effectiveAt", ignore = true)
    @Mapping(target = "expiredAt", ignore = true)
    @Mapping(target = "coolingUntil", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(OpportunityRequest request, @MappingTarget Opportunity opportunity);

    /**
     * 实体 -> VO
     * customerName/businessDomainName/projectTypeName/statusName/submittedByName/approvedByName/editable/submittable/approvable 由 Service 层设置
     */
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "businessDomainName", ignore = true)
    @Mapping(target = "projectTypeName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    @Mapping(target = "submittedByName", ignore = true)
    @Mapping(target = "approvedByName", ignore = true)
    @Mapping(target = "editable", ignore = true)
    @Mapping(target = "submittable", ignore = true)
    @Mapping(target = "approvable", ignore = true)
    OpportunityVO entityToVO(Opportunity opportunity);

    /**
     * 实体列表 -> VO列表
     */
    List<OpportunityVO> entitiesToVOs(List<Opportunity> opportunities);
}
