package com.cy.crm.module.admin.controller;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.dto.DictionaryRequest;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.vo.DictionaryVO;
import com.cy.crm.config.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DictionaryControllerTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DictionaryService dictionaryService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void all_shouldReturnGroupedDictionaries() throws Exception {
        DictionaryVO vo = new DictionaryVO();
        vo.setId(1L);
        vo.setType("test_type");
        vo.setCode("CODE");
        vo.setName("名称");

        when(dictionaryService.allByTypes()).thenReturn(Map.of("test_type", List.of(vo)));

        mockMvc.perform(get("/api/admin/dictionaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.test_type[0].name").value("名称"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void listByType_shouldReturnDictionaries() throws Exception {
        DictionaryVO vo = new DictionaryVO();
        vo.setId(1L);
        vo.setType("test_type");

        when(dictionaryService.listByType("test_type")).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/admin/dictionaries/test_type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].type").value("test_type"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void create_shouldReturnSuccess() throws Exception {
        doNothing().when(dictionaryService).create(any(DictionaryRequest.class));

        mockMvc.perform(post("/api/admin/dictionaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"test_type\",\"code\":\"NEW\",\"name\":\"新字典\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_shouldReturnSuccess() throws Exception {
        doNothing().when(dictionaryService).update(any(DictionaryRequest.class));

        mockMvc.perform(put("/api/admin/dictionaries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"test_type\",\"code\":\"CODE\",\"name\":\"更新\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void delete_shouldReturnSuccess() throws Exception {
        doNothing().when(dictionaryService).delete(1L);

        mockMvc.perform(delete("/api/admin/dictionaries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void delete_shouldReturnErrorWhenBuiltin() throws Exception {
        doThrow(BusinessException.dictionaryBuiltinNotDeletable())
                .when(dictionaryService).delete(1L);

        mockMvc.perform(delete("/api/admin/dictionaries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3009))
                .andExpect(jsonPath("$.message").value("预置字典项不可删除"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void delete_shouldReturnErrorWhenNotFound() throws Exception {
        doThrow(BusinessException.resourceNotFound("字典"))
                .when(dictionaryService).delete(999L);

        mockMvc.perform(delete("/api/admin/dictionaries/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("字典不存在"));
    }
}
