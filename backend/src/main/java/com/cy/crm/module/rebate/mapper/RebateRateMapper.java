package com.cy.crm.module.rebate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cy.crm.module.rebate.entity.RebateRate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RebateRateMapper extends BaseMapper<RebateRate> {

    @Select("SELECT COALESCE(rate, 0) AS rate " +
            "FROM t_rebate_rate " +
            "WHERE product_category = #{productCategory} " +
            "AND (channel_id = #{channelId} OR channel_id IS NULL) " +
            "AND effective_from <= #{effectiveDate} " +
            "AND (effective_to IS NULL OR effective_to >= #{effectiveDate}) " +
            "AND is_deleted = 0 " +
            "ORDER BY channel_id DESC NULLS LAST, effective_from DESC " +
            "LIMIT 1")
    BigDecimal findRateForChannelAndProduct(@Param("productCategory") String productCategory,
                                              @Param("channelId") Long channelId,
                                              @Param("effectiveDate") LocalDate effectiveDate);

    @Select("SELECT * FROM t_rebate_rate " +
            "WHERE channel_id = #{channelId} OR channel_id IS NULL " +
            "AND is_deleted = 0 " +
            "ORDER BY product_category, channel_id DESC NULLS LAST, effective_from DESC")
    List<RebateRate> findByChannelId(@Param("channelId") Long channelId);
}
