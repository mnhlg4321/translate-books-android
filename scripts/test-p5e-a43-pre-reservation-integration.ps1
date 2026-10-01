[CmdletBinding()]
param(
    [string]$ReportPath = '',
    [ValidateSet('', 'Success', 'ChildFailure')][string]$ProcessProbe = '',
    # Console probe: run one fixture with the entrypoint's real Read-Host
    # approval/key readers inside a real console (input is injected by
    # test-p5e-a43-entry-console.ps1). Child, SDK and private root stay synthetic.
    [switch]$ConsoleProbe,
    [string]$ProbeDirectory = ''
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$entrypointPath = Join-Path $PSScriptRoot 'p5e-a43-pre-reservation-launcher-entrypoint.ps1'
$parentPath = Join-Path $PSScriptRoot 'p5e-a43-pre-reservation-launcher.ps1'
$resolverPath = Join-Path $PSScriptRoot 'p5e-raw-toolchain.ps1'
$childPath = Join-Path $PSScriptRoot 'p5e-child-invocation-contract.ps1'
$runtimeGuardPath = Join-Path $PSScriptRoot 'p5e-a43-runtime-guards.ps1'
$digestPath = Join-Path $PSScriptRoot 'p5e-load-expected-digest.ps1'
$ps51Path = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'

foreach ($path in @($entrypointPath, $parentPath, $resolverPath, $childPath, $runtimeGuardPath, $digestPath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'P5E_A43_INTEGRATION_SOURCE_MISSING_STOP' }
}
foreach ($path in @($entrypointPath, $parentPath, $resolverPath, $childPath, $runtimeGuardPath, $digestPath)) {
    $tokens = $null
    $parseErrors = $null
    [System.Management.Automation.Language.Parser]::ParseFile($path, [ref]$tokens, [ref]$parseErrors) | Out-Null
    if ($parseErrors.Count -ne 0) { throw 'P5E_A43_INTEGRATION_SOURCE_PARSE_FAILED' }
}

# This loads the exact candidate source and its pinned libraries.  It does not
# execute the entrypoint because -Execute is intentionally absent.
. $entrypointPath -LibraryOnly -RepoRoot $repoRoot
$pinTable = Get-P5EA43IntegrationPinTable -Root $repoRoot
$candidateHash = Get-P5EA43IntegrationSha256 -Path $entrypointPath
$testSourceHashBeforeReport = Get-P5EA43IntegrationSha256 -Path $MyInvocation.MyCommand.Path
$qaRoot = Join-Path ([IO.Path]::GetTempPath()) ('p5e-a43-parent-integration-qa-' + [Guid]::NewGuid().ToString('N') + ' with spaces')
[void](New-Item -ItemType Directory -Path $qaRoot)
$caseRecords = New-Object 'System.Collections.Generic.List[object]'
$failureCount = 0

function Assert-Integration {
    param([Parameter(Mandatory = $true)][bool]$Condition,[Parameter(Mandatory = $true)][string]$Code)
    if (-not $Condition) { throw $Code }
}

function Assert-IntegrationEqual {
    param([AllowNull()][object]$Actual,[AllowNull()][object]$Expected,[Parameter(Mandatory = $true)][string]$Code)
    Assert-Integration -Condition ([string]$Actual -ceq [string]$Expected) -Code $Code
}

function Assert-IntegrationContains {
    param([AllowNull()][string]$Text,[Parameter(Mandatory = $true)][string]$Needle,[Parameter(Mandatory = $true)][string]$Code)
    Assert-Integration -Condition ($null -ne $Text -and $Text.Contains($Needle)) -Code $Code
}

function Assert-IntegrationNotContains {
    param([AllowNull()][string]$Text,[Parameter(Mandatory = $true)][string]$Needle,[Parameter(Mandatory = $true)][string]$Code)
    Assert-Integration -Condition ($null -eq $Text -or -not $Text.Contains($Needle)) -Code $Code
}

function Get-IntegrationSecretDigest {
    $secure = ConvertTo-SecureString -String 'synthetic-api-key-sentinel' -AsPlainText -Force
    try { return Get-P5EEndpointAccountFingerprintFromSecureKey -Endpoint 'https://openrouter.ai/api/v1/chat/completions' -ApiKey $secure }
    finally { $secure.Dispose() }
}

$sensitiveDigest = Get-IntegrationSecretDigest

function New-IntegrationFixture {
    param([Parameter(Mandatory = $true)][string]$Name,[hashtable]$Options = @{})
    Clear-P5EA43IntegrationEnvironment
    $root = Join-Path $qaRoot ($Name + ' fixture')
    $private = Join-Path $root 'private root with spaces'
    $auditRoot = Join-Path $root 'audit root with spaces'
    $reservation = Join-Path $private 'reservation root with spaces'
    $childWorking = Join-Path $root 'child working directory with spaces'
    $sdk = Join-Path $root 'Android SDK with spaces'
    $platformTools = Join-Path $sdk 'platform-tools'
    $buildTools = Join-Path $sdk 'build-tools\35.0.0\lib'
    $javaRoot = Join-Path $root 'Java home with spaces'
    foreach ($directory in @($root, $private, $auditRoot, $reservation, $childWorking, $platformTools, $buildTools, $javaRoot)) {
        [void](New-Item -ItemType Directory -Path $directory -Force)
    }
    $adb = Join-Path $platformTools 'adb.exe'
    $java = Join-Path $javaRoot 'java.exe'
    $jar = Join-Path $buildTools 'apksigner.jar'
    [IO.File]::WriteAllText($adb, 'synthetic adb binary placeholder')
    [IO.File]::WriteAllText($java, 'synthetic java binary placeholder')
    [IO.File]::WriteAllText($jar, 'synthetic apksigner jar placeholder')
    $fakeChild = Join-Path $root 'synthetic child.ps1'
    $fakeChildText = @'
[CmdletBinding()]
param([string]$Mode = 'exit0')
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
switch ($Mode) {
    'exit0' { exit 0 }
    'env-present' {
        $value = [Environment]::GetEnvironmentVariable('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT', 'Process')
        if ($value -match '^[0-9a-f]{64}$') { exit 0 }
        exit 42
    }
    'exit7-stderr' { [Console]::Error.Write('REMOTE_EXCEPTION_SENTINEL'); [Console]::Error.Flush(); exit 7 }
    'exit7-empty' { exit 7 }
    'timeout' { Start-Sleep -Milliseconds 1000; exit 0 }
    'output-large' { [Console]::Out.Write(('OUTPUT_SENTINEL' * 2048)); [Console]::Out.Flush(); exit 0 }
    default { exit 31 }
}
'@
    [IO.File]::WriteAllText($fakeChild, $fakeChildText, [Text.UTF8Encoding]::new($false))
    $state = New-Object hashtable
    $state.LiveCalls = 0
    $state.ResolverSpyCalls = 0
    $state.NowProviderCalls = 0
    $state.NowProviderValues = @()
    $state.NowIndex = 0
    $fixture = [pscustomobject][ordered]@{
        Name = $Name
        Root = $root
        PrivateRoot = $private
        AuditRoot = $auditRoot
        ReservationRoot = $reservation
        ChildWorkingDirectory = $childWorking
        SdkRoot = $sdk
        SdkFile = Join-Path $root 'sdk file instead of directory'
        AdbPath = $adb
        JavaPath = $java
        ApkSignerPath = $jar
        FakeChildPath = $fakeChild
        Ps51Path = $ps51Path
        Options = $Options
        State = $state
        Contract = $null
        Dependencies = $null
        Seams = $null
        CollisionMarkerBytes = ''
        SensitiveDigest = $sensitiveDigest
    }
    [IO.File]::WriteAllText($fixture.SdkFile, 'not an SDK directory')
    $fixture.Options = $Options
    $config = @{
        RepoRoot = $repoRoot
        PrivateRoot = $fixture.PrivateRoot
        PowershellPath = $fixture.Ps51Path
        LauncherPath = $entrypointPath
        ReservationRoot = $fixture.ReservationRoot
        AuditPath = Join-Path $fixture.AuditRoot 'pre-reservation-diagnostic.json'
        AndroidSdkPath = $fixture.SdkRoot
        LocalPropertiesPath = ''
        AdbPath = ''
        JavaPath = $fixture.JavaPath
        ApkSignerJarPath = ''
        BuildToolsVersion = '35.0.0'
        DecisionId = 'synthetic-decision-' + $Name.Replace(' ', '-')
        PacketIdentifier = 'P5E-A43-INTEGRATION-SYNTHETIC'
        Serial = '15e84958'
        Scope = 'RAW/GLOSSARY'
        CommandSourcePath = $pinTable.Command.Path
        CommandSourceSha256 = $pinTable.Command.Sha256
        ChildArgumentList = @()
        ChildWorkingDirectory = $fixture.ChildWorkingDirectory
        ReceiptValidityMilliseconds = if ($Options.ContainsKey('ReceiptValidityMilliseconds')) { [long]$Options.ReceiptValidityMilliseconds } else { 1800000L }
    }
    $fixture.Contract = New-P5EA43ExecutableContract -Configuration $config -CandidateSha256 $candidateHash -PinTable $pinTable

    $nowValues = if ($Options.ContainsKey('NowValues')) { @($Options.NowValues) } else { @() }
    $nowProvider = {
        $fixture.State.NowProviderCalls = [int]$fixture.State.NowProviderCalls + 1
        if ($nowValues.Count -gt 0) {
            $index = [Math]::Min([int]$fixture.State.NowIndex, $nowValues.Count - 1)
            $fixture.State.NowIndex = [int]$fixture.State.NowIndex + 1
            $value = [long]$nowValues[$index]
            $fixture.State.NowProviderValues += $value
            return $value
        }
        $value = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $fixture.State.NowProviderValues += $value
        return $value
    }.GetNewClosure()

    $realResolver = {
        param($contract)
        return Resolve-P5ERawToolchain -AndroidSdkPath ([string]$contract.SdkPath) -LocalPropertiesPath ([string]$contract.LocalPropertiesPath) -AdbPath ([string]$contract.AdbPath) -JavaPath ([string]$contract.JavaPath) -ApkSignerJarPath ([string]$contract.ApkSignerJarPath) -BuildToolsVersion ([string]$contract.BuildToolsVersion)
    }.GetNewClosure()
    $resolverInvoker = {
        param($contract)
        $fixture.State.ResolverSpyCalls = [int]$fixture.State.ResolverSpyCalls + 1
        if ($fixture.Options.ContainsKey('ResolverThrowSentinel')) { throw (New-Object System.InvalidOperationException ([string]$fixture.Options.ResolverThrowSentinel)) }
        if ($fixture.Options.ContainsKey('ResolverOutputMode')) {
            switch ([string]$fixture.Options.ResolverOutputMode) {
                'SdkFile' { return [pscustomobject][ordered]@{ sdkPath = $fixture.SdkFile; adbPath = $fixture.AdbPath; javaPath = $fixture.JavaPath; apksignerJarPath = $fixture.ApkSignerPath } }
                'AdbDirectory' { return [pscustomobject][ordered]@{ sdkPath = $fixture.SdkRoot; adbPath = $fixture.Root; javaPath = $fixture.JavaPath; apksignerJarPath = $fixture.ApkSignerPath } }
            }
        }
        return & $realResolver $contract
    }.GetNewClosure()

    $approvalReader = {
        param($prompt)
        if ($fixture.Options.ContainsKey('ApprovalState')) { return [pscustomobject][ordered]@{ State = [string]$fixture.Options.ApprovalState } }
        if ($fixture.Options.ContainsKey('ApprovalValue')) { return [string]$fixture.Options.ApprovalValue }
        return (ConvertTo-SecureString -String 'APPROVE_ONE_FRESH_EVENT' -AsPlainText -Force)
    }.GetNewClosure()
    $keyReader = {
        param($prompt)
        if ($fixture.Options.ContainsKey('KeyState')) { return [pscustomobject][ordered]@{ State = [string]$fixture.Options.KeyState } }
        return (ConvertTo-SecureString -String 'synthetic-api-key-sentinel' -AsPlainText -Force)
    }.GetNewClosure()
    $seams = @{
        ResolveToolchainInvoker = $resolverInvoker
        ApprovalReader = $approvalReader
        KeyReader = $keyReader
        NowProvider = $nowProvider
    }
    if ($Options.ContainsKey('RealConsoleReaders') -and [bool]$Options.RealConsoleReaders) {
        $seams.Remove('ApprovalReader')
        $seams.Remove('KeyReader')
    }
    if ($Options.ContainsKey('FingerprintLoaderMode') -and [string]$Options.FingerprintLoaderMode -eq 'PartialFailure') {
        $seams.FingerprintLoader = {
            param($secureKey)
            [Environment]::SetEnvironmentVariable($script:P5EA43EndpointEnvironmentName, ('a' * 64), 'Process')
            throw (New-Object System.InvalidOperationException 'REMOTE_EXCEPTION_SENTINEL')
        }.GetNewClosure()
    }
    if ($Options.ContainsKey('AfterKeyMode')) {
        $seams.AfterKeyHook = {
            param($receiptPath)
            if ([string]$fixture.Options.AfterKeyMode -eq 'TamperReceipt') {
                [IO.File]::WriteAllText($receiptPath, '{"tampered":true}', [Text.UTF8Encoding]::new($false))
            } elseif ([string]$fixture.Options.AfterKeyMode -eq 'MismatchedScope' -or [string]$fixture.Options.AfterKeyMode -eq 'MismatchedSerial') {
                $receipt = Get-Content -Raw -LiteralPath $receiptPath | ConvertFrom-Json
                if ([string]$fixture.Options.AfterKeyMode -eq 'MismatchedScope') { $receipt.scope = 'WRONG/SCOPE' }
                if ([string]$fixture.Options.AfterKeyMode -eq 'MismatchedSerial') { $receipt.serial = 'wrong-serial' }
                [IO.File]::WriteAllText($receiptPath, ($receipt | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
                $fixture.State.ReceiptSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $receiptPath).Hash.ToUpperInvariant()
            }
        }.GetNewClosure()
    }
    $seams.ChildInvocationOverride = {
        param($context, $contract, $commandCopyPath, $fingerprint, $childArgs)
        $fixture.State.ChildOverrideCalls = [int]$fixture.State.ChildOverrideCalls + 1
        $mode = if ($fixture.Options.ContainsKey('ChildMode')) { [string]$fixture.Options.ChildMode } else { 'env-present' }
        $filePath = [string]$contract.PowershellPath
        if ($fixture.Options.ContainsKey('ChildStartFailure') -and [bool]$fixture.Options.ChildStartFailure) {
            $filePath = Join-Path $fixture.Root 'missing powershell.exe'
        }
        $args = @('-NoLogo','-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',$fixture.FakeChildPath,'-Mode',$mode)
        $timeout = if ($fixture.Options.ContainsKey('ChildTimeoutMilliseconds')) { [long]$fixture.Options.ChildTimeoutMilliseconds } else { 5000L }
        $stdoutCap = if ($fixture.Options.ContainsKey('StdoutCaptureBytes')) { [long]$fixture.Options.StdoutCaptureBytes } else { 1048576L }
        $stderrCap = if ($fixture.Options.ContainsKey('StderrCaptureBytes')) { [long]$fixture.Options.StderrCaptureBytes } else { 1048576L }
        return Invoke-P5EPhaseChildInvocation -Phase 'Dispatch' -ExpectedEnvironmentName $contract.ExpectedEnvironmentName -ExpectedEnvironmentValue $fingerprint -FilePath $filePath -ArgumentList $args -WorkingDirectory ([string]$contract.ChildWorkingDirectory) -TimeoutMilliseconds $timeout -StdoutCaptureBytes $stdoutCap -StderrCaptureBytes $stderrCap -SensitiveValues @($fingerprint)
    }.GetNewClosure()
    if ($Options.ContainsKey('CleanupShouldThrow')) { $seams.CleanupShouldThrow = [bool]$Options.CleanupShouldThrow }
    if ($Options.ContainsKey('DiagnosticFailure')) {
        $seams.DiagnosticWriter = {
            param($path, $record)
            throw (New-Object System.InvalidOperationException 'REMOTE_EXCEPTION_SENTINEL')
        }.GetNewClosure()
    }
    $fixture.Seams = $seams
    return $fixture
}

function Get-IntegrationSafeResultText {
    param([AllowNull()][object]$Result)
    if ($null -eq $Result) { return '' }
    return ($Result | ConvertTo-Json -Depth 16 -Compress)
}

function Assert-IntegrationNoLeak {
    param([Parameter(Mandatory = $true)]$Fixture,[AllowNull()][object]$Result)
    $resultText = Get-IntegrationSafeResultText -Result $Result
    Assert-IntegrationNotContains -Text $resultText -Needle 'synthetic-api-key-sentinel' -Code 'KEY_SENTINEL_RESULT_LEAK'
    Assert-IntegrationNotContains -Text $resultText -Needle $Fixture.SensitiveDigest -Code 'DIGEST_RESULT_LEAK'
    Assert-IntegrationNotContains -Text $resultText -Needle 'REMOTE_EXCEPTION_SENTINEL' -Code 'EXCEPTION_RESULT_LEAK'
    if (Test-Path -LiteralPath ([string]$Fixture.Contract.AuditPath) -PathType Leaf) {
        $auditText = Get-Content -Raw -LiteralPath ([string]$Fixture.Contract.AuditPath)
        Assert-IntegrationNotContains -Text $auditText -Needle 'synthetic-api-key-sentinel' -Code 'KEY_SENTINEL_AUDIT_LEAK'
        Assert-IntegrationNotContains -Text $auditText -Needle $Fixture.SensitiveDigest -Code 'DIGEST_AUDIT_LEAK'
        Assert-IntegrationNotContains -Text $auditText -Needle 'REMOTE_EXCEPTION_SENTINEL' -Code 'EXCEPTION_AUDIT_LEAK'
    }
    Assert-Integration -Condition (-not [bool]$Fixture.State.LiveCalls) -Code 'SYNTHETIC_LIVE_CALL_OBSERVED'
    Assert-Integration -Condition (-not [bool]$Fixture.State.SecureKey) -Code 'SECURE_KEY_RETAINED'
    $envValue = [Environment]::GetEnvironmentVariable($script:P5EA43EndpointEnvironmentName, 'Process')
    Assert-Integration -Condition ([string]::IsNullOrWhiteSpace($envValue)) -Code 'EXPECTED_ENVIRONMENT_RETAINED'
}

function Assert-IntegrationBasicFailure {
    param([Parameter(Mandatory = $true)]$Fixture,[Parameter(Mandatory = $true)]$Result,[Parameter(Mandatory = $true)][string]$Code,[Parameter(Mandatory = $true)][string]$Stage)
    Assert-IntegrationEqual -Actual $Result.TypedCode -Expected $Code -Code 'INTEGRATION_FAILURE_CODE_MISMATCH'
    Assert-IntegrationEqual -Actual $Result.Stage -Expected $Stage -Code 'INTEGRATION_FAILURE_STAGE_MISMATCH'
    Assert-IntegrationEqual -Actual $Result.OuterExitCode -Expected 1 -Code 'INTEGRATION_FAILURE_EXIT_MISMATCH'
    Assert-IntegrationEqual -Actual $Result.ChildAttempts -Expected 0 -Code 'INTEGRATION_FAILURE_CHILD_ATTEMPTS'
    Assert-IntegrationEqual -Actual $Result.KeyPromptCount -Expected 0 -Code 'INTEGRATION_FAILURE_KEY_PROMPT'
    Assert-Integration -Condition (-not [bool]$Result.ReservationCreated) -Code 'INTEGRATION_FAILURE_RESERVED'
    Assert-Integration -Condition (-not [bool]$Result.OwnerRootCreated) -Code 'INTEGRATION_FAILURE_OWNER_ROOT'
    Assert-Integration -Condition (-not [bool]$Result.ReceiptCreated) -Code 'INTEGRATION_FAILURE_RECEIPT'
}

function Invoke-IntegrationCase {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [hashtable]$Options = @{},
        [Parameter(Mandatory = $true)][scriptblock]$Assert
    )
    $fixture = $null
    $result = $null
    try {
        $fixture = New-IntegrationFixture -Name $Name -Options $Options
        if ($Options.ContainsKey('Collision') -and [bool]$Options.Collision) {
            $binding = Get-P5EA43LauncherBinding -LauncherPath $fixture.Contract.LauncherPath -ReservationRoot $fixture.Contract.ReservationRoot
            $before = Get-Content -Raw -LiteralPath $binding.ReservationPath -ErrorAction SilentlyContinue
            if ($null -eq $before) { $null = New-P5EA43ReservationMarker -Context $binding; $fixture.CollisionMarkerBytes = Get-Content -Raw -LiteralPath $binding.ReservationPath }
        }
        if ($Options.ContainsKey('OwnerRootReuse') -and [bool]$Options.OwnerRootReuse) {
            $decisionKey = Get-P5EA43DecisionKey -CanonicalDecisionId ([string]$fixture.Contract.DecisionId)
            [void](New-Item -ItemType Directory -Path (Join-Path $fixture.Contract.PrivateRoot ('p5e-a43-owner-' + $decisionKey)))
        }
        $result = Invoke-P5EA43ExecutableLauncher -Contract $fixture.Contract -Seams $fixture.Seams -State $fixture.State
        & $Assert $fixture $result
        Assert-IntegrationNoLeak -Fixture $fixture -Result $result
        [void]$caseRecords.Add([ordered]@{
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
                resolverAdapterCalls = [int]$fixture.State.ResolverSpyCalls
                childContractCalls = [int]$fixture.State.ChildContractCalls
                receiptRevalidationCalls = [int]$fixture.State.ReceiptRevalidationCalls
                diagnosticStatus = [string]$result.DiagnosticStatus
            })
    } catch {
        $script:failureCount++
        $safeClass = 'Exception'
        try { $safeClass = $_.Exception.GetType().Name } catch { }
        [void]$caseRecords.Add([ordered]@{ name = $Name; status = 'FAIL'; exceptionClass = $safeClass })
        Write-Output ('INTEGRATION_CASE_FAIL=' + $Name + ';exceptionClass=' + $safeClass)
    } finally {
        Clear-P5EA43IntegrationEnvironment
        if ($null -ne $fixture -and (Test-Path -LiteralPath $fixture.Root)) {
            $rootFull = [IO.Path]::GetFullPath([string]$fixture.Root)
            $qaFull = [IO.Path]::GetFullPath([string]$qaRoot)
            if ($rootFull.StartsWith($qaFull, [StringComparison]::OrdinalIgnoreCase)) {
                Remove-Item -LiteralPath $rootFull -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

function Invoke-IntegrationProcessProbe {
    param([Parameter(Mandatory = $true)][ValidateSet('Success', 'ChildFailure')][string]$Mode)
    $fixture = $null
    try {
        $options = @{}
        if ($Mode -eq 'ChildFailure') { $options.ChildMode = 'exit7-empty' }
        $fixture = New-IntegrationFixture -Name ('process-probe-' + $Mode) -Options $options
        $result = Invoke-P5EA43ExecutableLauncher -Contract $fixture.Contract -Seams $fixture.Seams -State $fixture.State
        $expectedExit = if ($Mode -eq 'Success') { 0 } else { 1 }
        if ([int]$result.OuterExitCode -ne $expectedExit -or [int]$result.ChildAttempts -ne 1) { exit 1 }
        exit $expectedExit
    } catch {
        exit 1
    } finally {
        Clear-P5EA43IntegrationEnvironment
        if ($null -ne $fixture -and (Test-Path -LiteralPath $fixture.Root)) {
            $rootFull = [IO.Path]::GetFullPath([string]$fixture.Root)
            $qaFull = [IO.Path]::GetFullPath([string]$qaRoot)
            if ($rootFull.StartsWith($qaFull, [StringComparison]::OrdinalIgnoreCase)) {
                Remove-Item -LiteralPath $rootFull -Recurse -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

function Invoke-IntegrationConsoleProbe {
    $fixture = $null
    $exitCode = 1
    $facts = [ordered]@{ probe = 'console-approval-key-synthetic-child'; completed = $false }
    try {
        $fixture = New-IntegrationFixture -Name 'console-probe' -Options @{ RealConsoleReaders = $true }
        # The readiness flag carries only the synthetic private root so the
        # driver can tell when the approval was accepted (owner root appears).
        [IO.File]::WriteAllText((Join-Path $ProbeDirectory 'fixture-ready.flag'), [string]$fixture.PrivateRoot, [Text.UTF8Encoding]::new($false))
        $result = Invoke-P5EA43ExecutableLauncher -Contract $fixture.Contract -Seams $fixture.Seams -State $fixture.State
        $counts = [ordered]@{}
        foreach ($key in @($fixture.State.Keys | Sort-Object)) {
            if ($fixture.State[$key] -is [int]) { $counts[[string]$key] = [int]$fixture.State[$key] }
        }
        $facts.completed = $true
        $facts.status = [string]$result.Status
        $facts.stage = [string]$result.Stage
        $facts.typedCode = [string]$result.TypedCode
        $facts.outerExitCode = [int]$result.OuterExitCode
        $facts.keyPromptCount = [int]$result.KeyPromptCount
        $facts.reservationCreated = [bool]$result.ReservationCreated
        $facts.ownerRootCreated = [bool]$result.OwnerRootCreated
        $facts.receiptCreated = [bool]$result.ReceiptCreated
        $facts.childAttempts = [int]$result.ChildAttempts
        $facts.childExitCode = $result.ChildExitCode
        $facts.diagnosticStatus = [string]$result.DiagnosticStatus
        $facts.liveCalls = [int]$fixture.State.LiveCalls
        $facts.environmentRetained = -not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($script:P5EA43EndpointEnvironmentName, 'Process'))
        $facts.callCounters = $counts
        $exitCode = [int]$result.OuterExitCode
    } catch {
        $facts.exceptionClass = $_.Exception.GetType().Name
        $facts.exceptionCode = ([string]$_.Exception.Message).Split([char[]]"`r`n")[0]
    } finally {
        Clear-P5EA43IntegrationEnvironment
        [IO.File]::WriteAllText((Join-Path $ProbeDirectory 'probe-facts.json'), ($facts | ConvertTo-Json -Depth 6), [Text.UTF8Encoding]::new($false))
        if (Test-Path -LiteralPath $qaRoot) { Remove-Item -LiteralPath $qaRoot -Recurse -Force -ErrorAction SilentlyContinue }
    }
    exit $exitCode
}

if ($ConsoleProbe) {
    if ([string]::IsNullOrWhiteSpace($ProbeDirectory) -or -not (Test-Path -LiteralPath $ProbeDirectory -PathType Container)) { exit 2 }
    Invoke-IntegrationConsoleProbe
}

if (-not [string]::IsNullOrWhiteSpace($ProcessProbe)) {
    Invoke-IntegrationProcessProbe -Mode $ProcessProbe
}

$processProbeResults = [ordered]@{ successProcessExit = -1; childFailureProcessExit = -1; outputRedacted = $true }
foreach ($probeMode in @('Success', 'ChildFailure')) {
    $probeOutput = @(& $ps51Path -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File ([IO.Path]::GetFullPath($MyInvocation.MyCommand.Path)) -ProcessProbe $probeMode 2>&1)
    $probeExit = [int]$LASTEXITCODE
    $probeText = ($probeOutput | Out-String)
    if ($probeText.Contains('synthetic-api-key-sentinel') -or $probeText.Contains('REMOTE_EXCEPTION_SENTINEL') -or $probeText.Contains('OUTPUT_SENTINEL')) {
        throw 'P5E_A43_INTEGRATION_PROCESS_PROBE_REDACTION_STOP'
    }
    if ($probeMode -eq 'Success') {
        $processProbeResults.successProcessExit = $probeExit
        Assert-IntegrationEqual -Actual $probeExit -Expected 0 -Code 'PROCESS_PROBE_SUCCESS_EXIT'
    } else {
        $processProbeResults.childFailureProcessExit = $probeExit
        Assert-IntegrationEqual -Actual $probeExit -Expected 1 -Code 'PROCESS_PROBE_FAILURE_EXIT'
    }
}

Invoke-IntegrationCase -Name 'full-chain-success-real-resolver-and-child-contract' -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'CHILD_EXIT_ZERO' -Code 'SUCCESS_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'CHILD_RESULT' -Code 'SUCCESS_STAGE'
    Assert-IntegrationEqual -Actual $result.OuterExitCode -Expected 0 -Code 'SUCCESS_EXIT'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 1 -Code 'SUCCESS_CHILD_ATTEMPTS'
    Assert-IntegrationEqual -Actual $result.KeyPromptCount -Expected 1 -Code 'SUCCESS_KEY_PROMPT'
    Assert-Integration -Condition ([bool]$result.ReservationCreated -and [bool]$result.OwnerRootCreated -and [bool]$result.ReceiptCreated) -Code 'SUCCESS_RESERVATION_CHAIN'
    Assert-IntegrationEqual -Actual $fixture.State.ResolveToolchain -Expected 1 -Code 'RESOLVER_PARENT_CALL_COUNT'
    Assert-IntegrationEqual -Actual $fixture.State.ResolverSpyCalls -Expected 1 -Code 'RESOLVER_SPY_CALL_COUNT'
    Assert-IntegrationEqual -Actual $fixture.State.ChildContractCalls -Expected 1 -Code 'CHILD_CONTRACT_CALL_COUNT'
    Assert-IntegrationEqual -Actual $fixture.State.ChildOverrideCalls -Expected 1 -Code 'CHILD_OVERRIDE_CALL_COUNT'
    Assert-IntegrationEqual -Actual $fixture.State.ReceiptRevalidationCalls -Expected 1 -Code 'RECEIPT_REVALIDATION_COUNT'
    Assert-IntegrationEqual -Actual $fixture.State.ClearKey -Expected 1 -Code 'CLEAR_KEY_CALL_COUNT'
    Assert-IntegrationEqual -Actual $fixture.State.WriteDiagnostic -Expected 1 -Code 'DIAGNOSTIC_CALL_COUNT'
    Assert-Integration -Condition ([string]$fixture.State.ResolvedSdkPath -ceq [IO.Path]::GetFullPath($fixture.SdkRoot)) -Code 'RESOLVER_SDK_OUTPUT_NOT_TRANSFERRED'
    Assert-Integration -Condition (-not [string]::IsNullOrWhiteSpace([string]$fixture.State.ResolvedAdbPath)) -Code 'RESOLVER_ADB_OUTPUT_MISSING'
    Assert-Integration -Condition (-not [string]::IsNullOrWhiteSpace([string]$fixture.State.ResolvedJavaPath)) -Code 'RESOLVER_JAVA_OUTPUT_MISSING'
    Assert-Integration -Condition (-not [string]::IsNullOrWhiteSpace([string]$fixture.State.ResolvedApkSignerJarPath)) -Code 'RESOLVER_APKSIGNER_OUTPUT_MISSING'
    Assert-IntegrationEqual -Actual $result.DiagnosticStatus -Expected 'WRITTEN' -Code 'SUCCESS_DIAGNOSTIC_STATUS'
    Assert-Integration -Condition ([bool]$fixture.State.EnvironmentClearAfterRun) -Code 'SUCCESS_ENVIRONMENT_NOT_CLEARED'
    Assert-Integration -Condition (([IO.Path]::GetFullPath($fixture.Contract.ChildWorkingDirectory)) -cne ([IO.Path]::GetFullPath($fixture.Contract.RepoRoot))) -Code 'WORKING_DIRECTORY_NOT_SEPARATE'
    $copyBytes = [IO.File]::ReadAllBytes([string]$fixture.State.CommandCopyPath)
    $sourceBytes = [IO.File]::ReadAllBytes([string]$fixture.Contract.CommandSourcePath)
    Assert-Integration -Condition ([Convert]::ToBase64String($copyBytes) -ceq [Convert]::ToBase64String($sourceBytes)) -Code 'COMMAND_COPY_BYTES_MISMATCH'
    [Array]::Clear($copyBytes, 0, $copyBytes.Length); [Array]::Clear($sourceBytes, 0, $sourceBytes.Length)
    Assert-Integration -Condition (Test-Path -LiteralPath ([string]$result.ReservationPath) -ErrorAction SilentlyContinue) -Code 'RESULT_MARKER_PATH_NOT_OBSERVED'
} -Options @{ }

Invoke-IntegrationCase -Name 'sdk-output-file-red' -Options @{ ResolverOutputMode = 'SdkFile' } -Assert {
    param($fixture, $result)
    Assert-IntegrationBasicFailure -Fixture $fixture -Result $result -Code 'P5E_TOOLCHAIN_SDK_NOT_DIRECTORY_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
    Assert-IntegrationEqual -Actual $fixture.State.ResolverSpyCalls -Expected 1 -Code 'SDK_FILE_RESOLVER_NOT_CALLED_ONCE'
}

Invoke-IntegrationCase -Name 'adb-output-directory-red' -Options @{ ResolverOutputMode = 'AdbDirectory' } -Assert {
    param($fixture, $result)
    Assert-IntegrationBasicFailure -Fixture $fixture -Result $result -Code 'P5E_TOOLCHAIN_ADB_NOT_REGULAR_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
}

Invoke-IntegrationCase -Name 'resolver-unknown-redacted' -Options @{ ResolverThrowSentinel = 'REMOTE_EXCEPTION_SENTINEL' } -Assert {
    param($fixture, $result)
    Assert-IntegrationBasicFailure -Fixture $fixture -Result $result -Code 'UNKNOWN_STOP' -Stage 'TOOLCHAIN_PREFLIGHT'
}

Invoke-IntegrationCase -Name 'approval-wrong-no-reservation' -Options @{ ApprovalValue = 'WRONG_LITERAL' } -Assert {
    param($fixture, $result)
    Assert-IntegrationBasicFailure -Fixture $fixture -Result $result -Code 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP' -Stage 'APPROVAL'
}

Invoke-IntegrationCase -Name 'approval-empty-no-reservation' -Options @{ ApprovalValue = '' } -Assert {
    param($fixture, $result)
    Assert-IntegrationBasicFailure -Fixture $fixture -Result $result -Code 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP' -Stage 'APPROVAL'
}

Invoke-IntegrationCase -Name 'approval-eof-no-reservation' -Options @{ ApprovalState = 'EOF' } -Assert {
    param($fixture, $result)
    Assert-IntegrationBasicFailure -Fixture $fixture -Result $result -Code 'P5E_OWNER_APPROVAL_INPUT_EOF_STOP' -Stage 'APPROVAL'
}

Invoke-IntegrationCase -Name 'approval-cancel-no-reservation' -Options @{ ApprovalState = 'CANCELLED' } -Assert {
    param($fixture, $result)
    Assert-IntegrationBasicFailure -Fixture $fixture -Result $result -Code 'P5E_OWNER_APPROVAL_INPUT_CANCELLED_STOP' -Stage 'APPROVAL'
}

Invoke-IntegrationCase -Name 'receipt-tamper-after-key' -Options @{ AfterKeyMode = 'TamperReceipt' } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP' -Code 'RECEIPT_TAMPER_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'KEY' -Code 'RECEIPT_TAMPER_STAGE'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 0 -Code 'RECEIPT_TAMPER_CHILD_ATTEMPTS'
    Assert-IntegrationEqual -Actual $result.KeyPromptCount -Expected 1 -Code 'RECEIPT_TAMPER_KEY_PROMPT'
    Assert-Integration -Condition ([bool]$result.ReservationCreated -and [bool]$result.OwnerRootCreated -and [bool]$result.ReceiptCreated) -Code 'RECEIPT_TAMPER_CHAIN'
}

Invoke-IntegrationCase -Name 'receipt-scope-mismatch-after-key' -Options @{ AfterKeyMode = 'MismatchedScope' } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP' -Code 'RECEIPT_SCOPE_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'KEY' -Code 'RECEIPT_SCOPE_STAGE'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 0 -Code 'RECEIPT_SCOPE_CHILD_ATTEMPTS'
    Assert-IntegrationEqual -Actual $fixture.State.ReceiptRevalidationCalls -Expected 1 -Code 'RECEIPT_SCOPE_REVALIDATION'
}

Invoke-IntegrationCase -Name 'receipt-serial-mismatch-after-key' -Options @{ AfterKeyMode = 'MismatchedSerial' } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'P5E_OWNER_RECEIPT_BINDING_MISMATCH_STOP' -Code 'RECEIPT_SERIAL_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'KEY' -Code 'RECEIPT_SERIAL_STAGE'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 0 -Code 'RECEIPT_SERIAL_CHILD_ATTEMPTS'
    Assert-IntegrationEqual -Actual $fixture.State.ReceiptRevalidationCalls -Expected 1 -Code 'RECEIPT_SERIAL_REVALIDATION'
}

Invoke-IntegrationCase -Name 'receipt-expired-after-key' -Options @{ ReceiptValidityMilliseconds = 1; NowValues = @(1000000, 1000000, 1000002) } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'RECEIPT_EXPIRED_STOP' -Code 'RECEIPT_EXPIRY_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'KEY' -Code 'RECEIPT_EXPIRY_STAGE'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 0 -Code 'RECEIPT_EXPIRY_CHILD_ATTEMPTS'
    Assert-IntegrationEqual -Actual $fixture.State.NowProviderCalls -Expected 3 -Code 'RECEIPT_EXPIRY_CLOCK_CALLS'
}

Invoke-IntegrationCase -Name 'partial-key-failure-clears-process-state' -Options @{ FingerprintLoaderMode = 'PartialFailure' } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'P5E_OWNER_KEY_INPUT_UNAVAILABLE_STOP' -Code 'PARTIAL_KEY_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'KEY' -Code 'PARTIAL_KEY_STAGE'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 0 -Code 'PARTIAL_KEY_CHILD_ATTEMPTS'
    Assert-IntegrationEqual -Actual $fixture.State.ReadKeySelfCleared -Expected 1 -Code 'PARTIAL_KEY_SELF_CLEAR'
}

Invoke-IntegrationCase -Name 'child-start-failure' -Options @{ ChildStartFailure = $true } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'PROCESS_START_FAILED' -Code 'CHILD_START_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'CHILD_RESULT' -Code 'CHILD_START_STAGE'
    Assert-IntegrationEqual -Actual $result.OuterExitCode -Expected 2 -Code 'CHILD_START_EXIT'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 1 -Code 'CHILD_START_ATTEMPTS'
}

Invoke-IntegrationCase -Name 'child-exit7-stderr-redacted' -Options @{ ChildMode = 'exit7-stderr' } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'CHILD_EXIT_NONZERO' -Code 'CHILD_EXIT7_CODE'
    Assert-IntegrationEqual -Actual $result.ChildExitCode -Expected 7 -Code 'CHILD_EXIT7_VALUE'
    Assert-IntegrationEqual -Actual $result.OuterExitCode -Expected 1 -Code 'CHILD_EXIT7_EXIT'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 1 -Code 'CHILD_EXIT7_ATTEMPTS'
}

Invoke-IntegrationCase -Name 'child-timeout' -Options @{ ChildMode = 'timeout'; ChildTimeoutMilliseconds = 50 } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'PROCESS_TIMEOUT' -Code 'CHILD_TIMEOUT_CODE'
    Assert-IntegrationEqual -Actual $result.OuterExitCode -Expected 1 -Code 'CHILD_TIMEOUT_EXIT'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 1 -Code 'CHILD_TIMEOUT_ATTEMPTS'
}

Invoke-IntegrationCase -Name 'child-output-too-large' -Options @{ ChildMode = 'output-large'; StdoutCaptureBytes = 1024 } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'OUTPUT_TOO_LARGE' -Code 'CHILD_OUTPUT_CODE'
    Assert-IntegrationEqual -Actual $result.OuterExitCode -Expected 1 -Code 'CHILD_OUTPUT_EXIT'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 1 -Code 'CHILD_OUTPUT_ATTEMPTS'
}

Invoke-IntegrationCase -Name 'cleanup-failure-does-not-mask-child' -Options @{ ChildMode = 'exit7-empty'; CleanupShouldThrow = $true } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'CHILD_EXIT_NONZERO' -Code 'CLEANUP_MASK_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'CHILD_RESULT' -Code 'CLEANUP_MASK_STAGE'
    Assert-IntegrationContains -Text ([string]$result.SecondaryStatus) -Needle 'CLEANUP_FAILED:InvalidOperationException' -Code 'CLEANUP_SECONDARY_MISSING'
}

Invoke-IntegrationCase -Name 'diagnostic-failure-is-incomplete' -Options @{ DiagnosticFailure = $true } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'EVIDENCE_INCOMPLETE' -Code 'DIAGNOSTIC_FAILURE_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'DIAGNOSTIC' -Code 'DIAGNOSTIC_FAILURE_STAGE'
    Assert-IntegrationEqual -Actual $result.OuterExitCode -Expected 1 -Code 'DIAGNOSTIC_FAILURE_EXIT'
    Assert-IntegrationEqual -Actual $result.DiagnosticStatus -Expected 'EVIDENCE_INCOMPLETE' -Code 'DIAGNOSTIC_FAILURE_STATUS'
    Assert-Integration -Condition (-not (Test-Path -LiteralPath ([string]$fixture.Contract.AuditPath))) -Code 'DIAGNOSTIC_FAILURE_FILE_CREATED'
}

Invoke-IntegrationCase -Name 'reservation-collision-no-overwrite' -Options @{ Collision = $true; ApprovalValue = 'APPROVE_ONE_FRESH_EVENT' } -Assert {
    param($fixture, $result)
    Assert-IntegrationBasicFailure -Fixture $fixture -Result $result -Code 'P5E_A43_LAUNCHER_RESERVATION_ALREADY_EXISTS_STOP' -Stage 'LAUNCHER_RESERVATION'
}

Invoke-IntegrationCase -Name 'owner-root-reuse-stop' -Options @{ OwnerRootReuse = $true } -Assert {
    param($fixture, $result)
    Assert-IntegrationEqual -Actual $result.TypedCode -Expected 'P5E_OWNER_ROOT_ALREADY_EXISTS_STOP' -Code 'OWNER_REUSE_CODE'
    Assert-IntegrationEqual -Actual $result.Stage -Expected 'OWNER_ROOT' -Code 'OWNER_REUSE_STAGE'
    Assert-IntegrationEqual -Actual $result.ChildAttempts -Expected 0 -Code 'OWNER_REUSE_CHILD_ATTEMPTS'
    Assert-Integration -Condition ([bool]$result.ReservationCreated -and -not [bool]$result.OwnerRootCreated) -Code 'OWNER_REUSE_RESERVATION_STATE'
}

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    $ReportPath = Join-Path $repoRoot 'evidence\p5e-a43-parent-integration-20260930\P5E_A43_PARENT_INTEGRATION_QA_20260930_01.json'
}
$reportRoot = Split-Path -Parent ([IO.Path]::GetFullPath($ReportPath))
if (-not (Test-Path -LiteralPath $reportRoot -PathType Container)) { [void][IO.Directory]::CreateDirectory($reportRoot) }
$testSourceHash = Get-P5EA43IntegrationSha256 -Path $MyInvocation.MyCommand.Path
$caseArray = @($caseRecords.ToArray())
$report = [ordered]@{
    schema = 'p5e.a43.pre-reservation.integration-qa.v1'
    observedAtUtc = [DateTimeOffset]::UtcNow.ToString('o')
    status = if ($failureCount -eq 0) { 'PASS' } else { 'FAILED_REPAIRING' }
    candidateExecuted = $false
    liveActions = 0
    liveBoundary = 'NOT_EVALUATED'
    processExitProbe = $processProbeResults
    source = [ordered]@{ path = 'scripts/p5e-a43-pre-reservation-launcher-entrypoint.ps1'; sha256 = $candidateHash }
    testSource = [ordered]@{ path = 'scripts/test-p5e-a43-pre-reservation-integration.ps1'; sha256 = $testSourceHash }
    parent = [ordered]@{ path = 'scripts/p5e-a43-pre-reservation-launcher.ps1'; sha256 = Get-P5EA43IntegrationSha256 -Path $parentPath }
    resolver = [ordered]@{ path = 'scripts/p5e-raw-toolchain.ps1'; sha256 = Get-P5EA43IntegrationSha256 -Path $resolverPath }
    childContract = [ordered]@{ path = 'scripts/p5e-child-invocation-contract.ps1'; sha256 = Get-P5EA43IntegrationSha256 -Path $childPath }
    runtimeGuards = [ordered]@{ path = 'scripts/p5e-a43-runtime-guards.ps1'; sha256 = Get-P5EA43IntegrationSha256 -Path $runtimeGuardPath }
    expectedDigest = [ordered]@{ path = 'scripts/p5e-load-expected-digest.ps1'; sha256 = Get-P5EA43IntegrationSha256 -Path $digestPath }
    packetPins = [ordered]@{
        manifestSha256 = $pinTable.Manifest.Sha256
        commandSha256 = $pinTable.Command.Sha256
        helperSha256 = $pinTable.Helper.Sha256
        exporterSha256 = $pinTable.Exporter.Sha256
        bridgeSha256 = $pinTable.Bridge.Sha256
    }
    dependencySlots = @('ResolveToolchain','ReadApproval','ReserveLauncher','CreateOwnerRoot','CreateReceipt','ValidateReceipt','ReadKey','InvokeChild','ClearKey','WriteDiagnostic')
    cases = $caseArray
    passed = @($caseArray | Where-Object { $_.status -eq 'PASS' }).Count
    failed = $failureCount
    assertions = @(
        'real Resolve-P5ERawToolchain called once on synthetic SDK directory and its output paths transferred into parent',
        'SDK output file and executable-directory output fail the parent kind gate',
        'approval wrong/empty/EOF/cancel stops before reservation and key without echoing input',
        'strict current receipt validator checks packet pins/scope/serial/hash and revalidates expiry after key before child attempt',
        'command copy is exact bytes and child boundary uses Invoke-P5EPhaseChildInvocation once with separate working directory',
        'secure key and expected digest are process-cleared on success and partial key failure',
        'child start/exit/timeout/output and diagnostic/cleanup failures preserve primary typed result and bounded redaction',
        'all authority-like output is confined to per-case temporary sandbox; no live registry/provider/ADB action occurred'
    )
}
$json = $report | ConvertTo-Json -Depth 20
if (Test-Path -LiteralPath $ReportPath) { throw 'P5E_A43_INTEGRATION_REPORT_ALREADY_EXISTS_STOP' }
$stream = $null
$bytes = $null
try {
    $stream = [IO.File]::Open([IO.Path]::GetFullPath($ReportPath), [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes($json)
    $stream.Write($bytes, 0, $bytes.Length)
    $stream.Flush($true)
} finally {
    if ($null -ne $stream) { $stream.Dispose() }
    if ($null -ne $bytes) { [Array]::Clear($bytes, 0, $bytes.Length) }
}
Write-Output ('P5E_A43_PARENT_INTEGRATION_STATUS=' + [string]$report.status)
Write-Output ('P5E_A43_PARENT_INTEGRATION_CASES=' + [string]$report.cases.Count)
Write-Output ('P5E_A43_PARENT_INTEGRATION_PASSED=' + [string]$report.passed)
Write-Output ('P5E_A43_PARENT_INTEGRATION_FAILED=' + [string]$report.failed)
if ($failureCount -ne 0) { exit 1 }
exit 0
