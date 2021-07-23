package rikka.bsh;

import android.os.ParcelFileDescriptor;
import android.util.Log;

public class BSHHost {

    private static final String TAG = "BSHHost";

    // libcore/ojluni/src/main/java/java/lang/ProcessImpl.java

    private static byte[] createCBytes(String[] array) {
        if (array == null) {
            return null;
        }

        byte[][] bytes = new byte[array.length][];
        int count = bytes.length; // For added NUL bytes
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = array[i].getBytes();
            count += bytes[i].length;
        }
        byte[] block = new byte[count];
        int i = 0;
        for (byte[] arg : bytes) {
            System.arraycopy(arg, 0, block, i, arg.length);
            i += arg.length + 1;
            // No need to write NUL bytes explicitly
        }
        return block;
    }

    public static BSHHost create(String[] args, String[] env) {
        return new BSHHost(args, env);
    }

    private final String[] args;
    private final String[] env;
    private int stdinReadPipe;
    private int stdoutWritePipe;
    private int pid;
    private int ptmx;
    private int exitCode = Integer.MAX_VALUE;

    private BSHHost(String[] args, String[] env) {
        this.args = args;
        this.env = env;
    }

    public void prepare(ParcelFileDescriptor stdin, ParcelFileDescriptor stdout) {
        stdinReadPipe = stdin.detachFd();
        stdoutWritePipe = stdout.detachFd();
    }

    /**
     * Fork and execute, start transfer threads.
     */
    public void start() {
        Log.d(TAG, "start");


        byte[] argBlock = createCBytes(args);
        byte[] envBlock = createCBytes(env);

        int[] result = start(argBlock, args.length, envBlock, env != null ? env.length : -1, stdinReadPipe, stdoutWritePipe);
        pid = result[0];
        ptmx = result[1];

        new Thread(() -> exitCode = waitFor(pid)).start();
    }

    public int getPid() {
        return pid;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setWindowSize(long size) {
        Log.d(TAG, "setWindowSize");

        setWindowSize(ptmx, size);
    }

    private static native int[] start(byte[] argBlock, int argc, byte[] envBlock, int envc, int stdin_read_pipe, int stdout_write_pipe);

    private static native void setWindowSize(int ptmx, long size);

    private static native int waitFor(int pid);
}