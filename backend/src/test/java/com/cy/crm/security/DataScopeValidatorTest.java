package com.cy.crm.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.DataPermission;
import com.cy.crm.module.admin.mapper.DataPermissionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DataScopeValidator 单元测试
 * 测试各种数据权限场景下的访问控制
 */
@ExtendWith(MockitoExtension.class)
class DataScopeValidatorTest {

    @Mock
    private DataPermissionMapper dataPermissionMapper;

    private DataScopeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DataScopeValidator(dataPermissionMapper);
    }

    @Test
    void validateUnitAccess_withAllPermission_shouldAllow() {
        // given
        DataScope allScope = DataScope.all();
        Long userId = 1L;
        Long unitId = 100L;

        // when & then - should not throw
        assertDoesNotThrow(() -> validator.validateUnitAccess(userId, unitId, allScope));
    }

    @Test
    void validateUnitAccess_withUnitPermission_shouldAllowMatchingUnit() {
        // given
        DataScope unitScope = new DataScope();
        List<Long> unitIds = List.of(100L, 200L, 300L);
        unitScope.setUnitIds(unitIds);

        Long userId = 1L;
        Long unitId = 200L;

        // when & then - should not throw for unit in scope
        assertDoesNotThrow(() -> validator.validateUnitAccess(userId, unitId, unitScope));
    }

    @Test
    void validateUnitAccess_withUnitPermission_shouldDenyNonMatchingUnit() {
        // given
        DataScope unitScope = new DataScope();
        List<Long> unitIds = List.of(100L, 200L, 300L);
        unitScope.setUnitIds(unitIds);

        Long userId = 1L;
        Long unitId = 999L; // Not in scope

        // when & then - should throw for unit not in scope
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateUnitAccess(userId, unitId, unitScope));
        assertEquals(2006, ex.getCode());
        assertTrue(ex.getMessage().contains("数据权限不足"));
    }

    @Test
    void validateUnitAccess_withSelfOnlyAndRequireOwn_shouldAllowOwnData() {
        // given
        DataScope selfScope = new DataScope();
        selfScope.setSelfOnly(true);

        Long userId = 1L;
        Long unitId = 100L;

        // Mock: user belongs to this unit
        when(dataPermissionMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // when & then - should not throw when user owns the unit
        assertDoesNotThrow(() -> validator.validateUnitAccess(userId, unitId, selfScope, true));
    }

    @Test
    void validateUnitAccess_withSelfOnlyAndRequireOwn_shouldDenyOthersData() {
        // given
        DataScope selfScope = new DataScope();
        selfScope.setSelfOnly(true);

        Long userId = 1L;
        Long unitId = 100L;

        // Mock: user does not belong to this unit
        when(dataPermissionMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        // when & then - should throw when user doesn't own the unit
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateUnitAccess(userId, unitId, selfScope, true));
        assertEquals(2006, ex.getCode());
        assertTrue(ex.getMessage().contains("数据权限不足"));
    }

    @Test
    void validateUnitAccess_withChannelPermission_shouldDeny() {
        // given
        DataScope channelScope = new DataScope();
        List<Long> channelIds = List.of(1L, 2L);
        channelScope.setChannelIds(channelIds);

        Long userId = 1L;
        Long unitId = 100L;

        // when & then - should throw when no unit access
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateUnitAccess(userId, unitId, channelScope));
        assertEquals(2006, ex.getCode());
    }

    @Test
    void validateUnitAccess_withEmptyScope_shouldDeny() {
        // given
        DataScope emptyScope = new DataScope();

        Long userId = 1L;
        Long unitId = 100L;

        // when & then - should throw when no permissions
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateUnitAccess(userId, unitId, emptyScope));
        assertEquals(2006, ex.getCode());
    }

    @Test
    void validateChannelAccess_withAllPermission_shouldAllow() {
        // given
        DataScope allScope = DataScope.all();
        Long userId = 1L;
        Long channelId = 100L;

        // when & then - should not throw
        assertDoesNotThrow(() -> validator.validateChannelAccess(userId, channelId, allScope));
    }

    @Test
    void validateChannelAccess_withChannelPermission_shouldAllowMatchingChannel() {
        // given
        DataScope channelScope = new DataScope();
        List<Long> channelIds = List.of(100L, 200L, 300L);
        channelScope.setChannelIds(channelIds);

        Long userId = 1L;
        Long channelId = 200L;

        // when & then - should not throw for channel in scope
        assertDoesNotThrow(() -> validator.validateChannelAccess(userId, channelId, channelScope));
    }

    @Test
    void validateChannelAccess_withChannelPermission_shouldDenyNonMatchingChannel() {
        // given
        DataScope channelScope = new DataScope();
        List<Long> channelIds = List.of(100L, 200L, 300L);
        channelScope.setChannelIds(channelIds);

        Long userId = 1L;
        Long channelId = 999L; // Not in scope

        // when & then - should throw for channel not in scope
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateChannelAccess(userId, channelId, channelScope));
        assertEquals(2006, ex.getCode());
        assertTrue(ex.getMessage().contains("数据权限不足") || ex.getMessage().contains("无权访问"));
    }

    @Test
    void validateChannelAccess_withUnitPermission_shouldDeny() {
        // given
        DataScope unitScope = new DataScope();
        List<Long> unitIds = List.of(1L, 2L);
        unitScope.setUnitIds(unitIds);

        Long userId = 1L;
        Long channelId = 100L;

        // when & then - should throw when no channel access
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateChannelAccess(userId, channelId, unitScope));
        assertEquals(2006, ex.getCode());
    }

    @Test
    void validateCreatorAccess_withSelfOnly_shouldAllowOwnData() {
        // given
        DataScope selfScope = new DataScope();
        selfScope.setSelfOnly(true);

        Long userId = 1L;
        Long creatorId = 1L; // Same user

        // when & then - should not throw for own data
        assertDoesNotThrow(() -> validator.validateCreatorAccess(userId, creatorId, selfScope));
    }

    @Test
    void validateCreatorAccess_withSelfOnly_shouldDenyOthersData() {
        // given
        DataScope selfScope = new DataScope();
        selfScope.setSelfOnly(true);

        Long userId = 1L;
        Long creatorId = 2L; // Different user

        // when & then - should throw for others' data
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateCreatorAccess(userId, creatorId, selfScope));
        assertEquals(2006, ex.getCode());
        assertTrue(ex.getMessage().contains("数据权限不足") || ex.getMessage().contains("无权访问"));
    }

    @Test
    void validateCreatorAccess_withAllPermission_shouldAllowAnyCreator() {
        // given
        DataScope allScope = DataScope.all();

        Long userId = 1L;
        Long creatorId = 999L; // Different user

        // when & then - should not throw for any data
        assertDoesNotThrow(() -> validator.validateCreatorAccess(userId, creatorId, allScope));
    }
}
