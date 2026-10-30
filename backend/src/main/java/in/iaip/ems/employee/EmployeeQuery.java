package in.iaip.ems.employee;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeQuery(
    @Size(max = 100) String search,
    @Positive Long departmentId,
    @Positive Long roleId,
    @Pattern(regexp = "Active|Inactive") String status,
    @Pattern(regexp = "Male|Female|Other") String gender,
    LocalDate joiningDateFrom,
    LocalDate joiningDateTo,
    @DecimalMin("0") BigDecimal minSalary,
    @DecimalMin("0") BigDecimal maxSalary,
    Boolean salaryMaxExclusive,
    String sortBy,
    String sortDirection,
    @Min(1) Integer pageNumber,
    Integer pageSize) {}
