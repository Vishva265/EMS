package in.iaip.ems.security;

import in.iaip.ems.common.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  public record Login(
      @NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 200) String password) {}

  private final JdbcTemplate db;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final UserAccess access;
  private final boolean secure;

  public AuthController(
      JdbcTemplate db,
      PasswordEncoder passwords,
      JwtEncoder encoder,
      UserAccess access,
      @Value("${app.cookie-secure}") boolean secure) {
    this.db = db;
    this.passwords = passwords;
    this.encoder = encoder;
    this.access = access;
    this.secure = secure;
  }

  @GetMapping("/csrf")
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/login")
  Map<String, Object> login(@Valid @RequestBody Login form, HttpServletResponse response) {
    var rows =
        db.queryForList(
            "SELECT u.* FROM users u LEFT JOIN employees e ON e.id=u.employee_id WHERE u.username=?"
                + " AND u.is_active=true AND (e.id IS NULL OR e.status='Active')",
            form.username().trim().toLowerCase(Locale.ROOT));
    if (rows.isEmpty()
        || !passwords.matches(form.password(), rows.get(0).get("password_hash").toString()))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Username or password is incorrect.");
    var user = rows.get(0);
    Instant now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("personnel-office")
            .subject(user.get("username").toString())
            .audience(List.of("ems-browser"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(1800))
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(token, 1800).toString());
    return Map.of("username", user.get("username"), "role", user.get("access_role"));
  }

  @GetMapping("/me")
  UserAccess.CurrentUser me() {
    return access.current();
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout() {
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie("", 0).toString())
        .build();
  }

  private ResponseCookie cookie(String value, int age) {
    return ResponseCookie.from("EMS_TOKEN", value)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Strict")
        .path("/")
        .maxAge(age)
        .build();
  }
}
