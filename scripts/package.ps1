$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$output=Join-Path $root 'output'
New-Item -ItemType Directory -Force $output | Out-Null
$destination=Join-Path $output 'EMS-Source.zip'
if(Test-Path $destination){Remove-Item -LiteralPath $destination}
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::Open($destination,[IO.Compression.ZipArchiveMode]::Create)
try {
    # Submission allowlist: source, migration, API/dependency docs and setup only.
    $entries=@{}
    foreach($directory in @('frontend/src','backend/src','.github')){
        foreach($file in (Get-ChildItem -LiteralPath (Join-Path $root $directory) -Recurse -File)){
            $entry=$file.FullName.Substring($root.Length+1).Replace('\','/')
            $entries[$entry]=$file.FullName
        }
    }
    foreach($file in @('frontend/public/illustrations/personnel-notes.svg','frontend/package.json','frontend/package-lock.json','frontend/index.html','frontend/vite.config.js','frontend/.prettierrc.json','backend/pom.xml','.gitignore','.env.example','scripts/local.ps1','scripts/test.ps1','scripts/package.ps1','docs/API.md','docs/openapi.json','docs/backend-dependencies.txt','docs/DEPENDENCIES.md','docs/DEPLOYMENT.md','Dockerfile','.dockerignore','render.yaml','SECURITY.md')){
        $entries[$file]=Join-Path $root $file
    }
    $submissionReadme=Join-Path $root 'docs/SUBMISSION-README.md'
    $entries['README.md']=if(Test-Path $submissionReadme){$submissionReadme}else{Join-Path $root 'README.md'}
    foreach($entry in ($entries.Keys | Sort-Object)){
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$entries[$entry],$entry,[IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally {$archive.Dispose()}
Write-Host "Submission source package created: $destination"
