package com.cy.crm.module.admin.converter;

import com.cy.crm.module.admin.dto.UserRequest;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;

/**
 * 用户对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * 请求 DTO -> 实体
     */
    User requestToEntity(UserRequest request);

    /**
     * 更新实体
     * passwordHash/createdBy/createdAt/isInitialPassword 不应由请求更新
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isInitialPassword", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    void updateEntityFromRequest(UserRequest request, @MappingTarget User user);

    /**
     * 实体 -> VO
     * roles 由 Service 层查询后设置
     */
    @Mapping(target = "roles", ignore = true)
    UserVO entityToVO(User user);

    /**
     * 实体列表 -> VO列表
     */
    List<UserVO> entitiesToVOs(List<User> users);
}
