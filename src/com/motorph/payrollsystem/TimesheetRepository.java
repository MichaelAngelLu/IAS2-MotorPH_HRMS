package com.motorph.payrollsystem;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Replaces the positional arrays built in PrintNReadTxt.printRead()
 * (empHrsWkd[50 + user], weeklyPay[user + index], allEmpRate[user] ...).
 * Instead of "row number = employee + 25 * day", ask for an employee and a date range.
 *
 * A day with no timesheet row means no attendance = 0 hours.
 * (Careful: the old code computed (0 - 0) * 24 - 1 = -1 hour for absences, which
 * deducted a pay hour. This class returns 0 instead. See the notes.)
 */
public final class TimesheetRepository {

    /** Monday of pay period 1 (your UI hard-codes "September 5"). */
    private static final LocalDate FIRST_MONDAY = LocalDate.of(2022, 9, 5);
    /** Same unpaid break the old formula subtracted: (out - in) * 24 - 1. */
    private static final double BREAK_HOURS = 1.0;

    private TimesheetRepository() { }

    /** Pay period 1, 2, 3 ... (the combo box index) -> its Monday. */
    public static LocalDate periodStart(int period) {
        return FIRST_MONDAY.plusWeeks(period - 1L);
    }

    /** Hours worked per date between from and to (inclusive). Missing dates are not in the map. */
    public static Map<LocalDate, Double> hours(int employeeNumber, LocalDate from, LocalDate to) {
        String path = "/rest/v1/timesheets?select=work_date,time_in,time_out"
                + "&employee_id=eq." + employeeNumber
                + "&work_date=gte." + from + "&work_date=lte." + to
                + "&order=work_date.asc";
        JsonArray rows = SupabaseClient.request("GET", path, null, SessionManager.token(), false).getAsJsonArray();
        Map<LocalDate, Double> out = new LinkedHashMap<>();
        for (JsonElement e : rows) {
            JsonObject o = e.getAsJsonObject();
            LocalTime in = LocalTime.parse(o.get("time_in").getAsString());
            LocalTime outT = LocalTime.parse(o.get("time_out").getAsString());
            double h = Duration.between(in, outT).toMinutes() / 60.0 - BREAK_HOURS;
            out.put(LocalDate.parse(o.get("work_date").getAsString()), Math.max(0, h));
        }
        return out;
    }

    /** Monday..Friday hours for a pay period; index 0 = Monday. Absent days are 0. */
    public static double[] weekHours(int employeeNumber, int period) {
        LocalDate monday = periodStart(period);
        Map<LocalDate, Double> m = hours(employeeNumber, monday, monday.plusDays(4));
        double[] week = new double[5];
        for (int i = 0; i < 5; i++) week[i] = m.getOrDefault(monday.plusDays(i), 0.0);
        return week;
    }

    public static double totalHours(int employeeNumber, int period) {
        double sum = 0;
        for (double h : weekHours(employeeNumber, period)) sum += h;
        return sum;
    }

    /** Replaces weeklyPay[user + index]: hours x hourly rate from the employees table. */
    public static double weeklyPay(int employeeNumber, int period) {
        return totalHours(employeeNumber, period) * EmployeeRepository.hourlyRate(employeeNumber);
    }
}
