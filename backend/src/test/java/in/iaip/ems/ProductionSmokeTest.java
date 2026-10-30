package in.iaip.ems;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=prod", "app.cookie-secure=true"})
class ProductionSmokeTest {
  @LocalServerPort int port;

  @DynamicPropertySource
  static void requireTestDatabase(DynamicPropertyRegistry registry) {
    String url = System.getenv("DB_URL");
    if (url == null || !url.endsWith("/ems_test"))
      throw new IllegalStateException("Dedicated test database required");
  }

  private HttpResponse<String> get(String path) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void productionServesPublicShellKeepsApiPrivateAndUsesSecureCsrfCookie() throws Exception {
    assertEquals(200, get("/").statusCode());
    assertEquals(401, get("/api/employees").statusCode());
    var csrf = get("/api/auth/csrf");
    assertEquals(200, csrf.statusCode());
    assertTrue(
        csrf.headers().allValues("set-cookie").stream()
            .anyMatch(
                c ->
                    c.contains("XSRF-TOKEN=")
                        && c.contains("Secure")
                        && c.contains("SameSite=Strict")));
    assertEquals(404, get("/v3/api-docs").statusCode());
  }
}
