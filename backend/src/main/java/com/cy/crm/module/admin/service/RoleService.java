package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.converter.RoleConverter;
import com.cy.crm.module.admin.dto.RoleRequest;
import com.cy.crm.module.admin.entity.Role;
import com.cy.crm.module.admin.mapper.RoleMapper;
import com.cy.crm.module.admin.vo.RoleVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    private final RoleMapper roleMapper;
    private final RoleConverter roleConverter;

    public List<RoleVO> listRoles() {
        return roleMapper.selectList(null).stream()
                .map(roleConverter::entityToVO)
                .collect(Collectors.toList());
    }

    public RoleVO getRoleById(Long id) {
        Role role = roleMapper.selectById(id);
        return role != null ? roleConverter.entityToVO(role) : null;
    }

    public void createRole(RoleRequest request) {
        if (roleMapper.selectCount(new QueryWrapper<Role>().eq("code", request.getCode())) > 0) {
            throw new BusinessException(3003, "角色编码已存在");
        }
        Role role = roleConverter.requestToEntity(request);
        role.setIsBuiltin(0);
        roleMapper.insert(role);
    }

    public void updateRole(RoleRequest request) {
        Role role = roleMapper.selectById(request.getId());
        if (role == null) {
            throw BusinessException.resourceNotFound("角色");
        }
        if (role.getIsBuiltin() == 1) {
            throw BusinessException.forbidden();
        }
        roleConverter.updateEntityFromRequest(request, role);
        roleMapper.updateById(role);
    }

    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role != null && role.getIsBuiltin() == 1) {
            throw BusinessException.forbidden();
        }
        roleMapper.deleteById(id);
    }
}
