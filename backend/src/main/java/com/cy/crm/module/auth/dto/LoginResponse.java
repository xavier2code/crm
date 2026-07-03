package com.cy.crm.module.auth.dto;

import com.cy.crm.security.DataScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "登录响应")
public class LoginResponse {
    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "令牌类型（Bearer）")
    private String tokenType = "Bearer";

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    @Schema(description = "角色列表")
    private List<String> roles;

    @Schema(description = "菜单树")
    private List<Object> menuTree;

    @Schema(description = "权限编码列表")
    private List<String> permissionCodes;

    @Schema(description = "数据权限范围")
    private DataScope dataScope;

    @Schema(description = "是否需要强制修改密码（true 时表示只返回了临时 token，仅可用于改密）")
    private Boolean mustChangePassword;

    @Data
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID")
        private Long id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "真实姓名")
        private String realName;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "部门ID")
        private Long departmentId;

        @Schema(description = "部门名称")
        private String departmentName;
    }
}
