package com.saicharan.ems;

import com.saicharan.ems.dao.JdbcEmployeeDao;
import com.saicharan.ems.model.DepartmentSummary;
import com.saicharan.ems.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcEmployeeDaoTest {

    private JdbcEmployeeDao dao;

    @BeforeEach
    void setUp() {
        dao = new JdbcEmployeeDao(TestDatabase.fresh());
    }

    private Employee employee(String first, String email, String dept, String salary) {
        return new Employee(first, "Test", email, null, dept, "Engineer",
                new BigDecimal(salary), LocalDate.of(2023, 1, 15));
    }

    @Test
    void saveAssignsIdAndCanBeFoundAgain() {
        Employee saved = dao.save(employee("Ravi", "ravi@acme.com", "Engineering", "500000"));
        assertNotNull(saved.getId());

        Employee found = dao.findById(saved.getId()).orElseThrow();
        assertEquals("ravi@acme.com", found.getEmail());
        assertEquals(0, new BigDecimal("500000").compareTo(found.getSalary()));
        assertEquals(LocalDate.of(2023, 1, 15), found.getJoiningDate());
    }

    @Test
    void findByEmailIsCaseInsensitive() {
        dao.save(employee("Ravi", "ravi@acme.com", "Engineering", "500000"));
        assertTrue(dao.findByEmail("RAVI@ACME.COM").isPresent());
    }

    @Test
    void updateChangesStoredValues() {
        Employee e = dao.save(employee("Ravi", "ravi@acme.com", "Engineering", "500000"));
        e.setDesignation("Senior Engineer");
        assertTrue(dao.update(e));
        assertEquals("Senior Engineer", dao.findById(e.getId()).orElseThrow().getDesignation());
    }

    @Test
    void deactivatedEmployeesAreHiddenFromDefaultList() {
        Employee a = dao.save(employee("Ravi", "ravi@acme.com", "Engineering", "500000"));
        dao.save(employee("Anu", "anu@acme.com", "HR", "400000"));
        assertTrue(dao.deactivate(a.getId()));

        assertEquals(1, dao.findAll(false).size());
        assertEquals(2, dao.findAll(true).size());
    }

    @Test
    void deleteRemovesRow() {
        Employee e = dao.save(employee("Ravi", "ravi@acme.com", "Engineering", "500000"));
        assertTrue(dao.delete(e.getId()));
        assertTrue(dao.findById(e.getId()).isEmpty());
        assertFalse(dao.delete(e.getId()));
    }

    @Test
    void searchByNameMatchesPartialText() {
        dao.save(employee("Ravi", "ravi@acme.com", "Engineering", "500000"));
        dao.save(employee("Ravindra", "ravindra@acme.com", "Sales", "450000"));
        dao.save(employee("Anu", "anu@acme.com", "HR", "400000"));
        assertEquals(2, dao.searchByName("rav").size());
    }

    @Test
    void departmentRaiseUpdatesOnlyThatDepartment() {
        Employee eng = dao.save(employee("Ravi", "ravi@acme.com", "Engineering", "500000"));
        Employee hr = dao.save(employee("Anu", "anu@acme.com", "HR", "400000"));

        assertEquals(1, dao.increaseSalaryForDepartment("engineering", new BigDecimal("10")));
        assertEquals(0, new BigDecimal("550000.00").compareTo(dao.findById(eng.getId()).orElseThrow().getSalary()));
        assertEquals(0, new BigDecimal("400000").compareTo(dao.findById(hr.getId()).orElseThrow().getSalary()));
    }

    @Test
    void departmentSummaryAggregatesActiveEmployees() {
        dao.save(employee("Ravi", "ravi@acme.com", "Engineering", "500000"));
        dao.save(employee("Kiran", "kiran@acme.com", "Engineering", "700000"));
        dao.save(employee("Anu", "anu@acme.com", "HR", "400000"));

        List<DepartmentSummary> summary = dao.departmentSummary();
        DepartmentSummary eng = summary.get(0);
        assertEquals("Engineering", eng.department());
        assertEquals(2, eng.headCount());
        assertEquals(0, new BigDecimal("600000.00").compareTo(eng.averageSalary()));
        assertEquals(0, new BigDecimal("1200000.00").compareTo(eng.totalSalary()));
    }
}
