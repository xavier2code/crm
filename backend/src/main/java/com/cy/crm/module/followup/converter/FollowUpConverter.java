package com.cy.crm.module.followup.converter;

import com.cy.crm.module.followup.dto.FollowUpRequest;
import com.cy.crm.module.followup.entity.FollowUp;
import com.cy.crm.module.followup.vo.FollowUpVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 跟进记录对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface FollowUpConverter {

    FollowUpConverter INSTANCE = Mappers.getMapper(FollowUpConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    FollowUp requestToEntity(FollowUpRequest request);

    /**
     * 更新实体
     * createdBy 不应由请求更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(FollowUpRequest request, @MappingTarget FollowUp followUp);

    /**
     * 实体 -> VO
     * customerName/projectName/currentStageName/nextStageName/followUpMethodName/createdByName 由 Service 层查询后设置
     */
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "currentStageName", ignore = true)
    @Mapping(target = "nextStageName", ignore = true)
    @Mapping(target = "followUpMethodName", ignore = true)
    @Mapping(target = "contactName", ignore = true)
    @Mapping(target = "createdByName", ignore = true)
    FollowUpVO entityToVO(FollowUp followUp);

    /**
     * 实体列表 -> VO列表
     */
    List<FollowUpVO> entitiesToVOs(List<FollowUp> followUps);
}
