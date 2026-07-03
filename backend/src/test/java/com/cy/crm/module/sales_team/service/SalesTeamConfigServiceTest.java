package com.cy.crm.module.sales_team.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.sales_team.dto.SalesTeamConfigRequest;
import com.cy.crm.module.sales_team.entity.SalesTeamConfig;
import com.cy.crm.module.sales_team.mapper.SalesTeamConfigMapper;
import com.cy.crm.module.sales_team.vo.SalesTeamConfigVO;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.JwtAuthenticationToken;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销售梯队配置服务单测
 *
 * 覆盖：
 *  - 正常 CRUD
 *  - 按区域查询有效梯队
 *  - 日期范围校验
 *  - 重复 (team_code, region_code, effective_from) 拦截
 *  - 删除/更新不存在记录抛 1002
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SalesTeamConfigServiceTest {

    @Autowired private SalesTeamConfigService salesTeamConfigService;
    @Autowired private SalesTeamConfigMapper salesTeamConfigMapper;
    @Autowired private UserMapper userMapper;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("sales_team_test_user");
        user.setPasswordHash("password");
        user.setRealName("测试用户");
        user.setCreatedBy(1L);
        userMapper.insert(user);
        userId = user.getId();

        DataScope allScope = new DataScope();
        allScope.setAll(true);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                user.getUsername(), Collections.emptyList(), userId,
                Collections.emptyList(), Collections.emptyList(), allScope);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SalesTeamConfigRequest newRequest() {
        SalesTeamConfigRequest req = new SalesTeamConfigRequest();
        req.setTeamCode("TEAM_1");
        req.setTeamName("第一梯队");
        req.setRegionCode("EAST_CHINA");
        req.setUnitCodes("UNIT_A,UNIT_B");
        req.setSort(1);
        req.setRemark("测试");
        req.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        req.setEffectiveTo(LocalDate.of(2026, 12, 31));
        return req;
    }

    @Test
    public void shouldCreateConfig() {
        Long id = salesTeamConfigService.createConfig(newRequest());
        SalesTeamConfig entity = salesTeamConfigMapper.selectById(id);
        assertNotNull(entity);
        assertEquals("TEAM_1", entity.getTeamCode());
        assertEquals("EAST_CHINA", entity.getRegionCode());
        assertEquals(userId, entity.getCreatedBy());
    }

    @Test
    public void shouldUpdateConfig() {
        Long id = salesTeamConfigService.createConfig(newRequest());
        SalesTeamConfigRequest update = newRequest();
        update.setTeamName("第一梯队（更新）");
        update.setRemark("更新备注");
        salesTeamConfigService.updateConfig(id, update);

        SalesTeamConfig entity = salesTeamConfigMapper.selectById(id);
        assertEquals("第一梯队（更新）", entity.getTeamName());
        assertEquals("更新备注", entity.getRemark());
    }

    @Test
    public void shouldDeleteConfig() {
        Long id = salesTeamConfigService.createConfig(newRequest());
        salesTeamConfigService.deleteConfig(id);
        assertNull(salesTeamConfigMapper.selectById(id));
    }

    @Test
    public void shouldThrowWhenUpdateNotFound() {
        SalesTeamConfigRequest req = newRequest();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> salesTeamConfigService.updateConfig(99999999L, req));
        assertEquals(1002, ex.getCode());
    }

    @Test
    public void shouldThrowWhenDeleteNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> salesTeamConfigService.deleteConfig(99999999L));
        assertEquals(1002, ex.getCode());
    }

    @Test
    public void shouldListByRegionOnlyEffective() {
        SalesTeamConfigRequest past = newRequest();
        past.setEffectiveFrom(LocalDate.of(2024, 1, 1));
        past.setEffectiveTo(LocalDate.of(2024, 12, 31));
        salesTeamConfigService.createConfig(past);

        SalesTeamConfigRequest current = newRequest();
        current.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        current.setEffectiveTo(LocalDate.of(2026, 12, 31));
        salesTeamConfigService.createConfig(current);

        List<SalesTeamConfigVO> list = salesTeamConfigService.listByRegion("EAST_CHINA");
        assertEquals(1, list.size());
        assertEquals("TEAM_1", list.get(0).getTeamCode());
    }

    @Test
    public void shouldPageConfigsByRegionAndTeamCode() {
        salesTeamConfigService.createConfig(newRequest());

        SalesTeamConfigRequest other = newRequest();
        other.setTeamCode("TEAM_2");
        other.setTeamName("第二梯队");
        other.setRegionCode("SOUTH_CHINA");
        salesTeamConfigService.createConfig(other);

        Page<SalesTeamConfigVO> page = salesTeamConfigService.pageConfigs(1L, 10L, "EAST_CHINA", "TEAM_1");
        assertEquals(1, page.getRecords().size());
        assertEquals("第一梯队", page.getRecords().get(0).getTeamName());
    }

    @Test
    public void shouldThrowWhenDateRangeInvalid() {
        SalesTeamConfigRequest req = newRequest();
        req.setEffectiveFrom(LocalDate.of(2026, 12, 31));
        req.setEffectiveTo(LocalDate.of(2026, 1, 1));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> salesTeamConfigService.createConfig(req));
        assertEquals(1001, ex.getCode());
    }

    @Test
    public void shouldThrowWhenDuplicateTeamCodeRegionEffectiveFrom() {
        salesTeamConfigService.createConfig(newRequest());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> salesTeamConfigService.createConfig(newRequest()));
        assertEquals(3021, ex.getCode());
    }

    @Test
    public void shouldGetConfigDetail() {
        Long id = salesTeamConfigService.createConfig(newRequest());
        SalesTeamConfigVO vo = salesTeamConfigService.getConfig(id);
        assertNotNull(vo);
        assertEquals("第一梯队", vo.getTeamName());
        assertEquals("华东", vo.getRegionName());
    }

    @Test
    public void shouldThrowWhenGetNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> salesTeamConfigService.getConfig(99999999L));
        assertEquals(1002, ex.getCode());
    }
}
