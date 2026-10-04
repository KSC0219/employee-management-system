package com.saicharan.ems;

import com.saicharan.ems.model.Employee;
import com.saicharan.ems.service.EmployeeService;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Demo records loaded into the in-memory database. */
final class SampleData {

    private SampleData() {
    }

    static void load(EmployeeService service) {
        add(service, "Ravi", "Kumar", "ravi.kumar@acme.com", "9876543210", "Engineering", "Software Engineer", "650000", "2023-07-10");
        add(service, "Anitha", "Reddy", "anitha.reddy@acme.com", "9123456780", "Engineering", "Senior Engineer", "1200000", "2020-02-17");
        add(service, "Suresh", "Naidu", "suresh.naidu@acme.com", null, "Finance", "Accountant", "540000", "2022-11-01");
        add(service, "Priya", "Sharma", "priya.sharma@acme.com", "9988776655", "HR", "HR Executive", "480000", "2024-01-08");
        add(service, "Kiran", "Rao", "kiran.rao@acme.com", "9012345678", "Sales", "Sales Manager", "900000", "2019-05-20");
    }

    private static void add(EmployeeService s, String first, String last, String email, String phone,
                            String dept, String designation, String salary, String joined) {
        s.addEmployee(new Employee(first, last, email, phone, dept, designation,
                new BigDecimal(salary), LocalDate.parse(joined)));
    }
}
