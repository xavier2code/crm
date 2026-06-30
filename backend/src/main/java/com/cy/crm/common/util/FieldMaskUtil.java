package com.cy.crm.common.util;

import com.cy.crm.module.auth.service.CurrentUserService;
import lombok.experimental.UtilityClass;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 字段脱敏工具
 * 用于实现字段级权限控制中的敏感字段脱敏
 */
@UtilityClass
public class FieldMaskUtil {

    /**
     * 手机号脱敏：138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 金额脱敏：返回 null，由前端统一展示为 "**万"
     * 也可以返回字符串 "**万"，但 VO 中 amount 字段类型为 BigDecimal，这里选择 null 更兼容
     */
    public static BigDecimal maskAmount(BigDecimal amount) {
        return null;
    }

    /**
     * 判断是否当前用户是 BD 角色（非负责人/大区总/CYBD/管理员）
     */
    public static boolean isBdOnly(CurrentUserService currentUserService) {
        if (currentUserService == null) {
            return false;
        }
        return currentUserService.hasRole("CHANNEL_BD")
                && !currentUserService.hasAnyRole("CHANNEL_HEAD", "REGION_HEAD", "CYBD", "ADMIN");
    }

    /**
     * 判断当前用户是否可以查看完整返利金额
     * 仅渠道负责人、CYBD、管理员可见
     */
    public static boolean canViewRebateAmount(CurrentUserService currentUserService) {
        if (currentUserService == null) {
            return false;
        }
        return currentUserService.hasAnyRole("CHANNEL_HEAD", "CYBD", "ADMIN");
    }
}
