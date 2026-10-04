package com.saicharan.ems.model;

import java.math.BigDecimal;

/** Aggregated head-count and salary figures for one department. */
public record DepartmentSummary(String department, int headCount, BigDecimal averageSalary,
                                BigDecimal totalSalary) {

    @Override
    public String toString() {
        return String.format("%-14s %5d %15s %17s", department, headCount, averageSalary, totalSalary);
    }
}
