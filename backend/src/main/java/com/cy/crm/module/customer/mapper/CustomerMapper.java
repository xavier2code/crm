package com.cy.crm.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cy.crm.module.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
