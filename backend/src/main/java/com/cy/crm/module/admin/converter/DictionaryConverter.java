package com.cy.crm.module.admin.converter;

import com.cy.crm.module.admin.dto.DictionaryRequest;
import com.cy.crm.module.admin.entity.Dictionary;
import com.cy.crm.module.admin.vo.DictionaryVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;

/**
 * 字典对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface DictionaryConverter {

    /**
     * 请求 DTO -> 实体
     */
    @Mapping(target = "isBuiltin", ignore = true)
    Dictionary requestToEntity(DictionaryRequest request);

    /**
     * 更新实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isBuiltin", ignore = true)
    void updateEntityFromRequest(DictionaryRequest request, @MappingTarget Dictionary dictionary);

    /**
     * 实体 -> VO
     */
    DictionaryVO entityToVO(Dictionary dictionary);

    /**
     * 实体列表 -> VO列表
     */
    List<DictionaryVO> entitiesToVOs(List<Dictionary> dictionaries);
}
