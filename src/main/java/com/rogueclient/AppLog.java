package com.rogueclient;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Small holder for the log PrintStream Main.main() redirects System.out/err to, so other
 * code (like the DevTools "clear data" nuke) can close it before touching the log file -
 * required on Windows, where a file that's still open for writing can't be deleted.
 */
public class AppLog {

    private static PrintStream stream;

    public static void register(PrintStream logStream) {
        stream = logStream;
    }

    /**
     * Closes the redirected log file and points System.out/err back at the JVM's real
     * stdout/stderr file descriptors, so the log file on disk is no longer held open.
     * Uses FileDescriptor.out/err directly rather than a value captured earlier, since
     * static field initialization order can't be relied on to run before Main.main()
     * performs its redirect.
     */
    public static void close() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }
}
