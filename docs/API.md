# API usage

Base URL: http://127.0.0.1:8080. The frontend uses a same-origin /api proxy. Full generated operation/schema reference is openapi.json or live /swagger-ui.html.

## Authentication sequence

1. GET /api/auth/csrf with a cookie-aware client. Save the response token and XSRF-TOKEN cookie.
2. POST /api/auth/login with JSON username/password and header X-XSRF-TOKEN: response-token. Retain EMS_TOKEN and CSRF cookies; never paste credentials into URLs.
3. GET /api/auth/me confirms the account and role.
4. Include cookies on every protected request. For POST, PUT, PATCH, and logout include the CSRF header too.
5. POST /api/auth/logout clears the JWT cookie. Expiry or linked employee deactivation requires login again.

No public user registration exists. Synthetic demo accounts are provisioned from DEMO_PASSWORD on the first startup of an empty database.

## Routes and permissions

| Method | Route | Access |
|---|---|---|
| GET | /api/auth/csrf | Public |
| POST | /api/auth/login | Public, CSRF protected |
| GET | /api/auth/me | Authenticated |
| POST | /api/auth/logout | Authenticated, CSRF protected |
| GET | /api/departments and /api/roles | Authenticated |
| GET | /api/employees | Admin/Manager all; Employee own only |
| GET | /api/employees/me | Linked own record |
| GET | /api/employees/{id} | Admin/Manager; Employee owner |
| GET | /api/employees/{id}?reveal=true | Admin only |
| POST | /api/employees | Admin |
| PUT | /api/employees/{id} | Admin |
| PATCH | /api/employees/{id}/employment | Admin/Manager |
| PATCH | /api/employees/{id}/status | Admin |
| POST | /api/employees/{id}/transfers | Admin |
| GET | /api/employees/{id}/transfers | Admin/Manager; Employee owner |

## Create employee example

```json
{
  "fullName": "Demo Candidate",
  "email": "demo.candidate@example.test",
  "mobile": "9000012345",
  "aadhaar": "999988887777",
  "pan": "ZZZZZ1234Z",
  "dateOfBirth": "1996-02-10",
  "gender": "Other",
  "address": "Synthetic address, Pune",
  "departmentId": 1,
  "roleId": 1,
  "joiningDate": "2025-01-10",
  "annualSalary": 600000,
  "status": "Active"
}
```

All data above is synthetic. Send Aadhaar without display hyphens and uppercase PAN. The server assigns id/code/version. PUT uses the same fields plus the latest version and may not change department or status. Use the dedicated actions for those changes. Extra unknown fields are rejected.

Manager employment update: `{ "roleId": 2, "joiningDate": "2025-01-10", "annualSalary": 650000, "version": 0 }`.

Status action: `{ "status": "Inactive", "version": 0 }`.

Transfer: `{ "newDepartmentId": 2, "transferDate": "2026-01-10", "reason": "Synthetic administrative reassignment", "version": 0 }`. Use a date between joining and today, not earlier than latest history. Always read the current version first; stale writes return 409.

## List example

`GET /api/employees?search=Rahul&departmentId=1&minSalary=300000&maxSalary=500000&salaryMaxExclusive=true&sortBy=fullName&sortDirection=asc&pageNumber=1&pageSize=10`

Parameters: search, departmentId, roleId, status (Active/Inactive), gender (Male/Female/Other), joiningDateFrom, joiningDateTo, minSalary, maxSalary, salaryMaxExclusive, sortBy, sortDirection, pageNumber, pageSize. Omit empty values.

Allowed sorts: employeeCode, fullName, department, role, email, mobile, annualSalary, status, joiningDate. Direction asc/desc. Defaults code ascending, page 1, size 10. Page sizes 5/10/20. Dates are ISO YYYY-MM-DD, inclusive. Salary is annual INR; maximum is inclusive unless salaryMaxExclusive=true. Presets use exclusive upper limits. Results include items, pageNumber, pageSize, totalCount, totalPages.

Global search covers only permitted code/name/email/mobile/department/designation/address/status. Personal identifiers are absent from list responses and excluded from search. Detail responses mask them unless an Admin explicitly requests reveal.

Errors use application/problem+json with status/detail; validation adds an errors object keyed by field. 400 invalid input; 401 login required/invalid JWT; 403 permission/CSRF; 404 missing record; 409 duplicate/stale write; 500 safe generic unexpected error.

## Admin-created employee login
Admin opens an employee profile and uses Employee login to set a unique username (3-50 letters/digits/dot/underscore/hyphen) and initial password (12-64 characters, at most 72 UTF-8 bytes). GET/POST /api/employees/{id}/account are Admin-only. POST links exactly one Employee-role account to an active employee and BCrypt-hashes the password. It cannot create Manager/Admin access. Passwords are never returned or recoverable; Admin shares the chosen initial password privately. Existing account username is displayed, not its password. Newly registered employees can then sign in and access only their own record/history. The backend must be restarted after this code update.
