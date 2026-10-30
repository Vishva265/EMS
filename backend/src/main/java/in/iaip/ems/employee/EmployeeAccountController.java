package in.iaip.ems.employee;

import in.iaip.ems.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Admin provisions an Employee account only; this endpoint cannot grant privileged roles. */
@RestController
@RequestMapping("/api/employees/{id}/account")
@PreAuthorize("hasRole('Admin')")
public class EmployeeAccountController {
  private final JdbcTemplate db;
  private final PasswordEncoder passwords;

  public EmployeeAccountController(JdbcTemplate db, PasswordEncoder passwords) {
    this.db = db;
    this.passwords = passwords;
  }

  public record NewAccount(
      @NotBlank
          @Pattern(
              regexp = "[A-Za-z0-9][A-Za-z0-9._-]{2,49}",
              message = "Use 3–50 letters, digits, dots, underscores or hyphens")
          String username,
      @NotBlank @Size(min = 12, max = 64, message = "Use 12–64 characters") String password) {}

  @GetMapping
  public Map<String, Object> account(@PathVariable long id) {
    if (db.queryForObject("SELECT count(*) FROM employees WHERE id=?", Integer.class, id) == 0)
      throw new ApiException(HttpStatus.NOT_FOUND, "Employee not found.");
    var rows = db.queryForList("SELECT username,is_active FROM users WHERE employee_id=?", id);
    if (rows.isEmpty()) return Map.of("exists", false);
    return Map.of(
        "exists",
        true,
        "username",
        rows.get(0).get("username"),
        "enabled",
        rows.get(0).get("is_active"));
  }

  @PostMapping
  @Transactional
  public ResponseEntity<Map<String, Object>> create(
      @PathVariable long id, @Valid @RequestBody NewAccount form) {
    var employees = db.queryForList("SELECT status FROM employees WHERE id=? FOR UPDATE", id);
    if (employees.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "Employee not found.");
    if (!employees.get(0).get("status").equals("Active"))
      throw ApiException.badRequest("Activate this employee before creating a login.");
    String username = form.username().toLowerCase(Locale.ROOT);
    if (db.queryForObject("SELECT count(*) FROM users WHERE employee_id=?", Integer.class, id) > 0)
      throw ApiException.conflict("This employee already has a login account.");
    if (db.queryForObject("SELECT count(*) FROM users WHERE username=?", Integer.class, username)
        > 0) throw ApiException.conflict("That username is already in use. Choose another.");
    if (form.password().getBytes(StandardCharsets.UTF_8).length > 72)
      throw ApiException.badRequest("Password must fit within 72 UTF-8 bytes.");
    db.update(
        "INSERT INTO users(username,password_hash,access_role,employee_id) VALUES"
            + " (?,?,'Employee',?)",
        username,
        passwords.encode(form.password()),
        id);
    // Plaintext passwords are never persisted, logged, returned, or recoverable.
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("exists", true, "username", username, "enabled", true));
  }
}
