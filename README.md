# Employee Management System

![CI](https://github.com/KSC0219/employee-management-system/actions/workflows/ci.yml/badge.svg)

A Java console application for managing employee records. It stores data in **MySQL through JDBC**, validates every input, and keeps the code layered with clean **object-oriented design**.

## Features

- **Create, read, update and delete employees** from a menu-driven console
- **Search** by name, and **filter** by department
- **Soft delete:** deactivate an employee without losing history, or delete permanently after a confirmation prompt
- **Department raise:** give a percentage salary raise to a whole department in a single **JDBC transaction**, with rollback on failure
- **Department summary:** head count, average salary and total salary per department, using SQL aggregation
- **Validation:** names, email format, 10-digit mobile number, allowed departments, salary range, and no future joining dates. All problems are reported at once
- **Duplicate check:** an email can't be registered twice, case-insensitively
- **Safe SQL:** every query uses `PreparedStatement` (no SQL injection) and try-with-resources (no leaked connections)

## Architecture

```
EmployeeManagementApp   console menu, input parsing, friendly error messages
        │
EmployeeService         business rules: validation, duplicate checks, raise limits
        │
EmployeeDao (interface) ◄── JdbcEmployeeDao   SQL and JDBC only
        │
ConnectionFactory       reads db.properties, overridden by environment variables
        │
MySQL  (or in-memory H2 in MySQL mode for tests and demo)
```

Each layer has one job, and the service depends on the `EmployeeDao` **interface**, not on JDBC. That makes the business logic easy to test, and the storage can be swapped without touching the rest of the code.

## Tech stack

Java 17 · JDBC · MySQL 8 · H2 (tests) · JUnit 5 · Maven · GitHub Actions

## Getting started

**Prerequisites:** JDK 17+ and Maven.

### Quick demo (no database setup)

```bash
mvn compile exec:java
```

This starts with an in-memory database preloaded with 5 sample employees.

### With MySQL

```sql
CREATE DATABASE ems_db;
```

```bash
export DB_URL=jdbc:mysql://localhost:3306/ems_db
export DB_USER=root
export DB_PASSWORD=your_password
mvn compile exec:java
```

On Windows PowerShell, use `$env:DB_URL="..."` instead of `export`. The `employees` table is created automatically from `src/main/resources/schema.sql`.

### Run the tests

```bash
mvn test
```

14 tests cover the DAO (against a real SQL database, H2 in MySQL mode) and the service rules.

## Example session

```
===== Employee Management System =====
1. List employees           6. Deactivate employee
2. Add employee             7. Delete employee
3. Find employee by id      8. Department raise
4. Search by name           9. Department summary
5. Update employee          0. Exit
Choose an option: 9
Department     Count      Avg salary      Total salary
Engineering        2       925000.00        1850000.00
Finance            1       540000.00         540000.00
HR                 1       480000.00         480000.00
Sales              1       900000.00         900000.00
```

## Database schema

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Auto-increment primary key |
| first_name, last_name | VARCHAR(50) | Required |
| email | VARCHAR(120) | Required, unique |
| phone | VARCHAR(15) | Optional |
| department | VARCHAR(50) | One of the allowed departments |
| designation | VARCHAR(80) | Required |
| salary | DECIMAL(12,2) | 10,000 to 1,00,00,000 |
| joining_date | DATE | Not in the future |
| active | BOOLEAN | False when the employee is deactivated |
