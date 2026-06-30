package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.converter.DictionaryConverter;
import com.cy.crm.module.admin.dto.DictionaryRequest;
import com.cy.crm.module.admin.entity.Dictionary;
import com.cy.crm.module.admin.mapper.DictionaryMapper;
import com.cy.crm.module.admin.mapper.UnitMapper;
import com.cy.crm.module.admin.vo.DictionaryVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictionaryService extends ServiceImpl<DictionaryMapper, Dictionary> {

    private final DictionaryMapper dictionaryMapper;
    private final UnitMapper unitMapper;
    private final DictionaryConverter dictionaryConverter;

    public Map<String, List<DictionaryVO>> allByTypes() {
        List<String> types = dictionaryMapper.selectTypes();
        return types.stream().collect(Collectors.toMap(
                type -> type,
                type -> dictionaryMapper.selectByType(type).stream()
                        .map(dictionaryConverter::entityToVO)
                        .collect(Collectors.toList())
        ));
    }

    public List<DictionaryVO> listByType(String type) {
        return dictionaryMapper.selectByType(type).stream()
                .map(dictionaryConverter::entityToVO)
                .collect(Collectors.toList());
    }

    public DictionaryVO getById(Long id) {
        Dictionary dict = dictionaryMapper.selectById(id);
        return dict != null ? dictionaryConverter.entityToVO(dict) : null;
    }

    public void create(DictionaryRequest request) {
        if (dictionaryMapper.selectCount(new QueryWrapper<Dictionary>()
                .eq("type", request.getType()).eq("code", request.getCode())) > 0) {
            throw BusinessException.dictionaryCodeDuplicate();
        }
        Dictionary dict = dictionaryConverter.requestToEntity(request);
        dictionaryMapper.insert(dict);
    }

    public void update(DictionaryRequest request) {
        Dictionary dict = dictionaryMapper.selectById(request.getId());
        if (dict == null) {
            throw BusinessException.resourceNotFound("字典");
        }
        dictionaryConverter.updateEntityFromRequest(request, dict);
        dictionaryMapper.updateById(dict);
    }

    public void delete(Long id) {
        dictionaryMapper.deleteById(id);
    }

    public String getDictionaryName(String type, String code) {
        Dictionary dict = dictionaryMapper.selectOne(
                new QueryWrapper<Dictionary>()
                        .eq("type", type)
                        .eq("code", code)
        );
        return dict != null ? dict.getName() : code;
    }

    public String getUnitName(Long unitId) {
        if (unitId == null) {
            return null;
        }
        com.cy.crm.module.admin.entity.Unit unit = unitMapper.selectById(unitId);
        return unit != null ? unit.getName() : null;
    }

    public String getDictionaryRemark(String type, String code) {
        Dictionary dict = dictionaryMapper.selectOne(
                new QueryWrapper<Dictionary>()
                        .eq("type", type)
                        .eq("code", code)
        );
        return dict != null ? dict.getRemark() : null;
    }
}
