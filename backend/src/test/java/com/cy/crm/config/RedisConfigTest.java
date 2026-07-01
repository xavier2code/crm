package com.cy.crm.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 配置安全测试
 * 验证 ObjectMapper 使用白名单类型验证器，防止反序列化 RCE
 */
class RedisConfigTest {

    /**
     * 创建用于测试的安全 ObjectMapper
     * 使用与生产代码相同的 PolymorphicTypeValidator 配置（从 RedisConfig.createSecureTypeValidator()）
     * 这确保测试和生产代码使用完全相同的白名单配置（DRY 原则）
     */
    private ObjectMapper createSecureObjectMapper() {
        PolymorphicTypeValidator ptv = RedisConfig.createSecureTypeValidator();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        return mapper;
    }

    @Test
    void shouldAllowJavaStandardTypes() throws Exception {
        ObjectMapper mapper = createSecureObjectMapper();

        // 验证可以反序列化标准 Java 类型
        ArrayList<String> list = new ArrayList<>();
        list.add("test");
        String json = mapper.writeValueAsString(list);
        Object deserialized = mapper.readValue(json, Object.class);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof ArrayList);
    }

    @Test
    void shouldAllowProjectClasses() throws Exception {
        ObjectMapper mapper = createSecureObjectMapper();

        // 验证可以反序列化项目自己的类
        TestSerializable obj = new TestSerializable();
        obj.setValue("test");
        String json = mapper.writeValueAsString(obj);
        Object deserialized = mapper.readValue(json, Object.class);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof TestSerializable);
        assertEquals("test", ((TestSerializable) deserialized).getValue());
    }

    @Test
    void shouldRejectMaliciousTypes() {
        ObjectMapper mapper = createSecureObjectMapper();

        // 尝试反序列化恶意类（不在白名单中）
        // 使用真实的 Spring 框架类来模拟攻击
        // ClassPathXmlApplicationContext 是一个真实存在的 Spring 类，有默认构造函数
        // 但它不在白名单中，应该被拒绝
        String maliciousJson = "[\"org.springframework.context.support.ClassPathXmlApplicationContext\", []]";

        // 应该抛出异常或拒绝反序列化
        Exception exception = assertThrows(Exception.class, () -> {
            mapper.readValue(maliciousJson, Object.class);
        }, "Should reject deserialization of non-whitelisted classes");

        // 验证异常消息包含类型拒绝的相关信息
        String message = exception.getMessage();
        assertTrue(message != null && (message.contains("type") || message.contains("Class") || message.contains("allowed")),
                "Exception should indicate type validation failure");
    }

    @Test
    void shouldRejectSpringFrameworkClasses() {
        ObjectMapper mapper = createSecureObjectMapper();

        // 尝试反序列化 Spring 框架类（不在白名单中）
        // 攻击者可能利用 Spring 框架中的类来执行恶意操作
        // 使用 Spring 的 DefaultListableBeanFactory，它有默认构造函数
        String maliciousJson = "[\"org.springframework.beans.factory.support.DefaultListableBeanFactory\", {}]";

        Exception exception = assertThrows(Exception.class, () -> {
            mapper.readValue(maliciousJson, Object.class);
        }, "Should reject deserialization of Spring framework classes");

        // 验证异常消息包含类型拒绝的相关信息
        String message = exception.getMessage();
        assertTrue(message != null && (message.contains("type") || message.contains("Class") || message.contains("allowed")),
                "Exception should indicate type validation failure");
    }

    @Test
    void shouldSerializeAndDeserializeHashMap() throws Exception {
        ObjectMapper mapper = createSecureObjectMapper();

        HashMap<String, String> map = new HashMap<>();
        map.put("key", "value");

        String json = mapper.writeValueAsString(map);
        Object deserialized = mapper.readValue(json, Object.class);

        assertNotNull(deserialized);
        assertTrue(deserialized instanceof HashMap);
        assertEquals("value", ((HashMap<?, ?>) deserialized).get("key"));
    }

    /**
     * 测试用的可序列化类（模拟项目类）
     */
    public static class TestSerializable {
        private String value = "test";

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
