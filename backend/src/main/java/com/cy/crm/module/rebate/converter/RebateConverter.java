package com.cy.crm.module.rebate.converter;

import com.cy.crm.module.rebate.dto.RebateRequest;
import com.cy.crm.module.rebate.entity.Rebate;
import com.cy.crm.module.rebate.vo.RebateVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * 返利对象映射器
 * 符合开发文档 §38 MapStruct 映射规范
 */
@Mapper(componentModel = "spring")
public interface RebateConverter {

    RebateConverter INSTANCE = Mappers.getMapper(RebateConverter.class);

    /**
     * 请求 DTO -> 实体
     */
    Rebate requestToEntity(RebateRequest request);

    /**
     * 更新实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rebateType", ignore = true)
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
