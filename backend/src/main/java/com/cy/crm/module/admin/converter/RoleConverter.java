package com.cy.crm.module.admin.converter;

import com.cy.crm.module.admin.dto.RoleRequest;
import com.cy.crm.module.admin.entity.Role;
import com.cy.crm.module.admin.vo.RoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 角色对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {

    RoleConverter INSTANCE = Mappers.getMapper(RoleConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    Role requestToEntity(RoleRequest request);

    /**
     * 更新实体
     * isBuiltin 为内置标志，不应由请求更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isBuiltin", ignore = true)
    void updateEntityFromRequest(RoleRequest request, @MappingTarget Role role);

    /**
     * 实体 -> VO
     */
    RoleVO entityToVO(Role role);

    /**
     * 实体列表 -> VO列表
     */
    List<RoleVO> entitiesToVOs(List<Role> roles);
}
