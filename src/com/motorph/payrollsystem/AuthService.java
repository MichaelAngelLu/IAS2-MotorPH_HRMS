package com.motorph.payrollsystem;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.motorph.payrollsystem.SupabaseClient.SupabaseException;

/**
 * Login / password change against Supabase Auth. Passwords are verified server-side
 * (Supabase stores bcrypt hashes), so no password or hash is ever loaded into the app.
 */
public final class AuthService {

    private static final String EMAIL_DOMAIN = "motorph.local";   // must match migrate_credentials.py

    private AuthService() { }

    /** Replaces UserLogin.verifyLogin(). Returns true and starts a session on success. */
    public static boolean login(String employeeNumber, String password) {
        String emp = employeeNumber == null ? "" : employeeNumber.trim();
        if (!emp.matches("\\d{1,10}") || password == null || password.isEmpty()) {
            AuditLogger.logFailedLogin(emp);
            return false;
        }
        try {
            JsonObject session = signIn(emp, password);
            String jwt = session.get("access_token").getAsString();
            String uid = session.getAsJsonObject("user").get("id").getAsString();

            JsonArray rows = SupabaseClient.request("GET",
                    "/rest/v1/profiles?id=eq." + uid + "&select=employee_id,role,status,must_change_password",
                    null, jwt, false).getAsJsonArray();
            if (rows.size() == 0) {
                AuditLogger.logFailedLogin(emp);
                return false;
            }
            JsonObject p = rows.get(0).getAsJsonObject();
            if (!"Active".equals(p.get("status").getAsString())) {
                AuditLogger.logFailedLogin(emp);          // inactive account
                return false;
            }
            SessionManager.start(jwt, uid, p.get("employee_id").getAsInt(),
                    p.get("role").getAsString(), p.get("must_change_password").getAsBoolean());
            AuditLogger.log("LOGIN_SUCCESS", "employee:" + emp);
            return true;
        } catch (SupabaseException e) {
            AuditLogger.logFailedLogin(emp);
            return false;
        }
    }

    public static void logout() {
        AuditLogger.log("LOGOUT", "employee:" + SessionManager.employeeNumber());
        SessionManager.clear();
    }

    /**
     * Replaces PayrollSettings.checkCurrent() + saveNewPassword().
     * @return false if the current password is wrong
     * @throws IllegalArgumentException if the new password is too short
     */
    public static boolean changePassword(String current, String newPassword) {
        if (!SessionManager.isLoggedIn()) throw new SecurityException("Not signed in.");
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password should contain at least 8 characters.");
        }
        try {
            signIn(String.valueOf(SessionManager.employeeNumber()), current);   // verifies current password
        } catch (SupabaseException e) {
            // TEMPORARY diagnostic: shows Supabase's reason in the console (no password is printed).
            System.err.println("Password verification rejected: HTTP " + e.status + " - " + e.getMessage());
            if (e.status == 400 || e.status == 401) return false;   // Supabase rejected the password
            throw e;   // network / rate limit (429) / server error: let the caller show the real reason
        }
        JsonObject body = new JsonObject();
        body.addProperty("password", newPassword);
        SupabaseClient.request("PUT", "/auth/v1/user", body, SessionManager.token(), false);
        SupabaseClient.request("POST", "/rest/v1/rpc/clear_must_change_password",
                new JsonObject(), SessionManager.token(), false);
        SessionManager.passwordChanged();
        AuditLogger.log("PASSWORD_CHANGED", "employee:" + SessionManager.employeeNumber());
        return true;
    }

    private static JsonObject signIn(String employeeNumber, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("email", employeeNumber + "@" + EMAIL_DOMAIN);
        body.addProperty("password", password);
        return SupabaseClient.request("POST", "/auth/v1/token?grant_type=password",
                body, null, false).getAsJsonObject();
    }
}
