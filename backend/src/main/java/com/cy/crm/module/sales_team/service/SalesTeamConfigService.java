package com.cy.crm.module.sales_team.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.cy.crm.module.sales_team.converter.SalesTeamConfigConverter;
import com.cy.crm.module.sales_team.dto.SalesTeamConfigRequest;
import com.cy.crm.module.sales_team.entity.SalesTeamConfig;
import com.cy.crm.module.sales_team.mapper.SalesTeamConfigMapper;
import com.cy.crm.module.sales_team.vo.SalesTeamConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 销售梯队配置 Service
 *
 * 业务规则：
 * - 后台可配置，供渠道工作台按"梯队 + 区域"筛选客户/单位
 * - (team_code, region_code, effective_from) 唯一
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesTeamConfigService extends ServiceImpl<SalesTeamConfigMapper, SalesTeamConfig> {

    private static final String DICT_TYPE_REGION = "region";
    private static final String DICT_TYPE_SALES_TEAM = "sales_team";

    private final SalesTeamConfigMapper salesTeamConfigMapper;
    private final SalesTeamConfigConverter converter;
    private final DictionaryService dictionaryService;
    private final CurrentUserService currentUserService;

    /**
     * 分页查询销售梯队配置
     */
    public Page<SalesTeamConfigVO> pageConfigs(Long current, Long size,
                                               String regionCode, String teamCode) {
        QueryWrapper<SalesTeamConfig> wrapper = new QueryWrapper<>();
        if (regionCode != null && !regionCode.isEmpty()) {
            wrapper.eq("region_code", regionCode);
        }
        if (teamCode != null && !teamCode.isEmpty()) {
            wrapper.eq("team_code", teamCode);
        }
        wrapper.orderByAsc("sort").orderByDesc("created_at");

        Page<SalesTeamConfig> page = salesTeamConfigMapper.selectPage(new Page<>(current, size), wrapper);
        Page<SalesTeamConfigVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    /**
     * 按区域查询有效梯队列表
     */
    public List<SalesTeamConfigVO> listByRegion(String regionCode) {
        QueryWrapper<SalesTeamConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("region_code", regionCode);
        LocalDate today = LocalDate.now();
        wrapper.and(w -> w.isNull("effective_from").or().le("effective_from", today));
        wrapper.and(w -> w.isNull("effective_to").or().ge("effective_to", today));
        wrapper.orderByAsc("sort");
        return salesTeamConfigMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询详情
     */
    public SalesTeamConfigVO getConfig(Long id) {
        SalesTeamConfig entity = salesTeamConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.resourceNotFound("销售梯队配置");
        }
        return toVO(entity);
    }

    /**
     * 新建梯队配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(SalesTeamConfigRequest request) {
        validateDateRange(request);
        SalesTeamConfig entity = converter.requestToEntity(request);
        entity.setCreatedBy(currentUserService.getCurrentUserId());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        try {
            salesTeamConfigMapper.insert(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(3021, "该区域下已存在相同的梯队编码与生效日期");
        }
        return entity.getId();
    }

    /**
     * 编辑梯队配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(Long id, SalesTeamConfigRequest request) {
        validateDateRange(request);
        SalesTeamConfig entity = salesTeamConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.resourceNotFound("销售梯队配置");
        }
        converter.updateEntityFromRequest(request, entity);
        entity.setUpdatedAt(LocalDateTime.now());
        try {
            salesTeamConfigMapper.updateById(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(3021, "该区域下已存在相同的梯队编码与生效日期");
        }
    }

    /**
     * 删除梯队配置（软删）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SalesTeamConfig entity = salesTeamConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.resourceNotFound("销售梯队配置");
        }
        salesTeamConfigMapper.deleteById(id);
    }

    // ========== 内部辅助 ==========

    private SalesTeamConfigVO toVO(SalesTeamConfig entity) {
        SalesTeamConfigVO vo = converter.entityToVO(entity);
        vo.setRegionName(dictionaryService.getDictionaryName(DICT_TYPE_REGION, entity.getRegionCode()));
        return vo;
    }

    private void validateDateRange(SalesTeamConfigRequest request) {
        if (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
                && request.getEffectiveFrom().isAfter(request.getEffectiveTo())) {
            throw BusinessException.paramError("有效期起不能晚于有效期止");
        }
    }
}
