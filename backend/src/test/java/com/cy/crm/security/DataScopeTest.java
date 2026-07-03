package com.cy.crm.security;

import com.cy.crm.module.admin.entity.DataPermission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataScope / DataScopeDimension 单元测试
 *
 * 覆盖：
 * 1. 维度枚举与 code 字符串映射
 * 2. DataScope.fromPermissions 转换逻辑（含历史数字兼容）
 * 3. unknown scopeType 不应被静默降级为 ALL
 */
class DataScopeTest {

    @Test
    void dimension_fromCode_resolvesAllKnownValues() {
        assertEquals(DataScopeDimension.ALL, DataScopeDimension.fromCode("ALL"));
        assertEquals(DataScopeDimension.CHANNEL, DataScopeDimension.fromCode("CHANNEL"));
        assertEquals(DataScopeDimension.REGION, DataScopeDimension.fromCode("REGION"));
        assertEquals(DataScopeDimension.UNIT, DataScopeDimension.fromCode("UNIT"));
        assertEquals(DataScopeDimension.BUSINESS_DOMAIN, DataScopeDimension.fromCode("BUSINESS_DOMAIN"));
        assertEquals(DataScopeDimension.POLICE_TYPE, DataScopeDimension.fromCode("POLICE_TYPE"));
        assertEquals(DataScopeDimension.SELF, DataScopeDimension.fromCode("SELF"));
    }

    @Test
    void dimension_fromCode_fallsBackToLegacyDigits() {
        // 兼容迁移期
        assertEquals(DataScopeDimension.ALL, DataScopeDimension.fromCode("1"));
        assertEquals(DataScopeDimension.CHANNEL, DataScopeDimension.fromCode("2"));
        assertEquals(DataScopeDimension.REGION, DataScopeDimension.fromCode("3"));
        assertEquals(DataScopeDimension.UNIT, DataScopeDimension.fromCode("4"));
        assertEquals(DataScopeDimension.SELF, DataScopeDimension.fromCode("5"));
    }

    @Test
    void dimension_fromCode_returnsNullOnUnknown() {
        assertNull(DataScopeDimension.fromCode(null));
        assertNull(DataScopeDimension.fromCode(""));
        assertNull(DataScopeDimension.fromCode("UNKNOWN"));
        assertNull(DataScopeDimension.fromCode("99"));
    }

    @Test
    void fromPermissions_emptyDefaultsToSelfOnly() {
        DataScope scope = DataScope.fromPermissions(List.of());
        assertTrue(scope.getSelfOnly());
        assertFalse(scope.getAll());
        assertTrue(scope.getChannelIds().isEmpty());
        assertTrue(scope.getUnitIds().isEmpty());
        assertTrue(scope.getRegions().isEmpty());
    }

    @Test
    void fromPermissions_nullDefaultsToSelfOnly() {
        DataScope scope = DataScope.fromPermissions(null);
        assertTrue(scope.getSelfOnly());
    }

    @Test
    void fromPermissions_allGrantsAllAccess() {
        DataPermission dp = perm("ALL", "");
        DataScope scope = DataScope.fromPermissions(List.of(dp));
        assertTrue(scope.getAll());
    }

    @Test
    void fromPermissions_channelGroupsIntoList() {
        DataScope scope = DataScope.fromPermissions(List.of(
                perm("CHANNEL", "10"),
                perm("CHANNEL", "20")));
        assertEquals(List.of(10L, 20L), scope.getChannelIds());
        assertFalse(scope.getAll());
    }

    @Test
    void fromPermissions_unitGroupsIntoList() {
        DataScope scope = DataScope.fromPermissions(List.of(
                perm("UNIT", "100"),
                perm("UNIT", "200")));
        assertEquals(List.of(100L, 200L), scope.getUnitIds());
    }

    @Test
    void fromPermissions_regionKeepsStrings() {
        DataScope scope = DataScope.fromPermissions(List.of(
                perm("REGION", "EAST_CHINA"),
                perm("REGION", "SOUTH_CHINA")));
        assertEquals(List.of("EAST_CHINA", "SOUTH_CHINA"), scope.getRegions());
    }

    @Test
    void fromPermissions_businessDomainAndPoliceTypeStayInMemory() {
        DataScope scope = DataScope.fromPermissions(List.of(
                perm("BUSINESS_DOMAIN", "SECURITY"),
                perm("POLICE_TYPE", "TRAFFIC")));
        assertEquals(List.of("SECURITY"), scope.getBusinessDomainCodes());
        assertEquals(List.of("TRAFFIC"), scope.getPoliceTypeCodes());
        // 不应触发 SQL 维度
        assertTrue(scope.getChannelIds().isEmpty());
        assertFalse(scope.getAll());
        assertFalse(scope.getSelfOnly());
    }

    @Test
    void fromPermissions_selfMarksSelfOnly() {
        DataScope scope = DataScope.fromPermissions(List.of(perm("SELF", "")));
        assertTrue(scope.getSelfOnly());
    }

    @Test
    void fromPermissions_invalidNumberValuesAreIgnored() {
        // CHANNEL 应是 Long；非数字 scopeValue 会被忽略，不抛异常
        DataScope scope = DataScope.fromPermissions(List.of(
                perm("CHANNEL", "not-a-number"),
                perm("CHANNEL", "10")));
        assertEquals(List.of(10L), scope.getChannelIds());
    }

    @Test
    void fromPermissions_unknownScopeTypeDoesNotEscalateToAll() {
        // 关键安全属性：未知 scopeType 必须被忽略，绝不静默降级为 ALL
        DataScope scope = DataScope.fromPermissions(List.of(
                perm("SOME_FUTURE_DIMENSION", "x")));
        assertFalse(scope.getAll());
        assertTrue(scope.getSelfOnly()); // 默认兜底
    }

    @Test
    void fromPermissions_nullScopeTypeSkipped() {
        DataScope scope = DataScope.fromPermissions(List.of(
                new DataPermission(),
                perm("CHANNEL", "5")));
        assertEquals(List.of(5L), scope.getChannelIds());
    }

    @Test
    void dimension_isSqlApplicable_matchesInterceptorExpectations() {
        assertTrue(DataScopeDimension.ALL.isSqlApplicable());
        assertTrue(DataScopeDimension.CHANNEL.isSqlApplicable());
        assertTrue(DataScopeDimension.REGION.isSqlApplicable());
        assertTrue(DataScopeDimension.UNIT.isSqlApplicable());
        assertTrue(DataScopeDimension.SELF.isSqlApplicable());
        assertFalse(DataScopeDimension.BUSINESS_DOMAIN.isSqlApplicable());
        assertFalse(DataScopeDimension.POLICE_TYPE.isSqlApplicable());
    }

    private static DataPermission perm(String scopeType, String scopeValue) {
        DataPermission dp = new DataPermission();
        dp.setScopeType(scopeType);
        dp.setScopeValue(scopeValue);
        return dp;
    }
}
