package com.example.poverty.security;

/**
 * Defines canonical role names and Spring Security authorities used in the system.
 */
public final class RoleConstants {
    private RoleConstants() {}

    public static final String ROLE_CITIZEN_NAME = "群众";
    public static final String ROLE_ANALYST_NAME = "数据分析师";
    public static final String ROLE_ADMIN_NAME = "管理员";

    public static final String AUTHORITY_CITIZEN = "ROLE_CITIZEN";
    public static final String AUTHORITY_ANALYST = "ROLE_ANALYST";
    public static final String AUTHORITY_ADMIN = "ROLE_ADMIN";

    /**
     * Maps stored role names to granted authority strings.
     */
    public static String toAuthority(String roleName) {
        if (ROLE_ADMIN_NAME.equalsIgnoreCase(roleName) || "ADMIN".equalsIgnoreCase(roleName)) {
            return AUTHORITY_ADMIN;
        }
        if (ROLE_ANALYST_NAME.equalsIgnoreCase(roleName) || "ANALYST".equalsIgnoreCase(roleName)) {
            return AUTHORITY_ANALYST;
        }
        return AUTHORITY_CITIZEN;
    }

    public static boolean isAdminRole(String roleName) {
        return AUTHORITY_ADMIN.equals(toAuthority(roleName));
    }

    public static boolean isAnalystRole(String roleName) {
        String authority = toAuthority(roleName);
        return AUTHORITY_ANALYST.equals(authority) || AUTHORITY_ADMIN.equals(authority);
    }
}



