package com.cy.crm.module.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.constant.RoleConstants;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.entity.Channel;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.entity.UserChannel;
import com.cy.crm.module.admin.mapper.ChannelMapper;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.unit.dto.UnitAssignRequest;
import com.cy.crm.module.unit.entity.UnitAssignment;
import com.cy.crm.module.unit.mapper.UnitAssignmentMapper;
import com.cy.crm.module.unit.vo.UnitAssignmentPage;
import com.cy.crm.module.unit.vo.UnitAssignmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 单位分配 Service（业务侧 4 级分配链路，详见 CRM-渠道版-开发文档.md §9.5）。
 *
 * 业务规则：
 * - assignScope = BD：大区总 / BD 链路（仅需 unitId + userId）
 * - assignScope = CHANNEL_BD：渠道负责人 → 渠道 BD 链路（需 unitId + userId + channelId）
 * - 同一 (unit, user, channel) 唯一，由数据库唯一索引 uk_unit_assignment_triplet 保证
 * - 渠道 BD 角色只能新增 CHANNEL_BD 范围且 channelId 必须是自己作为 head 的渠道
 */
@Service
@RequiredArgsConstructor
public class UnitAssignmentService {

    public static final String SCOPE_BD = "BD";
    public static final String SCOPE_CHANNEL_BD = "CHANNEL_BD";

    private final UnitAssignmentMapper unitAssignmentMapper;
    private final UnitMapper unitMapper;
    private final UserMapper userMapper;
    private final ChannelMapper channelMapper;
    private final UserChannelMapper userChannelMapper;
    private final CurrentUserService currentUserService;

    /**
     * 分页查询分配记录（业务侧）。
     *
     * <p>渠道 BD 只看到自己的分配；其他角色看到全量或按区域过滤的分配。
     */
    public UnitAssignmentPage pageAssignments(Long unitId, Long userId, Long channelId,
                                             String assignScope, Long current, Long size) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Set<String> roles = roles(currentUserId);
        // 渠道 BD：强制过滤到 userId = 自己
        if (roles.contains(RoleConstants.CHANNEL_BD) && !roles.contains(RoleConstants.ADMIN)
                && !roles.contains(RoleConstants.CYBD)) {
            userId = currentUserId;
        }
        Page<UnitAssignmentVO> p = new Page<>(current, size);
        IPage<UnitAssignmentVO> res = unitAssignmentMapper.selectAssignmentPage(p, unitId, userId, channelId, assignScope);
        return new UnitAssignmentPage() {{
            setRecords(res.getRecords());
            setTotal(res.getTotal());
            setCurrent(res.getCurrent());
            setSize(res.getSize());
        }};
    }

    /**
     * 新增单位分配。渠道 BD 仅能指派自己 head 渠道下的单位给渠道 BD。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long assign(Long unitId, UnitAssignRequest request) {
        if (request.getAssignScope() == null
                || (!SCOPE_BD.equals(request.getAssignScope()) && !SCOPE_CHANNEL_BD.equals(request.getAssignScope()))) {
            throw BusinessException.paramError("非法的分配范围");
        }
        Unit unit = unitMapper.selectById(unitId);
        if (unit == null) {
            throw BusinessException.unitNotFound();
        }
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw BusinessException.resourceNotFound("用户");
        }
        if (SCOPE_CHANNEL_BD.equals(request.getAssignScope())) {
            if (request.getChannelId() == null) {
                throw BusinessException.paramError("CHANNEL_BD 范围必须指定渠道");
            }
            Channel channel = channelMapper.selectById(request.getChannelId());
            if (channel == null) {
                throw BusinessException.resourceNotFound("渠道");
            }
            assertAssignorCanOperateChannel(channel.getId());
        } else {
            // BD 范围：channelId 必须为空
            if (request.getChannelId() != null) {
                throw BusinessException.paramError("BD 范围不能指定渠道");
            }
            assertAssignorCanOperateRegion(unit.getRegion());
        }
        UnitAssignment ua = new UnitAssignment();
        ua.setUnitId(unitId);
        ua.setUserId(request.getUserId());
        ua.setAssignScope(request.getAssignScope());
        // BD 范围：channelId 存 0（sentinel）以兼容普通 unique 索引
        ua.setChannelId(SCOPE_BD.equals(request.getAssignScope())
                ? 0L
                : request.getChannelId());
        Long operator = currentUserService.getCurrentUserId();
        ua.setAssignedBy(operator != null ? operator : 1L);
        ua.setAssignedAt(LocalDateTime.now());
        try {
            unitAssignmentMapper.insert(ua);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(3001, "该单位已分配给该用户/渠道");
        }
        return ua.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        UnitAssignment existing = unitAssignmentMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.resourceNotFound("分配记录");
        }
        if (SCOPE_CHANNEL_BD.equals(existing.getAssignScope())) {
            assertAssignorCanOperateChannel(existing.getChannelId());
        } else {
            Unit unit = unitMapper.selectById(existing.getUnitId());
            if (unit != null) {
                assertAssignorCanOperateRegion(unit.getRegion());
            }
        }
        unitAssignmentMapper.deleteById(id);
    }

    public List<UnitAssignmentVO> listByUnit(Long unitId) {
        Page<UnitAssignmentVO> p = new Page<>(1, 100, false);
        IPage<UnitAssignmentVO> res = unitAssignmentMapper.selectAssignmentPage(p, unitId, null, null, null);
        List<UnitAssignmentVO> records = res.getRecords();
        for (UnitAssignmentVO vo : records) {
            if (vo.getChannelId() != null && vo.getChannelId() == 0L) {
                vo.setChannelId(null);
            }
        }
        return records;
    }

    public long countByUser(Long userId) {
        Long n = unitAssignmentMapper.selectCount(
                new QueryWrapper<UnitAssignment>().eq("user_id", userId));
        return n == null ? 0L : n;
    }

    public long countByUnit(Long unitId) {
        Long n = unitAssignmentMapper.selectCount(
                new QueryWrapper<UnitAssignment>().eq("unit_id", unitId));
        return n == null ? 0L : n;
    }

    // ---------- helpers ----------

    private Set<String> roles(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return currentUserService.getCurrentUserRoles().stream().collect(Collectors.toSet());
    }

    /**
     * 仅大区总/管理员/CYBD 可操作任意区域；其他人必须与单位区域一致。
     */
    private void assertAssignorCanOperateRegion(String unitRegion) {
        Set<String> roles = roles(currentUserService.getCurrentUserId());
        if (roles.contains(RoleConstants.ADMIN) || roles.contains(RoleConstants.CYBD)) {
            return;
        }
        if (roles.contains(RoleConstants.REGION_HEAD)) {
            // 大区总在 V2 seed 中 region 通过 dataScope 配置；这里简化为允许全部，
            // 进一步限制由 DataScope 拦截器在 unitMapper 查询时执行。
            return;
        }
        if (roles.contains(RoleConstants.CHANNEL_HEAD) || roles.contains(RoleConstants.CHANNEL_BD)) {
            // 渠道负责人 / 渠道 BD 通常不直接新增 BD 范围（应走 CHANNEL_BD 链路），
            // 若尝试则禁止
            throw BusinessException.forbidden();
        }
        throw BusinessException.forbidden();
    }

    /**
     * 操作 CHANNEL_BD 范围时，操作用户必须是该渠道的 head。
     */
    private void assertAssignorCanOperateChannel(Long channelId) {
        Set<String> roles = roles(currentUserService.getCurrentUserId());
        if (roles.contains(RoleConstants.ADMIN) || roles.contains(RoleConstants.CYBD)) {
            return;
        }
        Long operator = currentUserService.getCurrentUserId();
        Long count = userChannelMapper.selectCount(
                new QueryWrapper<UserChannel>()
                        .eq("user_id", operator)
                        .eq("channel_id", channelId)
                        .eq("assign_type", 1)); // 1 = head
        if (count == null || count == 0) {
            throw BusinessException.forbidden();
        }
    }
}
