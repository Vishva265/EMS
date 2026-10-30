$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$config=Get-Content (Join-Path $root '.local/config.json') -Raw | ConvertFrom-Json
$pgBin=if($env:PG_BIN){$env:PG_BIN}else{'C:\Program Files\PostgreSQL\18\bin'}
$env:PGPASSWORD=$config.ownerPassword
try {
    $exists=& "$pgBin\psql.exe" -h 127.0.0.1 -p 55432 -U ems_owner -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='ems_test'"
    if($LASTEXITCODE -ne 0){throw 'Start the local database first.'}
    if($exists -ne '1'){ & "$pgBin\createdb.exe" -h 127.0.0.1 -p 55432 -U ems_owner -O ems_app ems_test }
} finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
$env:JAVA_HOME=if($env:JAVA_HOME){$env:JAVA_HOME}else{'C:\Program Files\Java\jdk-17'}
$env:DB_URL='jdbc:postgresql://127.0.0.1:55432/ems_test';$env:DB_USER='ems_app';$env:DB_PASSWORD=$config.dbPassword
$env:JWT_SECRET=$config.jwtSecret;$env:ENCRYPTION_KEY=$config.encryptionKey;$env:DEMO_PASSWORD=$config.demoPassword
$localMaven=Join-Path $root '.tools/apache-maven-3.9.11/bin/mvn.cmd'
$maven=if(Test-Path $localMaven){$localMaven}else{'mvn.cmd'}
Set-Location (Join-Path $root 'backend')
& $maven "-Dmaven.repo.local=$root/.tools/m2" --no-transfer-progress test
if($LASTEXITCODE -ne 0){throw 'Integration tests failed. Read backend/target/surefire-reports.'}
