package com.cy.crm.module.admin.mapper;

import com.cy.crm.module.admin.entity.Dictionary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DictionaryMapper extends BaseMapper<Dictionary> {

    @Select("SELECT DISTINCT type FROM t_dictionary WHERE is_deleted = 0 ORDER BY type")
    List<String> selectTypes();

    @Select("SELECT * FROM t_dictionary WHERE type = #{type} AND is_deleted = 0 ORDER BY sort")
    List<Dictionary> selectByType(@Param("type") String type);
}
