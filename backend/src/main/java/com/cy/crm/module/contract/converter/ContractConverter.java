package com.cy.crm.module.contract.converter;

import com.cy.crm.module.contract.dto.ContractRequest;
import com.cy.crm.module.contract.entity.Contract;
import com.cy.crm.module.contract.vo.ContractVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * 合同对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface ContractConverter {

    ContractConverter INSTANCE = Mappers.getMapper(ContractConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    Contract requestToEntity(ContractRequest request);

    /**
     * 更新实体：将 DTO 字段复制到已有实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromRequest(ContractRequest request, @MappingTarget Contract contract);

    /**
     * 实体 -> VO
     * projectName/statusName 由 Service 层查询后设置，此处忽略
     */
    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "statusName", ignore = true)
    ContractVO entityToVO(Contract contract);
}
