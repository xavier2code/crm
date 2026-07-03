package com.cy.crm.module.unit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.module.unit.entity.UnitAssignment;
import com.cy.crm.module.unit.vo.UnitAssignmentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UnitAssignmentMapper extends BaseMapper<UnitAssignment> {

    /**
     * 分页查询分配记录，附带单位/用户/渠道/分配人名称。
     *
     * <p>对应 mapper xml：{@code resources/mapper/UnitAssignmentMapper.xml}。
     */
    IPage<UnitAssignmentVO> selectAssignmentPage(Page<UnitAssignmentVO> page,
                                                  @Param("unitId") Long unitId,
                                                  @Param("userId") Long userId,
                                                  @Param("channelId") Long channelId,
                                                  @Param("assignScope") String assignScope);
}
