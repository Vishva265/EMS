# Dependencies used

Java 17; Maven; Spring Boot parent 3.5.16. pom.xml is authoritative for restoration. The full resolved Java tree is in backend-dependencies.txt. JWT implementation is supplied through Spring Security OAuth2 JOSE and Nimbus.

| Maven artifact | Resolved version | Scope |
|---|---|---|
| org.springframework.boot:spring-boot-starter-web | 3.5.16 | compile[36m |
| org.springframework.boot:spring-boot-starter-jdbc | 3.5.16 | compile[36m |
| org.springframework.boot:spring-boot-starter-validation | 3.5.16 | compile[36m |
| org.springframework.boot:spring-boot-starter-security | 3.5.16 | compile[36m |
| org.springframework.boot:spring-boot-starter-oauth2-resource-server | 3.5.16 | compile[36m |
| org.flywaydb:flyway-core | 11.7.2 | compile[36m |
| org.flywaydb:flyway-database-postgresql | 11.7.2 | compile[36m |
| org.postgresql:postgresql | 42.7.11 | runtime[36m |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 2.8.14 | compile[36m |
| org.springframework.boot:spring-boot-starter-test | 3.5.16 | test[36m |
| org.springframework.security:spring-security-test | 6.5.11 | test[36m |
| org.springframework.security:spring-security-oauth2-jose | 6.5.11 | compile[36m |
| com.nimbusds:nimbus-jose-jwt | 9.37.4 | compile[36m |

## Frontend

| npm package | Pinned version |
|---|---|
| lucide-react | 0.468.0 |
| react | 19.2.0 |
| react-dom | 19.2.0 |
| @vitejs/plugin-react | 5.1.1 |
| prettier | 3.6.2 |
| vite | 7.3.7 |

Restore backend with `mvn dependency:resolve` or `mvn spring-boot:run` from backend. Restore frontend with `npm ci` from frontend. Submit manifests/lockfile, not copied JARs, node_modules, target or caches.

Reference: Spring Boot Java requirements - https://docs.spring.io/spring-boot/3.5/system-requirements.html