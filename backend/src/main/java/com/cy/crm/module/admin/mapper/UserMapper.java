package com.cy.crm.module.admin.mapper;

import com.cy.crm.module.admin.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT r.* FROM t_role r " +
            "INNER JOIN t_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<com.cy.crm.module.admin.entity.Role> selectRolesByUserId(@Param("userId") Long userId);

    Page<User> selectUserPage(Page<User> page, @Param("keyword") String keyword);
}
