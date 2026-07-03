package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.DataPermission;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.DataPermissionMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.vo.DataPermissionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DataPermissionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DataPermissionServiceTest {

    @Mock
    private DataPermissionMapper dataPermissionMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private DataPermissionService dataPermissionService;

    @Test
    void listByUser_returnsSevenDimensionVOs() {
        when(userMapper.selectById(1L)).thenReturn(new User());
        when(dataPermissionMapper.selectList(any()))
                .thenReturn(List.of(dp("CHANNEL", "10"), dp("REGION", "EAST")));

        List<DataPermissionVO> result = dataPermissionService.listByUser(1L);
        assertEquals(7, result.size());
        DataPermissionVO channel = result.stream()
                .filter(v -> "CHANNEL".equals(v.getScopeType())).findFirst().orElseThrow();
        assertEquals(List.of("10"), channel.getScopeValues());
        assertEquals("渠道", channel.getScopeTypeLabel());
    }

    @Test
    void listByUser_throwsWhenUserNotFound() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> dataPermissionService.listByUser(99L));
    }

    @Test
    void replaceScope_channelInsertValues() {
        when(userMapper.selectById(1L)).thenReturn(new User());

        dataPermissionService.replaceScope(1L, "CHANNEL", List.of("10", "20"));

        verify(dataPermissionMapper).delete(argThat(qw -> true));
        ArgumentCaptor<DataPermission> captor = ArgumentCaptor.forClass(DataPermission.class);
        verify(dataPermissionMapper, times(2)).insert(captor.capture());
        assertEquals("CHANNEL", captor.getAllValues().get(0).getScopeType());
        assertEquals("10", captor.getAllValues().get(0).getScopeValue());
        assertEquals("20", captor.getAllValues().get(1).getScopeValue());
    }

    @Test
    void replaceScope_emptyValuesDeletesAll() {
        when(userMapper.selectById(1L)).thenReturn(new User());

        dataPermissionService.replaceScope(1L, "CHANNEL", List.of());

        verify(dataPermissionMapper).delete(any());
        verify(dataPermissionMapper, never()).insert(any(com.cy.crm.module.admin.entity.DataPermission.class));
    }

    @Test
    void replaceScope_unknownScopeTypeThrows() {
        when(userMapper.selectById(1L)).thenReturn(new User());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> dataPermissionService.replaceScope(1L, "WHATEVER", List.of("x")));
        assertTrue(ex.getMessage().contains("WHATEVER"));
    }

    @Test
    void replaceScope_allInsertsMarkerRow() {
        when(userMapper.selectById(1L)).thenReturn(new User());

        dataPermissionService.replaceScope(1L, "ALL", List.of("ignored"));

        ArgumentCaptor<DataPermission> captor = ArgumentCaptor.forClass(DataPermission.class);
        verify(dataPermissionMapper, times(1)).insert(captor.capture());
        DataPermission row = captor.getValue();
        assertEquals("ALL", row.getScopeType());
        assertEquals("", row.getScopeValue());
    }

    private static DataPermission dp(String type, String value) {
        DataPermission d = new DataPermission();
        d.setScopeType(type);
        d.setScopeValue(value);
        return d;
    }
}
