package com.cy.crm.module.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cy.crm.module.notification.entity.NotificationTemplate;
import com.cy.crm.module.notification.mapper.NotificationTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 通知模板服务
 */
@Service
@RequiredArgsConstructor
public class NotificationTemplateService extends ServiceImpl<NotificationTemplateMapper, NotificationTemplate> {

    private final NotificationTemplateMapper templateMapper;

    /**
     * 根据模板代码获取模板
     */
    public NotificationTemplate getTemplateByCode(String code) {
        return templateMapper.selectOne(
                new QueryWrapper<NotificationTemplate>()
                        .eq("code", code)
                        .eq("status", 1)
        );
    }

    /**
     * 根据模板生成通知内容
     * @param templateCode 模板代码
     * @param params 参数映射，用于替换模板中的变量
     * @return 替换后的内容
     */
    public String generateContent(String templateCode, Map<String, Object> params) {
        NotificationTemplate template = getTemplateByCode(templateCode);
        if (template == null) {
            return null;
        }

        String content = template.getContent();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                if (content.contains(placeholder)) {
                    content = content.replace(placeholder, String.valueOf(entry.getValue()));
                }
            }
        }

        return content;
    }

    /**
     * 分页查询模板列表
     */
    public Page<NotificationTemplate> pageTemplates(Long current, Long size, String type, Integer status) {
        return templateMapper.selectPage(
                new Page<>(current, size),
                new QueryWrapper<NotificationTemplate>()
                        .eq(type != null, "type", type)
                        .eq(status != null, "status", status)
                        .orderByDesc("created_at")
        );
    }

    /**
     * 获取所有启用的模板
     */
    public List<NotificationTemplate> getActiveTemplates() {
        return templateMapper.selectList(
                new QueryWrapper<NotificationTemplate>()
                        .eq("status", 1)
                        .orderByAsc("created_at")
        );
    }
}
