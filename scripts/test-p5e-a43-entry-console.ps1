[CmdletBinding()]
param(
    [string]$ReportPath = ''
)

# Real-console entry QA. Unlike test-p5e-a43-entry-boundary.ps1 (redirected,
# noninteractive), the entrypoint here runs in its own console and receives
# keystrokes through WriteConsoleInput. Only a WRONG literal is ever typed;
# no key, ADB, device or provider is reachable. PrivateRoot is a temp path
# containing a space.

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$entrypointPath = Join-Path $PSScriptRoot 'p5e-a43-pre-reservation-launcher-entrypoint.ps1'
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

function Invoke-EntryConsoleCase {
    param([switch]$OmitJavaPath, [string]$Typed = '', [int]$PromptWaitSeconds = 12)
    $private = Join-Path ([IO.Path]::GetTempPath()) ('p5e entry console ' + [Guid]::NewGuid().ToString('N').Substring(0, 8))
    [void](New-Item -ItemType Directory -Path $private)
    $resultPath = Join-Path $private 'outer-result.txt'
    $outerPath = Join-Path ([IO.Path]::GetTempPath()) ('p5e-entry-console-outer-' + [Guid]::NewGuid().ToString('N').Substring(0, 8) + '.ps1')
    $hash = (Get-FileHash -LiteralPath $entrypointPath -Algorithm SHA256).Hash
    $argText = "'-NoLogo','-NoProfile','-ExecutionPolicy','Bypass','-File','$entrypointPath','-Execute'," +
        "'-RepoRoot','$repoRoot','-PrivateRoot','$private'," +
        "'-AndroidSdkPath','$env:LOCALAPPDATA\Android\Sdk'," +
        "'-ExpectedCandidateSha256','$hash','-DecisionId',('p5e-synth-' + [Guid]::NewGuid().ToString('N'))"
    if (-not $OmitJavaPath) { $argText += ",'-JavaPath','C:\Program Files\Android\Android Studio\jbr\bin\java.exe'" }
    $outer = "`$a = @($argText)`r`n" +
        "`$o = & '$powershellPath' @a 2>&1`r`n`$c = `$LASTEXITCODE`r`n" +
        "[IO.File]::WriteAllText('$resultPath', ('EXIT=' + `$c + [Environment]::NewLine + (`$o | Out-String)))`r`n"
    [IO.File]::WriteAllText($outerPath, $outer, [Text.UTF8Encoding]::new($false))
    $proc = $null
    try {
        $proc = Start-Process -FilePath $powershellPath -ArgumentList @('-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $outerPath + '"')) -WindowStyle Hidden -PassThru
        $auditSeen = $false
        $deadline = [DateTime]::UtcNow.AddSeconds($PromptWaitSeconds)
        while ([DateTime]::UtcNow -lt $deadline -and -not $proc.HasExited -and -not $auditSeen) {
            Start-Sleep -Milliseconds 250
            $auditSeen = Test-Path -LiteralPath (Join-Path $private '.p5e-a43-audit')
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
        if (-not $exited) {
            Get-CimInstance Win32_Process -Filter "ParentProcessId=$($proc.Id)" | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        }
        $result = if (Test-Path -LiteralPath $resultPath) { Get-Content -Raw -LiteralPath $resultPath } else { '' }
        return [pscustomobject][ordered]@{
            AuditSeen = $auditSeen; AliveAtPrompt = $aliveAtPrompt; Inject = $inject; Exited = $exited; Result = $result
            AuditFiles = @(Get-ChildItem -LiteralPath (Join-Path $private '.p5e-a43-audit') -File -ErrorAction SilentlyContinue).Count
        }
    } finally {
        if (Test-Path -LiteralPath $outerPath) { Remove-Item -LiteralPath $outerPath -Force }
        if (Test-Path -LiteralPath $private) { Remove-Item -LiteralPath $private -Recurse -Force }
    }
}

$results = New-Object 'System.Collections.Generic.List[object]'

# Success path: reaches the approval prompt in a live console and returns a
# typed terminal result with a nonzero exit after a wrong literal.
$prompt = Invoke-EntryConsoleCase -Typed 'WRONG_LITERAL'
Assert-EntryConsole $prompt.AuditSeen 'P5E_A43_ENTRY_CONSOLE_NO_AUDIT_BEFORE_PROMPT_STOP'
Assert-EntryConsole $prompt.AliveAtPrompt 'P5E_A43_ENTRY_CONSOLE_EXITED_BEFORE_PROMPT_STOP'
Assert-EntryConsole ($prompt.Inject -ceq 'OK') 'P5E_A43_ENTRY_CONSOLE_INPUT_NOT_DELIVERED_STOP'
Assert-EntryConsole $prompt.Exited 'P5E_A43_ENTRY_CONSOLE_NO_TERMINAL_RESULT_STOP'
Assert-EntryConsole ($prompt.Result -match '(?m)^EXIT=1\r?$') 'P5E_A43_ENTRY_CONSOLE_FALSE_EXIT_STOP'
Assert-EntryConsole ($prompt.Result.Contains('"TypedCode":"P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP"')) 'P5E_A43_ENTRY_CONSOLE_TYPED_RESULT_MISSING_STOP'
Assert-EntryConsole ($prompt.AuditFiles -eq 1) 'P5E_A43_ENTRY_CONSOLE_AUDIT_NOT_WRITTEN_STOP'
$results.Add([pscustomobject][ordered]@{ Name = 'console-prompt-wrong-literal'; Pass = $true; ExitCode = 1; TypedCode = 'P5E_OWNER_APPROVAL_LITERAL_REQUIRED_STOP' })

# Failure path: an outer-boundary failure must keep the stderr text and a
# nonzero exit, and must happen before any audit exists.
$missing = Invoke-EntryConsoleCase -OmitJavaPath -PromptWaitSeconds 8
Assert-EntryConsole $missing.Exited 'P5E_A43_ENTRY_CONSOLE_MISSING_ARG_HUNG_STOP'
Assert-EntryConsole (-not $missing.AuditSeen) 'P5E_A43_ENTRY_CONSOLE_MISSING_ARG_AUDIT_UNEXPECTED_STOP'
Assert-EntryConsole ($missing.Result -match '(?m)^EXIT=1\r?$') 'P5E_A43_ENTRY_CONSOLE_MISSING_ARG_FALSE_EXIT_STOP'
Assert-EntryConsole ($missing.Result.Contains('P5E_A43_INTEGRATION_CONTRACT_MISSING_STOP')) 'P5E_A43_ENTRY_CONSOLE_MISSING_ARG_CAUSE_LOST_STOP'
$results.Add([pscustomobject][ordered]@{ Name = 'console-outer-failure-preserved'; Pass = $true; ExitCode = 1; Cause = 'P5E_A43_INTEGRATION_CONTRACT_MISSING_STOP' })

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    $ReportPath = Join-Path $repoRoot 'evidence\p5e-a43-parent-integration-20260930\P5E_A43_ENTRY_CONSOLE_QA.json'
}
[void](New-Item -ItemType Directory -Path (Split-Path -Parent ([IO.Path]::GetFullPath($ReportPath))) -Force)
$reportJson = '{"status":"PASS","suite":"P5E_A43_ENTRY_CONSOLE_OFFLINE","liveActions":0,"approvalOrKeyInput":false,"typedLiteral":"WRONG_LITERAL","adb":false,"entrypointSha256":"' + (Get-FileHash -LiteralPath $entrypointPath -Algorithm SHA256).Hash + '","caseCount":' + [string]$results.Count + '}'
Set-Content -LiteralPath ([IO.Path]::GetFullPath($ReportPath)) -Value $reportJson -Encoding UTF8
$reportJson
exit 0
