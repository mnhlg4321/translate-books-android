[CmdletBinding()]
param(
    [string]$ReportPath = ''
)

# Real-console entry QA. Unlike test-p5e-a43-entry-boundary.ps1 (redirected,
# noninteractive), each case runs in its own Windows PowerShell 5.1 console and
# receives keystrokes through WriteConsoleInput (hidden window, real console;
# not claimed equal to an owner-visible window). Typed values are synthetic:
# wrong literals against the entrypoint, and the approval/key prompts of the
# integration fixture only inside a synthetic SDK/private root/child. No real
# key, ADB, device or provider is reachable. Paths contain spaces.
#
# Cases:
#   1 console-prompt-wrong-literal          entrypoint -Execute, prompt reached, typed stop, exit 1
#   2 console-outer-failure-preserved       missing -JavaPath: stderr cause + exit 1, no audit
#   3 console-approval-key-synthetic-child  integration fixture, real Read-Host approval+key,
#                                           synthetic child, CHILD_EXIT_ZERO, exit 0
#   4 legacy-cc01-silent-exit-defect        retained CC01 bytes: silent exit 0, no audit, no prompt
# Per-case observed facts are persisted in the report; temporary roots are
# removed only after those facts are captured.

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$entrypointPath = Join-Path $PSScriptRoot 'p5e-a43-pre-reservation-launcher-entrypoint.ps1'
$integrationPath = Join-Path $PSScriptRoot 'test-p5e-a43-pre-reservation-integration.ps1'
$legacyEntryPath = Join-Path $repoRoot 'evidence\p5e-a43-parent-integration-20260930\legacy\P5E_A43_ENTRY_CC01_LEGACY.ps1.txt'
$powershellPath = Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'

function Assert-EntryConsole {
    param([bool]$Condition, [string]$Code)
    if (-not $Condition) { throw $Code }
}

Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public static class P5EConsoleInject {
    [DllImport("kernel32.dll", SetLastError = true)] static extern bool AttachConsole(uint pid);
    [DllImport("kernel32.dll", SetLastError = true)] static extern bool FreeConsole();
    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    static extern IntPtr CreateFile(string n, uint a, uint s, IntPtr sa, uint d, uint f, IntPtr t);
    [StructLayout(LayoutKind.Explicit, CharSet = CharSet.Unicode)]
    struct INPUT_RECORD {
        [FieldOffset(0)] public ushort EventType; [FieldOffset(4)] public int KeyDown; [FieldOffset(8)] public ushort Repeat;
        [FieldOffset(10)] public ushort VK; [FieldOffset(12)] public ushort Scan; [FieldOffset(14)] public char Ch; [FieldOffset(16)] public uint Ctrl;
    }
    [DllImport("kernel32.dll", SetLastError = true)] static extern bool WriteConsoleInput(IntPtr h, INPUT_RECORD[] r, uint n, out uint w);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr h);
    public static string Send(uint pid, string text) {
        FreeConsole();
        if (!AttachConsole(pid)) return "ATTACH_FAIL_" + Marshal.GetLastWin32Error();
        IntPtr h = CreateFile("CONIN$", 0xC0000000, 3, IntPtr.Zero, 3, 0, IntPtr.Zero);
        if (h == (IntPtr)(-1)) { FreeConsole(); return "OPEN_FAIL_" + Marshal.GetLastWin32Error(); }
        var l = new List<INPUT_RECORD>();
        foreach (char c in text + "\r") {
            char u = char.ToUpper(c);
            ushort vk = (ushort)(c == '\r' ? 0x0D : ((u >= 'A' && u <= 'Z') || (c >= '0' && c <= '9') ? u : 0));
            foreach (int d in new[] { 1, 0 }) l.Add(new INPUT_RECORD { EventType = 1, KeyDown = d, Repeat = 1, VK = vk, Ch = c });
        }
        uint w; bool ok = WriteConsoleInput(h, l.ToArray(), (uint)l.Count, out w);
        CloseHandle(h); FreeConsole();
        return ok ? "OK" : "WRITE_FAIL_" + Marshal.GetLastWin32Error();
    }
}
'@

function Get-RedactedExcerpt {
    param([string]$Text, [string[]]$Paths)
    $value = [string]$Text
    foreach ($path in $Paths) { if (-not [string]::IsNullOrEmpty($path)) { $value = $value.Replace($path, '<path>') } }
    $value = $value -replace '[0-9A-Fa-f]{64}', '<sha256>'
    $value = ($value -replace '\s+', ' ').Trim()
    if ($value.Length -gt 320) { $value = $value.Substring(0, 320) }
    return $value
}

function Start-ConsoleChild {
    param([string]$OuterScriptPath)
    return Start-Process -FilePath $powershellPath -ArgumentList @('-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $OuterScriptPath + '"')) -WindowStyle Hidden -PassThru
}

function Stop-ConsoleChild {
    param($Process)
    if (-not $Process.HasExited) {
        Get-CimInstance Win32_Process -Filter "ParentProcessId=$($Process.Id)" | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-EntryConsoleCase {
    param([string]$EntryPath = $entrypointPath, [switch]$OmitJavaPath, [switch]$SeedHashAudit, [switch]$ExplicitAuditPath, [string]$Typed = '', [int]$PromptWaitSeconds = 12)
    $private = Join-Path ([IO.Path]::GetTempPath()) ('p5e entry console ' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    [void](New-Item -ItemType Directory -Path $private)
    $resultPath = Join-Path $private 'outer-result.txt'
    $watchDir = Join-Path $private '.p5e-a43-audit'
    $outerPath = Join-Path ([IO.Path]::GetTempPath()) ('p5e-entry-console-outer-' + [Guid]::NewGuid().ToString('N').Substring(0, 8) + '.ps1')
    $hash = (Get-FileHash -LiteralPath $EntryPath -Algorithm SHA256).Hash
    $argNames = @('-NoLogo', '-NoProfile', '-ExecutionPolicy', '-File', '-Execute', '-RepoRoot', '-PrivateRoot', '-AndroidSdkPath', '-ExpectedCandidateSha256', '-DecisionId')
    $argText = "'-NoLogo','-NoProfile','-ExecutionPolicy','Bypass','-File','$EntryPath','-Execute'," +
        "'-RepoRoot','$repoRoot','-PrivateRoot','$private'," +
        "'-AndroidSdkPath','$env:LOCALAPPDATA\Android\Sdk'," +
        "'-ExpectedCandidateSha256','$hash','-DecisionId',('p5e-synth-' + [Guid]::NewGuid().ToString('N'))"
    if (-not $OmitJavaPath) { $argText += ",'-JavaPath','C:\Program Files\Android\Android Studio\jbr\bin\java.exe'"; $argNames += '-JavaPath' }
    $seedPath = ''
    if ($SeedHashAudit) {
        # A previous run of the same candidate already used the default,
        # candidate-hash-named audit file (as in the real private root).
        [void](New-Item -ItemType Directory -Path (Join-Path $private '.p5e-a43-audit'))
        $seedPath = Join-Path $private ('.p5e-a43-audit\' + $hash.ToLowerInvariant() + '.json')
        [IO.File]::WriteAllText($seedPath, '{"seeded":true}', [Text.UTF8Encoding]::new($false))
    }
    if ($ExplicitAuditPath) {
        $watchDir = Join-Path $private 'audit by decision'
        $argText += ",'-AuditPath','" + (Join-Path $watchDir 'p5e-synth-decision.json') + "'"
        $argNames += '-AuditPath'
    }
    $outer = "`$a = @($argText)`r`n" +
        "`$o = & '$powershellPath' @a 2>&1`r`n`$c = `$LASTEXITCODE`r`n" +
        "[IO.File]::WriteAllText('$resultPath', ('EXIT=' + `$c + [Environment]::NewLine + (`$o | Out-String)))`r`n"
    [IO.File]::WriteAllText($outerPath, $outer, [Text.UTF8Encoding]::new($false))
    $proc = $null
    try {
        $proc = Start-ConsoleChild -OuterScriptPath $outerPath
        $auditSeen = $false
        $deadline = [DateTime]::UtcNow.AddSeconds($PromptWaitSeconds)
        while ([DateTime]::UtcNow -lt $deadline -and -not $proc.HasExited -and -not $auditSeen) {
            Start-Sleep -Milliseconds 250
            $auditSeen = Test-Path -LiteralPath $watchDir
        }
        $aliveAtPrompt = $false
        $inject = 'NOT_SENT'
        if ($auditSeen -and -not $proc.HasExited) {
            Start-Sleep -Seconds 2
            $aliveAtPrompt = -not $proc.HasExited
            if ($aliveAtPrompt -and $Typed) { $inject = [P5EConsoleInject]::Send([uint32]$proc.Id, $Typed) }
        }
        [void]$proc.WaitForExit(30000)
        $exited = $proc.HasExited
        Stop-ConsoleChild -Process $proc
        $result = if (Test-Path -LiteralPath $resultPath) { Get-Content -Raw -LiteralPath $resultPath } else { '' }
        $exitMatch = [regex]::Match($result, '(?m)^EXIT=(-?\d+)\r?$')
        $typedMatch = [regex]::Match($result, '"TypedCode":"([A-Z0-9_]+)"')
        return [pscustomobject][ordered]@{
            EntrySha256 = $hash
            ArgumentNames = $argNames
            PathsContainSpace = ($EntryPath.Contains(' ') -and $private.Contains(' '))
            AuditSeen = $auditSeen; AliveAtPrompt = $aliveAtPrompt; Inject = $inject; Exited = $exited
            InnerExitCode = if ($exitMatch.Success) { [int]$exitMatch.Groups[1].Value } else { $null }
            TypedCode = if ($typedMatch.Success) { $typedMatch.Groups[1].Value } else { '' }
            Result = $result
            ResultExcerpt = Get-RedactedExcerpt -Text $result -Paths @($private, $EntryPath, $repoRoot)
            AuditFiles = @(Get-ChildItem -LiteralPath $watchDir -File -ErrorAction SilentlyContinue).Count
            SeedUnchanged = if ($seedPath) { ((Test-Path -LiteralPath $seedPath) -and ((Get-Content -Raw -LiteralPath $seedPath) -ceq '{"seeded":true}')) } else { $null }
        }
    } finally {
        if ($null -ne $proc) { Stop-ConsoleChild -Process $proc }
        if (Test-Path -LiteralPath $outerPath) { Remove-Item -LiteralPath $outerPath -Force }
        if (Test-Path -LiteralPath $private) { Remove-Item -LiteralPath $private -Recurse -Force }
    }
}

function Invoke-ConsoleProbeCase {
    $probeDir = Join-Path ([IO.Path]::GetTempPath()) ('p5e probe console ' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    [void](New-Item -ItemType Directory -Path $probeDir)
    $resultPath = Join-Path $probeDir 'outer-result.txt'
    $outerPath = Join-Path ([IO.Path]::GetTempPath()) ('p5e-probe-console-outer-' + [Guid]::NewGuid().ToString('N').Substring(0, 8) + '.ps1')
    $outer = "`$o = & '$powershellPath' -NoLogo -NoProfile -ExecutionPolicy Bypass -File '$integrationPath' -ConsoleProbe -ProbeDirectory '$probeDir' 2>&1`r`n`$c = `$LASTEXITCODE`r`n" +
        "[IO.File]::WriteAllText('$resultPath', ('EXIT=' + `$c + [Environment]::NewLine + (`$o | Out-String)))`r`n"
    [IO.File]::WriteAllText($outerPath, $outer, [Text.UTF8Encoding]::new($false))
    $proc = $null
    try {
        $proc = Start-ConsoleChild -OuterScriptPath $outerPath
        $readyPath = Join-Path $probeDir 'fixture-ready.flag'
        $deadline = [DateTime]::UtcNow.AddSeconds(40)
        while ([DateTime]::UtcNow -lt $deadline -and -not $proc.HasExited -and -not (Test-Path -LiteralPath $readyPath)) { Start-Sleep -Milliseconds 250 }
        $fixtureReady = Test-Path -LiteralPath $readyPath
        $approvalInject = 'NOT_SENT'
        $keyInject = 'NOT_SENT'
        $ownerRootSeen = $false
        if ($fixtureReady -and -not $proc.HasExited) {
            $syntheticPrivate = (Get-Content -Raw -LiteralPath $readyPath).Trim()
            Start-Sleep -Seconds 3
            if (-not $proc.HasExited) { $approvalInject = [P5EConsoleInject]::Send([uint32]$proc.Id, 'APPROVE_ONE_FRESH_EVENT') }
            $deadline = [DateTime]::UtcNow.AddSeconds(20)
            while ([DateTime]::UtcNow -lt $deadline -and -not $proc.HasExited -and -not $ownerRootSeen) {
                Start-Sleep -Milliseconds 250
                $ownerRootSeen = @(Get-ChildItem -LiteralPath $syntheticPrivate -Directory -Filter 'p5e-a43-owner-*' -ErrorAction SilentlyContinue).Count -gt 0
            }
            if ($ownerRootSeen -and -not $proc.HasExited) {
                Start-Sleep -Seconds 2
                $keyInject = [P5EConsoleInject]::Send([uint32]$proc.Id, 'synthetic-console-key-not-a-secret')
            }
        }
        [void]$proc.WaitForExit(40000)
        $exited = $proc.HasExited
        Stop-ConsoleChild -Process $proc
        $result = if (Test-Path -LiteralPath $resultPath) { Get-Content -Raw -LiteralPath $resultPath } else { '' }
        $factsPath = Join-Path $probeDir 'probe-facts.json'
        $facts = if (Test-Path -LiteralPath $factsPath) { Get-Content -Raw -LiteralPath $factsPath | ConvertFrom-Json } else { $null }
        $exitMatch = [regex]::Match($result, '(?m)^EXIT=(-?\d+)\r?$')
        return [pscustomobject][ordered]@{
            FixtureReady = $fixtureReady; ApprovalInject = $approvalInject; OwnerRootSeen = $ownerRootSeen; KeyInject = $keyInject
            Exited = $exited
            InnerExitCode = if ($exitMatch.Success) { [int]$exitMatch.Groups[1].Value } else { $null }
            Facts = $facts
            ResultExcerpt = Get-RedactedExcerpt -Text $result -Paths @($probeDir, $integrationPath, $repoRoot)
        }
    } finally {
        if ($null -ne $proc) { Stop-ConsoleChild -Process $proc }
        if (Test-Path -LiteralPath $outerPath) { Remove-Item -LiteralPath $outerPath -Force }
        if (Test-Path -LiteralPath $probeDir) { Remove-Item -LiteralPath $probeDir -Recurse -Force }
    }
}

$cases = New-Object 'System.Collections.Generic.List[object]'

# Case 1 — success path to prompt: audit exists before the prompt, wrong
# literal is rejected with a typed terminal result and a nonzero exit.
$prompt = Invoke-EntryConsoleCase -Typed 'WRONG_LITERAL'
$cases.Add([ordered]@{ name = 'console-prompt-wrong-literal'; entrySha256 = $prompt.EntrySha256; argumentNames = $prompt.ArgumentNames; pathsContainSpace = $prompt.PathsContainSpace
        auditSeenBeforeInput = $prompt.AuditSeen; aliveAtPrompt = $prompt.AliveAtPrompt; inputDelivery = $prompt.Inject; exited = $prompt.Exited
        exitCode = $prompt.InnerExitCode; typedCode = $prompt.TypedCode; auditFiles = $prompt.AuditFiles; excerpt = $prompt.ResultExcerpt })
Assert-EntryConsole $prompt.AuditSeen 'P5E_A43_ENTRY_CONSOLE_NO_AUDIT_BEFORE_PROMPT_STOP'
Assert-EntryConsole $prompt.AliveAtPrompt 'P5E_A43_ENTRY_CONSOLE_EXITED_BEFORE_PROMPT_STOP'
Assert-EntryConsole ($prompt.Inject -ceq 'OK') 'P5E_A43_ENTRY_CONSOLE_INPUT_NOT_DELIVERED_STOP'
Assert-EntryConsole $prompt.Exited 'P5E_A43_ENTRY_CONSOLE_NO_TERMINAL_RESULT_STOP'
Assert-EntryConsole ($prompt.InnerExitCode -eq 1) 'P5E_A43_ENTRY_CONSOLE_FALSE_EXIT_STOP'
Assert-EntryConsole ($prompt.TypedCode -ceq 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP') 'P5E_A43_ENTRY_CONSOLE_TYPED_RESULT_MISSING_STOP'
Assert-EntryConsole ($prompt.AuditFiles -eq 1) 'P5E_A43_ENTRY_CONSOLE_AUDIT_NOT_WRITTEN_STOP'

# Case 2 — outer failure keeps stderr cause and exit, before any audit.
$missing = Invoke-EntryConsoleCase -OmitJavaPath -PromptWaitSeconds 8
$cases.Add([ordered]@{ name = 'console-outer-failure-preserved'; entrySha256 = $missing.EntrySha256; argumentNames = $missing.ArgumentNames; pathsContainSpace = $missing.PathsContainSpace
        auditSeenBeforeInput = $missing.AuditSeen; exited = $missing.Exited; exitCode = $missing.InnerExitCode; auditFiles = $missing.AuditFiles; excerpt = $missing.ResultExcerpt })
Assert-EntryConsole $missing.Exited 'P5E_A43_ENTRY_CONSOLE_MISSING_ARG_HUNG_STOP'
Assert-EntryConsole (-not $missing.AuditSeen) 'P5E_A43_ENTRY_CONSOLE_MISSING_ARG_AUDIT_UNEXPECTED_STOP'
Assert-EntryConsole ($missing.InnerExitCode -eq 1) 'P5E_A43_ENTRY_CONSOLE_MISSING_ARG_FALSE_EXIT_STOP'
Assert-EntryConsole ($missing.Result.Contains('P5E_A43_INTEGRATION_CONTRACT_MISSING_STOP')) 'P5E_A43_ENTRY_CONSOLE_MISSING_ARG_CAUSE_LOST_STOP'

# Case 3 — approval -> key -> synthetic child on the same parent control flow,
# with the real Read-Host readers. Only the fixture is typed with the
# approval literal; the live entrypoint never is.
$probe = Invoke-ConsoleProbeCase
$f = $probe.Facts
$cases.Add([ordered]@{ name = 'console-approval-key-synthetic-child'; integrationSha256 = (Get-FileHash -LiteralPath $integrationPath -Algorithm SHA256).Hash
        fixtureReady = $probe.FixtureReady; approvalInputDelivery = $probe.ApprovalInject; ownerRootSeenAfterApproval = $probe.OwnerRootSeen
        keyInputDelivery = $probe.KeyInject; exited = $probe.Exited; exitCode = $probe.InnerExitCode
        observed = $f; excerpt = $probe.ResultExcerpt })
Assert-EntryConsole $probe.FixtureReady 'P5E_A43_ENTRY_CONSOLE_PROBE_NOT_READY_STOP'
Assert-EntryConsole ($probe.ApprovalInject -ceq 'OK') 'P5E_A43_ENTRY_CONSOLE_PROBE_APPROVAL_NOT_DELIVERED_STOP'
Assert-EntryConsole $probe.OwnerRootSeen 'P5E_A43_ENTRY_CONSOLE_PROBE_APPROVAL_NOT_ACCEPTED_STOP'
Assert-EntryConsole ($probe.KeyInject -ceq 'OK') 'P5E_A43_ENTRY_CONSOLE_PROBE_KEY_NOT_DELIVERED_STOP'
Assert-EntryConsole $probe.Exited 'P5E_A43_ENTRY_CONSOLE_PROBE_NO_TERMINAL_RESULT_STOP'
Assert-EntryConsole ($null -ne $f -and [bool]$f.completed) 'P5E_A43_ENTRY_CONSOLE_PROBE_FACTS_MISSING_STOP'
Assert-EntryConsole ($f.typedCode -ceq 'CHILD_EXIT_ZERO' -and [int]$f.childAttempts -eq 1 -and [int]$f.keyPromptCount -eq 1) 'P5E_A43_ENTRY_CONSOLE_PROBE_NOT_SUCCESS_STOP'
Assert-EntryConsole ($probe.InnerExitCode -eq 0 -and [int]$f.outerExitCode -eq 0) 'P5E_A43_ENTRY_CONSOLE_PROBE_EXIT_STOP'
Assert-EntryConsole ($f.reservationCreated -and $f.ownerRootCreated -and $f.receiptCreated) 'P5E_A43_ENTRY_CONSOLE_PROBE_STATE_STOP'
Assert-EntryConsole ([int]$f.liveCalls -eq 0 -and -not [bool]$f.environmentRetained) 'P5E_A43_ENTRY_CONSOLE_PROBE_LIVE_OR_ENV_STOP'

# Case 4 — characterization of the retained legacy bytes (not a candidate).
$legacyCopy = Join-Path ([IO.Path]::GetTempPath()) ('p5e legacy cc01 ' + [Guid]::NewGuid().ToString('N').Substring(0, 8) + '.ps1')
try {
    Copy-Item -LiteralPath $legacyEntryPath -Destination $legacyCopy
    $legacy = Invoke-EntryConsoleCase -EntryPath $legacyCopy -PromptWaitSeconds 8
} finally {
    if (Test-Path -LiteralPath $legacyCopy) { Remove-Item -LiteralPath $legacyCopy -Force }
}
$cases.Add([ordered]@{ name = 'legacy-cc01-silent-exit-defect'; entrySha256 = $legacy.EntrySha256; argumentNames = $legacy.ArgumentNames; pathsContainSpace = $legacy.PathsContainSpace
        auditSeenBeforeInput = $legacy.AuditSeen; exited = $legacy.Exited; exitCode = $legacy.InnerExitCode; auditFiles = $legacy.AuditFiles
        outputLength = ([string]$legacy.Result).Length; excerpt = $legacy.ResultExcerpt })
Assert-EntryConsole ($legacy.EntrySha256 -ceq 'CC01C33F056F86F4ACC4E3BD45AAAA2910CCEE9719AC5830EDAE99987A7C8E23') 'P5E_A43_ENTRY_CONSOLE_LEGACY_BYTES_CHANGED_STOP'
Assert-EntryConsole ($legacy.Exited -and $legacy.InnerExitCode -eq 0 -and -not $legacy.AuditSeen -and $legacy.AuditFiles -eq 0) 'P5E_A43_ENTRY_CONSOLE_LEGACY_DEFECT_NOT_OBSERVED_STOP'
Assert-EntryConsole ([string]::IsNullOrWhiteSpace($legacy.TypedCode)) 'P5E_A43_ENTRY_CONSOLE_LEGACY_UNEXPECTED_TYPED_RESULT_STOP'

# Case 5 — state-consistent candidate for the latest owner-window pre-prompt
# exit: the default audit file is named by candidate hash only, and the real
# private root already holds that file for this candidate. The stop must be an
# outer-boundary failure (stderr + exit 1) before any new audit/prompt, and the
# existing file must stay untouched. This does not prove the owner's observed
# stderr (none was captured); it shows the mechanism is sufficient.
$collision = Invoke-EntryConsoleCase -SeedHashAudit -PromptWaitSeconds 8
$cases.Add([ordered]@{ name = 'console-existing-hash-audit-collision'; entrySha256 = $collision.EntrySha256; argumentNames = $collision.ArgumentNames; pathsContainSpace = $collision.PathsContainSpace
        exited = $collision.Exited; exitCode = $collision.InnerExitCode; auditFilesAfter = $collision.AuditFiles; seedUnchanged = $collision.SeedUnchanged
        typedCode = $collision.TypedCode; excerpt = $collision.ResultExcerpt })
Assert-EntryConsole $collision.Exited 'P5E_A43_ENTRY_CONSOLE_COLLISION_HUNG_STOP'
Assert-EntryConsole ($collision.InnerExitCode -eq 1) 'P5E_A43_ENTRY_CONSOLE_COLLISION_FALSE_EXIT_STOP'
Assert-EntryConsole ($collision.Result.Contains('P5E_A43_AUDIT_ALREADY_EXISTS_STOP')) 'P5E_A43_ENTRY_CONSOLE_COLLISION_CAUSE_LOST_STOP'
Assert-EntryConsole ($collision.AuditFiles -eq 1 -and [bool]$collision.SeedUnchanged -and [string]::IsNullOrWhiteSpace($collision.TypedCode)) 'P5E_A43_ENTRY_CONSOLE_COLLISION_STATE_CHANGED_STOP'

# Case 6 — same pre-existing hash-named audit, but with a per-decision explicit
# -AuditPath (the run guide's required form): prompt is reached and the typed
# terminal result is written at the explicit path; the old file is untouched.
$explicit = Invoke-EntryConsoleCase -SeedHashAudit -ExplicitAuditPath -Typed 'WRONG_LITERAL'
$cases.Add([ordered]@{ name = 'console-explicit-audit-path-after-existing-hash-audit'; entrySha256 = $explicit.EntrySha256; argumentNames = $explicit.ArgumentNames; pathsContainSpace = $explicit.PathsContainSpace
        auditSeenBeforeInput = $explicit.AuditSeen; aliveAtPrompt = $explicit.AliveAtPrompt; inputDelivery = $explicit.Inject; exited = $explicit.Exited
        exitCode = $explicit.InnerExitCode; typedCode = $explicit.TypedCode; explicitAuditFiles = $explicit.AuditFiles; seedUnchanged = $explicit.SeedUnchanged; excerpt = $explicit.ResultExcerpt })
Assert-EntryConsole ($explicit.AliveAtPrompt -and $explicit.Inject -ceq 'OK') 'P5E_A43_ENTRY_CONSOLE_EXPLICIT_PROMPT_NOT_REACHED_STOP'
Assert-EntryConsole ($explicit.Exited -and $explicit.InnerExitCode -eq 1 -and $explicit.TypedCode -ceq 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP') 'P5E_A43_ENTRY_CONSOLE_EXPLICIT_RESULT_STOP'
Assert-EntryConsole ($explicit.AuditFiles -eq 1 -and [bool]$explicit.SeedUnchanged) 'P5E_A43_ENTRY_CONSOLE_EXPLICIT_AUDIT_STATE_STOP'

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    $ReportPath = Join-Path $repoRoot 'evidence\p5e-a43-parent-integration-20260930\P5E_A43_ENTRY_CONSOLE_QA_03.json'
}
[void](New-Item -ItemType Directory -Path (Split-Path -Parent ([IO.Path]::GetFullPath($ReportPath))) -Force)
$caseArray = $cases.ToArray()
$caseTotal = $caseArray.Length
$report = [ordered]@{
    status = 'PASS'; suite = 'P5E_A43_ENTRY_CONSOLE_OFFLINE'; schemaVersion = 2
    liveActions = 0; realApprovalOrKeyInput = $false; syntheticInputsOnly = $true; adb = $false; provider = $false
    consoleKind = 'hidden-window real console via WriteConsoleInput; not owner-visible'
    entrypointSha256 = (Get-FileHash -LiteralPath $entrypointPath -Algorithm SHA256).Hash
    caseCount = $caseTotal; cases = $caseArray
}
$reportJson = $report | ConvertTo-Json -Depth 8
Set-Content -LiteralPath ([IO.Path]::GetFullPath($ReportPath)) -Value $reportJson -Encoding UTF8
$reportJson
exit 0
