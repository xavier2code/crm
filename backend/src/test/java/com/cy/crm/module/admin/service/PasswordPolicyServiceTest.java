package com.cy.crm.module.admin.service;

import com.cy.crm.common.exception.BusinessException;
import com.cy.crm.module.admin.mapper.LoginFailureMapper;
import com.cy.crm.module.admin.mapper.PasswordHistoryMapper;
import com.cy.crm.module.admin.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock
    private PasswordHistoryMapper passwordHistoryMapper;
    @Mock
    private LoginFailureMapper loginFailureMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordPolicyService passwordPolicyService;

    @Test
    void validateStrength_shouldPassForStrongPassword() {
        assertDoesNotThrow(() -> passwordPolicyService.validateStrength("Strong1!"));
    }

    @Test
    void validateStrength_shouldThrowWhenTooShort() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicyService.validateStrength("Short1!"));
        assertEquals(2009, ex.getCode());
        assertTrue(ex.getMessage().contains("长度至少"));
    }

    @Test
    void validateStrength_shouldThrowWhenMissingUppercase() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicyService.validateStrength("strong1!strong"));
        assertEquals(2009, ex.getCode());
        assertTrue(ex.getMessage().contains("大写字母"));
    }

    @Test
    void validateStrength_shouldThrowWhenMissingLowercase() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicyService.validateStrength("STRONG1!STRONG"));
        assertEquals(2009, ex.getCode());
        assertTrue(ex.getMessage().contains("小写字母"));
    }

    @Test
    void validateStrength_shouldThrowWhenMissingDigit() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicyService.validateStrength("Strong!Strong"));
        assertEquals(2009, ex.getCode());
        assertTrue(ex.getMessage().contains("数字"));
    }

    @Test
    void validateStrength_shouldThrowWhenMissingSpecialChar() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicyService.validateStrength("Strong1Strong"));
        assertEquals(2009, ex.getCode());
        assertTrue(ex.getMessage().contains("特殊字符"));
    }

    @Test
    void validateStrength_shouldRespectCustomRules() {
        setField(passwordPolicyService, "minLength", 6);
        setField(passwordPolicyService, "requireUppercase", false);
        setField(passwordPolicyService, "requireLowercase", true);
        setField(passwordPolicyService, "requireDigit", true);
        setField(passwordPolicyService, "requireSpecial", false);

        assertDoesNotThrow(() -> passwordPolicyService.validateStrength("abc123"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> passwordPolicyService.validateStrength("abcdef"));
        assertEquals(2009, ex.getCode());
        assertTrue(ex.getMessage().contains("数字"));
    }
}
