package com.cy.crm.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * 符合开发文档 §20 错误码体系
 *
 * 错误码分段：
 * - 1xxx：通用错误
 * - 2xxx：鉴权/权限
 * - 3xxx：客户/单位/字典
 * - 4xxx：商机/报备
 * - 5xxx：项目/过程/跟进
 * - 6xxx：返利/合同/报销（6010-6019）
 * - 7xxx：文件/导入导出
 * - 8xxx：通知
 * - 9xxx：定时任务/系统
 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * 业务错误码
     */
    private final int code;

    /**
     * 额外数据（用于返回上下文信息）
     */
    private final Object data;

    /**
     * 创建业务异常（默认通用错误码 1001）
     */
    public BusinessException(String message) {
        super(message);
        this.code = 1001;
        this.data = null;
    }

    /**
     * 创建业务异常（指定错误码）
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    /**
     * 创建业务异常（指定错误码和额外数据）
     */
    public BusinessException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    // ========== 通用错误（1xxx）便捷方法 ==========

    public static BusinessException paramError(String message) {
        return new BusinessException(1001, message);
    }

    public static BusinessException resourceNotFound() {
        return new BusinessException(1002, "资源不存在");
    }

    public static BusinessException resourceNotFound(String resourceName) {
        return new BusinessException(1002, resourceName + "不存在");
    }

    public static BusinessException systemError() {
        return new BusinessException(1099, "系统内部错误");
    }

    // ========== 鉴权/权限错误（2xxx）便捷方法 ==========

    public static BusinessException unauthorized() {
        return new BusinessException(2001, "未登录或登录已过期");
    }

    public static BusinessException badCredentials() {
        return new BusinessException(2002, "账号或密码错误");
    }

    public static BusinessException tokenInvalid() {
        return new BusinessException(2003, "Token无效或已过期");
    }

    public static BusinessException forbidden() {
        return new BusinessException(2004, "无权限访问该资源");
    }

    public static BusinessException roleNotConfigured() {
        return new BusinessException(2005, "角色未配置，请联系管理员");
    }

    public static BusinessException dataScopeDenied() {
        return new BusinessException(2006, "数据权限不足");
    }

    public static BusinessException forceChangePassword() {
        return new BusinessException(2007, "请修改密码后再使用系统");
    }

    public static BusinessException accountDisabled() {
        return new BusinessException(2008, "账号已停用");
    }

    public static BusinessException passwordTooWeak(String message) {
        return new BusinessException(2009, "密码强度不足：" + message);
    }

    public static BusinessException accountLocked() {
        return new BusinessException(2010, "账号已锁定，请30分钟后重试");
    }

    // ========== 客户/单位/字典错误（3xxx）便捷方法 ==========

    public static BusinessException unitNotFound() {
        return new BusinessException(3001, "单位不存在");
    }

    public static BusinessException customerExists() {
        return new BusinessException(3002, "客户+警种已存在");
    }

    public static BusinessException customerOwnedByOthers() {
        return new BusinessException(3003, "客户已存在但不在自己名下");
    }

    public static BusinessException dictionaryInUse() {
        return new BusinessException(3004, "字典已被使用，不可删除");
    }

    public static BusinessException dictionaryCodeDuplicate() {
        return new BusinessException(3005, "字典code重复");
    }

    public static BusinessException unitAssigned() {
        return new BusinessException(3006, "单位已分配，请先取消");
    }

    public static BusinessException customerNotFound() {
        return new BusinessException(3007, "客户不存在");
    }

    public static BusinessException contactNotFound() {
        return new BusinessException(3008, "联系人不存在");
    }

    public static BusinessException dictionaryBuiltinNotDeletable() {
        return new BusinessException(3009, "预置字典项不可删除");
    }

    // ========== 商机/报备错误（4xxx）便捷方法 ==========

    public static BusinessException opportunityExists() {
        return new BusinessException(4001, "客户已存在生效中报备");
    }

    public static BusinessException opportunityNoChance() {
        return new BusinessException(4002, "已用完恢复机会");
    }

    public static BusinessException opportunityExpired() {
        return new BusinessException(4003, "30天未跟进已失效");
    }

    public static BusinessException opportunityInCooling() {
        return new BusinessException(4004, "报备处于冷却期");
    }

    public static BusinessException opportunityNotFound() {
        return new BusinessException(4005, "商机不存在");
    }

    public static BusinessException opportunityNotInApproval() {
        return new BusinessException(4006, "商机不在审批中状态");
    }

    public static BusinessException opportunityNotActive() {
        return new BusinessException(4007, "商机不在生效中状态");
    }

    // ========== 项目/过程/跟进错误（5xxx）便捷方法 ==========

    public static BusinessException projectExists() {
        return new BusinessException(5001, "项目已存在，请勿重复创建");
    }

    public static BusinessException optimisticLockFailed() {
        return new BusinessException(5002, "数据已被他人修改，请刷新");
    }

    public static BusinessException projectStatusInvalid() {
        return new BusinessException(5003, "项目状态不允许此操作");
    }

    public static BusinessException projectNotInProgress() {
        return new BusinessException(5004, "项目不在过程中");
    }

    public static BusinessException stageSame() {
        return new BusinessException(5005, "当前阶段与下一步阶段相同");
    }

    public static BusinessException stageFeedbackRequired() {
        return new BusinessException(5006, "当前阶段与下一步阶段不同时必须填写阶段反馈");
    }

    // ========== 返利/合同错误（6xxx）便捷方法 ==========

    public static BusinessException rebateConfirmed() {
        return new BusinessException(6001, "返利单已确认");
    }

    public static BusinessException rebatePaid() {
        return new BusinessException(6002, "返利单已付款");
    }

    public static BusinessException rebateNoPermission() {
        return new BusinessException(6003, "无返利访问权限");
    }

    public static BusinessException contractSigned() {
        return new BusinessException(6004, "合同已签订");
    }

    // ========== 报销错误（6xxx / 6010-6019）便捷方法 ==========

    public static BusinessException reimbursementNotFound() {
        return new BusinessException(6010, "报销记录不存在");
    }

    public static BusinessException reimbursementStatusInvalid() {
        return new BusinessException(6011, "报销状态不允许此操作");
    }

    public static BusinessException reimbursementAlreadyApproved() {
        return new BusinessException(6012, "报销已审批，无法再次审批");
    }

    public static BusinessException reimbursementRejected() {
        return new BusinessException(6013, "报销已驳回");
    }

    public static BusinessException reimbursementAlreadyPaid() {
        return new BusinessException(6014, "报销已付款");
    }

    public static BusinessException reimbursementNotApplicant() {
        return new BusinessException(6015, "仅申请人本人可操作");
    }

    public static BusinessException reimbursementNotApprover() {
        return new BusinessException(6016, "仅审批人可执行此操作");
    }

    public static BusinessException reimbursementAttachmentNotFound() {
        return BusinessException.resourceNotFound("附件");
    }

    public static BusinessException reimbursementAttachmentUploadFailed(String msg) {
        return new BusinessException(6018, "附件上传失败：" + msg);
    }

}
