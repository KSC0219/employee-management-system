package com.saicharan.ems.util;

import com.saicharan.ems.exception.ValidationException;
import com.saicharan.ems.model.Employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Business validation rules applied before any employee is written to the database. */
public final class EmployeeValidator {

    public static final List<String> DEPARTMENTS =
            List.of("Engineering", "HR", "Finance", "Sales", "Marketing", "Operations", "Support");

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^[6-9]\\d{9}$"); // 10-digit Indian mobile
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{0,49}$");
    private static final BigDecimal MIN_SALARY = new BigDecimal("10000");
    private static final BigDecimal MAX_SALARY = new BigDecimal("10000000");

    private EmployeeValidator() {
    }

    public static void validate(Employee e) {
        List<String> errors = new ArrayList<>();

        if (isBlank(e.getFirstName()) || !NAME.matcher(e.getFirstName().trim()).matches()) {
            errors.add("first name must contain letters only (max 50 characters)");
        }
        if (isBlank(e.getLastName()) || !NAME.matcher(e.getLastName().trim()).matches()) {
            errors.add("last name must contain letters only (max 50 characters)");
        }
        if (isBlank(e.getEmail()) || !EMAIL.matcher(e.getEmail().trim()).matches()) {
            errors.add("email is not valid");
        }
        if (!isBlank(e.getPhone()) && !PHONE.matcher(e.getPhone().trim()).matches()) {
            errors.add("phone must be a 10-digit mobile number");
        }
        if (isBlank(e.getDepartment()) || DEPARTMENTS.stream().noneMatch(d -> d.equalsIgnoreCase(e.getDepartment().trim()))) {
            errors.add("department must be one of " + DEPARTMENTS);
        }
        if (isBlank(e.getDesignation())) {
            errors.add("designation is required");
        }
        if (e.getSalary() == null || e.getSalary().compareTo(MIN_SALARY) < 0 || e.getSalary().compareTo(MAX_SALARY) > 0) {
            errors.add("salary must be between " + MIN_SALARY + " and " + MAX_SALARY);
        }
        if (e.getJoiningDate() == null || e.getJoiningDate().isAfter(LocalDate.now())) {
            errors.add("joining date is required and cannot be in the future");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /** Trims text fields and normalises department casing, e.g. "engineering" becomes "Engineering". */
    public static void normalise(Employee e) {
        if (e.getFirstName() != null) e.setFirstName(e.getFirstName().trim());
        if (e.getLastName() != null) e.setLastName(e.getLastName().trim());
        if (e.getEmail() != null) e.setEmail(e.getEmail().trim().toLowerCase());
        if (e.getPhone() != null) e.setPhone(e.getPhone().isBlank() ? null : e.getPhone().trim());
        if (e.getDesignation() != null) e.setDesignation(e.getDesignation().trim());
        if (e.getDepartment() != null) {
            String dept = e.getDepartment().trim();
            DEPARTMENTS.stream().filter(d -> d.equalsIgnoreCase(dept)).findFirst().ifPresent(e::setDepartment);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
