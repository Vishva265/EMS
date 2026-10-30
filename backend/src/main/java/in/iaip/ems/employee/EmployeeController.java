package in.iaip.ems.employee;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EmployeeController {
  private final EmployeeService employees;
  private final EmployeeQueries queries;

  public EmployeeController(EmployeeService employees, EmployeeQueries queries) {
    this.employees = employees;
    this.queries = queries;
  }

  @GetMapping("/departments")
  public List<Map<String, Object>> departments() {
    return queries.departments();
  }

  @GetMapping("/roles")
  public List<Map<String, Object>> roles() {
    return queries.roles();
  }

  @GetMapping("/employees")
  public Map<String, Object> list(@Valid @ModelAttribute EmployeeQuery query) {
    return queries.list(query);
  }

  @GetMapping("/employees/me")
  public Map<String, Object> me() {
    return queries.detail(queries.myEmployeeId(), false);
  }

  @GetMapping("/employees/{id}")
  public Map<String, Object> detail(
      @PathVariable long id, @RequestParam(defaultValue = "false") boolean reveal) {
    return queries.detail(id, reveal);
  }

  @GetMapping("/employees/{id}/transfers")
  public List<Map<String, Object>> history(@PathVariable long id) {
    return queries.history(id);
  }

  @PostMapping("/employees")
  @PreAuthorize("hasRole('Admin')")
  public ResponseEntity<Map<String, Object>> create(
      @Valid @RequestBody EmployeeRequests.Profile form) {
    var employee = employees.create(form);
    return ResponseEntity.created(URI.create("/api/employees/" + employee.get("id")))
        .body(employee);
  }

  @PutMapping("/employees/{id}")
  @PreAuthorize("hasRole('Admin')")
  public Map<String, Object> update(
      @PathVariable long id, @Valid @RequestBody EmployeeRequests.Profile form) {
    return employees.update(id, form);
  }

  @PatchMapping("/employees/{id}/employment")
  @PreAuthorize("hasAnyRole('Admin','Manager')")
  public Map<String, Object> employment(
      @PathVariable long id, @Valid @RequestBody EmployeeRequests.Employment form) {
    return employees.updateEmployment(id, form);
  }

  @PatchMapping("/employees/{id}/status")
  @PreAuthorize("hasRole('Admin')")
  public ResponseEntity<Void> status(
      @PathVariable long id, @Valid @RequestBody EmployeeRequests.Status form) {
    employees.status(id, form);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/employees/{id}/transfers")
  @PreAuthorize("hasRole('Admin')")
  public Map<String, Object> transfer(
      @PathVariable long id, @Valid @RequestBody EmployeeRequests.Transfer form) {
    return employees.transfer(id, form);
  }
}
