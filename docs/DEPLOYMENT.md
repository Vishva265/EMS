# Free-tier deployment: Render + Neon

The Docker image builds React and copies its output into the Spring Boot JAR. One Render web service serves both UI and /api. Neon stores PostgreSQL data outside Render's temporary filesystem. Do not use GitHub Pages for this full-stack app: it cannot run Spring Boot/PostgreSQL.

## 1. GitHub checks

Push the deployment/security changes to Vishva265/EMS, run the Actions workflows, and review Security alerts. Enable Dependency graph, Dependabot alerts/security updates, secret scanning/push protection and private vulnerability reporting under repository settings where available. Avoid enabling CodeQL default setup alongside the advanced workflow; use one configuration.

Protect main with required checks after their first run. Require React build/npm audit, Java/PostgreSQL tests, source scan, image build/scan, CodeQL analyses and dependency review where applicable. The scans fail on High/Critical issues, including unfixed findings. Dependabot makes weekly update PRs; inspect and merge only after checks pass. No paid scanner is required for this public repository.

## 2. Free Neon database

Sign in to https://console.neon.tech and select Free. Create a fresh EMS demo project/database. Do not upload .local or real personnel data. Obtain the host, database, username and password from the Connect panel. Store credentials directly in Render's environment settings, never GitHub source or chat.

JDBC URL template (no embedded password):

`jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory`

The Java SSL factory uses the runtime trust store to verify the database certificate and hostname. Do not disable TLS validation to solve connection errors. Keep DB_USER and DB_PASSWORD separate. If connecting through Neon pooling causes migration issues, use the direct database endpoint for this small demo and its three-connection pool.

## 3. Free Render service

Sign in to https://dashboard.render.com. Create a Blueprint from https://github.com/Vishva265/EMS using render.yaml, or create a Web Service with Runtime Docker, root directory '.', Dockerfile './Dockerfile', branch main, and plan Free. Do not select a paid instance/database/disk. The blueprint creates only a Free web service; it does not create a billable Render database.

Environment settings:

| Variable | Value |
|---|---|
| SPRING_PROFILES_ACTIVE | prod |
| DB_URL | Neon JDBC URL above |
| DB_USER | Neon database username |
| DB_PASSWORD | Neon database password |
| JWT_SECRET | Fresh random value, at least 32 UTF-8 bytes |
| ENCRYPTION_KEY | Separate random 32 bytes encoded as Base64 |
| DEMO_PASSWORD | Strong private initial Admin password, at least 12 characters |
| SEED_DEMO | false (default) |

Generate keys locally using a cryptographic random generator. Never reuse the existing .local keys on public hosting. Retain a secure backup of ENCRYPTION_KEY; changing/loss prevents reading previously encrypted identities. Render sets PORT. The prod profile binds 0.0.0.0, handles forwarded HTTPS, enables Secure cookies and disables Swagger.

Default production provisioning creates only the initial admin account on an empty users table; username admin, password DEMO_PASSWORD. It does not create manager/employee accounts with that same password. Admin registers employees and creates each linked login privately. If the assessment specifically requires cloud synthetic seed data, SEED_DEMO=true opts into the same synthetic seed/accounts as local development; do not share the bootstrap password publicly. Keep the default false for public evaluation.

Choose 'After CI Checks Pass' for automatic deployments (render.yaml uses autoDeployTrigger: checksPass). First deployment must also be checked manually. Health path is /api/auth/csrf. Keep the assigned https://...onrender.com URL for both frontend and API. Do not put a database connection string into React/VITE_ variables.

## 4. Verify after deploy

- Visit the HTTPS URL; login page and local SVG assets must load without authentication.
- Sign in as Admin; register synthetic data and provision a separate Employee login.
- Confirm that the Employee sees only their own record, and cannot access another ID.
- Verify transfer history and codes, query composition, deactivation blocking login, and persistence after redeploy.
- Inspect cookie flags: EMS_TOKEN HttpOnly/Secure/SameSite=Strict; CSRF cookie Secure. Inspect CSP/X-Content-Type-Options/frame restrictions.
- Swagger /v3/api-docs and /swagger-ui.html must be unavailable under prod.
- Inspect Actions and Security alerts; do not describe the site as fully secure merely because scans pass.

## Cost and availability limits

Render Free sleeps after inactivity; initial requests can take time to wake it. Render describes Free as suitable for demonstrations rather than production use. Its Free database expires, so this design uses Neon's separate Free database. Both providers have usage limits and may change plan conditions. Remain on Free; review dashboard limits and do not attach a payment method or upgrade for this task. No custom domain is needed.

References checked 2026-10-30:
- https://render.com/docs/free
- https://render.com/docs/blueprint-spec
- https://render.com/docs/deploys
- https://neon.com/docs/introduction/plans
- https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference

## Publishing state

Configuration files alone do not mean deployment has occurred. Render/Neon account connection, cloud secret configuration, successful GitHub checks and a live HTTPS response must be confirmed before claiming the site is deployed.
