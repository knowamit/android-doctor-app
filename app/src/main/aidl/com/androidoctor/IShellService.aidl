package com.androidoctor;

// Runs inside Shizuku's shell-privileged process (bound via Shizuku UserService).
interface IShellService {
    // Reserved transaction id required by Shizuku for service teardown.
    void destroy() = 16777114;

    // Returns {stdout, stderr, exitCode}. Output is capped to stay under the
    // 1 MB binder transaction limit.
    String[] exec(String command, long timeoutMs) = 1;
}
