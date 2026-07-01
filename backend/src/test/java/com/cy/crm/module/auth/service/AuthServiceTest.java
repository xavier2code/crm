package com.cy.crm.module.auth.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.mapper.DataPermissionMapper;
import com.cy.crm.module.admin.mapper.MenuMapper;
import com.cy.crm.module.admin.mapper.RoleMenuMapper;
import com.cy.crm.module.admin.mapper.RoleOperationMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import com.cy.crm.module.admin.service.PasswordPolicyService;
import com.cy.crm.module.auth.dto.LoginRequest;
import com.cy.crm.module.auth.dto.LoginResponse;
import com.cy.crm.security.JwtUtil;
import com.cy.crm.module.admin.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordPolicyService passwordPolicyService;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private RoleOperationMapper roleOperationMapper;
    @Mock
    private MenuMapper menuMapper;
    @Mock
    private DataPermissionMapper dataPermissionMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldThrowWhenAccountLocked() {
        // given - set up request context for getClientIp()
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        LoginRequest request = new LoginRequest();
        request.setUsername("locked_user");
        request.setPassword("password");

        when(captchaService.validateCaptcha(any(), any())).thenReturn(true);
        when(passwordPolicyService.isAccountLocked("locked_user")).thenReturn(true);
        when(passwordPolicyService.getRemainingLockTime("locked_user")).thenReturn(1800L);

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertEquals(2010, ex.getCode());
        assertTrue(ex.getMessage().contains("账户已被锁定"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_shouldRecordFailureAndThrowOnBadCredentials() {
        // given - set up request context for getClientIp()
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("203.0.113.50");

        LoginRequest request = new LoginRequest();
        request.setUsername("test_user");
        request.setPassword("wrong_password");

        when(captchaService.validateCaptcha(any(), any())).thenReturn(true);
        when(passwordPolicyService.isAccountLocked("test_user")).thenReturn(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("密码错误"));

        // when / then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertEquals(2002, ex.getCode());
        verify(passwordPolicyService).recordLoginFailure("test_user", "203.0.113.50");
    }

    @Test
    void login_shouldSucceedWithValidCredentials() {
        // given - set up request context for getClientIp() (not used in success path but good practice)
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        LoginRequest request = new LoginRequest();
        request.setUsername("test_user");
        request.setPassword("correct_password");

        User user = new User();
        user.setId(1L);
        user.setUsername("test_user");
        user.setRealName("测试用户");

        when(captchaService.validateCaptcha(any(), any())).thenReturn(true);
        when(passwordPolicyService.isAccountLocked("test_user")).thenReturn(false);
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userMapper.selectRolesByUserId(1L)).thenReturn(java.util.Collections.emptyList());
        when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any(), any())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh_token");
        when(jwtUtil.extractJti(any())).thenReturn("jti-123");
        when(jwtUtil.extractExpiration(any())).thenReturn(System.currentTimeMillis() / 1000 + 7200);

        // when
        LoginResponse response = authService.login(request);

        // then
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        verify(passwordPolicyService).resetFailureCount("test_user");
    }

    // ========== getClientIp() Tests ==========

    @Test
    void getClientIp_shouldReturnRemoteAddrWhenNoProxyHeaders() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.100");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("192.168.1.100", result);
    }

    @Test
    void getClientIp_shouldReturnXForwardedFor() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("203.0.113.1", result);
    }

    @Test
    void getClientIp_shouldReturnProxyClientIp() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn("10.0.0.1");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("10.0.0.1", result);
    }

    @Test
    void getClientIp_shouldReturnWLProxyClientIp() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn("10.0.0.2");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("10.0.0.2", result);
    }

    @Test
    void getClientIp_shouldReturnHttpClientIp() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_CLIENT_IP")).thenReturn("172.16.0.1");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("172.16.0.1", result);
    }

    @Test
    void getClientIp_shouldReturnHttpXForwardedFor() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn("172.17.0.1");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("172.17.0.1", result);
    }

    @Test
    void getClientIp_shouldReturnFirstIpWhenMultipleInHeader() {
        // given - X-Forwarded-For can contain multiple IPs
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 203.0.113.2, 203.0.113.3");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("203.0.113.1", result);
    }

    @Test
    void getClientIp_shouldIgnoreUnknownHeaderValue() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("unknown");
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn("10.0.0.1");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("10.0.0.1", result);
    }

    @Test
    void getClientIp_shouldIgnoreEmptyHeaderValue() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("");
        lenient().when(httpServletRequest.getHeader("Proxy-Client-IP")).thenReturn("10.0.0.1");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("10.0.0.1", result);
    }

    @Test
    void getClientIp_shouldTrimWhitespaceFromFirstIp() {
        // given
        mockRequestContext();
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.1  , 203.0.113.2");
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");

        // when
        String result = invokeMethod(authService, "getClientIp");

        // then
        assertEquals("203.0.113.1", result);
    }

    /**
     * Helper method to set up the mock request context
     */
    private void mockRequestContext() {
        ServletRequestAttributes servletRequestAttributes = mock(ServletRequestAttributes.class);
        lenient().when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }
}
