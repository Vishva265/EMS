# Verification record

Verified locally on 2026-10-29. Java 17.0.12, Maven 3.9.11, Node 25.2.1, PostgreSQL 18.0. Spring Boot 3.5.16 and Vite 7.3.7.

## Automated integration checks

`powershell -File scripts/test.ps1` passed 13 tests, 0 failures, 0 errors on 2026-10-30 with Spring Boot 4.1.2. These run an actual HTTP server against a dedicated PostgreSQL ems_test database. They are not H2 or in-memory persistence tests.

1. Anonymous/CSRF rejection, employee ownership and employee write restrictions.
2. Manager employment updates allowed; registration/status/transfer/overposted fields rejected.
3. Field validation, normalized email uniqueness, encrypted stored Aadhaar, masked responses, identity exclusion from search.
4. Transfer keeps employee ID, changes code, records history, rejects stale and same-department requests.
5. A deliberately rejected history insert rolls back employee, history, and destination sequence together.
6. Twelve concurrent creations produce twelve unique codes.
7. Salary-band boundaries, custom inclusive limits, combined filters, descending sorting and pagination.
8. Deactivation blocks an existing employee JWT and a subsequent login; activation restores the record.
9. Concurrent transfers with the same version produce one success and one conflict, with one history entry.
10. Admin provisions an Employee login; weak passwords, duplicate usernames/accounts, inactive employees and non-Admin requests are rejected. The account can access only its linked profile; storage uses BCrypt.
11. An invalid/expired authentication cookie does not block the public shell, CSRF endpoint or signing in again, while the employee API remains protected.
12. Production profile serves the public shell, rejects anonymous API access, sets a Secure/SameSite CSRF cookie and disables OpenAPI.
13. Authenticated Admin cookie requests without a CSRF header, or with an invalid token, return 403 and leave the employee unchanged. The same request with the valid token succeeds. This regression first reproduced the live failure (204 without a header) before the fix.

Tests use synthetic fixtures and retain them in ems_test for inspection. The preview database ems is separate. Test execution refuses a DB_URL that does not end in /ems_test. The forced rollback constraint is removed in a finally block.

## Frontend and dependency checks

- `npm run build` passed.
- `npm audit` after patching Vite reported zero vulnerabilities at installation time. This is a point-in-time dependency result, not a complete security audit.
- Java compilation and dependency restoration passed.
- An empty database was created and Flyway applied V1 successfully before seeding.

## Browser checks

Administrator sign-in, live directory rendering, employee registration including automatic Aadhaar formatting, successful database-backed save, department transfer, and displayed before/after transfer history were exercised. Desktop navigation and registration layout were visually inspected. A 390px responsive viewport and mobile navigation were also checked. Screenshots are in docs/screenshots/. Native date controls required keyboard interaction in the browser automation; registration succeeded with the actual controls.

The interface uses responsive breakpoints and scrollable tables. Further candidate testing on their actual browser/device is recommended before submission. This record should be updated if later code changes invalidate any checked behavior.

## Remaining submission work

The candidate must review the system, prepare their PDF, print/sign/scan the declaration, and publish/share an assessor-accessible repository if submitting. None of those external submission actions are claimed here.

The first cloud security scan identified vulnerable transitive libraries in Spring Boot 3.5.16. The backend was upgraded to Boot 4.1.2 and springdoc 3.2.0, including the new Flyway starter and Jackson 3 test imports. The full PostgreSQL suite passed after the upgrade. Remote scan results must be checked separately; local tests alone do not establish security.

Live smoke testing also exposed Resource Server's default CSRF exemption for bearer authentication: with our cookie resolver it incorrectly exempted cookie-authenticated mutations. SecurityConfig now enforces CSRF on all unsafe methods after framework defaults are applied. CSRF tokens remain stable across stateless JWT validation instead of clearing on each request. An intermediate failed suite left the seeded test employee inactive; only that synthetic ems_test fixture was restored before the complete passing run.
