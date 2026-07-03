package com.cy.crm.module.rebate.converter;

import com.cy.crm.module.rebate.dto.RebateRequest;
import com.cy.crm.module.rebate.entity.Rebate;
import com.cy.crm.module.rebate.vo.RebateVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
/**
 * 返利对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface RebateConverter {

    /**
     * 请求 DTO -> 实体
     * id/createdAt/updatedAt/isDeleted/version 由框架自动管理
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "paymentNodeId", ignore = true)
    Rebate requestToEntity(RebateRequest request);

    /**
     * 更新实体
     * id/rebateType/审计字段 不可通过请求覆盖
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rebateType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "paymentNodeId", ignore = true)
    void updateEntityFromRequest(RebateRequest request, @MappingTarget Rebate rebate);

    /**
     * 实体 -> VO
     * channelName/confirmStatusName/paymentStatusName/rebateTypeName 由 Service 层设置
     */
    @Mapping(target = "channelName", ignore = true)
    @Mapping(target = "confirmStatusName", ignore = true)
    @Mapping(target = "paymentStatusName", ignore = true)
    @Mapping(target = "rebateTypeName", ignore = true)
    RebateVO entityToVO(Rebate rebate);
}
