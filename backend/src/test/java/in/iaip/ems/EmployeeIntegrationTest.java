package in.iaip.ems;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Runs on a separate real PostgreSQL test database, never on the preview database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeIntegrationTest {
  @DynamicPropertySource
  static void requireTestDatabase(DynamicPropertyRegistry properties) {
    String url = System.getenv("DB_URL");
    if (url == null || !url.endsWith("/ems_test"))
      throw new IllegalStateException(
          "Tests require the dedicated ems_test database; run scripts/test.ps1");
  }

  @LocalServerPort int port;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate db;

  @Value("${app.demo-password}")
  String password;

  record Reply(int status, JsonNode body) {}

  class Browser {
    final HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    String csrf;

    Reply request(String method, String path, Object body, boolean protectedWrite)
        throws Exception {
      var builder =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
              .timeout(Duration.ofSeconds(30))
              .header("Content-Type", "application/json");
      if (protectedWrite) builder.header("X-XSRF-TOKEN", csrf);
      builder.method(
          method,
          body == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
      var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      return new Reply(
          response.statusCode(),
          response.body().isBlank() ? json.nullNode() : json.readTree(response.body()));
    }

    Reply get(String path) throws Exception {
      return request("GET", path, null, false);
    }

    Reply write(String method, String path, Object body) throws Exception {
      return request(method, path, body, true);
    }
  }

  Browser login(String name) throws Exception {
    var browser = new Browser();
    browser.csrf = browser.get("/auth/csrf").body.path("token").asText();
    assertEquals(
        200,
        browser.write("POST", "/auth/login", Map.of("username", name, "password", password))
            .status);
    return browser;
  }

  Map<String, Object> profile(String email, int salary) {
    var m = new LinkedHashMap<String, Object>();
    m.put("fullName", "Test Candidate");
    m.put("email", email);
    m.put("mobile", "9000012345");
    m.put("aadhaar", "999988887777");
    m.put("pan", "ZZZZZ1234Z");
    m.put("dateOfBirth", "1996-02-10");
    m.put("gender", "Other");
    m.put("address", "Synthetic Test Address");
    m.put("departmentId", 1);
    m.put("roleId", 1);
    m.put("joiningDate", "2022-01-01");
    m.put("annualSalary", salary);
    m.put("status", "Active");
    return m;
  }

  String unique() {
    return "test" + UUID.randomUUID().toString().replace("-", "");
  }

  JsonNode create(Browser browser) throws Exception {
    var reply = browser.write("POST", "/employees", profile(unique() + "@example.test", 500000));
    assertEquals(201, reply.status, reply.body.toString());
    return reply.body;
  }

  @Test
  void loginCsrfAndOwnershipAreEnforced() throws Exception {
    var anonymous = new Browser();
    assertEquals(401, anonymous.get("/employees").status);
    assertEquals(
        403,
        anonymous.request(
                "POST", "/auth/login", Map.of("username", "admin", "password", password), false)
            .status);
    var employee = login("employee");
    var listing = employee.get("/employees");
    assertEquals(200, listing.status);
    assertEquals(1, listing.body.path("totalCount").asInt());
    long own = listing.body.path("items").get(0).path("id").asLong();
    long other = db.queryForObject("SELECT min(id) FROM employees WHERE id<>?", Long.class, own);
    assertEquals(403, employee.get("/employees/" + other).status);
    assertEquals(403, employee.get("/employees/" + own + "?reveal=true").status);
    assertEquals(
        403,
        employee.write("POST", "/employees", profile(unique() + "@example.test", 500000)).status);
  }

  @Test
  void managerCanOnlyEditEmployment() throws Exception {
    var admin = login("admin");
    var e = create(admin);
    long id = e.path("id").asLong();
    var manager = login("manager");
    assertEquals(
        403,
        manager.write("POST", "/employees", profile(unique() + "@example.test", 500000)).status);
    assertEquals(
        403,
        manager.write(
                "PATCH", "/employees/" + id + "/status", Map.of("status", "Inactive", "version", 0))
            .status);
    assertEquals(
        403,
        manager.write(
                "POST",
                "/employees/" + id + "/transfers",
                Map.of(
                    "newDepartmentId",
                    2,
                    "transferDate",
                    "2025-01-01",
                    "reason",
                    "Test",
                    "version",
                    0))
            .status);
    assertEquals(
        200,
        manager.write(
                "PATCH",
                "/employees/" + id + "/employment",
                Map.of(
                    "roleId", 2, "joiningDate", "2022-01-01", "annualSalary", 600000, "version", 0))
            .status);
    assertEquals(
        400,
        manager.write(
                "PATCH",
                "/employees/" + id + "/employment",
                Map.of(
                    "roleId",
                    2,
                    "joiningDate",
                    "2022-01-01",
                    "annualSalary",
                    600000,
                    "version",
                    1,
                    "fullName",
                    "Overposted Name"))
            .status);
  }

  @Test
  void validationUniquenessAndEncryptedStorage() throws Exception {
    var admin = login("admin");
    String email = unique() + "@example.test";
    var f = profile(email, 500000);
    f.put("aadhaar", "123");
    assertEquals(400, admin.write("POST", "/employees", f).status);
    f.put("aadhaar", "999988887777");
    var created = admin.write("POST", "/employees", f);
    assertEquals(201, created.status);
    long id = created.body.path("id").asLong();
    f.put("email", email.toUpperCase());
    assertEquals(409, admin.write("POST", "/employees", f).status);
    assertFalse(created.body.path("aadhaar").asText().contains("9999"));
    String encrypted =
        db.queryForObject(
            "SELECT aadhaar_ciphertext FROM employee_personal_details WHERE employee_id=?",
            String.class,
            id);
    assertTrue(encrypted.startsWith("v1:"));
    assertFalse(encrypted.contains("999988887777"));
    assertEquals(
        "999988887777",
        admin.get("/employees/" + id + "?reveal=true").body.path("aadhaar").asText());
    assertFalse(admin.get("/employees?search=" + email).body.path("items").get(0).has("aadhaar"));
    assertEquals(0, admin.get("/employees?search=999988887777").body.path("totalCount").asInt());
  }

  @Test
  void transferPreservesIdentityAndRejectsStaleRequests() throws Exception {
    var admin = login("admin");
    var e = create(admin);
    long id = e.path("id").asLong();
    var body =
        Map.of(
            "newDepartmentId",
            2,
            "transferDate",
            "2025-01-01",
            "reason",
            "Synthetic transfer",
            "version",
            0);
    var moved = admin.write("POST", "/employees/" + id + "/transfers", body);
    assertEquals(200, moved.status, moved.body.toString());
    assertEquals(id, moved.body.path("id").asLong());
    assertTrue(moved.body.path("employeeCode").asText().startsWith("H"));
    assertNotEquals(e.path("employeeCode").asText(), moved.body.path("employeeCode").asText());
    assertEquals(409, admin.write("POST", "/employees/" + id + "/transfers", body).status);
    assertEquals(
        400,
        admin.write(
                "POST",
                "/employees/" + id + "/transfers",
                Map.of(
                    "newDepartmentId",
                    2,
                    "transferDate",
                    "2025-01-01",
                    "reason",
                    "Same department",
                    "version",
                    1))
            .status);
    var history = admin.get("/employees/" + id + "/transfers");
    assertEquals(200, history.status);
    assertEquals(1, history.body.size());
  }

  @Test
  void failedTransferRollsBackEmployeeHistoryAndCounter() throws Exception {
    var admin = login("admin");
    var e = create(admin);
    long id = e.path("id").asLong();
    long before =
        db.queryForObject(
            "SELECT last_number FROM employee_code_sequences WHERE department_id=2", Long.class);
    db.execute(
        "ALTER TABLE employee_transfer_history ADD CONSTRAINT test_reject_reason CHECK(reason <>"
            + " 'FORCE_ROLLBACK')");
    try {
      assertEquals(
          409,
          admin.write(
                  "POST",
                  "/employees/" + id + "/transfers",
                  Map.of(
                      "newDepartmentId",
                      2,
                      "transferDate",
                      "2025-01-01",
                      "reason",
                      "FORCE_ROLLBACK",
                      "version",
                      0))
              .status);
      assertEquals(
          before,
          db.queryForObject(
              "SELECT last_number FROM employee_code_sequences WHERE department_id=2", Long.class));
      assertEquals(
          e.path("employeeCode").asText(),
          admin.get("/employees/" + id).body.path("employeeCode").asText());
      var history = admin.get("/employees/" + id + "/transfers");
      assertEquals(200, history.status);
      assertEquals(0, history.body.size());
    } finally {
      db.execute("ALTER TABLE employee_transfer_history DROP CONSTRAINT test_reject_reason");
    }
  }

  @Test
  void concurrentCreatesNeverDuplicateCodes() throws Exception {
    var admin = login("admin");
    var pool = Executors.newFixedThreadPool(6);
    try {
      var tasks = new ArrayList<Callable<String>>();
      for (int i = 0; i < 12; i++) tasks.add(() -> create(admin).path("employeeCode").asText());
      var codes = new HashSet<String>();
      for (var future : pool.invokeAll(tasks)) codes.add(future.get());
      assertEquals(12, codes.size());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void salaryBoundariesCombinedFiltersAndPagination() throws Exception {
    var admin = login("admin");
    String tag = unique();
    for (int salary : new int[] {99999, 100000, 299999, 300000, 499999, 500000, 999999, 1000000}) {
      assertEquals(
          201,
          admin.write("POST", "/employees", profile(tag + salary + "@example.test", salary))
              .status);
    }
    String base =
        "/employees?search="
            + tag
            + "&departmentId=1&roleId=1&status=Active&gender=Other&joiningDateFrom=2022-01-01&joiningDateTo=2022-01-01";
    assertEquals(
        2,
        admin
            .get(base + "&minSalary=100000&maxSalary=300000&salaryMaxExclusive=true")
            .body
            .path("totalCount")
            .asInt());
    assertEquals(
        2,
        admin
            .get(base + "&minSalary=300000&maxSalary=500000&salaryMaxExclusive=true")
            .body
            .path("totalCount")
            .asInt());
    assertEquals(
        2,
        admin
            .get(base + "&minSalary=500000&maxSalary=1000000&salaryMaxExclusive=true")
            .body
            .path("totalCount")
            .asInt());
    assertEquals(1, admin.get(base + "&minSalary=1000000").body.path("totalCount").asInt());
    assertEquals(
        1, admin.get(base + "&minSalary=300000&maxSalary=300000").body.path("totalCount").asInt());
    var first = admin.get(base + "&pageSize=5&sortBy=annualSalary&sortDirection=desc").body;
    var second =
        admin.get(base + "&pageSize=5&pageNumber=2&sortBy=annualSalary&sortDirection=desc").body;
    assertEquals(8, first.path("totalCount").asInt());
    assertEquals(5, first.path("items").size());
    assertEquals(3, second.path("items").size());
    assertEquals(1000000, first.path("items").get(0).path("annualSalary").asInt());
    assertEquals(400, admin.get("/employees?pageSize=7").status);
    assertEquals(400, admin.get("/employees?minSalary=500&maxSalary=100").status);
  }

  @Test
  void deactivationBlocksAnExistingEmployeeSession() throws Exception {
    var admin = login("admin");
    var employee = login("employee");
    var own = employee.get("/employees/me").body;
    long id = own.path("id").asLong();
    long version = own.path("version").asLong();
    try {
      assertEquals(
          204,
          admin.write(
                  "PATCH",
                  "/employees/" + id + "/status",
                  Map.of("status", "Inactive", "version", version))
              .status);
      assertEquals(401, employee.get("/employees/me").status);
      assertEquals(401, loginAttempt("employee").status);
    } finally {
      long current = db.queryForObject("SELECT version FROM employees WHERE id=?", Long.class, id);
      assertEquals(
          204,
          admin.write(
                  "PATCH",
                  "/employees/" + id + "/status",
                  Map.of("status", "Active", "version", current))
              .status);
    }
  }

  private Reply loginAttempt(String username) throws Exception {
    var b = new Browser();
    b.csrf = b.get("/auth/csrf").body.path("token").asText();
    return b.write("POST", "/auth/login", Map.of("username", username, "password", password));
  }

  @Test
  void concurrentTransfersCannotOverwriteEachOther() throws Exception {
    var admin = login("admin");
    var e = create(admin);
    long id = e.path("id").asLong();
    var pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> task =
          () ->
              admin.write(
                      "POST",
                      "/employees/" + id + "/transfers",
                      Map.of(
                          "newDepartmentId",
                          2,
                          "transferDate",
                          "2025-01-01",
                          "reason",
                          "Concurrent transfer test",
                          "version",
                          0))
                  .status;
      var results = pool.invokeAll(List.of(task, task));
      var statuses = List.of(results.get(0).get(), results.get(1).get());
      assertTrue(statuses.contains(200));
      assertTrue(statuses.contains(409));
      assertEquals(1, admin.get("/employees/" + id + "/transfers").body.size());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void adminCanProvisionEmployeeLoginWithOwnershipAndNoPasswordExposure() throws Exception {
    var admin = login("admin");
    var employee = create(admin);
    long id = employee.path("id").asLong();
    String path = "/employees/" + id + "/account", username = unique();
    var credentials = Map.of("username", username, "password", password);
    assertFalse(admin.get(path).body.path("exists").asBoolean());
    assertEquals(403, login("manager").write("POST", path, credentials).status);
    assertEquals(403, login("employee").get(path).status);
    assertEquals(
        400, admin.write("POST", path, Map.of("username", username, "password", "short")).status);
    var response = admin.write("POST", path, credentials);
    assertEquals(201, response.status, response.body.toString());
    assertFalse(response.body.has("password"));
    assertFalse(response.body.has("passwordHash"));
    assertEquals(username, admin.get(path).body.path("username").asText());
    assertEquals(409, admin.write("POST", path, credentials).status);
    var second = create(admin);
    assertEquals(
        409,
        admin.write("POST", "/employees/" + second.path("id").asLong() + "/account", credentials)
            .status);
    var newUser = login(username);
    assertEquals(id, newUser.get("/employees/me").body.path("id").asLong());
    assertEquals(1, newUser.get("/employees").body.path("totalCount").asInt());
    assertEquals(403, newUser.get("/employees/" + second.path("id").asLong()).status);
    String hash =
        db.queryForObject("SELECT password_hash FROM users WHERE employee_id=?", String.class, id);
    assertNotEquals(password, hash);
    assertTrue(hash.startsWith("$2"));
    assertEquals(
        204,
        admin.write(
                "PATCH",
                "/employees/" + second.path("id").asLong() + "/status",
                Map.of("status", "Inactive", "version", 0))
            .status);
    assertEquals(
        400,
        admin.write(
                "POST",
                "/employees/" + second.path("id").asLong() + "/account",
                Map.of("username", unique(), "password", password))
            .status);
  }
}
