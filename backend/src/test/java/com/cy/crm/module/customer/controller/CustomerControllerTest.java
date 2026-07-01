package com.cy.crm.module.customer.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.module.customer.vo.CustomerVO;
import com.cy.crm.module.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 客户控制器测试 - 验证分页参数校验
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Test
    @WithMockUser(username = "test_user", roles = {"USER"})
    void pageCustomers_sizeExceedsMax_shouldReturnValidationError() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .param("current", "1")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("参数校验失败"));
    }

    @Test
    @WithMockUser(username = "test_user", roles = {"USER"})
    void pageCustomers_sizeWithinMax_shouldReturnSuccess() throws Exception {
        when(customerService.pageCustomers(anyLong(), anyLong(), anyString(), anyLong(), any()))
                .thenReturn(new Page<CustomerVO>());

        mockMvc.perform(get("/api/customers")
                        .param("current", "1")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
