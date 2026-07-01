package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.converter.UnitConverter;
import com.cy.crm.module.admin.dto.UnitRequest;
import com.cy.crm.module.admin.entity.Unit;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.vo.UnitVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitService extends ServiceImpl<UnitMapper, Unit> {

    private final UnitMapper unitMapper;
    private final UnitConverter unitConverter;

    public Page<UnitVO> pageUnits(String keyword, String region, Long current, Long size) {
        Page<Unit> page = unitMapper.selectUnitPage(new Page<>(current, size), keyword, region);
        Page<UnitVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(unitConverter::entityToVO).collect(Collectors.toList()));
        return result;
    }

    public List<UnitVO> listAll() {
        return unitMapper.selectList(new QueryWrapper<Unit>().eq("status", 1).orderByAsc("name"))
                .stream().map(unitConverter::entityToVO).collect(Collectors.toList());
    }

    public UnitVO getById(Long id) {
        Unit unit = unitMapper.selectById(id);
        return unit != null ? unitConverter.entityToVO(unit) : null;
    }

    public void create(UnitRequest request) {
        if (unitMapper.selectCount(new QueryWrapper<Unit>().eq("name", request.getName()).eq("region", request.getRegion())) > 0) {
            throw new BusinessException(3002, "同区域内单位名称已存在");
        }
        Unit unit = unitConverter.requestToEntity(request);
        try {
            unitMapper.insert(unit);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(3002, "同区域内单位名称已存在");
        }
    }

    public void update(UnitRequest request) {
        Unit unit = unitMapper.selectById(request.getId());
        if (unit == null) {
            throw BusinessException.unitNotFound();
        }
        unitConverter.updateEntityFromRequest(request, unit);
        unitMapper.updateById(unit);
    }

    public void delete(Long id) {
        unitMapper.deleteById(id);
    }
}
