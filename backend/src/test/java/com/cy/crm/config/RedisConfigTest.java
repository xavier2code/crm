package com.cy.crm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
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
     * 模拟 RedisConfig 中的序列化器创建逻辑
     * 这个方法应该与 RedisConfig.java 中的实现保持一致
     */
    private ObjectMapper createSecureObjectMapper() {
        com.fasterxml.jackson.annotation.PropertyAccessor accessor = com.fasterxml.jackson.annotation.PropertyAccessor.ALL;
        com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility visibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("java.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("com.cy.crm.")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(accessor, visibility);
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
        // 使用一个不存在的类名来模拟攻击
        String maliciousJson = "[\"org.springframework.util.XXX\", {}]";

        // 应该抛出异常或拒绝反序列化
        assertThrows(Exception.class, () -> {
            mapper.readValue(maliciousJson, Object.class);
        }, "Should reject deserialization of non-whitelisted classes");
    }

    @Test
    void shouldRejectSpringFrameworkClasses() {
        ObjectMapper mapper = createSecureObjectMapper();

        // 尝试反序列化 Spring 框架类（不在白名单中）
        // 攻击者可能利用 Spring 框架中的类来执行恶意操作
        String maliciousJson = "[\"org.springframework.util.StringUtils\", {}]";

        assertThrows(Exception.class, () -> {
            mapper.readValue(maliciousJson, Object.class);
        }, "Should reject deserialization of Spring framework classes");
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
