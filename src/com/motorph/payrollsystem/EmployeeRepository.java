package com.motorph.payrollsystem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Replaces every read/write of csv/EmployeeDetails.csv.
 * Returns your existing EmployeeDetails objects (all-String fields) so the screens
 * keep working. RLS decides what comes back: an admin gets every row, an employee
 * gets only their own row.
 */
public final class EmployeeRepository {

    // The UI shows birthdays as MM/dd/yyyy; the database stores ISO dates.
    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private EmployeeRepository() { }

    /** Admin screens (Employee Master). Non-admins will only see their own row. */
    public static List<EmployeeDetails> findAll() {
        JsonArray rows = SupabaseClient.request("GET",
                "/rest/v1/employees?select=*&order=employee_id.asc",
                null, SessionManager.token(), false).getAsJsonArray();
        List<EmployeeDetails> list = new ArrayList<>();
        for (JsonElement e : rows) list.add(toModel(e.getAsJsonObject()));
        return list;
    }

    /** Use this instead of employee[empNo - 10001]; it is not position-dependent. */
    public static EmployeeDetails findById(int employeeNumber) {
        JsonArray rows = SupabaseClient.request("GET",
                "/rest/v1/employees?select=*&employee_id=eq." + employeeNumber,
                null, SessionManager.token(), false).getAsJsonArray();
        return rows.size() == 0 ? null : toModel(rows.get(0).getAsJsonObject());
    }

    public static double hourlyRate(int employeeNumber) {
        EmployeeDetails e = findById(employeeNumber);
        return e == null ? 0 : Double.parseDouble(e.hourlyRate);
    }

    /** Admin only. Replaces EmployeeMaster.updateCsv() for edits. */
    public static void update(int employeeNumber, String fname, String lname, String bday,
                              String address, String phone, String status,
                              String position, String supervisor) {
        SessionManager.requireRole("hr_admin");
        JsonObject b = new JsonObject();
        b.addProperty("first_name", fname);
        b.addProperty("last_name", lname);
        b.addProperty("birthday", toIsoDate(bday));
        b.addProperty("address", address);
        b.addProperty("phone", phone);
        b.addProperty("status", status);
        b.addProperty("position", position);
        b.addProperty("supervisor", supervisor);
        JsonArray changed = SupabaseClient.request("PATCH",
                "/rest/v1/employees?employee_id=eq." + employeeNumber,
                b, SessionManager.token(), true).getAsJsonArray();
        if (changed.size() == 0) throw new SecurityException("Update was not applied (not permitted or not found).");
        AuditLogger.log("RECORD_UPDATED", "employee:" + employeeNumber);
    }

    /**
     * Admin only. NOTE: this fails with a foreign-key error if the employee still has
     * timesheets or leave records (see the notes: decide between cascade or soft-delete).
     */
    public static void delete(int employeeNumber) {
        SessionManager.requireRole("hr_admin");
        JsonArray gone = SupabaseClient.request("DELETE",
                "/rest/v1/employees?employee_id=eq." + employeeNumber,
                null, SessionManager.token(), true).getAsJsonArray();
        if (gone.size() == 0) throw new SecurityException("Delete was not applied (not permitted or not found).");
        AuditLogger.log("RECORD_DELETED", "employee:" + employeeNumber);
    }

    // ------------------------------------------------------------------ mapping

    private static EmployeeDetails toModel(JsonObject o) {
        String basic = plain(o, "basic_salary");
        String semiMonthly = new BigDecimal(basic).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        return new EmployeeDetails(
                str(o, "employee_id"), str(o, "last_name"), str(o, "first_name"),
                fromIsoDate(str(o, "birthday")), str(o, "address"), str(o, "phone"),
                str(o, "sss_no"), str(o, "philhealth_no"), str(o, "tin_no"), str(o, "pagibig_no"),
                str(o, "status"), str(o, "position"), str(o, "supervisor"),
                basic, plain(o, "rice_subsidy"), plain(o, "phone_allow"), plain(o, "clothing_allow"),
                semiMonthly, plain(o, "hourly_rate"));
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    /** 90000.00 -> "90000" (the UI appends ".00" itself); 535.71 stays "535.71". */
    private static String plain(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return "0";
        BigDecimal b = o.get(key).getAsBigDecimal().stripTrailingZeros();
        return (b.scale() < 0 ? b.setScale(0) : b).toPlainString();
    }

    private static String fromIsoDate(String iso) {
        return iso.isEmpty() ? "" : LocalDate.parse(iso).format(UI_DATE);
    }

    private static String toIsoDate(String ui) {
        try {
            return LocalDate.parse(ui.trim(), UI_DATE).toString();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Birthday must be in MM/dd/yyyy format.");
        }
    }
}
