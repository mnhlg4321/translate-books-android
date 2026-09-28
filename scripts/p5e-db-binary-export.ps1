[CmdletBinding()]
param(
    [switch]$LibraryOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:P5EDatabaseExporterContractVersion = 'p5e.raw.db-binary-export.v1'
$script:P5EDatabaseExporterMaxBytes = 134217728L
$script:P5EDatabaseExporterMaxStderrBytes = 65536

function Get-P5EDatabaseExportTypedStop {
    param([Parameter(Mandatory = $true)][string]$Code)
    throw $Code
}

function ConvertTo-P5EDatabaseExportWindowsArgument {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Value)
    if ($Value.Length -gt 0 -and $Value -notmatch '[\s"]') { return $Value }
    $builder = [Text.StringBuilder]::new()
    [void]$builder.Append('"')
    $backslashes = 0
    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') {
            $backslashes++
            continue
        }
        if ($character -eq '"') {
            [void]$builder.Append(('\' * (2 * $backslashes + 1)))
            [void]$builder.Append('"')
            $backslashes = 0
            continue
        }
        if ($backslashes -gt 0) {
            [void]$builder.Append(('\' * $backslashes))
            $backslashes = 0
        }
        [void]$builder.Append($character)
    }
    if ($backslashes -gt 0) { [void]$builder.Append(('\' * (2 * $backslashes))) }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Set-P5EDatabaseExportProcessArguments {
    param(
        [Parameter(Mandatory = $true)][System.Diagnostics.ProcessStartInfo]$StartInfo,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList
    )
    $StartInfo.Arguments = [string]::Join(' ', @($ArgumentList | ForEach-Object {
        ConvertTo-P5EDatabaseExportWindowsArgument -Value ([string]$_)
    }))
}

if ($null -eq ('P5EDatabaseExportCapture' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading.Tasks;
using System.Threading;
using System.Runtime.InteropServices;
using System.Collections;
using System.Collections.Generic;
using Microsoft.Win32.SafeHandles;

public sealed class P5EProcessLaunchHandle : IDisposable
{
    internal IntPtr NativeProcessHandle;
    internal IntPtr NativeThreadHandle;
    public Process Process { get; internal set; }
    public Stream StandardInput { get; internal set; }
    public Stream StandardOutput { get; internal set; }
    public Stream StandardError { get; internal set; }
    public bool Resumed { get; internal set; }
    public bool Closed { get; internal set; }

    public bool Resume()
    {
        if (Closed) { return false; }
        if (Resumed) { return true; }
        if (NativeThreadHandle == IntPtr.Zero) { return false; }
        if (P5EProcessLauncher.ResumeSuspendedThread(NativeThreadHandle) == UInt32.MaxValue) { return false; }
        P5EProcessLauncher.CloseNativeHandle(NativeThreadHandle);
        NativeThreadHandle = IntPtr.Zero;
        Resumed = true;
        return true;
    }

    public void Abort()
    {
        if (Closed) { return; }
        if (!Resumed && NativeProcessHandle != IntPtr.Zero)
        {
            try { P5EProcessLauncher.TerminateSuspendedProcess(NativeProcessHandle, 137U); } catch { }
            try { P5EProcessLauncher.WaitForProcessExit(NativeProcessHandle, 5000U); } catch { }
        }
        CloseNativeHandles();
        CloseStreams();
        Closed = true;
    }

    internal void CloseNativeHandles()
    {
        if (NativeThreadHandle != IntPtr.Zero) { P5EProcessLauncher.CloseNativeHandle(NativeThreadHandle); NativeThreadHandle = IntPtr.Zero; }
        if (NativeProcessHandle != IntPtr.Zero) { P5EProcessLauncher.CloseNativeHandle(NativeProcessHandle); NativeProcessHandle = IntPtr.Zero; }
    }

    internal void CloseStreams()
    {
        try { if (StandardInput != null) { StandardInput.Dispose(); } } catch { }
        try { if (StandardOutput != null) { StandardOutput.Dispose(); } } catch { }
        try { if (StandardError != null) { StandardError.Dispose(); } } catch { }
        StandardInput = null;
        StandardOutput = null;
        StandardError = null;
    }

    public void Dispose()
    {
        if (Closed) { return; }
        CloseNativeHandles();
        CloseStreams();
        Closed = true;
    }
}

public static class P5EProcessLauncher
{
    private const uint CreateSuspended = 0x00000004;
    private const uint CreateNoWindow = 0x08000000;
    private const uint CreateUnicodeEnvironment = 0x00000400;
    private const uint StartfUseStdHandles = 0x00000100;
    private const uint HandleFlagInherit = 0x00000001;
    private const uint WaitTimeout = 0x00000102;

    [StructLayout(LayoutKind.Sequential)]
    private struct SecurityAttributes
    {
        public int Length;
        public IntPtr SecurityDescriptor;
        public bool InheritHandle;
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct StartupInfo
    {
        public int Cb;
        public string Reserved;
        public string Desktop;
        public string Title;
        public uint X;
        public uint Y;
        public uint XSize;
        public uint YSize;
        public uint XCountChars;
        public uint YCountChars;
        public uint FillAttribute;
        public uint Flags;
        public ushort ShowWindow;
        public ushort Reserved2;
        public IntPtr Reserved2Pointer;
        public IntPtr StandardInput;
        public IntPtr StandardOutput;
        public IntPtr StandardError;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct ProcessInformation
    {
        public IntPtr Process;
        public IntPtr Thread;
        public uint ProcessId;
        public uint ThreadId;
    }

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool CreateProcess(
        string applicationName,
        System.Text.StringBuilder commandLine,
        IntPtr processAttributes,
        IntPtr threadAttributes,
        bool inheritHandles,
        uint creationFlags,
        IntPtr environment,
        string currentDirectory,
        ref StartupInfo startupInfo,
        out ProcessInformation processInformation);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool CreatePipe(out IntPtr readPipe, out IntPtr writePipe, ref SecurityAttributes attributes, uint size);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool SetHandleInformation(IntPtr handle, uint mask, uint flags);

    [DllImport("kernel32.dll", SetLastError = true)]
    internal static extern bool CloseHandle(IntPtr handle);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern uint ResumeThread(IntPtr thread);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool TerminateProcess(IntPtr process, uint exitCode);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern uint WaitForSingleObject(IntPtr handle, uint milliseconds);

    private static int LastError()
    {
        return Marshal.GetLastWin32Error();
    }

    internal static uint ResumeSuspendedThread(IntPtr thread)
    {
        return ResumeThread(thread);
    }

    internal static void TerminateSuspendedProcess(IntPtr process, uint exitCode)
    {
        if (!TerminateProcess(process, exitCode)) { throw new System.ComponentModel.Win32Exception(LastError()); }
    }

    internal static void WaitForProcessExit(IntPtr process, uint milliseconds)
    {
        WaitForSingleObject(process, milliseconds);
    }

    internal static void CloseNativeHandle(IntPtr handle)
    {
        if (handle != IntPtr.Zero) { CloseHandle(handle); }
    }

    private static string BuildEnvironmentBlock(ProcessStartInfo startInfo)
    {
        var names = new List<string>();
        foreach (DictionaryEntry entry in startInfo.EnvironmentVariables) { names.Add((string)entry.Key); }
        names.Sort(StringComparer.OrdinalIgnoreCase);
        var builder = new StringBuilder();
        foreach (var name in names)
        {
            builder.Append(name);
            builder.Append('=');
            var value = startInfo.EnvironmentVariables[name];
            if (value != null) { builder.Append(value); }
            builder.Append('\0');
        }
        builder.Append('\0');
        return builder.ToString();
    }

    public static P5EProcessLaunchHandle Start(ProcessStartInfo startInfo)
    {
        if (startInfo == null) { throw new ArgumentNullException("startInfo"); }
        if (String.IsNullOrWhiteSpace(startInfo.FileName)) { throw new ArgumentException("FileName"); }
        var stdoutRead = IntPtr.Zero;
        var stdoutWrite = IntPtr.Zero;
        var stderrRead = IntPtr.Zero;
        var stderrWrite = IntPtr.Zero;
        var stdinRead = IntPtr.Zero;
        var stdinWrite = IntPtr.Zero;
        var processInformation = new ProcessInformation();
        var created = false;
        try
        {
            var attributes = new SecurityAttributes { Length = Marshal.SizeOf(typeof(SecurityAttributes)), SecurityDescriptor = IntPtr.Zero, InheritHandle = true };
            if (!CreatePipe(out stdoutRead, out stdoutWrite, ref attributes, 0U)) { throw new System.ComponentModel.Win32Exception(LastError()); }
            if (!SetHandleInformation(stdoutRead, HandleFlagInherit, 0U)) { throw new System.ComponentModel.Win32Exception(LastError()); }
            if (!CreatePipe(out stderrRead, out stderrWrite, ref attributes, 0U)) { throw new System.ComponentModel.Win32Exception(LastError()); }
            if (!SetHandleInformation(stderrRead, HandleFlagInherit, 0U)) { throw new System.ComponentModel.Win32Exception(LastError()); }
            if (!CreatePipe(out stdinRead, out stdinWrite, ref attributes, 0U)) { throw new System.ComponentModel.Win32Exception(LastError()); }
            if (!SetHandleInformation(stdinWrite, HandleFlagInherit, 0U)) { throw new System.ComponentModel.Win32Exception(LastError()); }

            var startup = new StartupInfo();
            startup.Cb = Marshal.SizeOf(typeof(StartupInfo));
            startup.Flags = StartfUseStdHandles;
            startup.StandardInput = stdinRead;
            startup.StandardOutput = stdoutWrite;
            startup.StandardError = stderrWrite;
            var applicationName = startInfo.FileName;
            var commandText = "\"" + startInfo.FileName.Replace("\"", "\\\"") + "\"" +
                (String.IsNullOrWhiteSpace(startInfo.Arguments) ? String.Empty : " " + startInfo.Arguments);
            var extension = Path.GetExtension(startInfo.FileName);
            if (String.Equals(extension, ".cmd", StringComparison.OrdinalIgnoreCase) || String.Equals(extension, ".bat", StringComparison.OrdinalIgnoreCase))
            {
                applicationName = Environment.GetEnvironmentVariable("ComSpec");
                if (String.IsNullOrWhiteSpace(applicationName)) { applicationName = "cmd.exe"; }
                commandText = "/d /s /c \"\"" + startInfo.FileName.Replace("\"", "\\\"") + "\"" +
                    (String.IsNullOrWhiteSpace(startInfo.Arguments) ? String.Empty : " " + startInfo.Arguments) + "\"";
            }
            var command = new StringBuilder(commandText);
            var environment = BuildEnvironmentBlock(startInfo);
            var environmentPointer = Marshal.StringToHGlobalUni(environment);
            try
            {
                var workingDirectory = String.IsNullOrWhiteSpace(startInfo.WorkingDirectory) ? null : startInfo.WorkingDirectory;
                var flags = CreateSuspended | CreateNoWindow | CreateUnicodeEnvironment;
                if (!CreateProcess(applicationName, command, IntPtr.Zero, IntPtr.Zero, true, flags, environmentPointer, workingDirectory, ref startup, out processInformation))
                {
                    throw new System.ComponentModel.Win32Exception(LastError());
                }
                created = true;
            }
            finally { Marshal.FreeHGlobal(environmentPointer); }

            CloseNativeHandle(stdoutWrite); stdoutWrite = IntPtr.Zero;
            CloseNativeHandle(stderrWrite); stderrWrite = IntPtr.Zero;
            CloseNativeHandle(stdinRead); stdinRead = IntPtr.Zero;
            var launch = new P5EProcessLaunchHandle();
            launch.NativeProcessHandle = processInformation.Process;
            launch.NativeThreadHandle = processInformation.Thread;
            launch.Process = Process.GetProcessById((int)processInformation.ProcessId);
            launch.StandardOutput = new FileStream(new SafeFileHandle(stdoutRead, true), FileAccess.Read);
            stdoutRead = IntPtr.Zero;
            launch.StandardError = new FileStream(new SafeFileHandle(stderrRead, true), FileAccess.Read);
            stderrRead = IntPtr.Zero;
            launch.StandardInput = new FileStream(new SafeFileHandle(stdinWrite, true), FileAccess.Write);
            stdinWrite = IntPtr.Zero;
            return launch;
        }
        catch
        {
            if (created && processInformation.Process != IntPtr.Zero) { try { TerminateProcess(processInformation.Process, 137U); } catch { } }
            CloseNativeHandle(processInformation.Thread);
            CloseNativeHandle(processInformation.Process);
            throw;
        }
        finally
        {
            CloseNativeHandle(stdoutRead);
            CloseNativeHandle(stdoutWrite);
            CloseNativeHandle(stderrRead);
            CloseNativeHandle(stderrWrite);
            CloseNativeHandle(stdinRead);
            CloseNativeHandle(stdinWrite);
        }
    }
}

public sealed class P5EProcessContainmentHandle : IDisposable
{
    internal IntPtr JobHandle;
    public bool Attached { get; internal set; }
    public string AttachStatus { get; internal set; }
    public int ProcessId { get; internal set; }
    public int NativeErrorCode { get; internal set; }
    public bool Closed { get; internal set; }

    public P5EProcessContainmentHandle()
    {
        JobHandle = IntPtr.Zero;
        AttachStatus = "NOT_ATTEMPTED";
    }

    public void Dispose()
    {
        if (!Closed && JobHandle != IntPtr.Zero)
        {
            try { P5EProcessTermination.CloseJob(JobHandle); } catch { }
            Closed = true;
            Attached = false;
        }
    }
}

public sealed class P5EProcessContainmentResult
{
    public P5EProcessContainmentResult()
    {
        Status = "NOT_ATTEMPTED";
        NativeErrorCode = 0;
    }
    public string Status { get; set; }
    public bool Verified { get; set; }
    public bool ParentExited { get; set; }
    public int ActiveProcessCount { get; set; }
    public int NativeErrorCode { get; set; }
}

public static class P5EProcessTermination
{
    private const uint JobObjectLimitKillOnJobClose = 0x00002000;
    private const int JobObjectExtendedLimitInformationClass = 9;
    private const int JobObjectBasicAccountingInformationClass = 1;

    [StructLayout(LayoutKind.Sequential)]
    private struct JoBObjectBasicLimitInformation
    {
        public long PerProcessUserTimeLimit;
        public long PerJobUserTimeLimit;
        public uint LimitFlags;
        public UIntPtr MinimumWorkingSetSize;
        public UIntPtr MaximumWorkingSetSize;
        public uint ActiveProcessLimit;
        public UIntPtr Affinity;
        public uint PriorityClass;
        public uint SchedulingClass;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct IoCounters
    {
        public ulong ReadOperationCount;
        public ulong WriteOperationCount;
        public ulong OtherOperationCount;
        public ulong ReadTransferCount;
        public ulong WriteTransferCount;
        public ulong OtherTransferCount;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct JobObjectExtendedLimitInformationStruct
    {
        public JoBObjectBasicLimitInformation BasicLimitInformation;
        public IoCounters IoInfo;
        public UIntPtr ProcessMemoryLimit;
        public UIntPtr JobMemoryLimit;
        public UIntPtr PeakProcessMemoryUsed;
        public UIntPtr PeakJobMemoryUsed;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct JobObjectBasicAccountingInformation
    {
        public long TotalUserTime;
        public long TotalKernelTime;
        public long ThisPeriodTotalUserTime;
        public long ThisPeriodTotalKernelTime;
        public uint TotalPageFaultCount;
        public uint TotalProcesses;
        public uint ActiveProcesses;
        public uint TotalTerminatedProcesses;
    }

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern IntPtr CreateJobObject(IntPtr jobAttributes, string name);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool SetInformationJobObject(IntPtr job, int informationClass, IntPtr information, uint length);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool TerminateJobObject(IntPtr job, uint exitCode);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool QueryInformationJobObject(IntPtr job, int informationClass, IntPtr information, uint length, out uint returnLength);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool CloseHandle(IntPtr handle);

    internal static void CloseJob(IntPtr job)
    {
        if (job != IntPtr.Zero) { CloseHandle(job); }
    }

    private static int LastError()
    {
        return Marshal.GetLastWin32Error();
    }

    public static P5EProcessContainmentHandle Attach(Process process)
    {
        var result = new P5EProcessContainmentHandle();
        if (process == null) { result.AttachStatus = "PROCESS_NULL"; return result; }
        result.ProcessId = process.Id;
        var job = CreateJobObject(IntPtr.Zero, null);
        if (job == IntPtr.Zero)
        {
            result.AttachStatus = "CREATE_JOB_FAILED";
            return result;
        }
        var limits = new JobObjectExtendedLimitInformationStruct();
        limits.BasicLimitInformation.LimitFlags = JobObjectLimitKillOnJobClose;
        var limitsPointer = Marshal.AllocHGlobal(Marshal.SizeOf(typeof(JobObjectExtendedLimitInformationStruct)));
        try
        {
            Marshal.StructureToPtr(limits, limitsPointer, false);
            if (!SetInformationJobObject(job, JobObjectExtendedLimitInformationClass, limitsPointer, (uint)Marshal.SizeOf(typeof(JobObjectExtendedLimitInformationStruct))))
            {
                result.AttachStatus = "CONFIGURE_JOB_FAILED";
                result.JobHandle = job;
                result.NativeErrorCode = LastError();
                CloseJob(job);
                result.JobHandle = IntPtr.Zero;
                return result;
            }
        }
        finally { Marshal.FreeHGlobal(limitsPointer); }
        try
        {
            if (!AssignProcessToJobObject(job, process.Handle))
            {
                result.AttachStatus = "ASSIGN_PROCESS_FAILED";
                result.NativeErrorCode = LastError();
                CloseJob(job);
                return result;
            }
        }
        catch
        {
            result.AttachStatus = "ASSIGN_PROCESS_EXCEPTION";
            result.NativeErrorCode = LastError();
            CloseJob(job);
            return result;
        }
        result.JobHandle = job;
        result.Attached = true;
        result.AttachStatus = "JOB_OBJECT_ATTACHED_KILL_ON_CLOSE";
        return result;
    }

    private static bool HasExited(Process process)
    {
        try { process.Refresh(); return process.HasExited; } catch { return false; }
    }

    private static bool TryGetActiveProcessCount(P5EProcessContainmentHandle containment, out int count, out int errorCode)
    {
        count = -1;
        errorCode = 0;
        if (containment == null || !containment.Attached || containment.JobHandle == IntPtr.Zero)
        {
            errorCode = 6;
            return false;
        }
        var size = Marshal.SizeOf(typeof(JobObjectBasicAccountingInformation));
        var pointer = Marshal.AllocHGlobal(size);
        try
        {
            uint returned;
            if (!QueryInformationJobObject(containment.JobHandle, JobObjectBasicAccountingInformationClass, pointer, (uint)size, out returned))
            {
                errorCode = LastError();
                return false;
            }
            var info = (JobObjectBasicAccountingInformation)Marshal.PtrToStructure(pointer, typeof(JobObjectBasicAccountingInformation));
            count = (int)info.ActiveProcesses;
            return true;
        }
        finally { Marshal.FreeHGlobal(pointer); }
    }

    public static P5EProcessContainmentResult EnsureTerminated(P5EProcessContainmentHandle containment, Process process, bool requestTermination, int waitMilliseconds)
    {
        var result = new P5EProcessContainmentResult();
        if (containment == null || !containment.Attached || containment.JobHandle == IntPtr.Zero)
        {
            result.Status = "EXTERNAL_PROCESS_STATE_UNKNOWN";
            return result;
        }
        var terminationRequested = false;
        if (requestTermination)
        {
            if (!TerminateJobObject(containment.JobHandle, 137U))
            {
                result.Status = "JOB_TERMINATE_FAILED";
                result.NativeErrorCode = LastError();
                return result;
            }
            terminationRequested = true;
        }
        var started = DateTime.UtcNow;
        while (true)
        {
            int active;
            int errorCode;
            var querySucceeded = TryGetActiveProcessCount(containment, out active, out errorCode);
            result.ActiveProcessCount = active;
            result.NativeErrorCode = errorCode;
            result.ParentExited = HasExited(process);
            if (!querySucceeded)
            {
                result.Status = "JOB_QUERY_FAILED";
                break;
            }
            if (result.ParentExited && active == 0)
            {
                result.Verified = true;
                result.Status = "TREE_EXIT_VERIFIED";
                break;
            }
            if (!terminationRequested)
            {
                if (!TerminateJobObject(containment.JobHandle, 137U))
                {
                    result.Status = "JOB_TERMINATE_FAILED";
                    result.NativeErrorCode = LastError();
                    break;
                }
                terminationRequested = true;
            }
            if ((DateTime.UtcNow - started).TotalMilliseconds >= waitMilliseconds)
            {
                result.Status = "TREE_EXIT_NOT_VERIFIED";
                break;
            }
            Thread.Sleep(25);
        }
        if (result.Verified)
        {
            containment.Dispose();
        }
        else
        {
            result.Status = "EXTERNAL_PROCESS_STATE_UNKNOWN";
        }
        return result;
    }

    public static P5EProcessContainmentResult TerminateAndVerify(P5EProcessContainmentHandle containment, Process process, int waitMilliseconds)
    {
        return EnsureTerminated(containment, process, true, waitMilliseconds);
    }

    public static P5EProcessContainmentResult VerifyAndClose(P5EProcessContainmentHandle containment, Process process, int waitMilliseconds)
    {
        return EnsureTerminated(containment, process, false, waitMilliseconds);
    }

    public static P5EProcessContainmentResult KillTree(Process process)
    {
        var containment = Attach(process);
        if (containment.Attached)
        {
            return TerminateAndVerify(containment, process, 5000);
        }
        return new P5EProcessContainmentResult { Status = "EXTERNAL_PROCESS_STATE_UNKNOWN", Verified = false, ParentExited = HasExited(process) };
    }
}

public sealed class P5EDatabaseExportCaptureResult
{
    public P5EDatabaseExportCaptureResult()
    {
        Stderr = "";
        CaptureErrorClass = "";
        ContainmentStatus = "NOT_ATTEMPTED";
        StdoutDrainStatus = "NOT_STARTED";
        StderrDrainStatus = "NOT_STARTED";
    }
    public long ByteLength { get; set; }
    public long StderrByteLength { get; set; }
    public string Stderr { get; set; }
    public bool StdoutTruncated { get; set; }
    public bool StderrTruncated { get; set; }
    public bool CaptureBounded { get; set; }
    public bool TimedOut { get; set; }
    public int? ExitCode { get; set; }
    public string CaptureErrorClass { get; set; }
    public string ContainmentStatus { get; set; }
    public string StdoutDrainStatus { get; set; }
    public string StderrDrainStatus { get; set; }
    public bool ContainmentVerified { get; set; }
    public bool OutputTooLarge { get; set; }
}

internal sealed class P5EDatabaseExportStreamResult
{
    public P5EDatabaseExportStreamResult()
    {
        Text = "";
        ErrorClass = "";
        Completed = false;
    }
    public long ByteLength { get; set; }
    public bool Truncated { get; set; }
    public string Text { get; set; }
    public string ErrorClass { get; set; }
    public bool Completed { get; set; }
}

public static class P5EDatabaseExportCapture
{
    private static P5EDatabaseExportStreamResult CopyStdout(Stream input, string path, long maximumBytes)
    {
        var result = new P5EDatabaseExportStreamResult();
        try
        {
            using (var output = new FileStream(path, FileMode.CreateNew, FileAccess.Write, FileShare.None, 65536, FileOptions.SequentialScan))
            {
                var buffer = new byte[65536];
                int read;
                while ((read = input.Read(buffer, 0, buffer.Length)) > 0)
                {
                    if (result.ByteLength + read <= maximumBytes)
                    {
                        output.Write(buffer, 0, read);
                    }
                    else
                    {
                        result.Truncated = true;
                        var remaining = Math.Max(0L, maximumBytes - result.ByteLength);
                        if (remaining > 0)
                        {
                            output.Write(buffer, 0, (int)Math.Min(remaining, read));
                        }
                    }
                    result.ByteLength += read;
                }
                output.Flush(true);
            }
            result.Completed = true;
        }
        catch (Exception exception)
        {
            result.ErrorClass = exception.GetType().Name;
        }
        return result;
    }

    private static P5EDatabaseExportStreamResult ReadStderr(Stream input, int maximumBytes)
    {
        var result = new P5EDatabaseExportStreamResult();
        using (var buffer = new MemoryStream())
        {
            var chunk = new byte[8192];
            int read;
            try
            {
                while ((read = input.Read(chunk, 0, chunk.Length)) > 0)
                {
                    if (result.ByteLength < maximumBytes)
                    {
                        var remaining = maximumBytes - result.ByteLength;
                        buffer.Write(chunk, 0, (int)Math.Min(remaining, read));
                    }
                    if (result.ByteLength + read > maximumBytes) { result.Truncated = true; }
                    result.ByteLength += read;
                }
                result.Text = Encoding.UTF8.GetString(buffer.ToArray());
                result.Completed = true;
            }
            catch (Exception exception)
            {
                result.ErrorClass = exception.GetType().Name;
            }
        }
        return result;
    }

    private static P5EDatabaseExportCaptureResult CaptureCore(
        Process process,
        Stream stdoutStream,
        Stream stderrStream,
        P5EProcessContainmentHandle containment,
        string partialPath,
        long maximumStdoutBytes,
        int maximumStderrBytes,
        int timeoutMilliseconds)
    {
        var result = new P5EDatabaseExportCaptureResult();
        Task<P5EDatabaseExportStreamResult> stdoutTask = null;
        Task<P5EDatabaseExportStreamResult> stderrTask = null;
        P5EProcessContainmentResult terminalResult = null;
        var initialDrainWaitCompleted = false;
        result.ContainmentStatus = containment.AttachStatus;
        try
        {
            stdoutTask = Task.Run(() => CopyStdout(stdoutStream, partialPath, maximumStdoutBytes));
            stderrTask = Task.Run(() => ReadStderr(stderrStream, maximumStderrBytes));
            if (containment.Attached && !process.WaitForExit(timeoutMilliseconds))
            {
                result.TimedOut = true;
                terminalResult = P5EProcessTermination.TerminateAndVerify(containment, process, 5000);
                result.ContainmentStatus = terminalResult.Status;
                result.ContainmentVerified = terminalResult.Verified;
            }
            initialDrainWaitCompleted = Task.WaitAll(new Task[] { stdoutTask, stderrTask }, 5000);
            if (!initialDrainWaitCompleted)
            {
                result.CaptureErrorClass = "TASK_WAIT_TIMEOUT";
                if (containment.Attached) { terminalResult = P5EProcessTermination.TerminateAndVerify(containment, process, 5000); }
                try { Task.WaitAll(new Task[] { stdoutTask, stderrTask }, 5000); } catch { }
            }
            var stdoutCompleted = stdoutTask.IsCompleted;
            var stderrCompleted = stderrTask.IsCompleted;
            var stdout = stdoutCompleted ? stdoutTask.GetAwaiter().GetResult() : null;
            var stderr = stderrCompleted ? stderrTask.GetAwaiter().GetResult() : null;
            if (stdoutCompleted)
            {
                result.StdoutDrainStatus = stdout.ErrorClass.Length == 0
                    ? (initialDrainWaitCompleted ? "DRAINED" : "DRAIN_COMPLETED_AFTER_TIMEOUT")
                    : (initialDrainWaitCompleted ? "DRAIN_FAILED" : "DRAIN_FAILED_AFTER_TIMEOUT");
                result.ByteLength = stdout.ByteLength;
                result.StdoutTruncated = stdout.Truncated;
                if (stdout.ErrorClass.Length > 0 && String.IsNullOrEmpty(result.CaptureErrorClass)) { result.CaptureErrorClass = "STDOUT_DRAIN_FAILURE"; }
            }
            else { result.StdoutDrainStatus = "DRAIN_TIMEOUT"; }
            if (stderrCompleted)
            {
                result.StderrDrainStatus = stderr.ErrorClass.Length == 0
                    ? (initialDrainWaitCompleted ? "DRAINED" : "DRAIN_COMPLETED_AFTER_TIMEOUT")
                    : (initialDrainWaitCompleted ? "DRAIN_FAILED" : "DRAIN_FAILED_AFTER_TIMEOUT");
                result.Stderr = stderr.Text;
                result.StderrByteLength = stderr.ByteLength;
                result.StderrTruncated = stderr.Truncated;
                if (stderr.ErrorClass.Length > 0 && String.IsNullOrEmpty(result.CaptureErrorClass)) { result.CaptureErrorClass = "STDERR_DRAIN_FAILURE"; }
            }
            else { result.StderrDrainStatus = "DRAIN_TIMEOUT"; }
            result.OutputTooLarge = (stdoutCompleted && stdout.Truncated) || (stderrCompleted && stderr.Truncated);
            result.CaptureBounded = initialDrainWaitCompleted && stdoutCompleted && stderrCompleted && String.IsNullOrEmpty(result.CaptureErrorClass);
            if (process.HasExited) { result.ExitCode = process.ExitCode; }
        }
        catch (Exception exception)
        {
            result.CaptureErrorClass = exception.GetType().Name;
            result.CaptureBounded = false;
        }
        if (containment.Attached)
        {
            terminalResult = (result.TimedOut || result.OutputTooLarge || !result.CaptureBounded || !String.IsNullOrEmpty(result.CaptureErrorClass))
                ? P5EProcessTermination.TerminateAndVerify(containment, process, 5000)
                : P5EProcessTermination.VerifyAndClose(containment, process, 5000);
            result.ContainmentStatus = terminalResult.Status;
            result.ContainmentVerified = terminalResult.Verified;
            if (!terminalResult.Verified && String.IsNullOrEmpty(result.CaptureErrorClass)) { result.CaptureErrorClass = "EXTERNAL_PROCESS_STATE_UNKNOWN"; }
        }
        else { result.ContainmentVerified = false; }
        return result;
    }

    public static P5EDatabaseExportCaptureResult Capture(
        Process process,
        string partialPath,
        long maximumStdoutBytes,
        int maximumStderrBytes,
        int timeoutMilliseconds)
    {
        var containment = P5EProcessTermination.Attach(process);
        if (!containment.Attached)
        {
            var failed = new P5EDatabaseExportCaptureResult();
            failed.ContainmentStatus = containment.AttachStatus;
            failed.CaptureErrorClass = "P5E_PROCESS_CONTAINMENT_ASSIGN_FAILED_STOP";
            failed.CaptureBounded = false;
            failed.ContainmentVerified = false;
            try { if (process.HasExited) { failed.ExitCode = process.ExitCode; } } catch { }
            return failed;
        }
        return CaptureCore(process, process.StandardOutput.BaseStream, process.StandardError.BaseStream, containment,
            partialPath, maximumStdoutBytes, maximumStderrBytes, timeoutMilliseconds);
    }

    public static P5EDatabaseExportCaptureResult Capture(
        P5EProcessLaunchHandle launch,
        string partialPath,
        long maximumStdoutBytes,
        int maximumStderrBytes,
        int timeoutMilliseconds)
    {
        var failed = new P5EDatabaseExportCaptureResult();
        if (launch == null || launch.Process == null)
        {
            failed.CaptureErrorClass = "PROCESS_LAUNCH_HANDLE_MISSING";
            failed.CaptureBounded = false;
            return failed;
        }
        var process = launch.Process;
        var containment = P5EProcessTermination.Attach(process);
        failed.ContainmentStatus = containment.AttachStatus;
        if (!containment.Attached)
        {
            failed.CaptureErrorClass = "P5E_PROCESS_CONTAINMENT_ASSIGN_FAILED_STOP";
            failed.CaptureBounded = false;
            failed.ContainmentVerified = false;
            launch.Abort();
            return failed;
        }
        if (!launch.Resume())
        {
            failed.CaptureErrorClass = "PROCESS_RESUME_FAILED";
            failed.CaptureBounded = false;
            var terminal = P5EProcessTermination.TerminateAndVerify(containment, process, 5000);
            failed.ContainmentStatus = terminal.Status;
            failed.ContainmentVerified = terminal.Verified;
            launch.Abort();
            return failed;
        }
        return CaptureCore(process, launch.StandardOutput, launch.StandardError, containment,
            partialPath, maximumStdoutBytes, maximumStderrBytes, timeoutMilliseconds);
    }
}

public sealed class P5EProcessTextCaptureResult
{
    public P5EProcessTextCaptureResult()
    {
        Stdout = "";
        Stderr = "";
        InputWriteCompleted = true;
        InputWriteErrorClass = "";
        CaptureErrorClass = "";
        ContainmentStatus = "NOT_ATTEMPTED";
        StdoutDrainStatus = "NOT_STARTED";
        StderrDrainStatus = "NOT_STARTED";
    }
    public string Stdout { get; set; }
    public string Stderr { get; set; }
    public long StdoutByteLength { get; set; }
    public long StderrByteLength { get; set; }
    public bool StdoutTruncated { get; set; }
    public bool StderrTruncated { get; set; }
    public bool CaptureBounded { get; set; }
    public bool TimedOut { get; set; }
    public int? ExitCode { get; set; }
    public bool InputWriteCompleted { get; set; }
    public string InputWriteErrorClass { get; set; }
    public string CaptureErrorClass { get; set; }
    public string ContainmentStatus { get; set; }
    public string StdoutDrainStatus { get; set; }
    public string StderrDrainStatus { get; set; }
    public bool ContainmentVerified { get; set; }
    public bool OutputTooLarge { get; set; }
}

internal sealed class P5EProcessTextStreamResult
{
    public P5EProcessTextStreamResult()
    {
        Bytes = new byte[0];
        ErrorClass = "";
        Completed = false;
    }
    public long ByteLength { get; set; }
    public bool Truncated { get; set; }
    public byte[] Bytes { get; set; }
    public string ErrorClass { get; set; }
    public bool Completed { get; set; }
}

internal sealed class P5EProcessCaptureSignal
{
    private int overflow;
    public bool OverflowDetected { get { return Interlocked.CompareExchange(ref overflow, 0, 0) != 0; } }
    public void MarkOverflow() { Interlocked.Exchange(ref overflow, 1); }
}

public static class P5EProcessTextCapture
{
    private static P5EProcessTextStreamResult Drain(Stream input, int maximumBytes, P5EProcessCaptureSignal signal)
    {
        var result = new P5EProcessTextStreamResult();
        using (var retained = new MemoryStream())
        {
            var chunk = new byte[8192];
            try
            {
                int read;
                while ((read = input.Read(chunk, 0, chunk.Length)) > 0)
                {
                    var remaining = Math.Max(0L, maximumBytes - retained.Length);
                    if (remaining > 0)
                    {
                        retained.Write(chunk, 0, (int)Math.Min(remaining, read));
                    }
                    if (result.ByteLength + read > maximumBytes)
                    {
                        result.Truncated = true;
                        signal.MarkOverflow();
                    }
                    result.ByteLength += read;
                }
                result.Bytes = retained.ToArray();
                result.Completed = true;
            }
            catch (Exception exception)
            {
                result.ErrorClass = exception.GetType().Name;
                result.Bytes = retained.ToArray();
            }
        }
        return result;
    }

    private static P5EProcessTextCaptureResult CaptureCore(
        Process process,
        Stream stdoutStream,
        Stream stderrStream,
        Stream stdinStream,
        P5EProcessContainmentHandle containment,
        int maximumStdoutBytes,
        int maximumStderrBytes,
        int timeoutMilliseconds,
        byte[] standardInput)
    {
        var result = new P5EProcessTextCaptureResult();
        Task<P5EProcessTextStreamResult> stdoutTask = null;
        Task<P5EProcessTextStreamResult> stderrTask = null;
        var signal = new P5EProcessCaptureSignal();
        P5EProcessContainmentResult terminalResult = null;
        var initialDrainWaitCompleted = false;
        result.ContainmentStatus = containment.AttachStatus;
        try
        {
            stdoutTask = Task.Run(() => Drain(stdoutStream, maximumStdoutBytes, signal));
            stderrTask = Task.Run(() => Drain(stderrStream, maximumStderrBytes, signal));
            if (standardInput != null)
            {
                try
                {
                    if (standardInput.Length > 0)
                    {
                        stdinStream.Write(standardInput, 0, standardInput.Length);
                        stdinStream.Flush();
                    }
                    stdinStream.Dispose();
                }
                catch (Exception exception)
                {
                    result.InputWriteCompleted = false;
                    result.InputWriteErrorClass = exception.GetType().Name;
                    if (containment.Attached) { terminalResult = P5EProcessTermination.TerminateAndVerify(containment, process, 5000); }
                }
            }
            else if (stdinStream != null)
            {
                try { stdinStream.Dispose(); } catch { }
            }
            var stopwatch = Stopwatch.StartNew();
            while (containment.Attached && !process.HasExited)
            {
                if (signal.OverflowDetected)
                {
                    result.OutputTooLarge = true;
                    terminalResult = P5EProcessTermination.TerminateAndVerify(containment, process, 5000);
                    break;
                }
                if (stopwatch.ElapsedMilliseconds >= timeoutMilliseconds)
                {
                    result.TimedOut = true;
                    terminalResult = P5EProcessTermination.TerminateAndVerify(containment, process, 5000);
                    break;
                }
                Thread.Sleep(25);
            }
            try { if (process.HasExited) { result.ExitCode = process.ExitCode; } } catch { }
            initialDrainWaitCompleted = Task.WaitAll(new Task[] { stdoutTask, stderrTask }, 5000);
            if (!initialDrainWaitCompleted)
            {
                result.CaptureErrorClass = "TASK_WAIT_TIMEOUT";
                if (containment.Attached)
                {
                    terminalResult = P5EProcessTermination.TerminateAndVerify(containment, process, 5000);
                }
                try { Task.WaitAll(new Task[] { stdoutTask, stderrTask }, 5000); } catch { }
            }
            var stdoutCompleted = stdoutTask.IsCompleted;
            var stderrCompleted = stderrTask.IsCompleted;
            var stdout = stdoutCompleted ? stdoutTask.GetAwaiter().GetResult() : null;
            var stderr = stderrCompleted ? stderrTask.GetAwaiter().GetResult() : null;
            if (stdoutCompleted)
            {
                result.StdoutDrainStatus = stdout.ErrorClass.Length == 0
                    ? (initialDrainWaitCompleted ? "DRAINED" : "DRAIN_COMPLETED_AFTER_TIMEOUT")
                    : (initialDrainWaitCompleted ? "DRAIN_FAILED" : "DRAIN_FAILED_AFTER_TIMEOUT");
                result.Stdout = Encoding.UTF8.GetString(stdout.Bytes);
                result.StdoutByteLength = stdout.ByteLength;
                result.StdoutTruncated = stdout.Truncated;
                if (stdout.ErrorClass.Length > 0 && String.IsNullOrEmpty(result.CaptureErrorClass)) { result.CaptureErrorClass = "STDOUT_DRAIN_FAILURE"; }
            }
            else
            {
                result.StdoutDrainStatus = "DRAIN_TIMEOUT";
            }
            if (stderrCompleted)
            {
                result.StderrDrainStatus = stderr.ErrorClass.Length == 0
                    ? (initialDrainWaitCompleted ? "DRAINED" : "DRAIN_COMPLETED_AFTER_TIMEOUT")
                    : (initialDrainWaitCompleted ? "DRAIN_FAILED" : "DRAIN_FAILED_AFTER_TIMEOUT");
                result.Stderr = Encoding.UTF8.GetString(stderr.Bytes);
                result.StderrByteLength = stderr.ByteLength;
                result.StderrTruncated = stderr.Truncated;
                if (stderr.ErrorClass.Length > 0 && String.IsNullOrEmpty(result.CaptureErrorClass)) { result.CaptureErrorClass = "STDERR_DRAIN_FAILURE"; }
            }
            else
            {
                result.StderrDrainStatus = "DRAIN_TIMEOUT";
            }
            result.OutputTooLarge = result.OutputTooLarge || (stdoutCompleted && stdout.Truncated) || (stderrCompleted && stderr.Truncated) || signal.OverflowDetected;
            result.CaptureBounded = initialDrainWaitCompleted && stdoutCompleted && stderrCompleted && result.InputWriteCompleted && String.IsNullOrEmpty(result.CaptureErrorClass);
            try { if (process.HasExited) { result.ExitCode = process.ExitCode; } } catch { }
        }
        catch (Exception exception)
        {
            result.CaptureErrorClass = exception.GetType().Name;
            result.CaptureBounded = false;
        }
        if (containment.Attached)
        {
            if (result.OutputTooLarge || result.TimedOut || !result.CaptureBounded || !String.IsNullOrEmpty(result.CaptureErrorClass))
            {
                terminalResult = P5EProcessTermination.TerminateAndVerify(containment, process, 5000);
            }
            else
            {
                terminalResult = P5EProcessTermination.VerifyAndClose(containment, process, 5000);
            }
        }
        if (terminalResult != null)
        {
            result.ContainmentStatus = terminalResult.Status;
            result.ContainmentVerified = terminalResult.Verified;
            if (!terminalResult.Verified && String.IsNullOrEmpty(result.CaptureErrorClass)) { result.CaptureErrorClass = "EXTERNAL_PROCESS_STATE_UNKNOWN"; }
        }
        return result;
    }

    public static P5EProcessTextCaptureResult Capture(
        Process process,
        int maximumStdoutBytes,
        int maximumStderrBytes,
        int timeoutMilliseconds,
        byte[] standardInput)
    {
        var containment = P5EProcessTermination.Attach(process);
        if (!containment.Attached)
        {
            var failed = new P5EProcessTextCaptureResult();
            failed.ContainmentStatus = containment.AttachStatus;
            failed.CaptureErrorClass = "P5E_PROCESS_CONTAINMENT_ASSIGN_FAILED_STOP";
            failed.CaptureBounded = false;
            failed.ContainmentVerified = false;
            try { if (process.HasExited) { failed.ExitCode = process.ExitCode; } } catch { }
            return failed;
        }
        return CaptureCore(process, process.StandardOutput.BaseStream, process.StandardError.BaseStream,
            standardInput == null ? null : process.StandardInput.BaseStream, containment,
            maximumStdoutBytes, maximumStderrBytes, timeoutMilliseconds, standardInput);
    }

    public static P5EProcessTextCaptureResult Capture(
        P5EProcessLaunchHandle launch,
        int maximumStdoutBytes,
        int maximumStderrBytes,
        int timeoutMilliseconds,
        byte[] standardInput)
    {
        var failed = new P5EProcessTextCaptureResult();
        if (launch == null || launch.Process == null)
        {
            failed.CaptureErrorClass = "PROCESS_LAUNCH_HANDLE_MISSING";
            failed.CaptureBounded = false;
            return failed;
        }
        var process = launch.Process;
        var containment = P5EProcessTermination.Attach(process);
        failed.ContainmentStatus = containment.AttachStatus;
        if (!containment.Attached)
        {
            failed.CaptureErrorClass = "P5E_PROCESS_CONTAINMENT_ASSIGN_FAILED_STOP";
            failed.CaptureBounded = false;
            failed.ContainmentVerified = false;
            launch.Abort();
            return failed;
        }
        if (!launch.Resume())
        {
            failed.CaptureErrorClass = "PROCESS_RESUME_FAILED";
            failed.CaptureBounded = false;
            var terminal = P5EProcessTermination.TerminateAndVerify(containment, process, 5000);
            failed.ContainmentStatus = terminal.Status;
            failed.ContainmentVerified = terminal.Verified;
            launch.Abort();
            return failed;
        }
        return CaptureCore(process, launch.StandardOutput, launch.StandardError,
            standardInput == null ? null : launch.StandardInput, containment,
            maximumStdoutBytes, maximumStderrBytes, timeoutMilliseconds, standardInput);
    }
}
'@
}

function Invoke-P5EDatabaseBinaryExportProcess {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][string[]]$ArgumentList,
        [Parameter(Mandatory = $true)][string]$PartialPath,
        [Parameter(Mandatory = $true)][long]$TimeoutMilliseconds,
        [long]$MaximumStdoutBytes = $script:P5EDatabaseExporterMaxBytes,
        [int]$MaximumStderrBytes = $script:P5EDatabaseExporterMaxStderrBytes
    )
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    foreach ($name in @('P5E_OWNER_ENDPOINT_ACCOUNT_FINGERPRINT')) {
        try { if ($startInfo.Environment.ContainsKey($name)) { [void]$startInfo.Environment.Remove($name) } } catch { }
    }
    Set-P5EDatabaseExportProcessArguments -StartInfo $startInfo -ArgumentList $ArgumentList
    $launch = $null
    try {
        $launch = [P5EProcessLauncher]::Start($startInfo)
        $capture = [P5EDatabaseExportCapture]::Capture($launch, $PartialPath,
            [long]$MaximumStdoutBytes, [int]$MaximumStderrBytes, [int]$TimeoutMilliseconds)
        return [pscustomobject]@{
            Outcome = if (-not $capture.ContainmentVerified) { 'EXTERNAL_PROCESS_STATE_UNKNOWN' } elseif ($capture.OutputTooLarge) { 'OUTPUT_TOO_LARGE' } elseif ($capture.TimedOut) { 'TIMEOUT' } elseif (-not [string]::IsNullOrWhiteSpace([string]$capture.CaptureErrorClass)) { 'CAPTURE_FAILED' } elseif (-not $capture.CaptureBounded) { 'CAPTURE_NOT_BOUNDED' } elseif ($null -eq $capture.ExitCode) { 'PROCESS_EXIT_UNKNOWN' } elseif ([int]$capture.ExitCode -eq 0) { 'PROCESS_EXITED_ZERO' } else { 'PROCESS_EXITED_NONZERO' }
            LaunchCount = 1L
            ExitCode = $capture.ExitCode
            TimedOut = [bool]$capture.TimedOut
            CaptureBounded = [bool]$capture.CaptureBounded
            ByteLength = [long]$capture.ByteLength
            StderrByteLength = [long]$capture.StderrByteLength
            StdoutTruncated = [bool]$capture.StdoutTruncated
            StderrTruncated = [bool]$capture.StderrTruncated
            CaptureErrorClass = [string]$capture.CaptureErrorClass
            ContainmentStatus = [string]$capture.ContainmentStatus
            StdoutDrainStatus = [string]$capture.StdoutDrainStatus
            StderrDrainStatus = [string]$capture.StderrDrainStatus
            ContainmentVerified = [bool]$capture.ContainmentVerified
            Stderr = if ([string]::IsNullOrWhiteSpace([string]$capture.Stderr)) { '' } else { 'P5E_EXPORTER_STDERR_REDACTED' }
            StderrRedacted = $true
            RedactionViolation = $false
            OutputTooLarge = [bool]($capture.StdoutTruncated -or $capture.StderrTruncated)
            LaunchErrorClass = ''
            LaunchNativeErrorCode = $null
            LaunchReason = ''
        }
    }
    catch {
        $exception = $_.Exception
        $nativeCode = $null
        $candidate = $exception
        while ($null -ne $candidate) {
            if ($candidate -is [System.ComponentModel.Win32Exception]) {
                $nativeCode = [int]$candidate.NativeErrorCode
                break
            }
            $candidate = $candidate.InnerException
        }
        $reason = if ($null -eq $nativeCode) { 'UNKNOWN_START_ERROR' } else {
            switch ([int]$nativeCode) {
                2 { 'PATH_OR_FILE_NOT_FOUND'; break }
                3 { 'PATH_OR_FILE_NOT_FOUND'; break }
                5 { 'ACCESS_DENIED'; break }
                193 { 'BAD_EXE_FORMAT'; break }
                267 { 'DIRECTORY_NAME_INVALID'; break }
                default { 'NATIVE_START_ERROR' }
            }
        }
        return [pscustomobject]@{
            Outcome = 'FAILED_BEFORE_LAUNCH'
            LaunchCount = 0L
            ExitCode = $null
            TimedOut = $false
            CaptureBounded = $false
            ByteLength = 0L
            StderrByteLength = 0L
            StdoutTruncated = $false
            StderrTruncated = $false
            CaptureErrorClass = ''
            ContainmentStatus = 'NOT_STARTED'
            StdoutDrainStatus = 'NOT_STARTED'
            StderrDrainStatus = 'NOT_STARTED'
            ContainmentVerified = $false
            Stderr = ''
            StderrRedacted = $true
            RedactionViolation = $false
            OutputTooLarge = $false
            LaunchErrorClass = $exception.GetType().Name
            LaunchNativeErrorCode = $nativeCode
            LaunchReason = $reason
        }
    }
    finally {
        if ($null -ne $launch) { $launch.Dispose() }
    }
}

if (-not $LibraryOnly) {
    Get-P5EDatabaseExportTypedStop -Code 'P5E_DATABASE_EXPORT_LIBRARY_ONLY_REQUIRED_STOP'
}
