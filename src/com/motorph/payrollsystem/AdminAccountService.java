package com.motorph.payrollsystem;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Account administration for the UserAdmin screen (replaces LogInCredentials.csv handling).
 *
 *  - Listing accounts reads the profiles table; RLS lets only admins see everyone.
 *  - Creating and deleting accounts calls the "admin-users" Edge Function, because those
 *    actions need the privileged key, which must never be inside this desktop app.
 *    The function re-checks, on the server, that the caller is an active admin.
 *
 * Passwords are sent once over HTTPS to the function and hashed by Supabase Auth.
 * Nothing here stores or logs them.
 */
public final class AdminAccountService {

    public static final class Account {
        public final int employeeNumber;
        public final String role;      // "admin" or "employee"
        public final String status;    // "Active" or "Inactive"
        Account(int employeeNumber, String role, String status) {
            this.employeeNumber = employeeNumber;
            this.role = role;
            this.status = status;
        }
    }

    private static final String FUNCTION_PATH = "/functions/v1/admin-users";

    private AdminAccountService() { }

    /** Every account, ordered by employee number. Admin only. */
    public static List<Account> listAccounts() {
        SessionManager.requireRole("admin");
        JsonArray rows = SupabaseClient.request("GET",
                "/rest/v1/profiles?select=employee_id,role,status&order=employee_id.asc",
                null, SessionManager.token(), false).getAsJsonArray();
        List<Account> list = new ArrayList<>();
        for (JsonElement e : rows) {
            JsonObject o = e.getAsJsonObject();
            list.add(new Account(o.get("employee_id").getAsInt(),
                    o.get("role").getAsString(), o.get("status").getAsString()));
        }
        return list;
    }

    /**
     * Creates a login for an existing employee. The account is flagged so the user must
     * change this temporary password. Role is "admin" or "employee".
     */
    public static void create(int employeeNumber, String temporaryPassword, String role) {
        SessionManager.requireRole("admin");
        if (temporaryPassword == null || temporaryPassword.length() < 8) {
            throw new IllegalArgumentException("Password should contain at least 8 characters.");
        }
        JsonObject body = new JsonObject();
        body.addProperty("action", "create");
        body.addProperty("employee_id", employeeNumber);
        body.addProperty("password", temporaryPassword);
        body.addProperty("role", role);
        SupabaseClient.request("POST", FUNCTION_PATH, body, SessionManager.token(), false);
    }

    /** Deletes an employee's login (their profile goes with it). */
    public static void delete(int employeeNumber) {
        SessionManager.requireRole("admin");
        JsonObject body = new JsonObject();
        body.addProperty("action", "delete");
        body.addProperty("employee_id", employeeNumber);
        SupabaseClient.request("POST", FUNCTION_PATH, body, SessionManager.token(), false);
    }
}
