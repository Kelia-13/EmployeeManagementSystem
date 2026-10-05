import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Employee Management System for Organization X.
 * Responsibilities: (1) manage employee records, (2) process payments,
 * (3) generate payroll reports.
 */
public class EmployeeManagementSystem {

    // ---- Business-rule constants ----
    public static final double STANDARD_HOURS = 160.0;       // standard hours per month
    public static final double MAX_HOURS = 744.0;            // 31 days x 24 h: physical upper limit
    public static final double OVERTIME_MULTIPLIER = 1.5;
    public static final double MANAGEMENT_BONUS_RATE = 0.10;
    public static final double MID_TAX_THRESHOLD = 2000.0;   // gross >= 2000 -> 20%
    public static final double HIGH_TAX_THRESHOLD = 5000.0;  // gross >= 5000 -> 30%
    public static final double LOW_TAX_RATE = 0.10;
    public static final double MID_TAX_RATE = 0.20;
    public static final double HIGH_TAX_RATE = 0.30;

    /** One employee record. */
    private static class Employee {
        final String id;
        final String name;
        final String department;
        final double baseSalary;
        double netPay;
        boolean paid;

        Employee(String id, String name, String department, double baseSalary) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.baseSalary = baseSalary;
        }
    }

    // LinkedHashMap keeps insertion order so reports are deterministic.
    private final Map<String, Employee> employees = new LinkedHashMap<>();

    // ------------------------------------------------------------------
    // 1. Employee management
    // ------------------------------------------------------------------

    /** @return true if added, false if the ID already exists.
     *  @throws IllegalArgumentException for missing/invalid data. */
    public boolean addEmployee(String id, String name, String department, double baseSalary) {
        if (isBlank(id)) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        if (isBlank(name)) {
            throw new IllegalArgumentException("Employee name is required");
        }
        if (isBlank(department)) {
            throw new IllegalArgumentException("Department is required");
        }
        if (baseSalary < 0 || Double.isNaN(baseSalary)) {
            throw new IllegalArgumentException("Base salary must be a non-negative number");
        }
        String key = id.trim();
        if (employees.containsKey(key)) {
            return false;
        }
        employees.put(key, new Employee(key, name.trim(), department.trim(), baseSalary));
        return true;
    }

    /** @return true if removed, false if the ID does not exist. */
    public boolean removeEmployee(String id) {
        if (id == null) {
            return false;
        }
        return employees.remove(id.trim()) != null;
    }

    public int getEmployeeCount() {
        return employees.size();
    }

    // ------------------------------------------------------------------
    // 2. Payment processing
    // ------------------------------------------------------------------

    /**
     * Computes and stores the net pay of an employee.
     * regular pay = base x (hours worked, capped at 160) / 160
     * overtime    = overtime hours x (base / 160) x 1.5
     * Management employees receive a 10% bonus on gross pay, then tax is
     * deducted according to the gross-pay bracket.
     *
     * @throws NoSuchElementException   if the employee is not registered
     * @throws IllegalArgumentException if hours are negative or above MAX_HOURS
     */
    public double processPayment(String id, double hoursWorked, double overtimeHours) {
        Employee e = (id == null) ? null : employees.get(id.trim());
        if (e == null) {
            throw new NoSuchElementException("Employee not found: " + id);
        }
        if (hoursWorked < 0 || overtimeHours < 0) {
            throw new IllegalArgumentException("Hours must not be negative");
        }
        if (hoursWorked > MAX_HOURS) {
            throw new IllegalArgumentException("Hours worked exceeds the monthly maximum");
        }

        double paidHours = (hoursWorked < STANDARD_HOURS) ? hoursWorked : STANDARD_HOURS;
        double hourlyRate = e.baseSalary / STANDARD_HOURS;
        double gross = hourlyRate * paidHours + overtimeHours * hourlyRate * OVERTIME_MULTIPLIER;

        if (e.department.equalsIgnoreCase("Management")) {
            gross = gross + gross * MANAGEMENT_BONUS_RATE;
        }

        double net = round2(gross - gross * taxRate(gross));
        e.netPay = net;
        e.paid = true;
        return net;
    }

    /** Tax rate for a given gross pay. */
    public static double taxRate(double gross) {
        if (gross >= HIGH_TAX_THRESHOLD) {
            return HIGH_TAX_RATE;
        } else if (gross >= MID_TAX_THRESHOLD) {
            return MID_TAX_RATE;
        }
        return LOW_TAX_RATE;
    }

    // ------------------------------------------------------------------
    // 3. Reporting
    // ------------------------------------------------------------------

    public String generatePayrollReport() {
        if (employees.isEmpty()) {
            return "No employees to report.";
        }
        StringBuilder lines = new StringBuilder();
        double total = 0;
        int paidCount = 0;
        Employee top = null;

        for (Employee e : employees.values()) {
            if (!e.paid) {
                continue;
            }
            lines.append(String.format(Locale.US, "%-6s %-18s %-12s %10.2f%n",
                    e.id, e.name, e.department, e.netPay));
            total += e.netPay;
            paidCount++;
            if (top == null || e.netPay > top.netPay) {
                top = e;
            }
        }
        if (paidCount == 0) {
            return "No payments have been processed.";
        }
        StringBuilder report = new StringBuilder("=== PAYROLL REPORT ===\n");
        report.append(lines);
        report.append(String.format(Locale.US, "Total payroll: %.2f%n", total));
        report.append(String.format(Locale.US, "Average net pay: %.2f%n", total / paidCount));
        report.append(String.format(Locale.US, "Top earner: %s (%.2f)", top.name, top.netPay));
        return report.toString();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
