package com.motorph.payrollsystem;

/**
 * Holds the signed-in user's token and role IN MEMORY ONLY (never written to disk).
 * requireRole() is the application-level RBAC check (SIP Control 2): call it at the top
 * of every method that performs an admin action, not just where buttons are disabled.
 */
public final class SessionManager {

    private static String accessToken;
    private static String userId;
    private static int employeeNumber;
    private static String role;               // "admin" or "employee"
    private static boolean mustChangePassword;

    private SessionManager() { }

    static void start(String token, String uid, int empNo, String userRole, boolean mustChange) {
        accessToken = token;
        userId = uid;
        employeeNumber = empNo;
        role = userRole;
        mustChangePassword = mustChange;
    }

    public static void clear() {
        accessToken = null;
        userId = null;
        employeeNumber = 0;
        role = null;
        mustChangePassword = false;
    }

    public static boolean isLoggedIn()          { return accessToken != null; }
    public static String token()                { return accessToken; }
    public static String userId()               { return userId; }
    public static int employeeNumber()          { return employeeNumber; }
    public static String role()                 { return role; }
    public static boolean isAdmin()             { return "admin".equals(role); }
    public static boolean mustChangePassword()  { return mustChangePassword; }
    static void passwordChanged()               { mustChangePassword = false; }

    /** Throws SecurityException (and writes an audit entry) unless the user holds the role. */
    public static void requireRole(String required) {
        if (!isLoggedIn() || !required.equalsIgnoreCase(role)) {
            AuditLogger.log("UNAUTHORIZED_ACCESS", "required_role:" + required);
            throw new SecurityException("You do not have permission to perform this action.");
        }
    }
}
