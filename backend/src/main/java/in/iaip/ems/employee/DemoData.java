package in.iaip.ems.employee;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Opt-in synthetic demo data. Password is provided outside source code. */
@Component
public class DemoData implements CommandLineRunner {
  private final JdbcTemplate db;
  private final EmployeeService employees;
  private final PasswordEncoder passwords;
  private final String password;
  private final boolean seedDemo;

  public DemoData(
      JdbcTemplate db,
      EmployeeService employees,
      PasswordEncoder passwords,
      @Value("${app.demo-password}") String password,
      @Value("${app.seed-demo:true}") boolean seedDemo) {
    this.db = db;
    this.employees = employees;
    this.passwords = passwords;
    this.password = password;
    this.seedDemo = seedDemo;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (password.isBlank() || db.queryForObject("SELECT count(*) FROM users", Integer.class) > 0)
      return;
    if (password.length() < 12)
      throw new IllegalStateException("DEMO_PASSWORD must have at least 12 characters");
    String hash = passwords.encode(password);
    db.update(
        "INSERT INTO users(username,password_hash,access_role) VALUES ('admin',?,'Admin')", hash);
    if (!seedDemo) return;
    db.update(
        "INSERT INTO users(username,password_hash,access_role) VALUES ('manager',?,'Manager')",
        hash);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_Admin"))));
    String[] names = {
      "Aarav Sharma",
      "Ananya Patel",
      "Vikram Singh",
      "Meera Nair",
      "Arjun Rao",
      "Kavya Menon",
      "Rohan Gupta",
      "Ishita Das",
      "Aditya Verma",
      "Priya Iyer",
      "Karan Mehta",
      "Neha Joshi",
      "Rahul Kumar",
      "Sneha Reddy",
      "Dev Malhotra",
      "Pooja Shah",
      "Nikhil Jain",
      "Tara Kapoor",
      "Aman Sethi",
      "Riya Bhat",
      "Varun Khanna",
      "Diya Bose",
      "Kabir Roy",
      "Sana Ali"
    };
    long first = 0;
    try {
      for (int i = 0; i < names.length; i++) {
        var form =
            new EmployeeRequests.Profile(
                names[i],
                names[i].toLowerCase().replace(' ', '.') + "@example.test",
                "9" + String.format("%09d", i + 100),
                "99990000" + String.format("%04d", i + 1),
                "ZZZZZ" + String.format("%04d", i + 1) + "Z",
                LocalDate.of(1990 + i % 10, 1 + i % 12, 10),
                i % 2 == 0 ? "Male" : "Female",
                "Synthetic address " + (i + 1) + ", Pune, Maharashtra",
                (long) (i % 5 + 1),
                (long) (i % 4 + 1),
                LocalDate.of(2021 + i % 4, 1 + i % 12, 12),
                new BigDecimal(new int[] {250000, 300000, 480000, 650000, 900000, 1200000}[i % 6]),
                i == 18 || i == 22 ? "Inactive" : "Active",
                null);
        long id = ((Number) employees.create(form).get("id")).longValue();
        if (i == 0) first = id;
      }
      db.update(
          "INSERT INTO users(username,password_hash,access_role,employee_id) VALUES"
              + " ('employee',?,'Employee',?)",
          hash,
          first);
      employees.transfer(
          first,
          new EmployeeRequests.Transfer(
              2L, LocalDate.of(2025, 1, 15), "Synthetic sample: administrative reassignment", 0L));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
