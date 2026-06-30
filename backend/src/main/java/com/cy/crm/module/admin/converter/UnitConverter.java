package com.cy.crm.module.admin.converter;

import com.cy.crm.module.admin.dto.UnitRequest;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.module.admin.vo.UnitVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 单位对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface UnitConverter {

    UnitConverter INSTANCE = Mappers.getMapper(UnitConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    Unit requestToEntity(UnitRequest request);

    /**
     * 更新实体
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(UnitRequest request, @MappingTarget Unit unit);

    /**
     * 实体 -> VO
     */
    UnitVO entityToVO(Unit unit);

    /**
     * 实体列表 -> VO列表
     */
    List<UnitVO> entitiesToVOs(List<Unit> units);
}
