package in.iaip.ems.security;

import in.iaip.ems.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserAccess {
  public record CurrentUser(long id, String username, String role, Long employeeId) {}

  private final JdbcTemplate db;

  public UserAccess(JdbcTemplate db) {
    this.db = db;
  }

  public CurrentUser current() {
    String name = SecurityContextHolder.getContext().getAuthentication().getName();
    return db.queryForObject(
        "SELECT id,username,access_role,employee_id FROM users WHERE username=?",
        (r, n) ->
            new CurrentUser(
                r.getLong("id"),
                r.getString("username"),
                r.getString("access_role"),
                r.getObject("employee_id", Long.class)),
        name);
  }

  public void checkEmployee(long id) {
    var user = current();
    if (user.role().equals("Employee") && !Long.valueOf(id).equals(user.employeeId()))
      throw new ApiException(HttpStatus.FORBIDDEN, "You may only view your own employee record.");
  }
}
