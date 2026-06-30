package com.cy.crm.module.auth.service;

import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.entity.UserChannel;
import com.cy.crm.module.admin.mapper.UserChannelMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserChannelMapper userChannelMapper;

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", username)
        );
        return user != null ? user.getId() : null;
    }

    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userId != null ? userMapper.selectById(userId) : null;
    }

    public List<Long> getCurrentUserRoleIds() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        return userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.cy.crm.module.admin.entity.UserRole>()
                        .eq("user_id", userId)
        ).stream().map(ur -> ur.getRoleId()).collect(Collectors.toList());
    }

    public List<String> getCurrentUserRoles() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        return userMapper.selectRolesByUserId(userId).stream()
                .map(com.cy.crm.module.admin.entity.Role::getCode)
                .collect(Collectors.toList());
    }

    public boolean hasRole(String roleCode) {
        return getCurrentUserRoles().contains(roleCode);
    }

    public boolean hasAnyRole(String... roleCodes) {
        List<String> userRoles = getCurrentUserRoles();
        for (String code : roleCodes) {
            if (userRoles.contains(code)) {
                return true;
            }
        }
        return false;
    }

    public Long getCurrentChannelId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        UserChannel uc = userChannelMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserChannel>()
                        .eq("user_id", userId)
                        .eq("assign_type", 2)
                        .last("LIMIT 1")
        );
        return uc != null ? uc.getChannelId() : null;
    }
}
