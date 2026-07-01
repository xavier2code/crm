package com.cy.crm.module.contract.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.contract.dto.ContractRequest;
import com.cy.crm.module.project.entity.Project;
import com.cy.crm.module.project.mapper.ProjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 合同服务测试 - 验证唯一约束
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class ContractServiceTest {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ProjectMapper projectMapper;

    @Test
    public void testCreateContract_duplicateProjectId_throwsException() {
        // 创建一个测试项目
        Project project = new Project();
        project.setName("测试项目");
        project.setOpportunityId(1L);
        project.setAmount(BigDecimal.valueOf(100000));
        project.setStatus(1);
        projectMapper.insert(project);

        ContractRequest request = new ContractRequest();
        request.setProjectId(project.getId());
        request.setAmount(BigDecimal.valueOf(100000));

        // 第一次创建应该成功
        Long contractId = contractService.createContract(request);
        assertNotNull(contractId);

        // 第二次创建应该抛出异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            contractService.createContract(request);
        });

        assertEquals(6001, exception.getCode());
        assertTrue(exception.getMessage().contains("该项目已有合同"));
    }
}
