package com.motorph.payrollsystem;

import com.google.gson.JsonObject;

/**
 * Writes security-sensitive events to audit_logs (SIP Control 5).
 * Logging must never break the app, so every failure is swallowed.
 * NEVER pass passwords or tokens into these methods.
 */
public final class AuditLogger {

    private AuditLogger() { }

    /** Use after sign-in. Examples: LOGIN_SUCCESS, RECORD_DELETED, LEAVE_APPROVED. */
    public static void log(String eventType, String target) {
        if (!SessionManager.isLoggedIn()) return;
        try {
            JsonObject body = new JsonObject();
            body.addProperty("user_id", SessionManager.userId());
            body.addProperty("event_type", eventType);
            body.addProperty("target", target);
            SupabaseClient.request("POST", "/rest/v1/audit_logs", body, SessionManager.token(), false);
        } catch (Exception e) {
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }

    /** Failed logins happen before a session exists, so they go through a narrow RPC. */
    public static void logFailedLogin(String employeeNumber) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("p_employee", employeeNumber == null ? "" : employeeNumber);
            SupabaseClient.request("POST", "/rest/v1/rpc/log_failed_login", body, null, false);
        } catch (Exception e) {
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }
}
