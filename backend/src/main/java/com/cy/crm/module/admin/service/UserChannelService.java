package com.cy.crm.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.dto.ChannelAssignRequest;
import com.cy.crm.module.admin.entity.Channel;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.entity.UserChannel;
import com.cy.crm.module.admin.mapper.ChannelMapper;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.vo.ChannelAssignmentVO;
import com.cy.crm.module.admin.vo.UserVO;
import com.cy.crm.module.auth.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户-渠道分配 Service
 *
 * 业务规则（CRM-渠道版-开发文档.md）：
 * - 同一用户在同一渠道下可同时为「渠道负责人（assign_type=1）」和「渠道 BD（assign_type=2）」
 * - 撤销 = 硬删除 t_user_channel 记录
 */
@Service
@RequiredArgsConstructor
public class UserChannelService {

    private final UserChannelMapper userChannelMapper;
    private final ChannelMapper channelMapper;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;

    public static final int ASSIGN_TYPE_HEAD = 1;
    public static final int ASSIGN_TYPE_BD = 2;

    public List<ChannelAssignmentVO> listByChannel(Long channelId, Integer assignType) {
        QueryWrapper<UserChannel> qw = new QueryWrapper<UserChannel>().eq("channel_id", channelId);
        if (assignType != null) {
            qw.eq("assign_type", assignType);
        }
        qw.orderByAsc("assign_type").orderByAsc("assigned_at");
        return enrich(userChannelMapper.selectList(qw));
    }

    public List<ChannelAssignmentVO> listByUser(Long userId, Integer assignType) {
        QueryWrapper<UserChannel> qw = new QueryWrapper<UserChannel>().eq("user_id", userId);
        if (assignType != null) {
            qw.eq("assign_type", assignType);
        }
        return enrich(userChannelMapper.selectList(qw));
    }

    public long countByChannel(Long channelId) {
        Long n = userChannelMapper.selectCount(
                new QueryWrapper<UserChannel>().eq("channel_id", channelId));
        return n == null ? 0L : n;
    }

    public List<UserVO> listAssignableUsers(Integer assignType) {
        // 仅返回「启用状态」且「至少有一个分配类型」的用户；简单起见返回所有启用用户，让前端下拉过滤
        List<User> users = userMapper.selectList(
                new QueryWrapper<User>().eq("status", 1).orderByAsc("id"));
        List<UserVO> result = new ArrayList<>(users.size());
        for (User u : users) {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setStatus(u.getStatus());
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void assign(Long channelId, ChannelAssignRequest request) {
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            throw BusinessException.resourceNotFound("渠道");
        }
        if (request.getAssignType() == null
                || (request.getAssignType() != ASSIGN_TYPE_HEAD && request.getAssignType() != ASSIGN_TYPE_BD)) {
            throw BusinessException.paramError("非法的分配类型");
        }
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw BusinessException.resourceNotFound("用户");
        }
        Long exists = userChannelMapper.selectCount(
                new QueryWrapper<UserChannel>()
                        .eq("user_id", request.getUserId())
                        .eq("channel_id", channelId)
                        .eq("assign_type", request.getAssignType()));
        if (exists != null && exists > 0) {
            throw new BusinessException(3001, "该用户在此渠道已存在该身份");
        }
        UserChannel uc = new UserChannel();
        uc.setUserId(request.getUserId());
        uc.setChannelId(channelId);
        uc.setAssignType(request.getAssignType());
        Long operatorId = currentUserService.getCurrentUserId();
        uc.setAssignedBy(operatorId != null ? operatorId : 1L);
        uc.setAssignedAt(java.time.LocalDateTime.now());
        try {
            userChannelMapper.insert(uc);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(3001, "该用户在此渠道已存在该身份");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long channelId, Long userId, Integer assignType) {
        if (assignType == null
                || (assignType != ASSIGN_TYPE_HEAD && assignType != ASSIGN_TYPE_BD)) {
            throw BusinessException.paramError("非法的分配类型");
        }
        Long count = userChannelMapper.selectCount(
                new QueryWrapper<UserChannel>()
                        .eq("channel_id", channelId)
                        .eq("user_id", userId)
                        .eq("assign_type", assignType));
        if (count == null || count == 0) {
            throw BusinessException.resourceNotFound("分配记录");
        }
        userChannelMapper.delete(
                new QueryWrapper<UserChannel>()
                        .eq("channel_id", channelId)
                        .eq("user_id", userId)
                        .eq("assign_type", assignType));
    }

    private List<ChannelAssignmentVO> enrich(List<UserChannel> records) {
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = records.stream().map(UserChannel::getUserId).distinct().collect(Collectors.toList());
        List<Long> channelIds = records.stream().map(UserChannel::getChannelId).distinct().collect(Collectors.toList());
        List<Long> assignedByIds = records.stream().map(UserChannel::getAssignedBy)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Channel> channelMap = channelMapper.selectBatchIds(channelIds).stream()
                .collect(Collectors.toMap(Channel::getId, c -> c));
        Map<Long, User> assignedByMap = assignedByIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(assignedByIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<ChannelAssignmentVO> result = new ArrayList<>(records.size());
        for (UserChannel uc : records) {
            ChannelAssignmentVO vo = new ChannelAssignmentVO();
            vo.setChannelId(uc.getChannelId());
            vo.setUserId(uc.getUserId());
            vo.setAssignType(uc.getAssignType());
            vo.setAssignedBy(uc.getAssignedBy());
            vo.setAssignedAt(uc.getAssignedAt());
            User u = userMap.get(uc.getUserId());
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setRealName(u.getRealName());
            }
            Channel c = channelMap.get(uc.getChannelId());
            if (c != null) {
                vo.setChannelName(c.getName());
            }
            User ab = assignedByMap.get(uc.getAssignedBy());
            if (ab != null) {
                vo.setAssignedByName(ab.getRealName() != null ? ab.getRealName() : ab.getUsername());
            }
            result.add(vo);
        }
        return result;
    }
}
