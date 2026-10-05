import java.util.*;

/** Plain-Java test harness (no external libraries). Prints: ID <TAB> PASS|FAIL <TAB> actual result */
public class EmployeeManagementSystemTest {

    interface Case { String run() throws Exception; }

    static int passed = 0, failed = 0;

    static void test(String id, Case c) {
        String status, actual;
        try { actual = c.run(); status = "PASS"; passed++; }
        catch (AssertionError e) { actual = "MISMATCH: " + e.getMessage(); status = "FAIL"; failed++; }
        catch (Exception e) { actual = "UNEXPECTED " + e; status = "FAIL"; failed++; }
        System.out.println(id + "\t" + status + "\t" + actual);
    }

    static void check(boolean cond, String msg) { if (!cond) throw new AssertionError(msg); }

    static EmployeeManagementSystem sys() { return new EmployeeManagementSystem(); }

    static EmployeeManagementSystem withEmp(String dept, double salary) {
        EmployeeManagementSystem s = sys();
        s.addEmployee("E001", "Alice Uwase", dept, salary);
        return s;
    }

    /** Runs a call that must throw `type`; returns "<Type> thrown". */
    static String expectThrows(Class<? extends Exception> type, Runnable r) {
        try { r.run(); }
        catch (Exception e) {
            check(type.isInstance(e), "expected " + type.getSimpleName() + " but got " + e.getClass().getSimpleName());
            return e.getClass().getSimpleName() + " thrown (\"" + e.getMessage() + "\")";
        }
        throw new AssertionError("no exception thrown");
    }

    static String pay(EmployeeManagementSystem s, double hrs, double ot, double expected) {
        double net = s.processPayment("E001", hrs, ot);
        check(Math.abs(net - expected) < 0.005, "net pay = " + net + ", expected " + expected);
        return String.format(Locale.US, "Net pay = %.2f", net);
    }

    public static void main(String[] args) {
        // =================== BLACK-BOX ===================
        // FR1 / FR2 : add employee
        test("BB-01", () -> { EmployeeManagementSystem s = sys();
            check(s.addEmployee("E001", "Alice Uwase", "Engineering", 3000), "add returned false");
            check(s.getEmployeeCount() == 1, "count=" + s.getEmployeeCount());
            return "Employee added; count = 1."; });
        test("BB-02", () -> { EmployeeManagementSystem s = withEmp("Engineering", 3000);
            boolean r = s.addEmployee("E001", "Bob", "Sales", 2000);
            check(!r && s.getEmployeeCount() == 1, "r=" + r + ", count=" + s.getEmployeeCount());
            return "Second add returned false; count = 1."; });
        test("BB-03", () -> expectThrows(IllegalArgumentException.class, () -> sys().addEmployee("", "Alice", "Engineering", 3000)));
        test("BB-04", () -> expectThrows(IllegalArgumentException.class, () -> sys().addEmployee("E001", "", "Engineering", 3000)));
        test("BB-05", () -> expectThrows(IllegalArgumentException.class, () -> sys().addEmployee("E001", "Alice", "Engineering", -1)));
        test("BB-06", () -> { EmployeeManagementSystem s = sys();
            check(s.addEmployee("E001", "Alice", "Engineering", 0), "salary 0 rejected");
            return "Employee added with salary 0; count = " + s.getEmployeeCount() + "."; });
        // FR3 : remove employee
        test("BB-07", () -> { EmployeeManagementSystem s = withEmp("Engineering", 3000);
            check(s.removeEmployee("E001") && s.getEmployeeCount() == 0, "not removed");
            return "Returned true; count = 0."; });
        test("BB-08", () -> { EmployeeManagementSystem s = sys();
            check(!s.removeEmployee("E999"), "returned true");
            return "Returned false; count = " + s.getEmployeeCount() + "."; });
        // FR4 : payment computation
        test("BB-09", () -> pay(withEmp("Engineering", 1600), 160, 0, 1440.00));
        test("BB-10", () -> pay(withEmp("Engineering", 3000), 160, 0, 2400.00));
        test("BB-11", () -> pay(withEmp("Engineering", 1999.99), 160, 0, 1799.99));
        test("BB-12", () -> pay(withEmp("Engineering", 2000), 160, 0, 1600.00));
        test("BB-13", () -> pay(withEmp("Engineering", 4999), 160, 0, 3999.20));
        test("BB-14", () -> pay(withEmp("Engineering", 5000), 160, 0, 3500.00));
        test("BB-15", () -> pay(withEmp("Management", 3000), 160, 0, 2640.00));
        test("BB-16", () -> pay(withEmp("Management", 4600), 160, 0, 3542.00));
        test("BB-17", () -> pay(withEmp("Engineering", 3200), 160, 10, 2800.00));
        test("BB-18", () -> pay(withEmp("Engineering", 3200), 80, 0, 1440.00));
        test("BB-19", () -> pay(withEmp("Engineering", 3000), 0, 0, 0.00));
        // FR5 : payment rejection
        test("BB-20", () -> expectThrows(IllegalArgumentException.class, () -> withEmp("Engineering", 3000).processPayment("E001", -1, 0)));
        test("BB-21", () -> expectThrows(IllegalArgumentException.class, () -> withEmp("Engineering", 3000).processPayment("E001", 160, -5)));
        test("BB-22", () -> expectThrows(IllegalArgumentException.class, () -> withEmp("Engineering", 3000).processPayment("E001", 745, 0)));
        test("BB-23", () -> pay(withEmp("Engineering", 3200), 744, 0, 2560.00));
        test("BB-24", () -> expectThrows(NoSuchElementException.class, () -> sys().processPayment("E999", 160, 0)));
        // FR6 : report
        test("BB-25", () -> { EmployeeManagementSystem s = sys();
            s.addEmployee("E001", "Alice Uwase", "Engineering", 3000);
            s.addEmployee("E002", "Eric Habimana", "Sales", 1600);
            s.processPayment("E001", 160, 0); s.processPayment("E002", 160, 0);
            String r = s.generatePayrollReport();
            check(r.contains("Alice Uwase") && r.contains("Eric Habimana"), "employees missing");
            check(r.contains("Total payroll: 3840.00"), "total wrong");
            check(r.contains("Average net pay: 1920.00"), "average wrong");
            check(r.contains("Top earner: Alice Uwase (2400.00)"), "top earner wrong");
            return "Report lists both employees; Total 3840.00; Average 1920.00; Top earner Alice Uwase (2400.00)."; });
        test("BB-26", () -> { String r = sys().generatePayrollReport();
            check(r.equals("No employees to report."), "got: " + r); return "Message returned: \"" + r + "\""; });
        // FR7 : count
        test("BB-27", () -> { EmployeeManagementSystem s = sys();
            check(s.getEmployeeCount() == 0, "initial count");
            s.addEmployee("E001", "A", "IT", 1000); s.addEmployee("E002", "B", "IT", 1000); s.addEmployee("E003", "C", "IT", 1000);
            s.removeEmployee("E002");
            check(s.getEmployeeCount() == 2, "count=" + s.getEmployeeCount());
            return "Count: 0 initially, 2 after three adds and one removal."; });

        // =================== WHITE-BOX ===================
        // addEmployee decisions
        test("WB-01", () -> expectThrows(IllegalArgumentException.class, () -> sys().addEmployee(null, "Alice", "IT", 1000)));
        test("WB-02", () -> expectThrows(IllegalArgumentException.class, () -> sys().addEmployee("   ", "Alice", "IT", 1000)));
        test("WB-03", () -> expectThrows(IllegalArgumentException.class, () -> sys().addEmployee("E001", null, "IT", 1000)));
        test("WB-04", () -> expectThrows(IllegalArgumentException.class, () -> sys().addEmployee("E001", "Alice", "  ", 1000)));
        test("WB-05", () -> expectThrows(IllegalArgumentException.class, () -> sys().addEmployee("E001", "Alice", "IT", Double.NaN)));
        test("WB-06", () -> { EmployeeManagementSystem s = withEmp("IT", 1000);
            boolean r = s.addEmployee("  E001  ", "Bob", "IT", 1000);
            check(!r && s.getEmployeeCount() == 1, "r=" + r);
            return "Returned false (ID trimmed to E001); count = 1."; });
        // removeEmployee
        test("WB-07", () -> { check(!sys().removeEmployee(null), "returned true"); return "Returned false, no exception."; });
        test("WB-08", () -> { EmployeeManagementSystem s = withEmp("IT", 1000);
            check(s.removeEmployee(" E001 ") && s.getEmployeeCount() == 0, "not removed");
            return "Returned true; count = 0."; });
        // processPayment decisions
        test("WB-09", () -> expectThrows(NoSuchElementException.class, () -> sys().processPayment(null, 160, 0)));
        test("WB-10", () -> expectThrows(IllegalArgumentException.class, () -> withEmp("IT", 1000).processPayment("E001", -1, 0)));
        test("WB-11", () -> expectThrows(IllegalArgumentException.class, () -> withEmp("IT", 1000).processPayment("E001", 0, -1)));
        test("WB-12", () -> expectThrows(IllegalArgumentException.class, () -> withEmp("IT", 1000).processPayment("E001", -1, -1)));
        test("WB-13", () -> expectThrows(IllegalArgumentException.class, () -> withEmp("IT", 1000).processPayment("E001", 744.01, 0)));
        test("WB-14", () -> pay(withEmp("Engineering", 3200), 100, 0, 1600.00));
        test("WB-15", () -> pay(withEmp("Engineering", 3200), 160, 0, 2560.00));
        test("WB-16", () -> pay(withEmp("Engineering", 3200), 200, 0, 2560.00));
        test("WB-17", () -> pay(withEmp("  management ", 3000), 160, 0, 2640.00));
        test("WB-18", () -> { double a = EmployeeManagementSystem.taxRate(0), b = EmployeeManagementSystem.taxRate(2000), c = EmployeeManagementSystem.taxRate(5000);
            check(a == 0.10 && b == 0.20 && c == 0.30, a + "," + b + "," + c);
            return "taxRate(0)=0.10, taxRate(2000)=0.20, taxRate(5000)=0.30"; });
        // generatePayrollReport decisions
        test("WB-19", () -> { EmployeeManagementSystem s = sys(); s.addEmployee("E001", "Alice", "IT", 1000);
            String r = s.generatePayrollReport();
            check(r.equals("No payments have been processed."), "got: " + r); return "Message returned: \"" + r + "\""; });
        test("WB-20", () -> { EmployeeManagementSystem s = sys();
            s.addEmployee("E001", "Alice", "IT", 3000); s.addEmployee("E002", "Zed", "IT", 3000);
            s.processPayment("E001", 160, 0);
            String r = s.generatePayrollReport();
            check(!r.contains("Zed") && r.contains("Average net pay: 2400.00"), "unpaid not skipped:\n" + r);
            return "Unpaid employee omitted; average = 2400.00 (divided by 1 paid employee)."; });
        test("WB-21", () -> { EmployeeManagementSystem s = sys();
            s.addEmployee("E001", "Low", "IT", 1600); s.addEmployee("E002", "High", "IT", 3000); s.addEmployee("E003", "Tie", "IT", 3000);
            s.processPayment("E001", 160, 0); s.processPayment("E002", 160, 0); s.processPayment("E003", 160, 0);
            String r = s.generatePayrollReport();
            check(r.contains("Top earner: High (2400.00)"), "got:\n" + r);
            return "Top earner = High (2400.00); equal-pay later employee did not replace it."; });
        test("WB-22", () -> { EmployeeManagementSystem s = withEmp("IT", 3000); s.processPayment("E001", 160, 0);
            String r = s.generatePayrollReport();
            check(r.contains("Total payroll: 2400.00") && r.contains("Average net pay: 2400.00") && r.contains("Top earner: Alice Uwase"), r);
            return "Loop ran once: Total = Average = 2400.00; top earner Alice Uwase."; });

        System.out.println("SUMMARY\t" + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
