package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.converter.UserConverter;
import com.cy.crm.module.admin.dto.UserRequest;
import com.cy.crm.module.admin.entity.Role;
import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.entity.UserRole;
import com.cy.crm.module.admin.mapper.RoleMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.mapper.UserRoleMapper;
import com.cy.crm.module.admin.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;

    public Page<UserVO> pageUsers(String keyword, Long current, Long size) {
        Page<User> page = userMapper.selectUserPage(new Page<>(current, size), keyword);
        Page<UserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    public UserVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        return user != null ? toVO(user) : null;
    }

    public User getUserEntityById(Long id) {
        return userMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserRequest request, Long operatorId) {
        if (userMapper.selectCount(new QueryWrapper<User>().eq("username", request.getUsername())) > 0) {
            throw new BusinessException(3001, "用户名已存在");
        }
        User user = userConverter.requestToEntity(request);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setIsInitialPassword(1);
        user.setCreatedBy(operatorId);
        try {
            userMapper.insert(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessException(3001, "用户名已存在");
        }
        saveUserRoles(user.getId(), request.getRoleIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserRequest request) {
        User user = userMapper.selectById(request.getId());
        if (user == null) {
            throw BusinessException.resourceNotFound("用户");
        }
        userConverter.updateEntityFromRequest(request, user);
        userMapper.updateById(user);
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", request.getId()));
        saveUserRoles(request.getId(), request.getRoleIds());
    }

    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<UserRole> list = roleIds.stream().map(roleId -> {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            return ur;
        }).collect(Collectors.toList());
        for (UserRole ur : list) {
            userRoleMapper.insert(ur);
        }
    }

    private UserVO toVO(User user) {
        UserVO vo = userConverter.entityToVO(user);
        List<Role> roles = userMapper.selectRolesByUserId(user.getId());
        vo.setRoles(roles.stream().map(Role::getName).collect(Collectors.toList()));
        return vo;
    }
}
