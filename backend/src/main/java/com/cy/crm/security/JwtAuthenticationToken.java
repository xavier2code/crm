package com.cy.crm.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * JWT认证令牌对象
 * 用于存储从JWT claims中提取的用户信息和权限
 */
@Getter
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String username;
    private final Long userId;
    private final List<String> roles;
    private final List<String> menus;
    private final DataScope dataScope;

    public JwtAuthenticationToken(String username,
                                  Collection<? extends GrantedAuthority> authorities,
                                  Long userId,
                                  List<String> roles,
                                  List<String> menus,
                                  DataScope dataScope) {
        super(authorities);
        this.username = username;
        this.userId = userId;
        this.roles = roles;
        this.menus = menus;
        this.dataScope = dataScope;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public Object getDetails() {
        return this;
    }
}
