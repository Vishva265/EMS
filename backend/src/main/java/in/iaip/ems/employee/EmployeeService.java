package in.iaip.ems.employee;

import in.iaip.ems.common.ApiException;
import in.iaip.ems.security.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {
  private final JdbcTemplate db;
  private final EmployeeQueries queries;
  private final IdentityVault vault;
  private final UserAccess access;

  public EmployeeService(
      JdbcTemplate db, IdentityVault vault, UserAccess access, EmployeeQueries queries) {
    this.db = db;
    this.queries = queries;
    this.vault = vault;
    this.access = access;
  }

  private void references(long department, long role) {
    if (db.queryForObject("SELECT count(*) FROM departments WHERE id=?", Integer.class, department)
            == 0
        || db.queryForObject("SELECT count(*) FROM roles WHERE id=?", Integer.class, role) == 0)
      throw ApiException.badRequest("Choose an existing department and designation.");
  }

  private void dates(LocalDate birth, LocalDate joining) {
    if (joining.isBefore(birth))
      throw ApiException.badRequest("Joining date cannot precede date of birth.");
  }

  private String nextCode(long department) {
    Long number =
        db.queryForObject(
            "UPDATE employee_code_sequences SET last_number=last_number+1 WHERE department_id=?"
                + " RETURNING last_number",
            Long.class,
            department);
    String prefix =
        db.queryForObject("SELECT prefix FROM departments WHERE id=?", String.class, department);
    return prefix + String.format(Locale.ROOT, "%04d", number);
  }

  @Transactional
  public Map<String, Object> create(EmployeeRequests.Profile f) {
    references(f.departmentId(), f.roleId());
    dates(f.dateOfBirth(), f.joiningDate());
    String email = f.email().trim();
    Long id =
        db.queryForObject(
            "INSERT INTO"
                + " employees(employee_code,full_name,email,normalized_email,mobile,department_id,role_id,joining_date,annual_salary,status)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id",
            Long.class,
            nextCode(f.departmentId()),
            f.fullName().trim(),
            email,
            email.toLowerCase(Locale.ROOT),
            f.mobile(),
            f.departmentId(),
            f.roleId(),
            f.joiningDate(),
            f.annualSalary(),
            f.status());
    db.update(
        "INSERT INTO employee_personal_details VALUES (?,?,?,?,?,?)",  
        id,
        vault.encrypt(f.aadhaar()),
        vault.encrypt(f.pan()),
        f.dateOfBirth(),
        f.gender(),
        f.address().trim());
    return queries.detail(id, false);
  }

  @Transactional
  public Map<String, Object> update(long id, EmployeeRequests.Profile f) {
    var old = lock(id);
    requireVersion(old, f.version());
    references(f.departmentId(), f.roleId());
    dates(f.dateOfBirth(), f.joiningDate());
    if (((Number) old.get("department_id")).longValue() != f.departmentId())
      throw ApiException.badRequest("Use department transfer to change departments.");
    if (!old.get("status").equals(f.status()))
      throw ApiException.badRequest("Use the status action to activate or deactivate employees.");
    validateJoiningHistory(id, f.joiningDate());
    String email = f.email().trim();
    db.update(
        "UPDATE employees SET"
            + " full_name=?,email=?,normalized_email=?,mobile=?,role_id=?,joining_date=?,annual_salary=?,version=version+1,updated_at=now()"
            + " WHERE id=?",
        f.fullName().trim(),
        email,
        email.toLowerCase(Locale.ROOT),
        f.mobile(),
        f.roleId(),
        f.joiningDate(),
        f.annualSalary(),
        id);
    db.update(
        "UPDATE employee_personal_details SET"
            + " aadhaar_ciphertext=?,pan_ciphertext=?,date_of_birth=?,gender=?,address=? WHERE"
            + " employee_id=?",
        vault.encrypt(f.aadhaar()),
        vault.encrypt(f.pan()),
        f.dateOfBirth(),
        f.gender(),
        f.address().trim(),
        id);
    return queries.detail(id, false);
  }

  @Transactional
  public Map<String, Object> updateEmployment(long id, EmployeeRequests.Employment f) {
    var old = lock(id);
    requireVersion(old, f.version());
    references(((Number) old.get("department_id")).longValue(), f.roleId());
    LocalDate birth =
        db.queryForObject(
            "SELECT date_of_birth FROM employee_personal_details WHERE employee_id=?",
            LocalDate.class,
            id);
    dates(birth, f.joiningDate());
    validateJoiningHistory(id, f.joiningDate());
    db.update(
        "UPDATE employees SET"
            + " role_id=?,joining_date=?,annual_salary=?,version=version+1,updated_at=now() WHERE"
            + " id=?",
        f.roleId(),
        f.joiningDate(),
        f.annualSalary(),
        id);
    return queries.detail(id, false);
  }

  private void validateJoiningHistory(long id, LocalDate joining) {
    LocalDate earliest =
        db.queryForObject(
            "SELECT min(transfer_date) FROM employee_transfer_history WHERE employee_id=?",
            LocalDate.class,
            id);
    if (earliest != null && joining.isAfter(earliest))
      throw ApiException.badRequest("Joining date must not follow an existing transfer.");
  }

  @Transactional
  public void status(long id, EmployeeRequests.Status f) {
    requireVersion(lock(id), f.version());
    if (Long.valueOf(id).equals(access.current().employeeId()) && f.status().equals("Inactive"))
      throw ApiException.badRequest("You cannot deactivate your own linked account.");
    db.update(
        "UPDATE employees SET status=?,version=version+1,updated_at=now() WHERE id=?",
        f.status(),
        id);
    // JWT validation checks the linked employee status on every subsequent request.
  }

  @Transactional
  public Map<String, Object> transfer(long id, EmployeeRequests.Transfer f) {
    var old = lock(id);
    requireVersion(old, f.version());
    long department = ((Number) old.get("department_id")).longValue();
    if (department == f.newDepartmentId())
      throw ApiException.badRequest("Choose a different destination department.");
    references(f.newDepartmentId(), ((Number) old.get("role_id")).longValue());
    if (!old.get("status").equals("Active"))
      throw ApiException.badRequest("Only active employees may be transferred.");
    LocalDate joined = ((java.sql.Date) old.get("joining_date")).toLocalDate();
    LocalDate latest =
        db.queryForObject(
            "SELECT max(transfer_date) FROM employee_transfer_history WHERE employee_id=?",
            LocalDate.class,
            id);
    if (f.transferDate().isBefore(joined) || (latest != null && f.transferDate().isBefore(latest)))
      throw ApiException.badRequest("Transfer date cannot precede joining or the latest transfer.");
    String code = nextCode(f.newDepartmentId());
    db.update(
        "INSERT INTO"
            + " employee_transfer_history(employee_id,old_department_id,new_department_id,old_employee_code,new_employee_code,transfer_date,reason,performed_by_user_id)"
            + " VALUES (?,?,?,?,?,?,?,?)",
        id,
        department,
        f.newDepartmentId(),
        old.get("employee_code"),
        code,
        f.transferDate(),
        f.reason().trim(),
        access.current().id());
    db.update(
        "UPDATE employees SET department_id=?,employee_code=?,version=version+1,updated_at=now()"
            + " WHERE id=?",
        f.newDepartmentId(),
        code,
        id);
    return queries.detail(id, false);
  }

  private Map<String, Object> lock(long id) {
    var rows = db.queryForList("SELECT * FROM employees WHERE id=? FOR UPDATE", id);
    if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "Employee not found.");
    return rows.get(0);
  }

  private void requireVersion(Map<String, Object> old, Long version) {
    if (version == null || ((Number) old.get("version")).longValue() != version)
      throw ApiException.conflict("This record has changed. Reload it before saving.");
  }
}
