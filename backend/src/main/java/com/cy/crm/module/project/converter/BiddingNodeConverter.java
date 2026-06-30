package com.cy.crm.module.project.converter;

import com.cy.crm.module.project.dto.BiddingNodeRequest;
import com.cy.crm.module.project.entity.BiddingNode;
import com.cy.crm.module.project.vo.ProjectDetailVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * 招投标节点对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface BiddingNodeConverter {

    BiddingNodeConverter INSTANCE = Mappers.getMapper(BiddingNodeConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    BiddingNode requestToEntity(BiddingNodeRequest request);

    /**
     * 更新实体
     * projectId/createdAt 不应由请求更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(BiddingNodeRequest request, @MappingTarget BiddingNode node);

    /**
     * 实体 -> VO
     * purchaseMethodName 由 Service 层设置
     */
    @Mapping(target = "purchaseMethodName", ignore = true)
    ProjectDetailVO.BiddingNodeVO entityToVO(BiddingNode node);
}
