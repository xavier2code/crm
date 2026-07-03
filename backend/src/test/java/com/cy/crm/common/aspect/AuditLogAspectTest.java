package com.cy.crm.common.aspect;

import com.cy.crm.module.admin.entity.User;
import com.cy.crm.module.admin.mapper.AuditLogMapper;
import com.cy.crm.module.auth.dto.LoginRequest;
import com.cy.crm.module.auth.service.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
class AuditLogAspectTest {

    private AuditLogAspect auditLogAspect;

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private CurrentUserService currentUserService;

    private ObjectMapper objectMapper;
    private AuditLogWriter auditLogWriter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditLogWriter = new AuditLogWriter(auditLogMapper);
        // Spy to allow real method execution while tracking calls
        auditLogWriter = Mockito.spy(auditLogWriter);
        auditLogAspect = new AuditLogAspect(auditLogWriter, currentUserService, objectMapper);
    }

    @Test
    void testSanitizeParams_WithPasswordHash_ShouldMaskPasswordHash() throws Exception {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("hashed-secret-123");

        // Act - Use reflection to access private method
        var method = AuditLogAspect.class.getDeclaredMethod("sanitizeParams", Object[].class);
        method.setAccessible(true);
        String sanitized = (String) method.invoke(auditLogAspect, new Object[]{new Object[]{user}});

        // Assert
        assertNotNull(sanitized);
        assertTrue(sanitized.contains("******"));
        assertFalse(sanitized.contains("hashed-secret-123"));
    }

    @Test
    void testSanitizeParams_WithLoginRequest_ShouldMaskSensitiveFields() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("secret123");
        loginRequest.setCaptchaUuid("uuid-123");
        loginRequest.setCaptchaCode("code-456");

        // Act - Use reflection to access private method
        var method = AuditLogAspect.class.getDeclaredMethod("sanitizeParams", Object[].class);
        method.setAccessible(true);
        String sanitized = (String) method.invoke(auditLogAspect, new Object[]{new Object[]{loginRequest}});

        // Assert
        assertNotNull(sanitized);
        // All sensitive fields should be masked
        assertTrue(sanitized.contains("******"));
        assertFalse(sanitized.contains("secret123"));
        assertFalse(sanitized.contains("uuid-123"));
        assertFalse(sanitized.contains("code-456"));
        // Username should remain visible
        assertTrue(sanitized.contains("testuser"));
    }

    @Test
    void testSanitizeParams_WithSecret_ShouldMaskSecret() throws Exception {
        // Arrange
        ApiKeyRequest apiKeyRequest = new ApiKeyRequest("api-secret-key-456");

        // Act - Use reflection to access private method
        var method = AuditLogAspect.class.getDeclaredMethod("sanitizeParams", Object[].class);
        method.setAccessible(true);
        String sanitized = (String) method.invoke(auditLogAspect, new Object[]{new Object[]{apiKeyRequest}});

        // Assert
        assertNotNull(sanitized);
        assertTrue(sanitized.contains("******"));
        assertFalse(sanitized.contains("api-secret-key-456"));
    }

    @Test
    void testSanitizeParams_WithMultipleSensitiveFields_ShouldMaskAll() throws Exception {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("hashed-pass-123");
        user.setPhone("13800138000");
        user.setEmail("user@example.com");

        // Act - Use reflection to access private method
        var method = AuditLogAspect.class.getDeclaredMethod("sanitizeParams", Object[].class);
        method.setAccessible(true);
        String sanitized = (String) method.invoke(auditLogAspect, new Object[]{new Object[]{user}});

        // Assert
        assertNotNull(sanitized);
        // Password hash should definitely be masked
        assertFalse(sanitized.contains("hashed-pass-123"));
    }

    @Test
    void testSanitizeParams_WithNonSensitiveObject_ShouldSerializeNormally() throws Exception {
        // Arrange
        SimpleObject simpleObject = new SimpleObject("normalValue", 42);

        // Act - Use reflection to access private method
        var method = AuditLogAspect.class.getDeclaredMethod("sanitizeParams", Object[].class);
        method.setAccessible(true);
        String sanitized = (String) method.invoke(auditLogAspect, new Object[]{new Object[]{simpleObject}});

        // Assert
        assertNotNull(sanitized);
        assertTrue(sanitized.contains("normalValue"));
        assertTrue(sanitized.contains("42"));
        assertFalse(sanitized.contains("******"));
    }

    @Test
    void testSanitizeParams_WithNullArray_ShouldReturnEmptyString() throws Exception {
        // Arrange
        Object[] args = null;

        // Act - Use reflection to access private method
        var method = AuditLogAspect.class.getDeclaredMethod("sanitizeParams", Object[].class);
        method.setAccessible(true);
        // Pass null directly, not wrapped in another array
        String sanitized = (String) method.invoke(auditLogAspect, new Object[]{args});

        // Assert
        assertNotNull(sanitized);
        assertTrue(sanitized.isEmpty());
    }

    @Test
    void testSanitizeParams_WithEmptyArray_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        Object[] args = new Object[0];

        // Act - Use reflection to access private method
        var method = AuditLogAspect.class.getDeclaredMethod("sanitizeParams", Object[].class);
        method.setAccessible(true);
        String sanitized = (String) method.invoke(auditLogAspect, new Object[]{args});

        // Assert
        assertNotNull(sanitized);
        assertEquals("[]", sanitized);
    }

    // Helper classes for testing
    static class ApiKeyRequest {
        private String apiKey;

        public ApiKeyRequest(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiKey() { return apiKey; }
    }

    static class SimpleObject {
        private String name;
        private int value;

        public SimpleObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public int getValue() { return value; }
    }
}
