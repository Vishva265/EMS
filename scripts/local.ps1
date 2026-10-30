param([ValidateSet('setup','backend','frontend','stop')][string]$Action='setup')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$state=Join-Path $root '.local'
$pgBin=if($env:PG_BIN){$env:PG_BIN}else{'C:\Program Files\PostgreSQL\18\bin'}
$javaHome=if($env:JAVA_HOME){$env:JAVA_HOME}else{'C:\Program Files\Java\jdk-17'}
$configPath=Join-Path $state 'config.json'
New-Item -ItemType Directory -Force $state | Out-Null
function New-Secret {
    $bytes=New-Object byte[] 32
    $generator=[System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes); return [Convert]::ToBase64String($bytes) }
    finally { $generator.Dispose() }
}
function Check-Exit { if($LASTEXITCODE -ne 0){throw "The previous command failed ($LASTEXITCODE)."} }
if($Action -eq 'setup'){
    if(!(Test-Path $configPath)){
        @{dbPassword=(New-Secret);ownerPassword=(New-Secret);jwtSecret=(New-Secret);encryptionKey=(New-Secret);demoPassword=(New-Secret)} | ConvertTo-Json | Set-Content $configPath
    }
    $config=Get-Content $configPath -Raw | ConvertFrom-Json
    $data=Join-Path $state 'postgres'
    if(!(Test-Path (Join-Path $data 'PG_VERSION'))){
        $pw=Join-Path $state 'init-password.txt'
        [IO.File]::WriteAllText($pw,$config.ownerPassword)
        try { & "$pgBin\initdb.exe" -D $data -U ems_owner -A scram-sha-256 --pwfile=$pw --encoding=UTF8 --locale=C; Check-Exit }
        finally { Remove-Item -LiteralPath $pw -ErrorAction SilentlyContinue }
    }
    & "$pgBin\pg_ctl.exe" -D $data status *> $null
    if($LASTEXITCODE -ne 0){ & "$pgBin\pg_ctl.exe" -D $data -l (Join-Path $state 'postgres.log') -o '-p 55432 -h 127.0.0.1' -w start; Check-Exit }
    $env:PGPASSWORD=$config.ownerPassword
    try {
        $exists=& "$pgBin\psql.exe" -h 127.0.0.1 -p 55432 -U ems_owner -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='ems_app'"; Check-Exit
        if($exists -ne '1'){
            # Random Base64 secrets contain no SQL quotes. Never print the generated statement.
            "CREATE ROLE ems_app LOGIN PASSWORD '$($config.dbPassword)';" | & "$pgBin\psql.exe" -h 127.0.0.1 -p 55432 -U ems_owner -d postgres -v ON_ERROR_STOP=1; Check-Exit
        }
        $exists=& "$pgBin\psql.exe" -h 127.0.0.1 -p 55432 -U ems_owner -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='ems'"; Check-Exit
        if($exists -ne '1'){ & "$pgBin\createdb.exe" -h 127.0.0.1 -p 55432 -U ems_owner -O ems_app ems; Check-Exit }
    } finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
    Write-Host 'Database ready on local port 55432. Credentials are in ignored .local/config.json.'
    Write-Host 'Next: run this script with backend in one terminal, and frontend in another.'
    exit
}
if($Action -eq 'stop'){
    & "$pgBin\pg_ctl.exe" -D (Join-Path $state 'postgres') -m fast -w stop; Check-Exit
    exit
}
if($Action -eq 'backend'){
    if(!(Test-Path $configPath)){throw 'Run setup first.'}
    $config=Get-Content $configPath -Raw | ConvertFrom-Json
    $env:JAVA_HOME=$javaHome
    $env:DB_URL='jdbc:postgresql://127.0.0.1:55432/ems';$env:DB_USER='ems_app';$env:DB_PASSWORD=$config.dbPassword
    $env:JWT_SECRET=$config.jwtSecret;$env:ENCRYPTION_KEY=$config.encryptionKey;$env:DEMO_PASSWORD=$config.demoPassword
    $localMaven=Join-Path $root '.tools/apache-maven-3.9.11/bin/mvn.cmd'
    $maven=if(Test-Path $localMaven){$localMaven}else{'mvn.cmd'}
    Set-Location (Join-Path $root 'backend')
    & $maven "-Dmaven.repo.local=$root/.tools/m2" spring-boot:run; Check-Exit
} else {
    Set-Location (Join-Path $root 'frontend')
    & npm.cmd run dev; Check-Exit
}
