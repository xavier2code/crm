package com.cy.crm.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.common.response.ApiResult;
import com.cy.crm.module.admin.dto.ChannelAssignRequest;
import com.cy.crm.module.admin.dto.ChannelRequest;
import com.cy.crm.module.admin.entity.Channel;
import com.cy.crm.module.admin.mapper.ChannelMapper;
import com.cy.crm.module.admin.service.UserChannelService;
import com.cy.crm.module.admin.vo.ChannelAssignmentVO;
import com.cy.crm.module.admin.vo.ChannelVO;
import com.cy.crm.module.admin.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 渠道主数据 + 渠道分配 controller。
 *
 * 渠道 CRUD 仅限管理员（system:channel）；渠道分配复用
 * {@link UserChannelService}，分配类型 1=渠道负责人 2=渠道 BD。
 */
@Tag(name = "渠道主数据")
@Validated
@RestController
@RequestMapping("/api/admin/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelMapper channelMapper;
    private final UserChannelService userChannelService;

    @Operation(summary = "渠道分页列表")
    @GetMapping
    public ApiResult<Page<ChannelVO>> pageChannels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") @Max(100) Long size) {
        Page<Channel> page = new Page<>(current, size);
        QueryWrapper<Channel> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like("name", keyword.trim());
        }
        if (region != null && !region.isBlank()) {
            qw.eq("region", region);
        }
        qw.orderByAsc("id");
        Page<Channel> result = channelMapper.selectPage(page, qw);
        Page<ChannelVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<ChannelVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        enrichAssignments(records);
        voPage.setRecords(records);
        return ApiResult.ok(voPage);
    }

    @Operation(summary = "全部启用渠道（下拉用）")
    @GetMapping("/all")
    public ApiResult<List<ChannelVO>> listAll() {
        List<Channel> list = channelMapper.selectList(
                new QueryWrapper<Channel>().eq("status", 1).orderByAsc("name"));
        List<ChannelVO> vos = list.stream().map(this::toVO).collect(Collectors.toList());
        enrichAssignments(vos);
        return ApiResult.ok(vos);
    }

    @Operation(summary = "渠道详情")
    @GetMapping("/{id}")
    public ApiResult<ChannelVO> getChannel(@PathVariable Long id) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw BusinessException.resourceNotFound("渠道");
        }
        ChannelVO vo = toVO(channel);
        enrichAssignments(List.of(vo));
        return ApiResult.ok(vo);
    }

    @Operation(summary = "创建渠道")
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Long> createChannel(@Valid @RequestBody ChannelRequest request) {
        if (channelNameExists(request.getName(), request.getRegion(), null)) {
            throw new BusinessException(3002, "同区域渠道名称已存在");
        }
        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setRegion(request.getRegion());
        channel.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        channelMapper.insert(channel);
        return ApiResult.ok(channel.getId());
    }

    @Operation(summary = "更新渠道")
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Void> updateChannel(@PathVariable Long id, @Valid @RequestBody ChannelRequest request) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw BusinessException.resourceNotFound("渠道");
        }
        if (channelNameExists(request.getName(), request.getRegion(), id)) {
            throw new BusinessException(3002, "同区域渠道名称已存在");
        }
        channel.setName(request.getName());
        channel.setRegion(request.getRegion());
        if (request.getStatus() != null) {
            channel.setStatus(request.getStatus());
        }
        channelMapper.updateById(channel);
        return ApiResult.ok();
    }

    @Operation(summary = "删除渠道")
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Void> deleteChannel(@PathVariable Long id) {
        Channel channel = channelMapper.selectById(id);
        if (channel == null) {
            throw BusinessException.resourceNotFound("渠道");
        }
        if (userChannelService.countByChannel(id) > 0) {
            throw new BusinessException(3006, "渠道已存在分配，请先撤销分配");
        }
        channelMapper.deleteById(id);
        return ApiResult.ok();
    }

    @Operation(summary = "查询某渠道下分配记录")
    @GetMapping("/{id}/assignments")
    public ApiResult<List<ChannelAssignmentVO>> listAssignments(
            @PathVariable Long id,
            @RequestParam(required = false) Integer assignType) {
        if (channelMapper.selectById(id) == null) {
            throw BusinessException.resourceNotFound("渠道");
        }
        return ApiResult.ok(userChannelService.listByChannel(id, assignType));
    }

    @Operation(summary = "新增渠道分配")
    @PostMapping("/{id}/assignments")
    public ApiResult<Void> assign(@PathVariable Long id, @Valid @RequestBody ChannelAssignRequest request) {
        userChannelService.assign(id, request);
        return ApiResult.ok();
    }

    @Operation(summary = "撤销渠道分配")
    @DeleteMapping("/{id}/assignments/{userId}")
    public ApiResult<Void> revoke(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam Integer assignType) {
        userChannelService.revoke(id, userId, assignType);
        return ApiResult.ok();
    }

    @Operation(summary = "可分配用户下拉")
    @GetMapping("/{id}/assignments/available-users")
    public ApiResult<List<UserVO>> listAvailableUsers(@PathVariable Long id) {
        return ApiResult.ok(userChannelService.listAssignableUsers(null));
    }

    private ChannelVO toVO(Channel c) {
        ChannelVO vo = new ChannelVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setRegion(c.getRegion());
        vo.setStatus(c.getStatus());
        return vo;
    }

    private void enrichAssignments(List<ChannelVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        for (ChannelVO vo : vos) {
            List<ChannelAssignmentVO> heads = userChannelService.listByChannel(vo.getId(),
                    UserChannelService.ASSIGN_TYPE_HEAD);
            List<ChannelAssignmentVO> bds = userChannelService.listByChannel(vo.getId(),
                    UserChannelService.ASSIGN_TYPE_BD);
            vo.setHeads(heads.stream().map(this::toUserVO).collect(Collectors.toList()));
            vo.setBds(bds.stream().map(this::toUserVO).collect(Collectors.toList()));
        }
    }

    private UserVO toUserVO(ChannelAssignmentVO a) {
        UserVO u = new UserVO();
        u.setId(a.getUserId());
        u.setUsername(a.getUsername());
        u.setRealName(a.getRealName());
        return u;
    }

    private boolean channelNameExists(String name, String region, Long excludeId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        QueryWrapper<Channel> qw = new QueryWrapper<Channel>().eq("name", name);
        if (region != null && !region.isBlank()) {
            qw.eq("region", region);
        }
        if (excludeId != null) {
            qw.ne("id", excludeId);
        }
        return channelMapper.selectCount(qw) > 0;
    }
}
