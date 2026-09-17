package com.microsandbox.sdk.ffi;

import com.microsandbox.sdk.exception.MicrosandboxException;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

import java.io.File;

public final class NativeBridge {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;
    private static final Object LOCK = new Object();
    private static volatile NativeBridge INSTANCE;

    private final MicrosandboxNative nativeLib;
    private final Runtime runtime;

    private NativeBridge(MicrosandboxNative nativeLib, Runtime runtime) {
        this.nativeLib = nativeLib;
        this.runtime = runtime;
    }

    public static NativeBridge getInstance() throws MicrosandboxException {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = doLoad();
                }
            }
        }
        return INSTANCE;
    }

    public static NativeBridge getInstanceOrNull() { return INSTANCE; }

    public static synchronized NativeBridge reload() throws MicrosandboxException {
        INSTANCE = doLoad();
        return INSTANCE;
    }

    private static NativeBridge doLoad() throws MicrosandboxException {
        try {
            MicrosandboxNative lib = LibraryLoader.load();
            Runtime rt = Runtime.getRuntime(lib);
            configureMsbPath(lib);
            return new NativeBridge(lib, rt);
        } catch (Exception e) {
            throw MicrosandboxException.ffiLoadError(e.getMessage());
        }
    }

    /**
     * Points the native SDK at the {@code msb} runtime binary.
     *
     * <p>The native library only consults the {@code MSB_PATH} environment
     * variable; when that is unset it cannot find {@code msb}, so every
     * lifecycle operation fails with {@code msb binary not found}. Locate the
     * binary the SDK ships next to the native library (application directory,
     * {@code java.library.path}, or {@code ~/.microsandbox/bin}) and register
     * it explicitly.</p>
     */
    private static void configureMsbPath(MicrosandboxNative lib) {
        String env = System.getenv("MSB_PATH");
        if (env != null && !env.trim().isEmpty()) {
            return; // the native library honours MSB_PATH itself
        }
        File msb = locateMsb();
        if (msb != null) {
            try {
                lib.msb_set_sdk_msb_path(msb.getAbsolutePath());
            } catch (Throwable ignored) {
                // Runtimes that lack the setter fall back to MSB_PATH only.
            }
        }
    }

    private static File locateMsb() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String name = windows ? "msb.exe" : "msb";
        java.util.List<File> candidates = new java.util.ArrayList<File>();
        addCandidate(candidates, System.getProperty("user.dir"), name);
        String libPath = System.getProperty("java.library.path", "");
        for (String dir : libPath.split(File.pathSeparator)) {
            if (dir != null && !dir.trim().isEmpty()) {
                addCandidate(candidates, dir, name);
            }
        }
        String home = System.getProperty("user.home");
        if (home != null) {
            addCandidate(candidates,
                    home + File.separator + ".microsandbox" + File.separator + "bin", name);
        }
        for (File f : candidates) {
            if (f.isFile()) return f;
        }
        return null;
    }

    private static void addCandidate(java.util.List<File> out, String dir, String name) {
        if (dir == null) return;
        File f = new File(dir, name);
        if (!out.contains(f)) out.add(f);
    }

    public MicrosandboxNative nativeLib() { return nativeLib; }
    public Runtime runtime() { return runtime; }

    // ── Buffer allocation ───────────────────────────────────────────────────
    // 注意：jnr 的 Pointer 没有 free()。allocateTemporary/allocateDirect 分配的
    // 内存由 JNR 的 MemoryManager 在 GC 时回收。

    public Pointer allocateBuffer() {
        return allocateBuffer(DEFAULT_BUFFER_SIZE);
    }

    public Pointer allocateBuffer(int size) {
        return runtime.getMemoryManager().allocateTemporary(size, true);
    }

    // ── Error handling helpers ──────────────────────────────────────────────

    public String checkError(String errPtr, Pointer buf) throws MicrosandboxException {
        if (errPtr != null && !errPtr.isEmpty()) {
            // 先把异常/消息构造出来（此时 errPtr 仍然有效）
            MicrosandboxException ex = MicrosandboxException.fromFfiError(errPtr);
            // 再释放 native 内存
            nativeLib.msb_free_string(errPtr);
            throw ex;
        }
        return readBuffer(buf);
    }

    public static String readBuffer(Pointer buf) {
        if (buf == null) {
            return "";
        }
        return buf.getString(0);
    }

    public static byte[] readBufferBytes(Pointer buf, long len) {
        if (buf == null || len <= 0) {
            return new byte[0];
        }
        byte[] result = new byte[(int) len];
        buf.get(0, result, 0, (int) len);
        return result;
    }

    // ── Handle extraction ───────────────────────────────────────────────────

    /**
     * Extracts an opaque handle pointer from a JSON field. The native side
     * emits handles either as bare numbers (e.g. {@code "handle":1}) or as
     * quoted decimal / hex strings, so this accepts both forms.
     */
    public static Pointer extractPointer(String json, String key) {
        Pointer exact = parsePointer(extractValue(json, key));
        if (exact != null) return exact;
        // The native SDK names handle fields per operation ("handle",
        // "stream_handle", "exec_handle", "client_handle", ...). Fall back
        // to the first *_handle numeric field when the named one is absent.
        return scanHandle(json);
    }

    private static Pointer parsePointer(String val) {
        if (val == null || val.isEmpty()) return null;
        String s = val.trim();
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return Pointer.wrap(
                        jnr.ffi.Runtime.getSystemRuntime(),
                        Long.parseUnsignedLong(s.substring(2), 16));
            }
            return Pointer.wrap(
                    jnr.ffi.Runtime.getSystemRuntime(),
                    Long.parseLong(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Pointer scanHandle(String json) {
        int i = 0;
        while (i < json.length()) {
            int q = json.indexOf('"', i);
            if (q < 0) break;
            int qe = json.indexOf('"', q + 1);
            if (qe < 0) break;
            String name = json.substring(q + 1, qe);
            i = qe + 1;
            if (!name.equals("handle") && !name.endsWith("_handle")) continue;
            int colon = json.indexOf(':', qe);
            if (colon < 0) break;
            int vs = colon + 1;
            while (vs < json.length() && Character.isWhitespace(json.charAt(vs))) vs++;
            int ve = vs;
            while (ve < json.length()) {
                char ch = json.charAt(ve);
                if (Character.isDigit(ch) || (ch >= 'a' && ch <= 'f')
                        || (ch >= 'A' && ch <= 'F') || ch == 'x' || ch == '-') ve++;
                else break;
            }
            if (ve > vs) {
                Pointer p = parsePointer(json.substring(vs, ve));
                if (p != null) return p;
            }
        }
        return null;
    }

    /**
     * Reads a field value that may be a quoted string or a bare token
     * (number / bool / null). Unlike a quote-only extractor this returns
     * "1" for {@code "handle":1}.
     */
    public static String extractValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            start++;
            int end = start;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '\\') { end += 2; continue; }
                if (c == '"') break;
                end++;
            }
            return json.substring(start, Math.min(end, json.length()));
        }
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            end++;
        }
        return json.substring(start, end);
    }

    // ── Convenience FFI call wrappers ──────────────────────────────────────

    public void callVoid(String errPtr) throws MicrosandboxException {
        checkError(errPtr, null);
    }

    public String callWithBuffer(BufferCall call) throws MicrosandboxException {
        Pointer buf = allocateBuffer();
        // 不再调用 buf.free()：Pointer 无此方法。
        // 临时内存由 JNR 管理，会在 GC 时回收；
        // 若需立即释放可在 JNR 提供相应 API 时手动处理。
        String errPtr = call.execute(buf, DEFAULT_BUFFER_SIZE);
        return checkError(errPtr, buf);
    }

    public String callWithBuffer(Pointer buf, int bufSize, BufferCall call)
            throws MicrosandboxException {
        String errPtr = call.execute(buf, bufSize);
        return checkError(errPtr, buf);
    }

    public interface BufferCall {
        String execute(Pointer buf, long bufLen);
    }

    // ── Cancel token management ─────────────────────────────────────────────

    public <T> T withCancel(CancelAction<T> action) throws MicrosandboxException {
        long cancelId = nativeLib.msb_cancel_alloc();
        try {
            return action.execute(cancelId);
        } finally {
            nativeLib.msb_cancel_unregister(cancelId);
        }
    }

    public interface CancelAction<T> {
        T execute(long cancelId) throws MicrosandboxException;
    }

    public <T> T withCancelAndInterrupt(CancelAction<T> action) throws MicrosandboxException {
        long cancelId = nativeLib.msb_cancel_alloc();
        Thread.currentThread().interrupt();   // 这里语义可疑，见下文
        try {
            return action.execute(cancelId);
        } finally {
            nativeLib.msb_cancel_unregister(cancelId);
        }
    }

    public void cancel(long cancelId) {
        nativeLib.msb_cancel_trigger(cancelId);
    }

    // ── Base64 helpers ─────────────────────────────────────────────────────

    public static String base64Encode(byte[] data) {
        if (data == null) return "";
        return java.util.Base64.getEncoder().encodeToString(data);
    }

    public static byte[] base64Decode(String b64) {
        if (b64 == null || b64.isEmpty()) return new byte[0];
        return java.util.Base64.getDecoder().decode(b64);
    }
}