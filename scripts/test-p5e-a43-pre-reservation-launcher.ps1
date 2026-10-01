[CmdletBinding()]
param(
    [string]$ReportPath = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $PSScriptRoot 'p5e-a43-pre-reservation-launcher.ps1'
$toolchainPath = Join-Path $PSScriptRoot 'p5e-raw-toolchain.ps1'
$ps51Path = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'

$tokens = $null
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile($sourcePath, [ref]$tokens, [ref]$parseErrors) | Out-Null
if ($parseErrors.Count -ne 0) { throw 'PRE_RESERVATION_SOURCE_PARSE_FAILED' }
[System.Management.Automation.Language.Parser]::ParseFile($toolchainPath, [ref]$tokens, [ref]$parseErrors) | Out-Null
if ($parseErrors.Count -ne 0) { throw 'TOOLCHAIN_SOURCE_PARSE_FAILED' }

. $sourcePath -LibraryOnly
. $toolchainPath -LibraryOnly

$qaRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-a43-pre-reservation-qa-' + [Guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $qaRoot)
$results = New-Object System.Collections.ArrayList

function Assert-QA {
    param([Parameter(Mandatory = $true)][bool]$Condition,[Parameter(Mandatory = $true)][string]$Code)
    if (-not $Condition) { throw $Code }
}

function Assert-QAEqual {
    param([AllowNull()][object]$Actual,[AllowNull()][object]$Expected,[Parameter(Mandatory = $true)][string]$Code)
    Assert-QA -Condition ([string]$Actual -ceq [string]$Expected) -Code $Code
}

function Assert-QANotContains {
    param([AllowNull()][string]$Text,[Parameter(Mandatory = $true)][string]$Sentinel,[Parameter(Mandatory = $true)][string]$Code)
    Assert-QA -Condition ($null -eq $Text -or -not $Text.Contains($Sentinel)) -Code $Code
}

function New-QAFixture {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [hashtable]$Options = @{}
    )
    $caseRoot = Join-Path $qaRoot ($Name + ' with spaces')
    $repo = Join-Path $caseRoot 'repo root'
    $private = Join-Path $caseRoot 'private root'
    $sdk = Join-Path $caseRoot 'Android SDK with spaces'
    $platformTools = Join-Path $sdk 'platform-tools'
    $buildTools = Join-Path $sdk 'build-tools\35.0.0\lib'
    $javaRoot = Join-Path $caseRoot 'Java home with spaces'
    $pin = Join-Path $repo 'pinned dependency.txt'
    $launcher = Join-Path $repo 'launcher template.ps1'
    $reservationRoot = Join-Path $private 'reservation root'
    $auditRoot = Join-Path $caseRoot 'audit root'
    $auditPath = Join-Path $auditRoot 'pre-reservation-diagnostic.json'
    foreach ($directory in @($repo, $private, $platformTools, $buildTools, $javaRoot, $reservationRoot, $auditRoot)) {
        [void](New-Item -ItemType Directory -Path $directory -Force)
    }
    $adb = Join-Path $platformTools 'adb.exe'
    $java = Join-Path $javaRoot 'java.exe'
    $apksigner = Join-Path $buildTools 'apksigner.jar'
    $sdkFile = Join-Path $caseRoot 'sdk-file-not-directory'
    $adbDirectory = Join-Path $caseRoot 'adb-directory'
    $javaDirectory = Join-Path $caseRoot 'java-directory'
    $apksignerDirectory = Join-Path $caseRoot 'apksigner-directory'
    [IO.File]::WriteAllText($adb, 'synthetic adb')
    [IO.File]::WriteAllText($java, 'synthetic java')
    [IO.File]::WriteAllText($apksigner, 'synthetic apksigner')
    [IO.File]::WriteAllText($pin, 'synthetic pin')
    [IO.File]::WriteAllText($launcher, 'synthetic tracked parent launcher template')
    [IO.File]::WriteAllText($sdkFile, 'not a directory')
    foreach ($directory in @($adbDirectory, $javaDirectory, $apksignerDirectory)) {
        [void](New-Item -ItemType Directory -Path $directory -Force)
    }

    $state = [pscustomobject][ordered]@{
        Resolve = 0
        Approval = 0
        Reserve = 0
        Owner = 0
        Receipt = 0
        ReceiptGate = 0
        Key = 0
        Child = 0
        ClearKey = 0
        WriteDiagnostic = 0
        LiveCalls = 0
    }
    $fixture = [pscustomobject][ordered]@{
        Name = $Name
        Root = $caseRoot
        RepoRoot = $repo
        PrivateRoot = $private
        SdkRoot = $sdk
        SdkFile = $sdkFile
        AdbPath = $adb
        JavaPath = $java
        ApkSignerPath = $apksigner
        AdbDirectory = $adbDirectory
        JavaDirectory = $javaDirectory
        ApkSignerDirectory = $apksignerDirectory
        PinPath = $pin
        LauncherPath = $launcher
        ReservationRoot = $reservationRoot
        AuditPath = $auditPath
        State = $state
        Options = $Options
        Contract = $null
        Dependencies = $null
    }
    $fixture.Contract = @{
        RepoRoot = $repo
        PrivateRoot = $private
        PowershellPath = $ps51Path
        LauncherPath = $launcher
        ReservationRoot = $reservationRoot
        AuditPath = $auditPath
        SdkPath = $sdk
        JavaPath = $java
        ApprovalLiteral = 'APPROVE_ONE_FRESH_EVENT'
        PinnedFiles = @([pscustomobject][ordered]@{
                Path = $pin
                Sha256 = Get-P5EA43Sha256Upper -Path $pin
            })
        ExpectedLauncherSha256 = Get-P5EA43Sha256Upper -Path $launcher
    }

    $fixture.Dependencies = @{
        ResolveToolchain = {
            param($contract)
            $fixture.State.Resolve = [int]$fixture.State.Resolve + 1
            if ($fixture.Options.ContainsKey('ResolverThrowMessage')) {
                throw (New-Object System.InvalidOperationException ([string]$fixture.Options.ResolverThrowMessage))
            }
            if ($fixture.Options.ContainsKey('ResolverExceptionCode')) {
                throw ([string]$fixture.Options.ResolverExceptionCode)
            }
            if ($fixture.Options.ContainsKey('ToolchainMode')) {
                $mode = [string]$fixture.Options.ToolchainMode
                if ($mode -eq 'SdkFileShape') {
                    return [pscustomobject][ordered]@{ sdkPath = $fixture.SdkFile; adbPath = $fixture.AdbPath; javaPath = $fixture.JavaPath; apksignerJarPath = $fixture.ApkSignerPath }
                }
                if ($mode -eq 'AdbDirectoryShape') {
                    return [pscustomobject][ordered]@{ sdkPath = $fixture.SdkRoot; adbPath = $fixture.AdbDirectory; javaPath = $fixture.JavaPath; apksignerJarPath = $fixture.ApkSignerPath }
                }
                if ($mode -eq 'JavaDirectoryShape') {
                    return [pscustomobject][ordered]@{ sdkPath = $fixture.SdkRoot; adbPath = $fixture.AdbPath; javaPath = $fixture.JavaDirectory; apksignerJarPath = $fixture.ApkSignerPath }
                }
                if ($mode -eq 'ApkSignerDirectoryShape') {
                    return [pscustomobject][ordered]@{ sdkPath = $fixture.SdkRoot; adbPath = $fixture.AdbPath; javaPath = $fixture.JavaPath; apksignerJarPath = $fixture.ApkSignerDirectory }
                }
                if ($mode -eq 'AncestorReparseShape') {
                    return [pscustomobject][ordered]@{ sdkPath = [string]$fixture.Options.AncestorSdkPath; adbPath = $fixture.AdbPath; javaPath = $fixture.JavaPath; apksignerJarPath = $fixture.ApkSignerPath }
                }
            }
            $sdkPath = [string]$contract.SdkPath
            $javaPath = [string]$contract.JavaPath
            return Resolve-P5ERawToolchain -AndroidSdkPath $sdkPath -BuildToolsVersion '35.0.0' -JavaPath $javaPath
        }.GetNewClosure()
        ReadApproval = {
            param($contract)
            $fixture.State.Approval = [int]$fixture.State.Approval + 1
            if ($fixture.Options.ContainsKey('ApprovalThrowMessage')) {
                throw (New-Object System.InvalidOperationException ([string]$fixture.Options.ApprovalThrowMessage))
            }
            $state = if ($fixture.Options.ContainsKey('ApprovalState')) { [string]$fixture.Options.ApprovalState } else { 'VALUE' }
            $value = if ($fixture.Options.ContainsKey('ApprovalValue')) { [string]$fixture.Options.ApprovalValue } else { 'APPROVE_ONE_FRESH_EVENT' }
            return [pscustomobject][ordered]@{ State = $state; Value = $value }
        }.GetNewClosure()
        ReserveLauncher = {
            param($context)
            $fixture.State.Reserve = [int]$fixture.State.Reserve + 1
            if ($fixture.Options.ContainsKey('ReservationThrowCode')) { throw ([string]$fixture.Options.ReservationThrowCode) }
            return New-P5EA43ReservationMarker -Context $context
        }.GetNewClosure()
        CreateOwnerRoot = {
            param($context)
            $fixture.State.Owner = [int]$fixture.State.Owner + 1
            if ($fixture.Options.ContainsKey('OwnerThrowCode')) { throw ([string]$fixture.Options.OwnerThrowCode) }
            $path = Join-Path $fixture.PrivateRoot 'owner root fixture'
            if (Test-Path -LiteralPath $path) { throw 'P5E_OWNER_ROOT_ALREADY_EXISTS_STOP' }
            [void](New-Item -ItemType Directory -Path $path)
            return [pscustomobject][ordered]@{ Created = $true; Path = $path }
        }.GetNewClosure()
        CreateReceipt = {
            param($context)
            $fixture.State.Receipt = [int]$fixture.State.Receipt + 1
            if ($fixture.Options.ContainsKey('ReceiptThrowCode')) { throw ([string]$fixture.Options.ReceiptThrowCode) }
            $path = Join-Path $context.OwnerRootPath 'OWNER_DECISION_RECEIPT.json'
            $text = '{"schemaVersion":"p5e.synthetic.receipt.v1","binding":"fixture"}'
            Write-P5EA43CreateNewUtf8 -Path $path -Text $text
            return [pscustomobject][ordered]@{ Created = $true; Path = $path; Sha256 = Get-P5EA43Sha256Upper -Path $path }
        }.GetNewClosure()
        ValidateReceipt = {
            param($context)
            $fixture.State.ReceiptGate = [int]$fixture.State.ReceiptGate + 1
            if ($fixture.Options.ContainsKey('ReceiptCode')) {
                return [pscustomobject][ordered]@{ Valid = $false; TypedCode = [string]$fixture.Options.ReceiptCode }
            }
            return [pscustomobject][ordered]@{ Valid = $true; TypedCode = 'RECEIPT_CURRENT' }
        }.GetNewClosure()
        ReadKey = {
            param($context)
            $fixture.State.Key = [int]$fixture.State.Key + 1
            if ($fixture.Options.ContainsKey('KeyUnavailable') -and [bool]$fixture.Options.KeyUnavailable) { return $null }
            if ($fixture.Options.ContainsKey('KeyState')) { return [pscustomobject][ordered]@{ State = [string]$fixture.Options.KeyState } }
            return [pscustomobject][ordered]@{ State = 'VALUE'; Secret = 'SECRET_SENTINEL_MUST_NOT_ESCAPE' }
        }.GetNewClosure()
        InvokeChild = {
            param($context)
            $fixture.State.Child = [int]$fixture.State.Child + 1
            if ($fixture.Options.ContainsKey('ChildThrowMessage')) {
                throw (New-Object System.InvalidOperationException ([string]$fixture.Options.ChildThrowMessage))
            }
            $mode = if ($fixture.Options.ContainsKey('ChildMode')) { [string]$fixture.Options.ChildMode } else { 'Success' }
            if ($mode -eq 'StartFailure') {
                return [pscustomobject][ordered]@{ TypedCode = 'PROCESS_START_FAILED'; OuterExitCode = 1; ProcessCreated = 'FALSE'; ChildExitCode = $null }
            }
            if ($mode -eq 'Nonzero') {
                return [pscustomobject][ordered]@{ TypedCode = 'CHILD_EXIT_NONZERO'; OuterExitCode = 1; ProcessCreated = 'TRUE'; ChildExitCode = 7 }
            }
            return [pscustomobject][ordered]@{ TypedCode = 'CHILD_EXIT_ZERO'; OuterExitCode = 0; ProcessCreated = 'TRUE'; ChildExitCode = 0 }
        }.GetNewClosure()
        ClearKey = {
            param($context)
            $fixture.State.ClearKey = [int]$fixture.State.ClearKey + 1
            if ($fixture.Options.ContainsKey('CleanupThrowMessage')) {
                throw (New-Object System.InvalidOperationException ([string]$fixture.Options.CleanupThrowMessage))
            }
            return $true
        }.GetNewClosure()
        WriteDiagnostic = {
            param($path, $record)
            $fixture.State.WriteDiagnostic = [int]$fixture.State.WriteDiagnostic + 1
            if ($fixture.Options.ContainsKey('DiagnosticThrowMessage')) {
                throw (New-Object System.InvalidOperationException ([string]$fixture.Options.DiagnosticThrowMessage))
            }
            if ($fixture.Options.ContainsKey('DiagnosticReturnFalse') -and [bool]$fixture.Options.DiagnosticReturnFalse) { return $false }
            return Write-P5EA43DiagnosticCreateNew -Path $path -Record $record
        }.GetNewClosure()
    }
    return $fixture
}

function Assert-QAResult {
    param(
        [Parameter(Mandatory = $true)]$Result,
        [Parameter(Mandatory = $true)][string]$Code,
        [Parameter(Mandatory = $true)][string]$Stage,
        [int]$OuterExitCode = 1,
        [int]$ChildAttempts = 0,
        [int]$KeyPromptCount = 0,
        [bool]$ReservationCreated = $false,
        [bool]$OwnerRootCreated = $false,
        [bool]$ReceiptCreated = $false
    )
    Assert-QAEqual -Actual $Result.TypedCode -Expected $Code -Code 'RESULT_TYPED_CODE_MISMATCH'
    Assert-QAEqual -Actual $Result.Stage -Expected $Stage -Code 'RESULT_STAGE_MISMATCH'
    Assert-QAEqual -Actual $Result.OuterExitCode -Expected $OuterExitCode -Code 'RESULT_OUTER_EXIT_MISMATCH'
    Assert-QAEqual -Actual $Result.ChildAttempts -Expected $ChildAttempts -Code 'RESULT_CHILD_ATTEMPTS_MISMATCH'
    Assert-QAEqual -Actual $Result.KeyPromptCount -Expected $KeyPromptCount -Code 'RESULT_KEY_PROMPT_MISMATCH'
    Assert-QAEqual -Actual $Result.ReservationCreated -Expected $ReservationCreated -Code 'RESULT_RESERVATION_MISMATCH'
    Assert-QAEqual -Actual $Result.OwnerRootCreated -Expected $OwnerRootCreated -Code 'RESULT_OWNER_ROOT_MISMATCH'
    Assert-QAEqual -Actual $Result.ReceiptCreated -Expected $ReceiptCreated -Code 'RESULT_RECEIPT_MISMATCH'
    Assert-QA -Condition ([bool]$Result.NonzeroExit -eq ($OuterExitCode -ne 0)) -Code 'RESULT_NONZERO_FLAG_MISMATCH'
}

function Assert-QANoExternalActions {
    param([Parameter(Mandatory = $true)]$Fixture)
    Assert-QAEqual -Actual $Fixture.State.LiveCalls -Expected 0 -Code 'SYNTHETIC_LIVE_CALL_OBSERVED'
}

function New-QAReparseJunction {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Target
    )
    try {
        New-Item -ItemType Junction -Path $Path -Target $Target -ErrorAction Stop | Out-Null
    } catch {
        throw 'REPARSE_FIXTURE_CREATE_FAILED'
    }
}

function Invoke-QACase {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [hashtable]$Options = @{},
        [scriptblock]$Setup,
        [Parameter(Mandatory = $true)][scriptblock]$Assert
    )
    $fixture = New-QAFixture -Name $Name -Options $Options
    $result = $null
    try {
        if ($null -ne $Setup) { & $Setup $fixture }
        $result = Invoke-P5EA43PreReservation -Contract $fixture.Contract -Dependencies $fixture.Dependencies
        & $Assert $fixture $result
        Assert-QANoExternalActions -Fixture $fixture
        $resultText = $result | ConvertTo-Json -Depth 12 -Compress
        Assert-QANotContains -Text $resultText -Sentinel 'SECRET_SENTINEL' -Code 'SECRET_IN_RESULT'
        Assert-QANotContains -Text $resultText -Sentinel 'REMOTE_EXCEPTION_SENTINEL' -Code 'RAW_EXCEPTION_IN_RESULT'
        if (Test-Path -LiteralPath $fixture.AuditPath -PathType Leaf) {
            $auditText = Get-Content -Raw -LiteralPath $fixture.AuditPath
            Assert-QANotContains -Text $auditText -Sentinel 'SECRET_SENTINEL' -Code 'SECRET_IN_AUDIT'
            Assert-QANotContains -Text $auditText -Sentinel 'REMOTE_EXCEPTION_SENTINEL' -Code 'RAW_EXCEPTION_IN_AUDIT'
        }
        [void]$results.Add([ordered]@{
                name = $Name
                status = 'PASS'
                typedCode = [string]$result.TypedCode
                stage = [string]$result.Stage
                outerExitCode = [int]$result.OuterExitCode
                reservationCreated = [bool]$result.ReservationCreated
                ownerRootCreated = [bool]$result.OwnerRootCreated
                receiptCreated = [bool]$result.ReceiptCreated
                keyPromptCount = [int]$result.KeyPromptCount
                childAttempts = [int]$result.ChildAttempts
                diagnosticStatus = [string]$result.DiagnosticStatus
            })
    } catch {
        $safeTyped = if ($null -ne $result) { [string]$result.TypedCode } else { 'NO_RESULT' }
        $safeStage = if ($null -ne $result) { [string]$result.Stage } else { 'NO_STAGE' }
        Write-Output ('QA_FAIL=' + $Name + ';typed=' + $safeTyped + ';stage=' + $safeStage + ';exceptionClass=' + (Get-P5EA43SafeExceptionClass -ErrorRecord $_))
        [void]$results.Add([ordered]@{ name = $Name; status = 'FAIL'; exceptionClass = Get-P5EA43SafeExceptionClass -ErrorRecord $_ })
    } finally {
        if (Test-Path -LiteralPath $fixture.Root) {
            Remove-Item -LiteralPath $fixture.Root -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

function Invoke-QAProcessExitFixture {
    $fixture = New-QAFixture -Name 'process exit actual fixture'
    $fixtureScriptPath = Join-Path $fixture.Root 'process exit fixture.ps1'
    $template = @'
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. '__SOURCE__' -LibraryOnly
$fixtureRoot = '__ROOT__'
$repo = '__REPO__'
$private = '__PRIVATE__'
$sdk = '__SDK__'
$adb = '__ADB__'
$java = '__JAVA__'
$jar = '__JAR__'
$launcher = '__LAUNCHER__'
$reservation = '__RESERVATION__'
$audit = '__AUDIT__'
$deps = @{
    ResolveToolchain = { param($c) return [pscustomobject][ordered]@{ sdkPath = $sdk; adbPath = $adb; javaPath = $java; apksignerJarPath = $jar } }
    ReadApproval = { param($c) return [pscustomobject][ordered]@{ State = 'VALUE'; Value = 'APPROVE_ONE_FRESH_EVENT' } }
    ReserveLauncher = { param($c) return New-P5EA43ReservationMarker -Context $c }
    CreateOwnerRoot = { param($c) $path = Join-Path $private 'owner'; [void](New-Item -ItemType Directory -Path $path); return [pscustomobject][ordered]@{ Created = $true; Path = $path } }
    CreateReceipt = { param($c) $path = Join-Path $c.OwnerRootPath 'receipt.json'; Write-P5EA43CreateNewUtf8 -Path $path -Text '{"receipt":"synthetic"}'; return [pscustomobject][ordered]@{ Created = $true; Path = $path; Sha256 = Get-P5EA43Sha256Upper -Path $path } }
    ValidateReceipt = { param($c) return [pscustomobject][ordered]@{ Valid = $true; TypedCode = 'RECEIPT_CURRENT' } }
    ReadKey = { param($c) return [pscustomobject][ordered]@{ State = 'VALUE'; Secret = 'SECRET_SENTINEL_MUST_NOT_ESCAPE' } }
    InvokeChild = { param($c) return [pscustomobject][ordered]@{ TypedCode = 'CHILD_EXIT_NONZERO'; OuterExitCode = 1; ProcessCreated = 'TRUE'; ChildExitCode = 7 } }
    ClearKey = { param($c) return $true }
    WriteDiagnostic = { param($path, $record) return Write-P5EA43DiagnosticCreateNew -Path $path -Record $record }
}
$contract = @{
    RepoRoot = $repo
    PrivateRoot = $private
    PowershellPath = '__PS51__'
    LauncherPath = $launcher
    ReservationRoot = $reservation
    AuditPath = $audit
    SdkPath = $sdk
    JavaPath = $java
    ApprovalLiteral = 'APPROVE_ONE_FRESH_EVENT'
    PinnedFiles = @()
    ExpectedLauncherSha256 = Get-P5EA43Sha256Upper -Path $launcher
}
$result = Invoke-P5EA43PreReservation -Contract $contract -Dependencies $deps
$result | ConvertTo-Json -Depth 12 -Compress
exit ([int]$result.OuterExitCode)
'@
    function ConvertTo-QASingleQuotedPath { param([string]$Value) return ("'" + $Value.Replace("'", "''") + "'") }
    $template = $template.Replace("'__SOURCE__'", (ConvertTo-QASingleQuotedPath -Value $sourcePath))
    $template = $template.Replace("'__ROOT__'", (ConvertTo-QASingleQuotedPath -Value $fixture.Root))
    $template = $template.Replace("'__REPO__'", (ConvertTo-QASingleQuotedPath -Value $fixture.RepoRoot))
    $template = $template.Replace("'__PRIVATE__'", (ConvertTo-QASingleQuotedPath -Value $fixture.PrivateRoot))
    $template = $template.Replace("'__SDK__'", (ConvertTo-QASingleQuotedPath -Value $fixture.SdkRoot))
    $template = $template.Replace("'__ADB__'", (ConvertTo-QASingleQuotedPath -Value $fixture.AdbPath))
    $template = $template.Replace("'__JAVA__'", (ConvertTo-QASingleQuotedPath -Value $fixture.JavaPath))
    $template = $template.Replace("'__JAR__'", (ConvertTo-QASingleQuotedPath -Value $fixture.ApkSignerPath))
    $template = $template.Replace("'__LAUNCHER__'", (ConvertTo-QASingleQuotedPath -Value $fixture.LauncherPath))
    $template = $template.Replace("'__RESERVATION__'", (ConvertTo-QASingleQuotedPath -Value $fixture.ReservationRoot))
    $template = $template.Replace("'__AUDIT__'", (ConvertTo-QASingleQuotedPath -Value $fixture.AuditPath))
    $template = $template.Replace("'__PS51__'", (ConvertTo-QASingleQuotedPath -Value $ps51Path))
    [IO.File]::WriteAllText($fixtureScriptPath, $template, [Text.UTF8Encoding]::new($false))
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $ps51Path
    $psi.Arguments = '-NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "' + $fixtureScriptPath + '"'
    $psi.WorkingDirectory = $repoRoot
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    try {
        Assert-QA -Condition $process.Start() -Code 'PROCESS_FIXTURE_START_FAILED'
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        Assert-QAEqual -Actual $process.ExitCode -Expected 1 -Code 'PROCESS_EXIT_NOT_NONZERO'
        Assert-QANotContains -Text ($stdout + $stderr) -Sentinel 'SECRET_SENTINEL' -Code 'SECRET_IN_PROCESS_OUTPUT'
        Assert-QANotContains -Text ($stdout + $stderr) -Sentinel 'REMOTE_EXCEPTION_SENTINEL' -Code 'RAW_EXCEPTION_IN_PROCESS_OUTPUT'
        Assert-QA -Condition ($stdout.Contains('CHILD_EXIT_NONZERO')) -Code 'PROCESS_FIXTURE_RESULT_MISSING'
        [void]$results.Add([ordered]@{ name = 'actual-process-exit-nonzero'; status = 'PASS'; exitCode = [int]$process.ExitCode; stdoutBytes = ([Text.Encoding]::UTF8.GetByteCount($stdout)); stderrBytes = ([Text.Encoding]::UTF8.GetByteCount($stderr)) })
    } finally {
        $process.Dispose()
        if (Test-Path -LiteralPath $fixture.Root) { Remove-Item -LiteralPath $fixture.Root -Recurse -Force -ErrorAction SilentlyContinue }
    }
}

try {
    Invoke-QACase -Name 'sdk-directory-green' -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'CHILD_EXIT_ZERO' -Stage 'CHILD_RESULT' -OuterExitCode 0 -ChildAttempts 1 -KeyPromptCount 1 -ReservationCreated $true -OwnerRootCreated $true -ReceiptCreated $true
        Assert-QA -Condition ($f.State.Resolve -eq 1 -and $f.State.Reserve -eq 1 -and $f.State.Child -eq 1) -Code 'GREEN_DEPENDENCY_COUNTS_WRONG'
        Assert-QA -Condition ((Test-Path -LiteralPath $r.ReservationPath -PathType Leaf) -and (Test-Path -LiteralPath $f.AuditPath -PathType Leaf)) -Code 'GREEN_ARTIFACTS_MISSING'
    }
    Invoke-QACase -Name 'sdk-file-real-resolver-red' -Setup { param($f) $f.Contract.SdkPath = $f.SdkFile } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_RAW_TOOLCHAIN_SDK_MISSING_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
        Assert-QA -Condition ($f.State.Approval -eq 0 -and $f.State.Reserve -eq 0 -and $f.State.Key -eq 0 -and $f.State.Child -eq 0) -Code 'SDK_FILE_PROGRESS_LEAK'
    }
    Invoke-QACase -Name 'sdk-file-parent-shape-red' -Options @{ ToolchainMode = 'SdkFileShape' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_TOOLCHAIN_SDK_NOT_DIRECTORY_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
    }
    Invoke-QACase -Name 'adb-directory-parent-shape-red' -Options @{ ToolchainMode = 'AdbDirectoryShape' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_TOOLCHAIN_ADB_NOT_REGULAR_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
    }
    Invoke-QACase -Name 'java-directory-parent-shape-red' -Options @{ ToolchainMode = 'JavaDirectoryShape' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_TOOLCHAIN_JAVA_NOT_REGULAR_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
    }
    Invoke-QACase -Name 'apksigner-directory-parent-shape-red' -Options @{ ToolchainMode = 'ApkSignerDirectoryShape' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_TOOLCHAIN_APKSIGNER_NOT_REGULAR_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
    }
    Invoke-QACase -Name 'missing-java-real-resolver-red' -Setup { param($f) $f.Contract.JavaPath = Join-Path $f.Root 'missing java.exe' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_RAW_TOOLCHAIN_JAVA_MISSING_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
    }
    Invoke-QACase -Name 'sdk-reparse-real-resolver-red' -Setup {
        param($f)
        $junction = Join-Path $f.Root 'SDK reparse junction'
        New-QAReparseJunction -Path $junction -Target $f.SdkRoot
        $f.Contract.SdkPath = $junction
    } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_RAW_TOOLCHAIN_SDK_REPARSE_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
    }
    Invoke-QACase -Name 'sdk-ancestor-reparse-parent-guard-red' -Setup {
        param($f)
        $target = Join-Path $f.Root 'ancestor target'
        $targetSdk = Join-Path $target 'sdk'
        [void](New-Item -ItemType Directory -Path $targetSdk -Force)
        $junction = Join-Path $f.Root 'ancestor reparse junction'
        New-QAReparseJunction -Path $junction -Target $target
        $f.Options.ToolchainMode = 'AncestorReparseShape'
        $f.Options.AncestorSdkPath = Join-Path $junction 'sdk'
    } -Assert {
        param($f, $r)
        Assert-QA -Condition ($r.TypedCode -eq 'P5E_TOOLCHAIN_SDK_ANCESTOR_REPARSE_STOP' -or $r.TypedCode -eq 'P5E_TOOLCHAIN_SDK_PATH_SWAP_STOP') -Code 'ANCESTOR_REPARSE_NOT_TYPED'
        Assert-QAEqual -Actual $r.Stage -Expected 'TOOLCHAIN_PREFLIGHT' -Code 'ANCESTOR_REPARSE_STAGE_WRONG'
    }
    Invoke-QACase -Name 'approval-wrong-does-not-reserve' -Options @{ ApprovalValue = 'WRONG_APPROVAL_SECRET_SENTINEL' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP' -Stage 'APPROVAL'
        Assert-QA -Condition ($f.State.Reserve -eq 0 -and $f.State.Owner -eq 0 -and $f.State.Receipt -eq 0 -and $f.State.Key -eq 0 -and $f.State.Child -eq 0) -Code 'WRONG_APPROVAL_SIDE_EFFECT'
    }
    Invoke-QACase -Name 'approval-empty-does-not-reserve' -Options @{ ApprovalValue = '' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP' -Stage 'APPROVAL'
        Assert-QAEqual -Actual $f.State.Reserve -Expected 0 -Code 'EMPTY_APPROVAL_RESERVED'
    }
    Invoke-QACase -Name 'approval-eof-does-not-reserve' -Options @{ ApprovalState = 'EOF'; ApprovalValue = '' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_OWNER_APPROVAL_INPUT_EOF_STOP' -Stage 'APPROVAL'
        Assert-QAEqual -Actual $f.State.Key -Expected 0 -Code 'EOF_PROMPTED_KEY'
    }
    Invoke-QACase -Name 'approval-cancel-does-not-reserve' -Options @{ ApprovalState = 'CANCELLED'; ApprovalValue = '' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_OWNER_APPROVAL_INPUT_CANCELLED_STOP' -Stage 'APPROVAL'
        Assert-QAEqual -Actual $f.State.Reserve -Expected 0 -Code 'CANCEL_APPROVAL_RESERVED'
    }
    Invoke-QACase -Name 'unknown-preflight-redacts-message' -Options @{ ResolverThrowMessage = 'REMOTE_EXCEPTION_SENTINEL_WITH_SECRET' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'UNKNOWN_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
        Assert-QA -Condition ($r.ExceptionClass -eq 'InvalidOperationException') -Code 'UNKNOWN_EXCEPTION_CLASS_MISSING'
    }
    Invoke-QACase -Name 'launcher-hash-mismatch-before-reservation' -Setup { param($f) $f.Contract.ExpectedLauncherSha256 = [string]::new('0', 64) } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_LAUNCHER_HASH_MISMATCH_STOP' -Stage 'LAUNCHER_RESERVATION'
        Assert-QA -Condition ($f.State.Reserve -eq 0 -and $f.State.Child -eq 0) -Code 'HASH_MISMATCH_RESERVED'
    }
    Invoke-QACase -Name 'reservation-collision-no-overwrite' -Setup {
        param($f)
        $binding = Get-P5EA43LauncherBinding -LauncherPath $f.LauncherPath -ReservationRoot $f.ReservationRoot
        Write-P5EA43CreateNewUtf8 -Path $binding.ReservationPath -Text 'immutable collision fixture'
        $f.Options.PreexistingReservationText = 'immutable collision fixture'
    } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_A43_LAUNCHER_RESERVATION_ALREADY_EXISTS_STOP' -Stage 'LAUNCHER_RESERVATION'
        Assert-QAEqual -Actual (Get-Content -Raw -LiteralPath $r.ReservationPath) -Expected 'immutable collision fixture' -Code 'COLLISION_MARKER_OVERWRITTEN'
        Assert-QA -Condition ($f.State.Owner -eq 0 -and $f.State.Key -eq 0 -and $f.State.Child -eq 0) -Code 'COLLISION_PROGRESS_LEAK'
    }
    Invoke-QACase -Name 'owner-root-failure-preserves-reservation-stop' -Options @{ OwnerThrowCode = 'P5E_OWNER_ROOT_CREATE_FAILED_STOP' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_OWNER_ROOT_CREATE_FAILED_STOP' -Stage 'OWNER_ROOT' -ReservationCreated $true
        Assert-QA -Condition ($f.State.Receipt -eq 0 -and $f.State.Key -eq 0 -and $f.State.Child -eq 0) -Code 'OWNER_FAILURE_PROGRESS_LEAK'
    }
    Invoke-QACase -Name 'receipt-failure-preserves-origin' -Options @{ ReceiptThrowCode = 'P5E_OWNER_RECEIPT_CREATE_FAILED_STOP' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_OWNER_RECEIPT_CREATE_FAILED_STOP' -Stage 'RECEIPT' -ReservationCreated $true -OwnerRootCreated $true
        Assert-QAEqual -Actual $f.State.Key -Expected 0 -Code 'RECEIPT_FAILURE_PROMPTED_KEY'
    }
    Invoke-QACase -Name 'expired-receipt-no-key-or-child' -Options @{ ReceiptCode = 'RECEIPT_EXPIRED_STOP' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'RECEIPT_EXPIRED_STOP' -Stage 'RECEIPT_VALIDATION' -ReservationCreated $true -OwnerRootCreated $true -ReceiptCreated $true
        Assert-QA -Condition ($f.State.Key -eq 0 -and $f.State.Child -eq 0) -Code 'EXPIRED_RECEIPT_PROGRESS_LEAK'
    }
    Invoke-QACase -Name 'key-unavailable-after-receipt' -Options @{ KeyUnavailable = $true } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_OWNER_KEY_INPUT_UNAVAILABLE_STOP' -Stage 'KEY' -ReservationCreated $true -OwnerRootCreated $true -ReceiptCreated $true -KeyPromptCount 1
        Assert-QAEqual -Actual $f.State.Child -Expected 0 -Code 'KEY_FAILURE_CHILD_STARTED'
    }
    Invoke-QACase -Name 'child-start-failure-terminal' -Options @{ ChildMode = 'StartFailure' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'PROCESS_START_FAILED' -Stage 'CHILD_RESULT' -ReservationCreated $true -OwnerRootCreated $true -ReceiptCreated $true -KeyPromptCount 1 -ChildAttempts 1
        Assert-QAEqual -Actual $r.ChildExitCode -Expected '' -Code 'START_FAILURE_EXIT_NOT_UNKNOWN'
    }
    Invoke-QACase -Name 'child-nonzero-terminal' -Options @{ ChildMode = 'Nonzero' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'CHILD_EXIT_NONZERO' -Stage 'CHILD_RESULT' -ReservationCreated $true -OwnerRootCreated $true -ReceiptCreated $true -KeyPromptCount 1 -ChildAttempts 1
        Assert-QAEqual -Actual $r.ChildExitCode -Expected 7 -Code 'CHILD_NONZERO_EXIT_NOT_RETAINED'
    }
    Invoke-QACase -Name 'cleanup-failure-keeps-child-origin' -Options @{ ChildMode = 'Nonzero'; CleanupThrowMessage = 'REMOTE_EXCEPTION_SENTINEL_CLEANUP' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'CHILD_EXIT_NONZERO' -Stage 'CHILD_RESULT' -OuterExitCode 1 -ChildAttempts 1 -KeyPromptCount 1 -ReservationCreated $true -OwnerRootCreated $true -ReceiptCreated $true
        Assert-QA -Condition ($r.SecondaryStatus.Contains('CLEANUP_FAILED:InvalidOperationException')) -Code 'CLEANUP_SECONDARY_MISSING'
        Assert-QANotContains -Text $r.SecondaryStatus -Sentinel 'REMOTE_EXCEPTION_SENTINEL' -Code 'CLEANUP_RAW_MESSAGE_LEAK'
    }
    Invoke-QACase -Name 'diagnostic-write-failure-is-incomplete' -Options @{ ApprovalValue = 'WRONG_APPROVAL_SECRET_SENTINEL'; DiagnosticThrowMessage = 'REMOTE_EXCEPTION_SENTINEL_DIAGNOSTIC' } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP' -Stage 'APPROVAL'
        Assert-QAEqual -Actual $r.DiagnosticStatus -Expected 'EVIDENCE_INCOMPLETE' -Code 'DIAGNOSTIC_FAILURE_NOT_MARKED'
        Assert-QA -Condition (-not $r.DiagnosticWritten) -Code 'DIAGNOSTIC_FAILURE_WRITTEN_FLAG'
    }
    Invoke-QACase -Name 'success-diagnostic-false-is-incomplete' -Options @{ DiagnosticReturnFalse = $true } -Assert {
        param($f, $r)
        Assert-QAResult -Result $r -Code 'EVIDENCE_INCOMPLETE' -Stage 'DIAGNOSTIC' -OuterExitCode 1 -ChildAttempts 1 -KeyPromptCount 1 -ReservationCreated $true -OwnerRootCreated $true -ReceiptCreated $true
        Assert-QAEqual -Actual $r.DiagnosticStatus -Expected 'EVIDENCE_INCOMPLETE' -Code 'DIAGNOSTIC_FALSE_NOT_TERMINAL'
    }
    Invoke-QAProcessExitFixture

    $sourceHash = Get-P5EA43Sha256Upper -Path $sourcePath
    $toolchainHash = Get-P5EA43Sha256Upper -Path $toolchainPath
    $failed = @($results | Where-Object { $_.status -ne 'PASS' })
    $report = [ordered]@{
        schema = 'p5e.a43.pre-reservation.repair-qa.v1'
        observedAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
        status = if ($failed.Count -eq 0) { 'PASS' } else { 'FAILED_REPAIRING' }
        source = [ordered]@{ path = 'scripts/p5e-a43-pre-reservation-launcher.ps1'; sha256 = $sourceHash }
        resolver = [ordered]@{ path = 'scripts/p5e-raw-toolchain.ps1'; sha256 = $toolchainHash }
        testSource = [ordered]@{ path = 'scripts/test-p5e-a43-pre-reservation-launcher.ps1'; sha256 = Get-P5EA43Sha256Upper -Path $PSCommandPath }
        runtime = [ordered]@{ powershell = $PSVersionTable.PSVersion.ToString(); ps51Path = $ps51Path; platform = $env:OS }
        cases = @($results)
        passed = @($results | Where-Object { $_.status -eq 'PASS' }).Count
        failed = $failed.Count
        liveActions = 0
        liveBoundary = 'NOT_EVALUATED'
        candidateExecuted = $false
        ownerDecisionCreated = $false
        receiptOrEventCreated = $false
        providerDatabaseCredentialCalls = 0
        inheritedEvidenceReusedAsLive = $false
    }
    if (-not [string]::IsNullOrWhiteSpace($ReportPath)) {
        $reportFullPath = [IO.Path]::GetFullPath($ReportPath)
        if (Test-Path -LiteralPath $reportFullPath) { throw 'QA_REPORT_ALREADY_EXISTS_STOP' }
        $reportParent = [IO.Directory]::GetParent($reportFullPath).FullName
        if (-not (Test-Path -LiteralPath $reportParent -PathType Container)) { [void](New-Item -ItemType Directory -Path $reportParent -Force) }
        Write-P5EA43CreateNewUtf8 -Path $reportFullPath -Text ($report | ConvertTo-Json -Depth 12)
    }
    $report | ConvertTo-Json -Depth 12
    if ($failed.Count -ne 0) { exit 1 }
    exit 0
} finally {
    if (Test-Path -LiteralPath $qaRoot) { Remove-Item -LiteralPath $qaRoot -Recurse -Force -ErrorAction SilentlyContinue }
}
