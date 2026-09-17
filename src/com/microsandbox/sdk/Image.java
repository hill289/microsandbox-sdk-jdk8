package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import com.microsandbox.sdk.model.ImageInfo;
import jnr.ffi.Pointer;

import java.util.ArrayList;
import java.util.List;

/**
 * Image management operations for the microsandbox SDK.
 *
 * <p>Provides access to the OCI image cache: list, get, inspect, remove,
 * prune, load, and save images.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public final class Image {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private Image() {} // utility class

    // ── Image operations ──────────────────────────────────────────────────

    /**
     * Fetches a single cached image by reference.
     */
    public static ImageInfo get(String reference) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_image_get(
                    cancelId, reference, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return ImageInfo.fromJson(json);
        });
    }

    /**
     * Lists every cached image, ordered by creation time (newest first).
     */
    public static List<ImageInfo> list() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_image_list(
                    cancelId, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return parseImageList(json);
        });
    }

    /**
     * Removes a cached image. When force=false, sandboxes that still reference
     * the image cause the call to fail with ErrImageInUse.
     */
    public static void remove(String reference, boolean force) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_image_remove(
                    cancelId, reference, force ? 1 : 0, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Removes a cached image with force=false.
     */
    public static void remove(String reference) throws MicrosandboxException {
        remove(reference, false);
    }

    /**
     * Removes cached image data that is not used by sandboxes.
     */
    public static String prune() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_image_prune(
                    cancelId, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Imports images from a local archive into the cache.
     *
     * @param inputPath path to the archive file
     * @param tags      extra references to apply to the first image
     * @return list of imported image handles
     */
    public static List<ImageInfo> load(String inputPath, String... tags)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        String tagsJson = arrayToJson(tags);
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_image_load(
                    cancelId, inputPath, tagsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return parseImageList(json);
        });
    }

    /**
     * Exports cached images to an archive file.
     *
     * @param references image references to export
     * @param outputPath path where the archive will be written
     * @param format     archive format ("docker" or "oci"; empty = "docker")
     */
    public static void save(String[] references, String outputPath, String format)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        String refsJson = arrayToJson(references);
        String fmt = (format == null || format.isEmpty()) ? "docker" : format;
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_image_save(
                    cancelId, refsJson, outputPath, fmt, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static List<ImageInfo> parseImageList(String json) {
        List<ImageInfo> result = new ArrayList<ImageInfo>();
        int arrStart = json.indexOf('[');
        if (arrStart < 0) return result;
        int arrEnd = json.lastIndexOf(']');
        if (arrEnd < 0) return result;
        String arr = json.substring(arrStart + 1, arrEnd);

        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = arr.substring(objStart, i + 1);
                    result.add(ImageInfo.fromJson(obj));
                    objStart = -1;
                }
            }
        }
        return result;
    }

    private static String arrayToJson(String[] items) {
        if (items == null || items.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(items[i].replace("\\", "\\\\")
                    .replace("\"", "\\\"")).append('"');
        }
        sb.append(']');
        return sb.toString();
    }
}
