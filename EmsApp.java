import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console menu that drives EmployeeManagementSystem. */
public class EmsApp {
    public static void main(String[] args) {
        EmployeeManagementSystem ems = new EmployeeManagementSystem();
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("\n1) Add employee  2) Remove employee  3) Process payment");
            System.out.println("4) Payroll report  5) Employee count  0) Exit");
            System.out.print("Choice: ");
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        System.out.print("ID: ");         String id = in.nextLine();
                        System.out.print("Name: ");       String name = in.nextLine();
                        System.out.print("Department: "); String dept = in.nextLine();
                        System.out.print("Base salary: "); double sal = Double.parseDouble(in.nextLine());
                        System.out.println(ems.addEmployee(id, name, dept, sal) ? "Added." : "ID already exists.");
                        break;
                    case "2":
                        System.out.print("ID: ");
                        System.out.println(ems.removeEmployee(in.nextLine()) ? "Removed." : "ID not found.");
                        break;
                    case "3":
                        System.out.print("ID: ");             String pid = in.nextLine();
                        System.out.print("Hours worked: ");   double h = Double.parseDouble(in.nextLine());
                        System.out.print("Overtime hours: "); double ot = Double.parseDouble(in.nextLine());
                        System.out.printf("Net pay: %.2f%n", ems.processPayment(pid, h, ot));
                        break;
                    case "4": System.out.println(ems.generatePayrollReport()); break;
                    case "5": System.out.println("Employees: " + ems.getEmployeeCount()); break;
                    case "0": return;
                    default:  System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException | NoSuchElementException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }
}
