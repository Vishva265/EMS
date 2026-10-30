package in.iaip.ems.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JwtEncoder jwtEncoder(@Value("${app.jwt-secret}") String secret) {
    requireKey(secret);
    return new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${app.jwt-secret}") String secret) {
    requireKey(secret);
    var decoder =
        NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer("personnel-office"),
            jwt ->
                jwt.getAudience().contains("ems-browser")
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
    return decoder;
  }

  private void requireKey(String key) {
    if (key.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http, JdbcTemplate db, @Value("${app.cookie-secure}") boolean secureCookies)
      throws Exception {
    var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrf.setCookieCustomizer(cookie -> cookie.secure(secureCookies).sameSite("Strict"));
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'"
                                + " 'unsafe-inline'; img-src 'self' data:; font-src 'self';"
                                + " connect-src 'self'; object-src 'none'; base-uri 'none';"
                                + " form-action 'self'; frame-ancestors 'none'")))
        .csrf(
            c ->
                c.csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/illustrations/**",
                        "/favicon.ico",
                        "/api/auth/csrf",
                        "/api/auth/login",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            o ->
                o.bearerTokenResolver(
                        request -> {
                          String path = request.getServletPath();
                          // An expired cookie must not prevent loading the login screen or signing
                          // in again.
                          if (!path.startsWith("/api/")
                              || path.equals("/api/auth/login")
                              || path.equals("/api/auth/csrf")) return null;
                          if (request.getCookies() != null)
                            for (var cookie : request.getCookies())
                              if (cookie.getName().equals("EMS_TOKEN")) return cookie.getValue();
                          return null;
                        })
                    .jwt(
                        j ->
                            j.jwtAuthenticationConverter(
                                jwt -> {
                                  var roles =
                                      db.queryForList(
                                          "SELECT u.access_role FROM users u LEFT JOIN employees e"
                                              + " ON e.id=u.employee_id WHERE u.username=? AND"
                                              + " u.is_active=true AND (e.id IS NULL OR"
                                              + " e.status='Active')",
                                          String.class,
                                          jwt.getSubject());
                                  if (roles.isEmpty())
                                    throw new OAuth2AuthenticationException(
                                        new OAuth2Error("invalid_token"));
                                  return new JwtAuthenticationToken(
                                      jwt,
                                      List.of(new SimpleGrantedAuthority("ROLE_" + roles.get(0))),
                                      jwt.getSubject());
                                })))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/problem+json");
                          res.getWriter()
                              .write("{\"detail\":\"Please sign in to continue.\",\"status\":401}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/problem+json");
                          res.getWriter()
                              .write(
                                  "{\"detail\":\"This action is not permitted or the security token"
                                      + " has expired.\",\"status\":403}");
                        }));
    return http.build();
  }
}
