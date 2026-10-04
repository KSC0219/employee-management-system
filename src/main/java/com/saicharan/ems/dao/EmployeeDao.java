package com.saicharan.ems.dao;

import com.saicharan.ems.model.DepartmentSummary;
import com.saicharan.ems.model.Employee;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Data access contract for employees. Implementations hide the JDBC details. */
public interface EmployeeDao {

    Employee save(Employee employee);

    Optional<Employee> findById(long id);

    Optional<Employee> findByEmail(String email);

    List<Employee> findAll(boolean includeInactive);

    List<Employee> findByDepartment(String department);

    List<Employee> searchByName(String text);

    boolean update(Employee employee);

    boolean deactivate(long id);

    boolean delete(long id);

    int increaseSalaryForDepartment(String department, BigDecimal percent);

    List<DepartmentSummary> departmentSummary();
}
