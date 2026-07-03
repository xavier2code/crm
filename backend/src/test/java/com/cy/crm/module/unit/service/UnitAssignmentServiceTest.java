package com.cy.crm.module.unit.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.Channel;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.ChannelMapper;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.unit.dto.UnitAssignRequest;
import com.cy.crm.module.unit.entity.UnitAssignment;
import com.cy.crm.module.unit.mapper.UnitAssignmentMapper;
import com.cy.crm.module.unit.vo.UnitAssignmentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnitAssignmentServiceTest {

    @Mock private UnitAssignmentMapper unitAssignmentMapper;
    @Mock private UnitMapper unitMapper;
    @Mock private UserMapper userMapper;
    @Mock private ChannelMapper channelMapper;
    @Mock private UserChannelMapper userChannelMapper;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks private UnitAssignmentService service;

    private static final Long OP_ID = 100L;

    private void asAdmin() {
        when(currentUserService.getCurrentUserId()).thenReturn(OP_ID);
        when(currentUserService.getCurrentUserRoles()).thenReturn(List.of("ADMIN"));
    }

    private void asChannelHead() {
        when(currentUserService.getCurrentUserId()).thenReturn(OP_ID);
        when(currentUserService.getCurrentUserRoles()).thenReturn(List.of("CHANNEL_HEAD"));
    }

    private void asChannelBd() {
        when(currentUserService.getCurrentUserId()).thenReturn(OP_ID);
        when(currentUserService.getCurrentUserRoles()).thenReturn(List.of("CHANNEL_BD"));
    }

    @Test
    void assign_shouldThrowWhenScopeInvalid() {
        UnitAssignRequest req = new UnitAssignRequest();
        req.setUserId(1L);
        req.setAssignScope("INVALID");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(1L, req));
        assertEquals(1001, ex.getCode());
    }

    @Test
    void assign_shouldThrowWhenUnitNotFound() {
        asAdmin();
        UnitAssignRequest req = new UnitAssignRequest();
        req.setUserId(1L);
        req.setAssignScope(UnitAssignmentService.SCOPE_BD);

        when(unitMapper.selectById(1L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(1L, req));
        assertEquals(3001, ex.getCode());
    }

    @Test
    void assign_shouldInsertBdScope() {
        asAdmin();
        Unit unit = new Unit();
        unit.setId(10L);
        unit.setRegion("REGION_A");
        User user = new User();
        user.setId(20L);

        when(unitMapper.selectById(10L)).thenReturn(unit);
        when(userMapper.selectById(20L)).thenReturn(user);
        when(unitAssignmentMapper.insert(any(UnitAssignment.class))).thenReturn(1);

        UnitAssignRequest req = new UnitAssignRequest();
        req.setUserId(20L);
        req.setAssignScope(UnitAssignmentService.SCOPE_BD);

        service.assign(10L, req);

        ArgumentCaptor<UnitAssignment> cap = ArgumentCaptor.forClass(UnitAssignment.class);
        verify(unitAssignmentMapper).insert(cap.capture());
        UnitAssignment ua = cap.getValue();
        assertEquals(10L, ua.getUnitId());
        assertEquals(20L, ua.getUserId());
        assertEquals("BD", ua.getAssignScope());
        assertEquals(0L, ua.getChannelId()); // BD 范围用 0 作 sentinel
        assertEquals(OP_ID, ua.getAssignedBy());
        assertNotNull(ua.getAssignedAt());
    }

    @Test
    void assign_channelBdRequiresChannelId() {
        asAdmin();
        Unit unit = new Unit();
        unit.setId(10L);
        User user = new User();
        user.setId(20L);

        when(unitMapper.selectById(10L)).thenReturn(unit);
        when(userMapper.selectById(20L)).thenReturn(user);

        UnitAssignRequest req = new UnitAssignRequest();
        req.setUserId(20L);
        req.setAssignScope(UnitAssignmentService.SCOPE_CHANNEL_BD);
        // channelId = null

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(10L, req));
        assertEquals(1001, ex.getCode());
    }

    @Test
    void assign_channelBd_throwsWhenChannelMissing() {
        asAdmin();
        Unit unit = new Unit();
        unit.setId(10L);
        User user = new User();
        user.setId(20L);

        when(unitMapper.selectById(10L)).thenReturn(unit);
        when(userMapper.selectById(20L)).thenReturn(user);
        when(channelMapper.selectById(50L)).thenReturn(null);

        UnitAssignRequest req = new UnitAssignRequest();
        req.setUserId(20L);
        req.setAssignScope(UnitAssignmentService.SCOPE_CHANNEL_BD);
        req.setChannelId(50L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(10L, req));
        assertEquals(1002, ex.getCode());
    }

    @Test
    void assign_channelBd_forbiddenWhenNotHead() {
        asChannelHead();
        Unit unit = new Unit();
        unit.setId(10L);
        User user = new User();
        user.setId(20L);
        Channel channel = new Channel();
        channel.setId(50L);
        channel.setName("华东渠道");

        when(unitMapper.selectById(10L)).thenReturn(unit);
        when(userMapper.selectById(20L)).thenReturn(user);
        when(channelMapper.selectById(50L)).thenReturn(channel);
        when(userChannelMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        UnitAssignRequest req = new UnitAssignRequest();
        req.setUserId(20L);
        req.setAssignScope(UnitAssignmentService.SCOPE_CHANNEL_BD);
        req.setChannelId(50L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(10L, req));
        assertEquals(2004, ex.getCode());
    }

    @Test
    void assign_channelBd_okWhenHead() {
        asChannelHead();
        Unit unit = new Unit();
        unit.setId(10L);
        User user = new User();
        user.setId(20L);
        Channel channel = new Channel();
        channel.setId(50L);
        channel.setName("华东渠道");

        when(unitMapper.selectById(10L)).thenReturn(unit);
        when(userMapper.selectById(20L)).thenReturn(user);
        when(channelMapper.selectById(50L)).thenReturn(channel);
        when(userChannelMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(unitAssignmentMapper.insert(any(UnitAssignment.class))).thenReturn(1);

        UnitAssignRequest req = new UnitAssignRequest();
        req.setUserId(20L);
        req.setAssignScope(UnitAssignmentService.SCOPE_CHANNEL_BD);
        req.setChannelId(50L);

        service.assign(10L, req);

        ArgumentCaptor<UnitAssignment> cap = ArgumentCaptor.forClass(UnitAssignment.class);
        verify(unitAssignmentMapper).insert(cap.capture());
        assertEquals(50L, cap.getValue().getChannelId());
    }

    @Test
    void assign_throwsOnDuplicate() {
        asAdmin();
        Unit unit = new Unit();
        unit.setId(10L);
        unit.setRegion("R");
        User user = new User();
        user.setId(20L);

        when(unitMapper.selectById(10L)).thenReturn(unit);
        when(userMapper.selectById(20L)).thenReturn(user);
        when(unitAssignmentMapper.insert(any(UnitAssignment.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        UnitAssignRequest req = new UnitAssignRequest();
        req.setUserId(20L);
        req.setAssignScope(UnitAssignmentService.SCOPE_BD);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(10L, req));
        assertEquals(3001, ex.getCode());
    }

    @Test
    void revoke_throwsWhenRecordNotFound() {
        asAdmin();
        when(unitAssignmentMapper.selectById(99L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.revoke(99L));
        assertEquals(1002, ex.getCode());
    }

    @Test
    void revoke_bdScope_succeeds() {
        asAdmin();
        UnitAssignment ua = new UnitAssignment();
        ua.setId(1L);
        ua.setUnitId(10L);
        ua.setAssignScope("BD");
        when(unitAssignmentMapper.selectById(1L)).thenReturn(ua);
        Unit unit = new Unit();
        unit.setId(10L);
        unit.setRegion("R");
        when(unitMapper.selectById(10L)).thenReturn(unit);

        service.revoke(1L);
        verify(unitAssignmentMapper).deleteById(1L);
    }

    @Test
    void pageAssignments_channelBdForcesUserIdFilter() {
        asChannelBd();
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UnitAssignmentVO> emptyPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);
        // 关键：unitId/userId/channelId/assignScope 都会传 null，mockito 5 不接受 eq(null)
        // 这里用具名 any 即可
        when(unitAssignmentMapper.selectAssignmentPage(
                any(), isNull(), any(), isNull(), isNull()))
                .thenReturn(emptyPage);

        service.pageAssignments(null, null, null, null, 1L, 10L);

        verify(unitAssignmentMapper).selectAssignmentPage(
                any(), isNull(), any(), isNull(), isNull());
    }

    @Test
    void countByUser_returnsZeroWhenNull() {
        when(unitAssignmentMapper.selectCount(any(Wrapper.class))).thenReturn(null);
        assertEquals(0L, service.countByUser(1L));
    }
}
