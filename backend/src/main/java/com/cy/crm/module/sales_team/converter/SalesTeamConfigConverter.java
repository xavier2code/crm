package com.cy.crm.module.sales_team.converter;

import com.cy.crm.module.sales_team.dto.SalesTeamConfigRequest;
import com.cy.crm.module.sales_team.entity.SalesTeamConfig;
import com.cy.crm.module.sales_team.vo.SalesTeamConfigVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * 销售梯队配置对象映射
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface SalesTeamConfigConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    SalesTeamConfig requestToEntity(SalesTeamConfigRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(SalesTeamConfigRequest request, @MappingTarget SalesTeamConfig entity);

    @Mapping(target = "regionName", ignore = true)
    SalesTeamConfigVO entityToVO(SalesTeamConfig entity);
}
