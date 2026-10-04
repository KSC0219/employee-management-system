package com.saicharan.ems.service;

import com.saicharan.ems.dao.EmployeeDao;
import com.saicharan.ems.exception.DuplicateEmailException;
import com.saicharan.ems.exception.EmployeeNotFoundException;
import com.saicharan.ems.exception.ValidationException;
import com.saicharan.ems.model.DepartmentSummary;
import com.saicharan.ems.model.Employee;
import com.saicharan.ems.util.EmployeeValidator;

import java.math.BigDecimal;
import java.util.List;

/** Business logic layer: validation, duplicate checks and rules sit here, not in the DAO or the UI. */
public class EmployeeService {

    private final EmployeeDao dao;

    public EmployeeService(EmployeeDao dao) {
        this.dao = dao;
    }

    public Employee addEmployee(Employee employee) {
        EmployeeValidator.normalise(employee);
        EmployeeValidator.validate(employee);
        dao.findByEmail(employee.getEmail()).ifPresent(existing -> {
            throw new DuplicateEmailException(employee.getEmail());
        });
        return dao.save(employee);
    }

    public Employee getEmployee(long id) {
        return dao.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    public List<Employee> listEmployees(boolean includeInactive) {
        return dao.findAll(includeInactive);
    }

    public List<Employee> employeesInDepartment(String department) {
        return dao.findByDepartment(department);
    }

    public List<Employee> searchByName(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return dao.searchByName(text.trim());
    }

    public Employee updateEmployee(Employee employee) {
        getEmployee(employee.getId()); // throws if missing
        EmployeeValidator.normalise(employee);
        EmployeeValidator.validate(employee);
        dao.findByEmail(employee.getEmail())
                .filter(other -> !other.getId().equals(employee.getId()))
                .ifPresent(other -> {
                    throw new DuplicateEmailException(employee.getEmail());
                });
        dao.update(employee);
        return employee;
    }

    /** Soft delete: the record is kept for history but hidden from normal lists. */
    public void deactivateEmployee(long id) {
        if (!dao.deactivate(id)) {
            throw new EmployeeNotFoundException(id);
        }
    }

    public void deleteEmployee(long id) {
        if (!dao.delete(id)) {
            throw new EmployeeNotFoundException(id);
        }
    }

    public int giveDepartmentRaise(String department, BigDecimal percent) {
        if (percent == null || percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new ValidationException(List.of("raise percent must be greater than 0 and at most 50"));
        }
        return dao.increaseSalaryForDepartment(department, percent);
    }

    public List<DepartmentSummary> departmentSummary() {
        return dao.departmentSummary();
    }
}
