package com.cy.crm.module.admin.mapper;

import com.cy.crm.module.admin.entity.Unit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UnitMapper extends BaseMapper<Unit> {

    Page<Unit> selectUnitPage(Page<Unit> page,
                                @Param("keyword") String keyword,
                                @Param("region") String region);
}
