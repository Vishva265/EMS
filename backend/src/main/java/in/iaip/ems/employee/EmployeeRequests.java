package in.iaip.ems.employee;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request-only records make the accepted fields explicit and prevent overposting. */
public final class EmployeeRequests {
  private EmployeeRequests() {}

  public record Profile(
      @NotBlank
          @Size(max = 100)
          @Pattern(regexp = "[A-Za-z ]+", message = "Use alphabets and spaces only")
          String fullName,
      @NotBlank @Email @Size(max = 160) String email,
      @NotNull @Pattern(regexp = "[0-9]{10}", message = "Enter exactly 10 digits") String mobile,
      @NotNull @Pattern(regexp = "[0-9]{12}", message = "Enter exactly 12 digits") String aadhaar,
      @NotNull @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]", message = "Use format ABCDE1234F")
          String pan,
      @NotNull @PastOrPresent LocalDate dateOfBirth,
      @NotNull @Pattern(regexp = "Male|Female|Other") String gender,
      @NotBlank @Size(max = 500) String address,
      @NotNull @Positive Long departmentId,
      @NotNull @Positive Long roleId,
      @NotNull LocalDate joiningDate,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 12, fraction = 2)
          BigDecimal annualSalary,
      @NotNull @Pattern(regexp = "Active|Inactive") String status,
      @PositiveOrZero Long version) {}

  public record Employment(
      @NotNull @Positive Long roleId,
      @NotNull LocalDate joiningDate,
      @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 12, fraction = 2)
          BigDecimal annualSalary,
      @NotNull @PositiveOrZero Long version) {}

  public record Status(
      @NotNull @Pattern(regexp = "Active|Inactive") String status,
      @NotNull @PositiveOrZero Long version) {}

  public record Transfer(
      @NotNull @Positive Long newDepartmentId,
      @NotNull @PastOrPresent LocalDate transferDate,
      @NotBlank @Size(max = 500) String reason,
      @NotNull @PositiveOrZero Long version) {}
}
