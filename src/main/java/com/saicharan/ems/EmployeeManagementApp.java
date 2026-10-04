package com.saicharan.ems;

import com.saicharan.ems.dao.JdbcEmployeeDao;
import com.saicharan.ems.db.ConnectionFactory;
import com.saicharan.ems.exception.DataAccessException;
import com.saicharan.ems.exception.DuplicateEmailException;
import com.saicharan.ems.exception.EmployeeNotFoundException;
import com.saicharan.ems.exception.ValidationException;
import com.saicharan.ems.model.DepartmentSummary;
import com.saicharan.ems.model.Employee;
import com.saicharan.ems.service.EmployeeService;
import com.saicharan.ems.util.EmployeeValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Console menu for managing employees. Run: mvn compile exec:java */
public class EmployeeManagementApp {

    private final EmployeeService service;
    private final Scanner in;

    public EmployeeManagementApp(EmployeeService service, Scanner in) {
        this.service = service;
        this.in = in;
    }

    public static void main(String[] args) {
        ConnectionFactory factory = ConnectionFactory.fromEnvironment();
        factory.initSchema();
        EmployeeService service = new EmployeeService(new JdbcEmployeeDao(factory));

        if (factory.getUrl().startsWith("jdbc:h2:mem") && service.listEmployees(true).isEmpty()) {
            SampleData.load(service);
            System.out.println("Using an in-memory demo database with sample employees.");
            System.out.println("Set DB_URL, DB_USER and DB_PASSWORD to use MySQL instead.\n");
        }
        new EmployeeManagementApp(service, new Scanner(System.in)).run();
    }

    void run() {
        while (true) {
            System.out.println("""

                    ===== Employee Management System =====
                    1. List employees           6. Deactivate employee
                    2. Add employee             7. Delete employee
                    3. Find employee by id      8. Department raise
                    4. Search by name           9. Department summary
                    5. Update employee          0. Exit""");
            String choice = prompt("Choose an option");
            try {
                switch (choice) {
                    case "1" -> printEmployees(service.listEmployees(false));
                    case "2" -> addEmployee();
                    case "3" -> System.out.println(service.getEmployee(readLong("Employee id")));
                    case "4" -> printEmployees(service.searchByName(prompt("Name contains")));
                    case "5" -> updateEmployee();
                    case "6" -> {
                        service.deactivateEmployee(readLong("Employee id"));
                        System.out.println("Employee deactivated.");
                    }
                    case "7" -> deleteEmployee();
                    case "8" -> {
                        int n = service.giveDepartmentRaise(prompt("Department"), readDecimal("Raise %"));
                        System.out.println("Salary updated for " + n + " employee(s).");
                    }
                    case "9" -> printSummary(service.departmentSummary());
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Please choose a number from the menu.");
                }
            } catch (ValidationException e) {
                System.out.println("Please fix the following:");
                e.getErrors().forEach(err -> System.out.println("  - " + err));
            } catch (EmployeeNotFoundException | DuplicateEmailException e) {
                System.out.println(e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private void addEmployee() {
        Employee e = new Employee(
                prompt("First name"), prompt("Last name"), prompt("Email"),
                prompt("Phone (optional)"), prompt("Department " + EmployeeValidator.DEPARTMENTS),
                prompt("Designation"), readDecimal("Salary"), readDate("Joining date (yyyy-mm-dd)"));
        Employee saved = service.addEmployee(e);
        System.out.println("Added employee with id " + saved.getId());
    }

    private void updateEmployee() {
        Employee e = service.getEmployee(readLong("Employee id"));
        System.out.println("Press Enter to keep the current value.");
        e.setFirstName(promptDefault("First name", e.getFirstName()));
        e.setLastName(promptDefault("Last name", e.getLastName()));
        e.setEmail(promptDefault("Email", e.getEmail()));
        e.setPhone(promptDefault("Phone", e.getPhone()));
        e.setDepartment(promptDefault("Department", e.getDepartment()));
        e.setDesignation(promptDefault("Designation", e.getDesignation()));
        String salary = promptDefault("Salary", e.getSalary().toPlainString());
        try {
            e.setSalary(new BigDecimal(salary));
        } catch (NumberFormatException ex) {
            System.out.println("Invalid salary, keeping " + e.getSalary());
        }
        service.updateEmployee(e);
        System.out.println("Employee updated.");
    }

    private void deleteEmployee() {
        long id = readLong("Employee id");
        if (prompt("Permanently delete employee " + id + "? (yes/no)").equalsIgnoreCase("yes")) {
            service.deleteEmployee(id);
            System.out.println("Employee deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private void printEmployees(List<Employee> employees) {
        if (employees.isEmpty()) {
            System.out.println("No employees found.");
            return;
        }
        System.out.printf("%-4s %-22s %-28s %-12s %-22s %12s  %s%n",
                "ID", "Name", "Email", "Department", "Designation", "Salary", "Joined");
        employees.forEach(System.out::println);
    }

    private void printSummary(List<DepartmentSummary> summary) {
        System.out.printf("%-14s %5s %15s %17s%n", "Department", "Count", "Avg salary", "Total salary");
        summary.forEach(System.out::println);
    }

    // ---- input helpers ----------------------------------------------------------

    private String prompt(String label) {
        System.out.print(label + ": ");
        return in.hasNextLine() ? in.nextLine().trim() : "0";
    }

    private String promptDefault(String label, String current) {
        String value = prompt(label + " [" + (current == null ? "" : current) + "]");
        return value.isEmpty() ? current : value;
    }

    private long readLong(String label) {
        while (true) {
            try {
                return Long.parseLong(prompt(label));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private BigDecimal readDecimal(String label) {
        while (true) {
            try {
                return new BigDecimal(prompt(label));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private LocalDate readDate(String label) {
        while (true) {
            try {
                return LocalDate.parse(prompt(label));
            } catch (DateTimeParseException e) {
                System.out.println("Please use the format yyyy-mm-dd.");
            }
        }
    }
}
