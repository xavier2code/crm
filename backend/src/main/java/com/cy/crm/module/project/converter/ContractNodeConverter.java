package com.cy.crm.module.project.converter;

import com.cy.crm.module.project.dto.ContractNodeRequest;
import com.cy.crm.module.project.entity.ContractNode;
import com.cy.crm.module.project.vo.ProjectDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * 合同节点对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface ContractNodeConverter {

    ContractNodeConverter INSTANCE = Mappers.getMapper(ContractNodeConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    ContractNode requestToEntity(ContractNodeRequest request);

    /**
     * 更新实体
     * projectId/createdAt 不应由请求更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ContractNodeRequest request, @MappingTarget ContractNode node);

    /**
     * 实体 -> VO
     */
    ProjectDetailVO.ContractNodeVO entityToVO(ContractNode node);
}
