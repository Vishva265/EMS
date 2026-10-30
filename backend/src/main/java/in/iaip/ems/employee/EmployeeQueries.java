package in.iaip.ems.employee;

import in.iaip.ems.common.ApiException;
import in.iaip.ems.security.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmployeeQueries {
  private final JdbcTemplate db;
  private final NamedParameterJdbcTemplate named;
  private final IdentityVault vault;
  private final UserAccess access;
  private static final String JOINS =
      " FROM employees e JOIN departments d ON d.id=e.department_id JOIN roles r ON r.id=e.role_id"
          + " JOIN employee_personal_details p ON p.employee_id=e.id ";
  private static final String COLUMNS =
      "SELECT e.*,d.name AS department,r.name AS role,p.date_of_birth,p.gender,p.address";

  public EmployeeQueries(JdbcTemplate db, IdentityVault vault, UserAccess access) {
    this.db = db;
    this.named = new NamedParameterJdbcTemplate(db);
    this.vault = vault;
    this.access = access;
  }

  public List<Map<String, Object>> departments() {
    return db.queryForList("SELECT id,name,prefix FROM departments ORDER BY id");
  }

  public List<Map<String, Object>> roles() {
    return db.queryForList("SELECT id,name FROM roles ORDER BY id");
  }

  public Map<String, Object> list(EmployeeQuery q) {
    int page = q.pageNumber() == null ? 1 : q.pageNumber(),
        size = q.pageSize() == null ? 10 : q.pageSize();
    if (!List.of(5, 10, 20).contains(size))
      throw ApiException.badRequest("Page size must be 5, 10, or 20.");
    if (q.minSalary() != null
        && q.maxSalary() != null
        && q.minSalary().compareTo(q.maxSalary()) > 0)
      throw ApiException.badRequest("Minimum salary must not exceed maximum salary.");
    if (q.joiningDateFrom() != null
        && q.joiningDateTo() != null
        && q.joiningDateFrom().isAfter(q.joiningDateTo()))
      throw ApiException.badRequest("Invalid joining-date range.");
    var sorts =
        Map.of(
            "employeeCode",
            "e.employee_code",
            "fullName",
            "e.full_name",
            "department",
            "d.name",
            "role",
            "r.name",
            "email",
            "e.email",
            "mobile",
            "e.mobile",
            "annualSalary",
            "e.annual_salary",
            "status",
            "e.status",
            "joiningDate",
            "e.joining_date");
    String sort = q.sortBy() == null ? "employeeCode" : q.sortBy();
    String direction =
        q.sortDirection() == null ? "asc" : q.sortDirection().toLowerCase(Locale.ROOT);
    if (!sorts.containsKey(sort) || !List.of("asc", "desc").contains(direction))
      throw ApiException.badRequest("Invalid sorting option.");
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    var args = new HashMap<String, Object>();
    var user = access.current();
    if (user.role().equals("Employee")) {
      where.append(" AND e.id=:owner");
      args.put("owner", user.employeeId());
    }
    if (q.search() != null && !q.search().isBlank()) {
      where.append(
          " AND (concat_ws('"
              + " ',e.employee_code,e.full_name,e.email,e.mobile,d.name,r.name,p.address,e.status)"
              + " ILIKE :search ESCAPE '\\')");
      args.put(
          "search",
          "%"
              + q.search().trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
              + "%");
    }
    add(where, args, "e.department_id=:department", "department", q.departmentId());
    add(where, args, "e.role_id=:role", "role", q.roleId());
    add(where, args, "e.status=:status", "status", q.status());
    add(where, args, "p.gender=:gender", "gender", q.gender());
    add(where, args, "e.joining_date>=:joinedFrom", "joinedFrom", q.joiningDateFrom());
    add(where, args, "e.joining_date<=:joinedTo", "joinedTo", q.joiningDateTo());
    add(where, args, "e.annual_salary>=:minSalary", "minSalary", q.minSalary());
    add(
        where,
        args,
        "e.annual_salary"
            + (Boolean.TRUE.equals(q.salaryMaxExclusive()) ? "<" : "<=")
            + ":maxSalary",
        "maxSalary",
        q.maxSalary());
    long count = named.queryForObject("SELECT count(*)" + JOINS + where, args, Long.class);
    args.put("limit", size);
    args.put("offset", ((long) page - 1) * size);
    var items =
        named.query(
            COLUMNS
                + JOINS
                + where
                + " ORDER BY "
                + sorts.get(sort)
                + " "
                + direction
                + ",e.id ASC LIMIT :limit OFFSET :offset",
            args,
            this::summary);
    return Map.of(
        "items",
        items,
        "pageNumber",
        page,
        "pageSize",
        size,
        "totalCount",
        count,
        "totalPages",
        (count + size - 1) / size);
  }

  private void add(
      StringBuilder sql, Map<String, Object> args, String clause, String key, Object value) {
    if (value != null) {
      sql.append(" AND ").append(clause);
      args.put(key, value);
    }
  }

  private Map<String, Object> summary(ResultSet r, int row) throws SQLException {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", r.getLong("id"));
    m.put("employeeCode", r.getString("employee_code"));
    m.put("fullName", r.getString("full_name"));
    m.put("email", r.getString("email"));
    m.put("mobile", r.getString("mobile"));
    m.put("departmentId", r.getLong("department_id"));
    m.put("department", r.getString("department"));
    m.put("roleId", r.getLong("role_id"));
    m.put("role", r.getString("role"));
    m.put("annualSalary", r.getBigDecimal("annual_salary"));
    m.put("status", r.getString("status"));
    m.put("joiningDate", r.getObject("joining_date", LocalDate.class));
    m.put("version", r.getLong("version"));
    return m;
  }

  public Map<String, Object> detail(long id, boolean reveal) {
    access.checkEmployee(id);
    if (reveal && !access.current().role().equals("Admin"))
      throw new ApiException(HttpStatus.FORBIDDEN, "Only Admin may reveal identity information.");
    var rows =
        db.query(
            COLUMNS + ",p.aadhaar_ciphertext,p.pan_ciphertext" + JOINS + " WHERE e.id=?",
            (r, n) -> {
              var m = summary(r, n);
              m.put("dateOfBirth", r.getObject("date_of_birth", LocalDate.class));
              m.put("gender", r.getString("gender"));
              m.put("address", r.getString("address"));
              String aadhaar = vault.decrypt(r.getString("aadhaar_ciphertext")),
                  pan = vault.decrypt(r.getString("pan_ciphertext"));
              m.put("aadhaar", reveal ? aadhaar : "XXXX-XXXX-" + aadhaar.substring(8));
              m.put("pan", reveal ? pan : "XXXXX" + pan.substring(5, 9) + "X");
              return m;
            },
            id);
    if (rows.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "Employee not found.");
    return rows.get(0);
  }

  public long myEmployeeId() {
    Long id = access.current().employeeId();
    if (id == null)
      throw new ApiException(HttpStatus.NOT_FOUND, "This account has no linked employee profile.");
    return id;
  }

  public List<Map<String, Object>> history(long id) {
    detail(id, false);
    return db.query(
        "SELECT h.*,o.name old_department,n.name new_department,u.username actor FROM"
            + " employee_transfer_history h JOIN departments o ON o.id=h.old_department_id JOIN"
            + " departments n ON n.id=h.new_department_id JOIN users u ON"
            + " u.id=h.performed_by_user_id WHERE h.employee_id=? ORDER BY transfer_date DESC,h.id"
            + " DESC",
        (r, n) ->
            Map.<String, Object>of(
                "id",
                r.getLong("id"),
                "oldDepartment",
                r.getString("old_department"),
                "newDepartment",
                r.getString("new_department"),
                "oldEmployeeCode",
                r.getString("old_employee_code"),
                "newEmployeeCode",
                r.getString("new_employee_code"),
                "transferDate",
                r.getObject("transfer_date", LocalDate.class),
                "reason",
                r.getString("reason"),
                "performedBy",
                r.getString("actor")),
        id);
  }
}
