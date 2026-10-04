package com.saicharan.ems;

import com.saicharan.ems.dao.JdbcEmployeeDao;
import com.saicharan.ems.exception.DuplicateEmailException;
import com.saicharan.ems.exception.EmployeeNotFoundException;
import com.saicharan.ems.exception.ValidationException;
import com.saicharan.ems.model.Employee;
import com.saicharan.ems.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeServiceTest {

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(new JdbcEmployeeDao(TestDatabase.fresh()));
    }

    private Employee valid(String email) {
        return new Employee("Ravi", "Kumar", email, "9876543210", "engineering", "Software Engineer",
                new BigDecimal("650000"), LocalDate.of(2023, 7, 10));
    }

    @Test
    void addNormalisesAndSaves() {
        Employee saved = service.addEmployee(valid("  Ravi.Kumar@Acme.com "));
        assertNotNull(saved.getId());
        assertEquals("ravi.kumar@acme.com", saved.getEmail());
        assertEquals("Engineering", saved.getDepartment());
    }

    @Test
    void duplicateEmailIsRejected() {
        service.addEmployee(valid("ravi@acme.com"));
        assertThrows(DuplicateEmailException.class, () -> service.addEmployee(valid("RAVI@acme.com")));
    }

    @Test
    void invalidInputListsEveryProblem() {
        Employee bad = new Employee("R4vi", "", "not-an-email", "12345", "Space", "",
                new BigDecimal("100"), LocalDate.now().plusDays(5));
        ValidationException ex = assertThrows(ValidationException.class, () -> service.addEmployee(bad));
        assertEquals(8, ex.getErrors().size());
    }

    @Test
    void missingEmployeeThrowsNotFound() {
        assertThrows(EmployeeNotFoundException.class, () -> service.getEmployee(999));
        assertThrows(EmployeeNotFoundException.class, () -> service.deleteEmployee(999));
    }

    @Test
    void updateCannotStealAnotherEmployeesEmail() {
        service.addEmployee(valid("ravi@acme.com"));
        Employee second = service.addEmployee(valid("anu@acme.com"));
        second.setEmail("ravi@acme.com");
        assertThrows(DuplicateEmailException.class, () -> service.updateEmployee(second));
    }

    @Test
    void raisePercentIsBounded() {
        assertThrows(ValidationException.class, () -> service.giveDepartmentRaise("Engineering", BigDecimal.ZERO));
        assertThrows(ValidationException.class, () -> service.giveDepartmentRaise("Engineering", new BigDecimal("80")));
    }
}
