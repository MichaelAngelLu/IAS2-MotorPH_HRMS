package com.motorph.payrollsystem;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Account administration for the UserAdmin screen. IT ADMINISTRATORS ONLY.
 *
 * Separation of duties: HR administrators manage people data and leave decisions;
 * IT administrators manage accounts and read the audit log. Neither can do the other's job.
 *
 *  - Listing accounts reads the profiles table; RLS lets only an IT admin see everyone's.
 *  - The employee list for new accounts comes from a database function that returns names only,
 *    so IT never sees salaries, birthdays or government IDs.
 *  - Creating and deleting accounts calls the "admin-users" Edge Function, because those
 *    actions need the privileged key, which must never be inside this desktop app.
 *    The function re-checks on the server that the caller is an active IT admin.
 *
 * Passwords are sent once over HTTPS to the function and hashed by Supabase Auth.
 * Nothing here stores or logs them.
 */
public final class AdminAccountService {

    public static final class Account {
        public final int employeeNumber;
        public final String role;      // "employee", "hr_admin" or "it_admin"
        public final String status;    // "Active" or "Inactive"
        Account(int employeeNumber, String role, String status) {
            this.employeeNumber = employeeNumber;
            this.role = role;
            this.status = status;
        }
    }

    /** An employee who does not have a login yet. Name only, nothing sensitive. */
    public static final class Person {
        public final int employeeNumber;
        public final String firstName;
        public final String lastName;
        Person(int employeeNumber, String firstName, String lastName) {
            this.employeeNumber = employeeNumber;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    private static final String FUNCTION_PATH = "/functions/v1/admin-users";

    private AdminAccountService() { }

    /** Every account, ordered by employee number. IT admin only. */
    public static List<Account> listAccounts() {
        SessionManager.requireRole("it_admin");
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

    /** Employees without a login, as employee number and name only. IT admin only. */
    public static List<Person> listUnprovisionedEmployees() {
        SessionManager.requireRole("it_admin");
        JsonArray rows = SupabaseClient.request("POST", "/rest/v1/rpc/list_unprovisioned_employees",
                new JsonObject(), SessionManager.token(), false).getAsJsonArray();
        List<Person> list = new ArrayList<>();
        for (JsonElement e : rows) {
            JsonObject o = e.getAsJsonObject();
            list.add(new Person(o.get("employee_id").getAsInt(),
                    o.get("first_name").getAsString(), o.get("last_name").getAsString()));
        }
        return list;
    }

    /**
     * Creates a login for an existing employee. The account is flagged so the user must
     * change this temporary password. Role is "employee" or "hr_admin"; IT administrator
     * accounts are appointed in the database by its owner, never from the app.
     */
    public static void create(int employeeNumber, String temporaryPassword, String role) {
        SessionManager.requireRole("it_admin");
        if (temporaryPassword == null || temporaryPassword.length() < 8) {
            throw new IllegalArgumentException("Password should contain at least 8 characters.");
        }
        if (!"employee".equals(role) && !"hr_admin".equals(role)) {
            throw new IllegalArgumentException("Role must be employee or hr_admin.");
        }
        JsonObject body = new JsonObject();
        body.addProperty("action", "create");
        body.addProperty("employee_id", employeeNumber);
        body.addProperty("password", temporaryPassword);
        body.addProperty("role", role);
        SupabaseClient.request("POST", FUNCTION_PATH, body, SessionManager.token(), false);
    }

    /** Deletes an employee's login (their profile goes with it). IT administrator accounts are refused. */
    public static void delete(int employeeNumber) {
        SessionManager.requireRole("it_admin");
        JsonObject body = new JsonObject();
        body.addProperty("action", "delete");
        body.addProperty("employee_id", employeeNumber);
        SupabaseClient.request("POST", FUNCTION_PATH, body, SessionManager.token(), false);
    }
}
