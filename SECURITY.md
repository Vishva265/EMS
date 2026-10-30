# Reporting and maintaining security

This is an internship demonstration application. Use synthetic data on the public demo. Do not submit real employee identity numbers through a public evaluation deployment.

Report suspected vulnerabilities privately to the repository owner, not in a public issue containing credentials or personal data. If GitHub private vulnerability reporting is enabled, use the repository Security tab.

GitHub checks cover React builds, PostgreSQL-backed Java tests, CodeQL static analysis, npm audit, Trivy dependency/secret/image scans, and pull-request dependency review. Weekly Dependabot updates cover npm, Maven, Actions and Docker. Updates are proposed as pull requests, not silently merged.

A passing scan is not proof of complete security. Check Security > Code scanning / Dependabot, review changes, and keep required checks enabled. Do not bypass failing High/Critical findings to deploy. Public GitHub repositories support these workflows; private-repository CodeQL/dependency-review availability depends on the account plan.

Known limits: single application instance, no distributed login throttling, refresh tokens, password reset, forced initial-password change, or stolen-JWT revocation. Existing tokens expire after 30 minutes; disabled users/linked employees are checked per request. Public hosting requires HTTPS, Secure cookies, separate external secrets, durable database storage and key backups.
