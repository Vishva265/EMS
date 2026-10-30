# Understand and modify the project

## Read the code in this order

1. PRD.md: the behavior being built.
2. frontend/src/api.js: how React calls Java, cookies and CSRF.
3. backend/.../employee/EmployeeController.java: endpoint-to-method mapping.
4. EmployeeRequests.java and EmployeeQuery.java: allowed fields and validation.
5. EmployeeQueries.java: list/detail/history SQL and response fields.
6. EmployeeService.java: create/edit/status/transfer business rules.
7. security/SecurityConfig.java, AuthController.java, UserAccess.java, IdentityVault.java.
8. resources/db/migration/V1__schema.sql: tables and constraints.
9. EmployeeIntegrationTest.java: behavior proven against PostgreSQL.

The Java package root is backend/src/main/java/in/iaip/ems. No generated setters, Lombok, or hidden database entities are involved. Java records are immutable request containers; JdbcTemplate executes parameterized SQL; @Transactional commits a complete operation or rolls it back.

## Frontend map

| File | Responsibility |
|---|---|
| App.jsx | Login/session state, directory query state, actions, dialog selection |
| components/Workspace.jsx | Sidebar, topbar, mobile navigation, page frame |
| components/EmployeeTable.jsx | Columns, rows, sorting controls, pagination |
| components/Filters.jsx | Filter inputs and exact salary preset boundaries |
| components/EmployeeForm.jsx | Create/edit form and request payload |
| components/EmployeeDetails.jsx | Personal/employment view, masking, history |
| components/TransferForm.jsx | Department-transfer form |
| components/Login.jsx | Login form |
| components/Modal.jsx | Shared dialog, keyboard focus, reusable labeled field |
| api.js | Fetch calls, CSRF header, errors and formatting |
| styles.css | Visual design and responsive breakpoints |

Data flows down through component props. A callback reports a user action to App. App calls the API and refreshes the directory after successful changes. Forms own their input state; the database remains the source of truth. useState holds local state; useEffect fetches external data; useCallback keeps the dialog close handler stable.

## Backend map

| File | Responsibility |
|---|---|
| EmsApplication.java | Starts Spring Boot |
| employee/EmployeeController.java | HTTP routes and role guards |
| employee/EmployeeRequests.java | Create/update/status/transfer request records |
| employee/EmployeeQuery.java | Search/filter parameter validation |
| employee/EmployeeQueries.java | Read queries, authorization scope, response maps |
| employee/EmployeeService.java | Transactional writes, codes, transfers, concurrency |
| employee/DemoData.java | Optional synthetic demo data and accounts |
| security/AuthController.java | Login, logout, current user, CSRF token |
| security/SecurityConfig.java | JWT validation, cookies, CSRF, role authorities |
| security/UserAccess.java | Current account and employee ownership |
| security/IdentityVault.java | AES-GCM identity encryption |
| common/ApiException.java / ApiErrors.java | Safe business/validation error responses |

## Trace a registration

EmployeeForm builds an explicit payload -> api.js sends POST /api/employees with CSRF token -> Spring Security checks login/Admin -> @Valid checks request fields -> EmployeeService.create checks references/dates -> nextCode atomically increments department counter -> employee/personal rows are inserted -> transaction commits -> React refreshes the list.

## Trace a transfer

TransferForm sends destination, date, reason, and version -> Admin guard -> lock employee with SELECT ... FOR UPDATE -> compare version -> validate destination/date/state -> increment destination sequence with UPDATE ... RETURNING -> insert history -> change department/code on the same employee ID -> commit. A failure rolls back all writes. A stale version returns 409 instead of overwriting a newer update.

## Common interview modifications

### Add a table column for joining date
The API already returns joiningDate. Add its heading/cell in EmployeeTable.jsx and format with dateLabel from api.js. Backend sorting already allowlists joiningDate. No database change is necessary.

### Add a new employee field
Create V2__add_field.sql (never edit an applied migration). Add request validation in EmployeeRequests, explicit insert/update and response mapping, then form/detail/table controls where appropriate. Check both API validation and saved/reloaded behavior. If it is sensitive, update field permissions and encryption first.

### Change a salary filter
Change salaryParameters in Filters.jsx. Preserve the difference between exclusive preset upper bounds and inclusive custom bounds. Update the salary boundary assertions in EmployeeIntegrationTest.

### Change a permission
Update SECURITY-AND-RBAC.md, controller @PreAuthorize, request-field restrictions and UI action visibility. Add a forbidden API test. Hiding a button never replaces backend authorization.

### Change employee-code formatting
Change nextCode in EmployeeService. Existing codes are historical data; plan migration/backward compatibility before rewriting them. Keep atomic allocation and database uniqueness. Never replace the counter with employee count.

### Add a department
Add a migration inserting department name/prefix and its employee_code_sequences row. The lookup-driven UI then loads it automatically. Verify the badge still has a sensible default and that code generation works.

### Change validation
Change the Bean Validation annotation or service business rule first, then align the HTML field constraint and message. Test invalid requests directly against the API as well as through the browser.

## Be ready to explain

- Why employee ID stays fixed while code changes.
- Why an atomic counter and transaction are needed during concurrent requests.
- How a stale version prevents lost updates.
- Why designation is not a login permission.
- Why JWT in a cookie requires CSRF protection.
- Why passwords are hashed but identity numbers are encrypted.
- How search OR predicates combine with filter AND predicates before pagination.
- Which parts are implemented, which assumptions you made, and which assistance you used.

## Shared visual theme
Base layout rules are in styles.css; directory-preview.css now holds the approved shared theme for all screens. Doodle.jsx contains distinct original inline SVG illustrations and references the notebook SVG in frontend/public/illustrations. paper-drift and branch-sway animations are defined in the theme, with prefers-reduced-motion disabling them. Change visuals here without touching API or business logic.

## Admin-created employee login
Admin opens an employee profile and uses Employee login to set a unique username (3-50 letters/digits/dot/underscore/hyphen) and initial password (12-64 characters, at most 72 UTF-8 bytes). GET/POST /api/employees/{id}/account are Admin-only. POST links exactly one Employee-role account to an active employee and BCrypt-hashes the password. It cannot create Manager/Admin access. Passwords are never returned or recoverable; Admin shares the chosen initial password privately. Existing account username is displayed, not its password. Newly registered employees can then sign in and access only their own record/history. The backend must be restarted after this code update.
