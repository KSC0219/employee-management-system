package com.saicharan.ems.dao;

import com.saicharan.ems.db.ConnectionFactory;
import com.saicharan.ems.exception.DataAccessException;
import com.saicharan.ems.model.DepartmentSummary;
import com.saicharan.ems.model.Employee;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link EmployeeDao}.
 * Every query uses PreparedStatement (prevents SQL injection) and try-with-resources
 * (connections and statements are always closed).
 */
public class JdbcEmployeeDao implements EmployeeDao {

    private static final String COLUMNS =
            "id, first_name, last_name, email, phone, department, designation, salary, joining_date, active";

    private final ConnectionFactory connectionFactory;

    public JdbcEmployeeDao(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Employee save(Employee e) {
        String sql = "INSERT INTO employees (first_name, last_name, email, phone, department, designation, "
                + "salary, joining_date, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEmployee(ps, e);
            ps.setBoolean(9, e.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setId(keys.getLong(1));
                }
            }
            return e;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to save employee " + e.getEmail(), ex);
        }
    }

    @Override
    public Optional<Employee> findById(long id) {
        List<Employee> rows = query("SELECT " + COLUMNS + " FROM employees WHERE id = ?", id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        List<Employee> rows = query("SELECT " + COLUMNS + " FROM employees WHERE LOWER(email) = LOWER(?)", email);
        return rows.stream().findFirst();
    }

    @Override
    public List<Employee> findAll(boolean includeInactive) {
        String sql = "SELECT " + COLUMNS + " FROM employees"
                + (includeInactive ? "" : " WHERE active = TRUE") + " ORDER BY id";
        return query(sql);
    }

    @Override
    public List<Employee> findByDepartment(String department) {
        return query("SELECT " + COLUMNS + " FROM employees WHERE LOWER(department) = LOWER(?) AND active = TRUE "
                + "ORDER BY last_name, first_name", department);
    }

    @Override
    public List<Employee> searchByName(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return query("SELECT " + COLUMNS + " FROM employees WHERE LOWER(first_name) LIKE ? "
                + "OR LOWER(last_name) LIKE ? ORDER BY first_name", pattern, pattern);
    }

    @Override
    public boolean update(Employee e) {
        String sql = "UPDATE employees SET first_name = ?, last_name = ?, email = ?, phone = ?, department = ?, "
                + "designation = ?, salary = ?, joining_date = ?, active = ? WHERE id = ?";
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindEmployee(ps, e);
            ps.setBoolean(9, e.isActive());
            ps.setLong(10, e.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to update employee " + e.getId(), ex);
        }
    }

    @Override
    public boolean deactivate(long id) {
        return executeUpdate("UPDATE employees SET active = FALSE WHERE id = ?", id) == 1;
    }

    @Override
    public boolean delete(long id) {
        return executeUpdate("DELETE FROM employees WHERE id = ?", id) == 1;
    }

    /**
     * Gives every active employee in a department the same percentage raise,
     * inside one transaction: either all rows are updated or none are.
     */
    @Override
    public int increaseSalaryForDepartment(String department, BigDecimal percent) {
        BigDecimal factor = BigDecimal.ONE.add(percent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        String sql = "UPDATE employees SET salary = ROUND(salary * ?, 2) "
                + "WHERE LOWER(department) = LOWER(?) AND active = TRUE";
        try (Connection conn = connectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, factor);
                ps.setString(2, department);
                int updated = ps.executeUpdate();
                conn.commit();
                return updated;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to apply salary increase for " + department, ex);
        }
    }

    @Override
    public List<DepartmentSummary> departmentSummary() {
        String sql = "SELECT department, COUNT(*) AS head_count, AVG(salary) AS avg_salary, SUM(salary) AS total_salary "
                + "FROM employees WHERE active = TRUE GROUP BY department ORDER BY head_count DESC, department";
        List<DepartmentSummary> result = new ArrayList<>();
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new DepartmentSummary(
                        rs.getString("department"),
                        rs.getInt("head_count"),
                        rs.getBigDecimal("avg_salary").setScale(2, RoundingMode.HALF_UP),
                        rs.getBigDecimal("total_salary").setScale(2, RoundingMode.HALF_UP)));
            }
            return result;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to build department summary", ex);
        }
    }

    // ---- helpers --------------------------------------------------------------

    private void bindEmployee(PreparedStatement ps, Employee e) throws SQLException {
        ps.setString(1, e.getFirstName());
        ps.setString(2, e.getLastName());
        ps.setString(3, e.getEmail());
        ps.setString(4, e.getPhone());
        ps.setString(5, e.getDepartment());
        ps.setString(6, e.getDesignation());
        ps.setBigDecimal(7, e.getSalary());
        ps.setDate(8, Date.valueOf(e.getJoiningDate()));
    }

    private List<Employee> query(String sql, Object... params) {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<Employee> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException ex) {
            throw new DataAccessException("Query failed: " + sql, ex);
        }
    }

    private int executeUpdate(String sql, Object... params) {
        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Update failed: " + sql, ex);
        }
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setId(rs.getLong("id"));
        e.setFirstName(rs.getString("first_name"));
        e.setLastName(rs.getString("last_name"));
        e.setEmail(rs.getString("email"));
        e.setPhone(rs.getString("phone"));
        e.setDepartment(rs.getString("department"));
        e.setDesignation(rs.getString("designation"));
        e.setSalary(rs.getBigDecimal("salary"));
        e.setJoiningDate(rs.getDate("joining_date").toLocalDate());
        e.setActive(rs.getBoolean("active"));
        return e;
    }
}
