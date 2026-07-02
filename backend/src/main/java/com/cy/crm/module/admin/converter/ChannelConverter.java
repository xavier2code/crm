package com.cy.crm.module.admin.converter;

import com.cy.crm.module.admin.dto.ChannelRequest;
import com.cy.crm.module.admin.entity.Channel;
import com.cy.crm.module.admin.vo.ChannelVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 渠道对象映射器
 */
@Mapper(componentModel = "spring")
public interface ChannelConverter {

    Channel requestToEntity(ChannelRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(ChannelRequest request, @MappingTarget Channel channel);

    @Mapping(target = "heads", ignore = true)
    @Mapping(target = "bds", ignore = true)
    ChannelVO entityToVO(Channel channel);

    List<ChannelVO> entitiesToVOs(List<Channel> channels);
}
