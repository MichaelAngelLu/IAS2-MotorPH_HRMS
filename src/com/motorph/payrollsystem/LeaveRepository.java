package com.motorph.payrollsystem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Replaces every read/write of csv/AppliedLeaves.csv.
 *
 * The old code identified a leave by its position in an array and rewrote the whole
 * file on every change. Here each leave has a database id (Row.id); keep it next to
 * the table row (e.g. in a List<Row> parallel to the JTable) and use it for
 * approve / reject / cancel / delete.
 *
 * Status text: the DB_* constants below must match leave_applications_status_check in your
 * database. The screens still use "Applied", "Approved", "Rejected" and "Cancelled".
 * RLS: employees see/file/cancel their own leaves; only admins approve, reject, delete.
 */
public final class LeaveRepository {

    // Status text AS STORED in the database. Change only these two if your constraint differs.
    private static final String DB_PENDING = "Applied";
    private static final String DB_CANCELLED = "Cancelled";

    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH);

    public static final class Row {
        public final long id;
        public final LeaveDetails details;
        Row(long id, LeaveDetails details) {
            this.id = id;
            this.details = details;
        }
    }

    private LeaveRepository() { }

    /** Newest first. Admin gets everyone's leaves, an employee gets their own. */
    public static List<Row> list() {
        JsonArray rows = SupabaseClient.request("GET",
                "/rest/v1/leave_applications?select=*&order=id.desc",
                null, SessionManager.token(), false).getAsJsonArray();
        List<Row> out = new ArrayList<>();
        for (JsonElement e : rows) {
            JsonObject o = e.getAsJsonObject();
            String reason = o.has("reason") && !o.get("reason").isJsonNull() ? o.get("reason").getAsString() : "";
            out.add(new Row(o.get("id").getAsLong(), new LeaveDetails(
                    o.get("employee_id").getAsString(),
                    o.get("leave_type").getAsString(),
                    LocalDate.parse(o.get("start_date").getAsString()).format(UI_DATE),
                    LocalDate.parse(o.get("end_date").getAsString()).format(UI_DATE),
                    toUiStatus(o.get("status").getAsString()),
                    reason)));
        }
        return out;
    }

    /** The signed-in employee files a leave for themselves. */
    public static void file(String leaveType, LocalDate start, LocalDate end, String reason) {
        if (end.isBefore(start)) throw new IllegalArgumentException("End date cannot be before start date.");
        JsonObject b = new JsonObject();
        b.addProperty("employee_id", SessionManager.employeeNumber());
        b.addProperty("leave_type", leaveType);
        b.addProperty("start_date", start.toString());
        b.addProperty("end_date", end.toString());
        b.addProperty("reason", reason);
        b.addProperty("status", DB_PENDING);
        SupabaseClient.request("POST", "/rest/v1/leave_applications", b, SessionManager.token(), false);
        AuditLogger.log("LEAVE_FILED", "employee:" + SessionManager.employeeNumber());
    }

    /** Admin only. Replaces EmployeeMaster.approve(). */
    public static void approve(long id) { decide(id, "Approved", "LEAVE_APPROVED"); }

    /** Admin only. Replaces EmployeeMaster.reject(). */
    public static void reject(long id)  { decide(id, "Rejected", "LEAVE_REJECTED"); }

    private static void decide(long id, String status, String auditEvent) {
        SessionManager.requireRole("admin");
        JsonObject b = new JsonObject();
        b.addProperty("status", status);
        b.addProperty("decided_by", SessionManager.userId());
        JsonArray changed = SupabaseClient.request("PATCH",
                "/rest/v1/leave_applications?id=eq." + id,
                b, SessionManager.token(), true).getAsJsonArray();
        if (changed.size() == 0) throw new SecurityException("Update was not applied (not permitted or not found).");
        AuditLogger.log(auditEvent, "leave:" + id);
    }

    /** Employee cancels their own pending leave (allowed by policy lv_self_cancel). */
    public static void cancel(long id) {
        JsonObject b = new JsonObject();
        b.addProperty("status", DB_CANCELLED);
        JsonArray changed = SupabaseClient.request("PATCH",
                "/rest/v1/leave_applications?id=eq." + id,
                b, SessionManager.token(), true).getAsJsonArray();
        if (changed.size() == 0) throw new IllegalStateException("Only your own pending leaves can be cancelled.");
        AuditLogger.log("LEAVE_CANCELLED", "leave:" + id);
    }

    /** Admin only. Replaces EmployeeMaster.delete(). */
    public static void delete(long id) {
        SessionManager.requireRole("admin");
        JsonArray gone = SupabaseClient.request("DELETE",
                "/rest/v1/leave_applications?id=eq." + id,
                null, SessionManager.token(), true).getAsJsonArray();
        if (gone.size() == 0) throw new SecurityException("Delete was not applied (not permitted or not found).");
        AuditLogger.log("LEAVE_DELETED", "leave:" + id);
    }

    /** Maps whatever the database holds to the four statuses the screens understand. */
    private static String toUiStatus(String db) {
        if (db.equals(DB_PENDING) || db.equals("Pending") || db.equals("Applied")) return "Applied";
        if (db.startsWith("Cancelled")) return "Cancelled";      // also "Cancelled by Employee"
        return db;
    }
}