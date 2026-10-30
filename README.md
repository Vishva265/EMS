# Employee Management System - submission source

IAIP 2026 | Process Automation and Application Development Intern

Candidate name: ______________________________
Candidate code: ______________________________
GitHub repository URL: ______________________________

## Stack and architecture
React 19.2.0 / JavaScript, Vite 7.3.7, Java 17, Spring Boot 4.1.2, Maven, PostgreSQL. Layered monolithic backend with a separate React frontend. Spring JDBC uses explicit parameterized SQL. Exact resolved Java dependencies: docs/backend-dependencies.txt. Frontend packages: frontend/package.json and package-lock.json.

## Features
Employee registration/detail/edit/deactivation; personal-field validation; department-based unique codes; transactional department transfers with immutable employee ID/history; combined server-side search, filters, sorting, pagination; JWT login, Admin/Manager/Employee authorization, ownership checks; encrypted Aadhaar/PAN and masked views. Admin can create a linked Employee login from an active employee's profile.

## Prerequisites
Install Java 17+, Maven 3.9+, a Vite-compatible Node release (Node 22.12+ recommended), npm, and PostgreSQL. Tested environment used Java 17.0.12, Maven 3.9.11, Node 25.2.1 and PostgreSQL 18.0. Use maintained compatible patch versions where possible.

## Windows quick start
Open a terminal in the extracted project root. Set JAVA_HOME and PG_BIN if Java/PostgreSQL paths differ from scripts/local.ps1 defaults. Maven must be on PATH; downloaded .tools are intentionally not submitted.

1. `powershell -File scripts/local.ps1 setup`
2. In terminal 1: `powershell -File scripts/local.ps1 backend`
3. In terminal 2: `cd frontend`, then `npm ci`, then `npm run dev`
4. Open http://127.0.0.1:5173/.

The setup initializes an isolated PostgreSQL cluster under .local/postgres on local port 55432, creates a nonsuperuser ems_app login and ems database, and generates separate local passwords/keys. On first backend startup, Flyway creates the schema and optional demo provisioning creates 5 departments, 4 designations, 24 synthetic employees and sample transfer history.

Demo usernames: admin, manager, employee. From the project root, retrieve only their generated password with `(Get-Content .local/config.json -Raw | ConvertFrom-Json).demoPassword`. No actual credentials are included in this archive. Keep .local/config.json private and retain its encryption key for existing data. The demo Employee account links to one seeded employee; for a new employee, Admin opens View > Employee login and assigns credentials.

Stop frontend/backend with Ctrl+C in their terminals. Stop the project database with `powershell -File scripts/local.ps1 stop`. Port-in-use means another instance must be stopped before starting a duplicate.

## Manual setup on another operating system
Create a nonsuperuser PostgreSQL login and empty database named ems owned by that login. Set DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, ENCRYPTION_KEY and optionally DEMO_PASSWORD in your terminal/IDE. .env.example documents the names; it is not loaded automatically.

JWT_SECRET must contain at least 32 UTF-8 bytes. ENCRYPTION_KEY is 32 random bytes encoded as Base64, generated independently. DEMO_PASSWORD (12+ characters) opts into synthetic data/account creation only when no users exist. Without it, schema/lookups are created but no accounts are provisioned. Never commit keys or passwords.

From backend: `mvn spring-boot:run`. From frontend: `npm ci` then `npm run dev`. Flyway restores database structure from backend/src/main/resources/db/migration/V1__schema.sql. Do not copy a local PostgreSQL storage folder. Vite forwards /api to 127.0.0.1:8080; align vite.config.js if changing the backend port.

## API documentation and dependencies
Swagger: http://127.0.0.1:8080/swagger-ui.html. OpenAPI export: docs/openapi.json. Request examples and role permissions: docs/API.md. Login uses an HttpOnly cookie; writes also require a CSRF header. The CSRF endpoint is /api/auth/csrf. Swagger CSRF support is enabled. Refer to live /v3/api-docs after changing endpoints.

Maven restores declared dependencies automatically from pom.xml. `npm ci` restores frontend dependencies from the lockfile. Packages and versions are documented in docs/DEPENDENCIES.md and docs/backend-dependencies.txt. Do not submit node_modules, target, downloaded JARs or dependency caches.

## Build and verification
From frontend: `npm run build`. On the Windows setup above: `powershell -File scripts/test.ps1`. This creates/uses a separate ems_test database. Tests reject DB_URL values that do not end in /ems_test. Tests cover ownership, CSRF, role/field restrictions, validation/encryption, code uniqueness, rollback, transfers, filters/pagination, deactivation and employee-login provisioning.

For manual test execution configure the same environment with DB_URL pointing to a dedicated ems_test database and run `mvn test`. For a distributable run `mvn -DskipTests package` after tests pass; start the resulting JAR with the same environment. Frontend production files are generated by npm run build; a deployment must provide API proxying and HTTPS.

## Assumptions and limits
Soft deactivation preserves records/history; department codes never reuse an allocated value; code numbers have minimum four-digit padding. Dates are ISO business dates. Transfers are immediate, Active employees only, no future date or date preceding joining/latest transfer. Designation is separate from login role. Manager may edit only designation, joining date and salary; Employee accesses own masked profile/history. Salary presets use exclusive upper bounds; custom limits are inclusive. Aadhaar/PAN validation checks format, not identity verification. All sample data is synthetic.

Local HTTP has COOKIE_SECURE=false. Deployments require HTTPS and secure cookies. JWT lifetime is 30 minutes; logout clears its browser cookie but no stolen-token revocation or refresh-token system is implemented. There is no password reset or forced initial-password change. Bundled Flyway reports a PostgreSQL 18 support-version warning; migrations and integration behavior were exercised on 18.0. PostgreSQL 17 is within its stated range. No public deployment is required or included.

## Assistance disclosure and declaration
AI assistance was used for requirements interpretation, planning, implementation, interface design, documentation, and testing. The organizer advises against AI use and requires disclosure; disclosure alone does not establish permission. Candidate must accurately disclose assistance, personally verify/understand the submission, and complete the separate ink-signed declaration. No declaration or signature is supplied in this source archive.

## Hosted demo and security checks
See docs/DEPLOYMENT.md for the Free Render + Neon setup. Dockerfile serves React from Spring Boot at one origin. render.yaml selects Free and waits for passing CI checks. GitHub workflows run builds, PostgreSQL tests, npm audit, Trivy and CodeQL; Dependabot proposes npm/Maven/Actions/Docker updates weekly. SECURITY.md describes scope and limits. Configuration does not mean a live deployment has occurred.
