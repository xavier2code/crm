package com.cy.crm.module.auth.service;

import com.cy.crm.module.admin.entity.User;
import com.cy.crm.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.cy.crm.module.admin.mapper.DataPermissionMapper;
import com.cy.crm.module.admin.mapper.MenuMapper;
import com.cy.crm.module.admin.mapper.RoleMenuMapper;
import com.cy.crm.module.admin.mapper.RoleOperationMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.service.DictionaryService;
import com.cy.crm.module.admin.service.PasswordPolicyService;
import com.cy.crm.module.auth.dto.CurrentUserResponse;
import com.cy.crm.module.auth.dto.LoginRequest;
import com.cy.crm.module.auth.dto.LoginResponse;
import com.cy.crm.common.util.IpUtils;
import com.cy.crm.security.DataScope;
import com.cy.crm.security.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final RoleOperationMapper roleOperationMapper;
    private final MenuMapper menuMapper;
    private final DataPermissionMapper dataPermissionMapper;
    private final DictionaryService dictionaryService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final PasswordPolicyService passwordPolicyService;
    private final TokenBlacklistService tokenBlacklistService;

    public LoginResponse login(LoginRequest request) {
        // 1. 验证验证码
        if (!captchaService.validateCaptcha(request.getCaptchaUuid(), request.getCaptchaCode())) {
            throw BusinessException.paramError("验证码错误");
        }

        // 2. 检查账户是否被锁定
        if (passwordPolicyService.isAccountLocked(request.getUsername())) {
            Long remainingTime = passwordPolicyService.getRemainingLockTime(request.getUsername());
            throw new BusinessException(2010, "账户已被锁定，请" + (remainingTime / 60) + "分钟后重试");
        }

        try {
            // 3. 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 4. 认证成功，重置失败计数
            passwordPolicyService.resetFailureCount(request.getUsername());

            User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", request.getUsername()));
            user.setRoles(userMapper.selectRolesByUserId(user.getId()));

            // 获取角色编码
            List<String> roles = getRoleCodes(user);

            // 获取菜单权限（用于前端路由）
            List<String> menuCodes = getMenuCodes(user);

            // 获取操作权限（用于按钮级权限控制）
            List<String> ops = getOperationCodes(user);

            // 构建结构化数据权限范围
            DataScope dataScope = buildDataScope(user);

            // 生成JWT令牌
            String accessToken = jwtUtil.generateAccessToken(
                    user.getId(), user.getUsername(), roles, menuCodes, ops, dataScope
            );
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            // 5. 保存会话到Redis
            String jti = jwtUtil.extractJti(accessToken);
            Long expiresAt = jwtUtil.extractExpiration(accessToken);
            long ttl = expiresAt - (System.currentTimeMillis() / 1000);

            com.cy.crm.module.auth.service.TokenBlacklistService.SessionInfo sessionInfo =
                    new com.cy.crm.module.auth.service.TokenBlacklistService.SessionInfo();
            sessionInfo.setUsername(user.getUsername());
            sessionInfo.setJti(jti);
            sessionInfo.setLoginTime(java.time.LocalDateTime.now());
            sessionInfo.setLastActiveTime(java.time.LocalDateTime.now());

            tokenBlacklistService.saveSession(user.getUsername(), jti, sessionInfo, ttl);

            // 构建响应
            LoginResponse response = new LoginResponse();
            response.setAccessToken(accessToken);
            response.setRefreshToken(refreshToken);
            response.setTokenType("Bearer");

            // 设置用户信息
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setRealName(user.getRealName());
            userInfo.setPhone(user.getPhone());
            userInfo.setEmail(user.getEmail());
            // TODO: 设置部门信息
            response.setUserInfo(userInfo);

            // 设置角色、菜单树、权限编码、数据权限
            response.setRoles(roles);
            response.setMenuTree(buildMenuTree(user));
            response.setPermissionCodes(ops);
            response.setDataScope(dataScope);

            return response;
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // 认证失败，记录失败次数
            passwordPolicyService.recordLoginFailure(request.getUsername(), getClientIp());
            throw BusinessException.badCredentials();
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        HttpServletRequest request =
            ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return IpUtils.getClientIp(request);
    }

    /**
     * 构建菜单树
     */
    private List<Object> buildMenuTree(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = user.getRoles().stream()
                .map(r -> r.getId())
                .collect(Collectors.toList());

        // 获取角色关联的菜单ID
        List<Long> menuIds = roleMenuMapper.selectList(
                new QueryWrapper<com.cy.crm.module.admin.entity.RoleMenu>()
                        .in("role_id", roleIds)
        ).stream()
                .map(rm -> rm.getMenuId())
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取菜单列表并构建树形结构
        List<com.cy.crm.module.admin.entity.Menu> menus = menuMapper.selectList(
                new QueryWrapper<com.cy.crm.module.admin.entity.Menu>()
                        .in("id", menuIds)
                        .eq("status", 1)
                        .orderByAsc("sort")
        );

        return buildTree(menus, null);
    }

    /**
     * 递归构建菜单树
     */
    private List<Object> buildTree(List<com.cy.crm.module.admin.entity.Menu> menus, Long parentId) {
        List<Object> tree = new ArrayList<>();

        for (com.cy.crm.module.admin.entity.Menu menu : menus) {
            if (Objects.equals(menu.getParentId(), parentId)) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", menu.getId());
                node.put("name", menu.getName());
                node.put("code", menu.getCode());
                node.put("path", menu.getPath());
                node.put("icon", menu.getIcon());
                node.put("type", menu.getType());
                node.put("parentId", menu.getParentId());
                node.put("sort", menu.getSort());

                // 递归获取子菜单
                List<Object> children = buildTree(menus, menu.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }

                tree.add(node);
            }
        }

        return tree;
    }

    /**
     * 获取菜单编码列表
     */
    private List<String> getMenuCodes(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = user.getRoles().stream()
                .map(r -> r.getId())
                .collect(Collectors.toList());

        List<Long> menuIds = roleMenuMapper.selectList(
                new QueryWrapper<com.cy.crm.module.admin.entity.RoleMenu>()
                        .in("role_id", roleIds)
        ).stream()
                .map(rm -> rm.getMenuId())
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }

        return menuMapper.selectList(
                new QueryWrapper<com.cy.crm.module.admin.entity.Menu>()
                        .in("id", menuIds)
                        .select("code")
        ).stream()
                .map(m -> m.getCode())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取操作权限编码列表
     */
    private List<String> getOperationCodes(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = user.getRoles().stream()
                .map(r -> r.getId())
                .collect(Collectors.toList());

        return roleOperationMapper.selectList(
                new QueryWrapper<com.cy.crm.module.admin.entity.RoleOperation>()
                        .in("role_id", roleIds)
        ).stream()
                .map(ro -> ro.getOperationCode())
                .collect(Collectors.toList());
    }

    /**
     * 构建结构化数据权限范围
     */
    private DataScope buildDataScope(User user) {
        DataScope scope = new DataScope();

        // 获取用户的数据权限配置
        List<com.cy.crm.module.admin.entity.DataPermission> permissions = dataPermissionMapper.selectList(
                new QueryWrapper<com.cy.crm.module.admin.entity.DataPermission>()
                        .eq("user_id", user.getId())
        );

        if (permissions.isEmpty()) {
            // 默认只能看自己的数据
            scope.setSelfOnly(true);
            return scope;
        }

        // 按权限类型分类
        for (com.cy.crm.module.admin.entity.DataPermission dp : permissions) {
            Integer scopeType = dp.getScopeType();
            String scopeValue = dp.getScopeValue();

            if (scopeType == null) continue;

            switch (scopeType) {
                case 1 -> { // ALL
                    scope.setAll(true);
                    return scope; // 全部权限直接返回
                }
                case 2 -> { // CHANNEL
                    if (scope.getChannelIds() == null) {
                        scope.setChannelIds(new ArrayList<>());
                    }
                    try {
                        scope.getChannelIds().add(Long.valueOf(scopeValue));
                    } catch (NumberFormatException e) {
                        // 忽略无效值
                    }
                }
                case 3 -> { // REGION
                    if (scope.getRegions() == null) {
                        scope.setRegions(new ArrayList<>());
                    }
                    scope.getRegions().add(scopeValue);
                }
                case 4 -> { // UNIT
                    if (scope.getUnitIds() == null) {
                        scope.setUnitIds(new ArrayList<>());
                    }
                    try {
                        scope.getUnitIds().add(Long.valueOf(scopeValue));
                    } catch (NumberFormatException e) {
                        // 忽略无效值
                    }
                }
                case 5 -> scope.setSelfOnly(true); // SELF
            }
        }

        return scope;
    }

    public CurrentUserResponse getCurrentUser(String username) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        user.setRoles(userMapper.selectRolesByUserId(user.getId()));
        CurrentUserResponse response = new CurrentUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setRoles(getRoleCodes(user));
        return response;
    }

    /**
     * 刷新访问令牌
     */
    public com.cy.crm.module.auth.dto.TokenResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            throw BusinessException.tokenInvalid();
        }

        String username = jwtUtil.extractUsername(refreshToken);
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw BusinessException.resourceNotFound("用户");
        }

        // 生成新的令牌对
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getUsername(),
                getRoleCodes(user), getMenuCodes(user), getOperationCodes(user), buildDataScope(user)
        );
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        com.cy.crm.module.auth.dto.TokenResponse response = new com.cy.crm.module.auth.dto.TokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setTokenType("Bearer");
        return response;
    }

    /**
     * 修改密码
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw BusinessException.resourceNotFound("用户");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw BusinessException.badCredentials();
        }

        // 检查密码历史记录（不能使用最近5次的密码）
        if (passwordPolicyService.isPasswordInHistory(user.getId(), newPassword)) {
            throw BusinessException.paramError("不能使用最近使用过的密码");
        }

        // TODO: 检查密码强度

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        // 记录密码历史
        passwordPolicyService.recordPasswordHistory(user.getId(), user.getPasswordHash());

        // 清除该用户的所有会话（强制重新登录）
        tokenBlacklistService.removeAllSessions(username);
    }

    /**
     * 退出登录（单个设备）
     */
    public void logout(String token) {
        if (token != null) {
            String jti = jwtUtil.extractJti(token);
            Long expiresAt = jwtUtil.extractExpiration(token);
            long ttl = expiresAt - (System.currentTimeMillis() / 1000);
            // 将token加入黑名单，直到其自然过期
            tokenBlacklistService.addToBlacklist(jti, ttl);
            // 删除会话记录
            tokenBlacklistService.removeSession(jti);
        }
    }

    /**
     * 退出所有设备
     */
    public void logoutAll(String username) {
        // 清除该用户的所有Token和会话
        tokenBlacklistService.removeAllSessions(username);
    }

    /**
     * 获取用户会话列表
     */
    public List<com.cy.crm.module.auth.dto.SessionInfo> getUserSessions(String username) {
        List<com.cy.crm.module.auth.service.TokenBlacklistService.SessionInfo> sessions =
                tokenBlacklistService.getUserSessions(username);

        return sessions.stream().map(s -> {
            com.cy.crm.module.auth.dto.SessionInfo dto = new com.cy.crm.module.auth.dto.SessionInfo();
            dto.setSessionId(s.getJti());
            dto.setLoginTime(s.getLoginTime());
            dto.setLastActiveTime(s.getLastActiveTime());
            dto.setClientIp(s.getClientIp());
            dto.setUserAgent(s.getUserAgent());
            return dto;
        }).collect(Collectors.toList());
    }

    private List<String> getRoleCodes(User user) {
        return user.getRoles() != null
                ? user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
