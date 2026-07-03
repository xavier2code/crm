package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.converter.RoleConverter;
import com.cy.crm.module.admin.dto.RoleRequest;
import com.cy.crm.module.admin.entity.Role;
import com.cy.crm.module.admin.entity.RoleMenu;
import com.cy.crm.module.admin.entity.RoleOperation;
import com.cy.crm.module.admin.mapper.RoleMapper;
import com.cy.crm.module.admin.mapper.RoleMenuMapper;
import com.cy.crm.module.admin.mapper.RoleOperationMapper;
import com.cy.crm.module.admin.vo.RoleVO;
import com.cy.crm.security.DataScopeDimension;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final RoleOperationMapper roleOperationMapper;
    private final RoleConverter roleConverter;

    public List<RoleVO> listRoles() {
        return roleMapper.selectList(null).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public RoleVO getRoleById(Long id) {
        Role role = roleMapper.selectById(id);
        return role != null ? toVO(role) : null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleRequest request) {
        if (roleMapper.selectCount(new QueryWrapper<Role>().eq("code", request.getCode())) > 0) {
            throw new BusinessException(3003, "角色编码已存在");
        }
        validateDataScopeType(request.getDataScopeType());
        Role role = roleConverter.requestToEntity(request);
        role.setIsBuiltin(0);
        if (role.getDataScopeType() == null) {
            role.setDataScopeType(DataScopeDimension.SELF.getCode());
        }
        try {
            roleMapper.insert(role);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(3003, "角色编码已存在");
        }
        replaceRoleMenus(role.getId(), request.getMenuIds());
        replaceRoleOperations(role.getId(), request.getOperationCodes());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleRequest request) {
        Role role = roleMapper.selectById(request.getId());
        if (role == null) {
            throw BusinessException.resourceNotFound("角色");
        }
        if (role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw BusinessException.forbidden();
        }
        validateDataScopeType(request.getDataScopeType());
        roleConverter.updateEntityFromRequest(request, role);
        if (role.getDataScopeType() == null) {
            role.setDataScopeType(DataScopeDimension.SELF.getCode());
        }
        roleMapper.updateById(role);
        replaceRoleMenus(role.getId(), request.getMenuIds());
        replaceRoleOperations(role.getId(), request.getOperationCodes());
    }

    public void deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role != null && role.getIsBuiltin() != null && role.getIsBuiltin() == 1) {
            throw BusinessException.forbidden();
        }
        roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("role_id", id));
        roleOperationMapper.delete(new QueryWrapper<RoleOperation>().eq("role_id", id));
        roleMapper.deleteById(id);
    }

    private RoleVO toVO(Role role) {
        RoleVO vo = roleConverter.entityToVO(role);
        List<RoleMenu> menus = roleMenuMapper.selectList(
                new QueryWrapper<RoleMenu>().eq("role_id", role.getId()));
        vo.setMenuIds(menus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList()));
        List<RoleOperation> ops = roleOperationMapper.selectList(
                new QueryWrapper<RoleOperation>().eq("role_id", role.getId()));
        vo.setOperationCodes(ops.stream()
                .map(RoleOperation::getOperationCode)
                .collect(Collectors.toList()));
        return vo;
    }

    private void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("role_id", roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : menuIds) {
            if (menuId == null) {
                continue;
            }
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    private void replaceRoleOperations(Long roleId, List<String> operationCodes) {
        roleOperationMapper.delete(new QueryWrapper<RoleOperation>().eq("role_id", roleId));
        if (operationCodes == null || operationCodes.isEmpty()) {
            return;
        }
        for (String code : operationCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            RoleOperation ro = new RoleOperation();
            ro.setRoleId(roleId);
            ro.setOperationCode(code.trim());
            roleOperationMapper.insert(ro);
        }
    }

    private void validateDataScopeType(String dataScopeType) {
        if (dataScopeType == null) {
            return;
        }
        if (DataScopeDimension.fromCode(dataScopeType) == null) {
            throw BusinessException.paramError("不支持的 dataScopeType: " + dataScopeType);
        }
    }
}
