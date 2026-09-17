package com.microsandbox.sdk.ffi;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.types.u_int64_t;

/**
 * JNR-FFI interface for the microsandbox native library.
 *
 * <p>Note: jnr.ffi.Pointer is NOT generic. Always use the raw {@code Pointer}
 * type; do not write {@code Pointer<Byte>}.
 */
public interface MicrosandboxNative {

    static MicrosandboxNative load() {
        return LibraryLoader.create(MicrosandboxNative.class)
                .load("microsandbox");
    }

    // ── Core lifecycle ─────────────────────────────────────────────────────

    void msb_free_string(String str);

    void msb_set_sdk_msb_path(String msbPath);

    @u_int64_t
    long msb_cancel_alloc();

    void msb_cancel_trigger(@u_int64_t long cancelId);

    void msb_cancel_unregister(@u_int64_t long cancelId);

    // ── Sandbox lifecycle (by name) ────────────────────────────────────────

    String msb_sandbox_create(@u_int64_t long cancelId,
                              String name,
                              String optsJson,
                              int connectOrCreate,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_sandbox_lookup(@u_int64_t long cancelId,
                              String name,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_sandbox_connect(@u_int64_t long cancelId,
                               String name,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_sandbox_start(@u_int64_t long cancelId,
                             String name,
                             int detached,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_sandbox_handle_lifecycle(@u_int64_t long cancelId,
                                        String name,
                                        String expectedId,
                                        String operation,
                                        String optsJson,
                                        @Out Pointer buf,
                                        @u_int64_t long bufLen);

    String msb_sandbox_handle_stop(@u_int64_t long cancelId,
                                   String name,
                                   @u_int64_t long timeoutMs,
                                   @Out Pointer buf,
                                   @u_int64_t long bufLen);

    String msb_sandbox_handle_request_stop(@u_int64_t long cancelId,
                                           String name,
                                           @Out Pointer buf,
                                           @u_int64_t long bufLen);

    String msb_sandbox_handle_kill(@u_int64_t long cancelId,
                                   String name,
                                   @u_int64_t long timeoutMs,
                                   @Out Pointer buf,
                                   @u_int64_t long bufLen);

    String msb_sandbox_handle_request_kill(@u_int64_t long cancelId,
                                           String name,
                                           @Out Pointer buf,
                                           @u_int64_t long bufLen);

    String msb_sandbox_handle_request_drain(@u_int64_t long cancelId,
                                            String name,
                                            @Out Pointer buf,
                                            @u_int64_t long bufLen);

    String msb_sandbox_handle_wait_until_stopped(@u_int64_t long cancelId,
                                                 String name,
                                                 @Out Pointer buf,
                                                 @u_int64_t long bufLen);

    String msb_sandbox_handle_ping(@u_int64_t long cancelId,
                                   String name,
                                   @Out Pointer buf,
                                   @u_int64_t long bufLen);

    String msb_sandbox_handle_touch(@u_int64_t long cancelId,
                                    String name,
                                    @Out Pointer buf,
                                    @u_int64_t long bufLen);

    String msb_sandbox_handle_modify(@u_int64_t long cancelId,
                                     String name,
                                     String optsJson,
                                     @Out Pointer buf,
                                     @u_int64_t long bufLen);

    // ── Sandbox lifecycle (by handle) ──────────────────────────────────────

    String msb_sandbox_close(@u_int64_t long cancelId,
                             @In Pointer handle,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_sandbox_detach(@u_int64_t long cancelId,
                              @In Pointer handle,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_sandbox_stop(@u_int64_t long cancelId,
                            @In Pointer handle,
                            @u_int64_t long timeoutMs,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_sandbox_request_stop(@u_int64_t long cancelId,
                                    @In Pointer handle,
                                    @Out Pointer buf,
                                    @u_int64_t long bufLen);

    String msb_sandbox_stop_and_wait(@u_int64_t long cancelId,
                                     @In Pointer handle,
                                     @Out Pointer buf,
                                     @u_int64_t long bufLen);

    String msb_sandbox_kill(@u_int64_t long cancelId,
                            @In Pointer handle,
                            @u_int64_t long timeoutMs,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_sandbox_request_kill(@u_int64_t long cancelId,
                                    @In Pointer handle,
                                    @Out Pointer buf,
                                    @u_int64_t long bufLen);

    String msb_sandbox_drain(@u_int64_t long cancelId,
                             @In Pointer handle,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_sandbox_request_drain(@u_int64_t long cancelId,
                                     @In Pointer handle,
                                     @Out Pointer buf,
                                     @u_int64_t long bufLen);

    String msb_sandbox_wait(@u_int64_t long cancelId,
                            @In Pointer handle,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_sandbox_wait_until_stopped(@u_int64_t long cancelId,
                                          @In Pointer handle,
                                          @Out Pointer buf,
                                          @u_int64_t long bufLen);

    String msb_sandbox_ping(@u_int64_t long cancelId,
                            @In Pointer handle,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_sandbox_touch(@u_int64_t long cancelId,
                             @In Pointer handle,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_sandbox_modify(@u_int64_t long cancelId,
                              @In Pointer handle,
                              String optsJson,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_sandbox_owns_lifecycle(@In Pointer handle,
                                      @Out Pointer buf,
                                      @u_int64_t long bufLen);

    String msb_sandbox_list(@u_int64_t long cancelId,
                            String filterJson,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_sandbox_logs(@u_int64_t long cancelId,
                            @In Pointer handle,
                            String optsJson,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_sandbox_handle_logs(@u_int64_t long cancelId,
                                   String name,
                                   String optsJson,
                                   @Out Pointer buf,
                                   @u_int64_t long bufLen);

    String msb_sandbox_remove(@u_int64_t long cancelId,
                              String name,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_sandbox_remove_persisted(@u_int64_t long cancelId,
                                        @In Pointer handle,
                                        @Out Pointer buf,
                                        @u_int64_t long bufLen);

    // ── Exec ───────────────────────────────────────────────────────────────

    String msb_sandbox_exec(@u_int64_t long cancelId,
                            @In Pointer handle,
                            String cmd,
                            String execOptsJson,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_sandbox_exec_default(@u_int64_t long cancelId,
                                    @In Pointer handle,
                                    String execOptsJson,
                                    @Out Pointer buf,
                                    @u_int64_t long bufLen);

    String msb_sandbox_exec_stream(@u_int64_t long cancelId,
                                   @In Pointer handle,
                                   String cmd,
                                   String execOptsJson,
                                   @Out Pointer buf,
                                   @u_int64_t long bufLen);

    String msb_sandbox_exec_default_stream(@u_int64_t long cancelId,
                                           @In Pointer handle,
                                           String execOptsJson,
                                           @Out Pointer buf,
                                           @u_int64_t long bufLen);

    String msb_exec_recv(@u_int64_t long cancelId,
                         @In Pointer execHandle,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    String msb_exec_close(@u_int64_t long cancelId,
                          @In Pointer execHandle,
                          @Out Pointer buf,
                          @u_int64_t long bufLen);

    String msb_exec_id(@In Pointer execHandle,
                       @Out Pointer buf,
                       @u_int64_t long bufLen);

    String msb_exec_signal(@u_int64_t long cancelId,
                           @In Pointer execHandle,
                           int signal,
                           @Out Pointer buf,
                           @u_int64_t long bufLen);

    String msb_exec_resize(@u_int64_t long cancelId,
                           @In Pointer execHandle,
                           short rows,
                           short cols,
                           @Out Pointer buf,
                           @u_int64_t long bufLen);

    String msb_exec_stdin_write(@u_int64_t long cancelId,
                                @In Pointer execHandle,
                                String dataB64,
                                @Out Pointer buf,
                                @u_int64_t long bufLen);

    String msb_exec_stdin_close(@u_int64_t long cancelId,
                                @In Pointer execHandle,
                                @Out Pointer buf,
                                @u_int64_t long bufLen);

    String msb_exec_collect(@u_int64_t long cancelId,
                            @In Pointer execHandle,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_exec_wait(@u_int64_t long cancelId,
                         @In Pointer execHandle,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    String msb_exec_kill(@u_int64_t long cancelId,
                         @In Pointer execHandle,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    // ── Attach ─────────────────────────────────────────────────────────────

    String msb_sandbox_attach(@u_int64_t long cancelId,
                              @In Pointer handle,
                              String cmd,
                              String optsJson,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_sandbox_attach_default(@u_int64_t long cancelId,
                                      @In Pointer handle,
                                      String optsJson,
                                      @Out Pointer buf,
                                      @u_int64_t long bufLen);

    String msb_sandbox_attach_shell(@u_int64_t long cancelId,
                                    @In Pointer handle,
                                    @Out Pointer buf,
                                    @u_int64_t long bufLen);

    // ── Filesystem ─────────────────────────────────────────────────────────

    String msb_fs_read(@u_int64_t long cancelId,
                       @In Pointer handle,
                       String path,
                       @Out Pointer buf,
                       @u_int64_t long bufLen);

    String msb_fs_write(@u_int64_t long cancelId,
                        @In Pointer handle,
                        String path,
                        String dataB64,
                        @Out Pointer buf,
                        @u_int64_t long bufLen);

    String msb_fs_list(@u_int64_t long cancelId,
                       @In Pointer handle,
                       String path,
                       @Out Pointer buf,
                       @u_int64_t long bufLen);

    String msb_fs_stat(@u_int64_t long cancelId,
                       @In Pointer handle,
                       String path,
                       @Out Pointer buf,
                       @u_int64_t long bufLen);

    String msb_fs_copy_from_host(@u_int64_t long cancelId,
                                 @In Pointer handle,
                                 String hostPath,
                                 String guestPath,
                                 @Out Pointer buf,
                                 @u_int64_t long bufLen);

    String msb_fs_copy_to_host(@u_int64_t long cancelId,
                               @In Pointer handle,
                               String guestPath,
                               String hostPath,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_fs_mkdir(@u_int64_t long cancelId,
                        @In Pointer handle,
                        String path,
                        @Out Pointer buf,
                        @u_int64_t long bufLen);

    String msb_fs_remove(@u_int64_t long cancelId,
                         @In Pointer handle,
                         String path,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    String msb_fs_remove_dir(@u_int64_t long cancelId,
                             @In Pointer handle,
                             String path,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_fs_copy(@u_int64_t long cancelId,
                       @In Pointer handle,
                       String src,
                       String dst,
                       @Out Pointer buf,
                       @u_int64_t long bufLen);

    String msb_fs_rename(@u_int64_t long cancelId,
                         @In Pointer handle,
                         String src,
                         String dst,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    String msb_fs_exists(@u_int64_t long cancelId,
                         @In Pointer handle,
                         String path,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    String msb_fs_read_stream(@u_int64_t long cancelId,
                              @In Pointer handle,
                              String path,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_fs_read_stream_recv(@u_int64_t long cancelId,
                                   @In Pointer streamHandle,
                                   @Out Pointer buf,
                                   @u_int64_t long bufLen);

    String msb_fs_read_stream_close(@In Pointer streamHandle,
                                    @Out Pointer buf,
                                    @u_int64_t long bufLen);

    String msb_fs_write_stream(@u_int64_t long cancelId,
                               @In Pointer handle,
                               String path,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_fs_write_stream_write(@u_int64_t long cancelId,
                                     @In Pointer streamHandle,
                                     String dataB64,
                                     @Out Pointer buf,
                                     @u_int64_t long bufLen);

    String msb_fs_write_stream_close(@u_int64_t long cancelId,
                                     @In Pointer streamHandle,
                                     @Out Pointer buf,
                                     @u_int64_t long bufLen);

    // ── Metrics ────────────────────────────────────────────────────────────

    String msb_sandbox_metrics(@u_int64_t long cancelId,
                               @In Pointer handle,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_sandbox_metrics_stream(@u_int64_t long cancelId,
                                      @In Pointer handle,
                                      int intervalMs,
                                      @Out Pointer buf,
                                      @u_int64_t long bufLen);

    String msb_metrics_recv(@u_int64_t long cancelId,
                            @In Pointer streamHandle,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_metrics_close(@In Pointer streamHandle,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_all_sandbox_metrics(@u_int64_t long cancelId,
                                   @Out Pointer buf,
                                   @u_int64_t long bufLen);

    String msb_sandbox_handle_metrics(@u_int64_t long cancelId,
                                      String name,
                                      @Out Pointer buf,
                                      @u_int64_t long bufLen);

    // ── Logs (streaming) ───────────────────────────────────────────────────

    String msb_sandbox_log_stream(@u_int64_t long cancelId,
                                  @In Pointer handle,
                                  String optsJson,
                                  @Out Pointer buf,
                                  @u_int64_t long bufLen);

    String msb_sandbox_handle_log_stream(@u_int64_t long cancelId,
                                         String name,
                                         String optsJson,
                                         @Out Pointer buf,
                                         @u_int64_t long bufLen);

    String msb_log_recv(@u_int64_t long cancelId,
                        @In Pointer streamHandle,
                        @Out Pointer buf,
                        @u_int64_t long bufLen);

    String msb_log_close(@In Pointer streamHandle,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    // ── Volume ─────────────────────────────────────────────────────────────

    String msb_volume_create(@u_int64_t long cancelId,
                             String name,
                             String optsJson,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_volume_remove(@u_int64_t long cancelId,
                             String name,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_volume_list(@u_int64_t long cancelId,
                           @Out Pointer buf,
                           @u_int64_t long bufLen);

    String msb_volume_get(@u_int64_t long cancelId,
                          String name,
                          @Out Pointer buf,
                          @u_int64_t long bufLen);

    String msb_volume_get_default(@u_int64_t long cancelId,
                                  @Out Pointer buf,
                                  @u_int64_t long bufLen);

    String msb_volume_fs_op(@u_int64_t long cancelId,
                            String target,
                            String op,
                            String argsJson,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    // ── SSH ────────────────────────────────────────────────────────────────

    String msb_sandbox_ssh_connect(@u_int64_t long cancelId,
                                   @In Pointer handle,
                                   String optsJson,
                                   @Out Pointer buf,
                                   @u_int64_t long bufLen);

    String msb_sandbox_ssh_server(@u_int64_t long cancelId,
                                  @In Pointer handle,
                                  String optsJson,
                                  @Out Pointer buf,
                                  @u_int64_t long bufLen);

    String msb_ssh_server_close(@u_int64_t long cancelId,
                                @In Pointer serverHandle,
                                @Out Pointer buf,
                                @u_int64_t long bufLen);

    String msb_ssh_server_serve_stdio(@u_int64_t long cancelId,
                                      @In Pointer serverHandle,
                                      @Out Pointer buf,
                                      @u_int64_t long bufLen);

    String msb_ssh_server_serve_connection(@u_int64_t long cancelId,
                                           @In Pointer serverHandle,
                                           @Out Pointer buf,
                                           @u_int64_t long bufLen);

    String msb_ssh_client_exec(@u_int64_t long cancelId,
                               @In Pointer clientHandle,
                               String command,
                               String optsJson,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_ssh_client_attach(@u_int64_t long cancelId,
                                 @In Pointer clientHandle,
                                 String optsJson,
                                 @Out Pointer buf,
                                 @u_int64_t long bufLen);

    String msb_ssh_client_close(@u_int64_t long cancelId,
                                @In Pointer clientHandle,
                                @Out Pointer buf,
                                @u_int64_t long bufLen);

    String msb_ssh_client_sftp(@u_int64_t long cancelId,
                               @In Pointer clientHandle,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    // ── SFTP ───────────────────────────────────────────────────────────────

    String msb_sftp_read(@u_int64_t long cancelId,
                         @In Pointer sftpHandle,
                         String path,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    String msb_sftp_write(@u_int64_t long cancelId,
                          @In Pointer sftpHandle,
                          String path,
                          String dataB64,
                          @Out Pointer buf,
                          @u_int64_t long bufLen);

    String msb_sftp_mkdir(@u_int64_t long cancelId,
                          @In Pointer sftpHandle,
                          String path,
                          @Out Pointer buf,
                          @u_int64_t long bufLen);

    String msb_sftp_remove_file(@u_int64_t long cancelId,
                                @In Pointer sftpHandle,
                                String path,
                                @Out Pointer buf,
                                @u_int64_t long bufLen);

    String msb_sftp_remove_dir(@u_int64_t long cancelId,
                               @In Pointer sftpHandle,
                               String path,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_sftp_rename(@u_int64_t long cancelId,
                           @In Pointer sftpHandle,
                           String oldPath,
                           String newPath,
                           @Out Pointer buf,
                           @u_int64_t long bufLen);

    String msb_sftp_real_path(@u_int64_t long cancelId,
                              @In Pointer sftpHandle,
                              String path,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_sftp_read_link(@u_int64_t long cancelId,
                              @In Pointer sftpHandle,
                              String path,
                              @Out Pointer buf,
                              @u_int64_t long bufLen);

    String msb_sftp_symlink(@u_int64_t long cancelId,
                            @In Pointer sftpHandle,
                            String target,
                            String linkPath,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_sftp_close(@u_int64_t long cancelId,
                          @In Pointer sftpHandle,
                          @Out Pointer buf,
                          @u_int64_t long bufLen);

    // ── Version / Info ─────────────────────────────────────────────────────

    String msb_version(@Out Pointer buf,
                       @u_int64_t long bufLen);

    String msb_default_backend_info(@Out Pointer buf,
                                    @u_int64_t long bufLen);

    // ── Image ──────────────────────────────────────────────────────────────

    String msb_image_get(@u_int64_t long cancelId,
                         String reference,
                         @Out Pointer buf,
                         @u_int64_t long bufLen);

    String msb_image_list(@u_int64_t long cancelId,
                          @Out Pointer buf,
                          @u_int64_t long bufLen);

    String msb_image_inspect(@u_int64_t long cancelId,
                             String reference,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_image_remove(@u_int64_t long cancelId,
                            String reference,
                            int force,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_image_prune(@u_int64_t long cancelId,
                           @Out Pointer buf,
                           @u_int64_t long bufLen);

    String msb_image_load(@u_int64_t long cancelId,
                          String inputPath,
                          String tagsJson,
                          @Out Pointer buf,
                          @u_int64_t long bufLen);

    String msb_image_save(@u_int64_t long cancelId,
                          String referencesJson,
                          String outputPath,
                          String format,
                          @Out Pointer buf,
                          @u_int64_t long bufLen);

    // ── Snapshot ───────────────────────────────────────────────────────────

    String msb_sandbox_handle_snapshot(@u_int64_t long cancelId,
                                       String sandboxName,
                                       String snapshotName,
                                       @Out Pointer buf,
                                       @u_int64_t long bufLen);

    String msb_snapshot_create(@u_int64_t long cancelId,
                               String sourceSandbox,
                               String optsJson,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_snapshot_open(@u_int64_t long cancelId,
                             String pathOrName,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_snapshot_verify(@u_int64_t long cancelId,
                               String pathOrName,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_snapshot_get(@u_int64_t long cancelId,
                            String nameOrDigest,
                            @Out Pointer buf,
                            @u_int64_t long bufLen);

    String msb_snapshot_list(@u_int64_t long cancelId,
                             @Out Pointer buf,
                             @u_int64_t long bufLen);

    String msb_snapshot_list_dir(@u_int64_t long cancelId,
                                 String dir,
                                 @Out Pointer buf,
                                 @u_int64_t long bufLen);

    String msb_snapshot_remove(@u_int64_t long cancelId,
                               String pathOrName,
                               int force,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_snapshot_reindex(@u_int64_t long cancelId,
                                String dir,
                                @Out Pointer buf,
                                @u_int64_t long bufLen);

    String msb_snapshot_export(@u_int64_t long cancelId,
                               String nameOrPath,
                               String out,
                               String optsJson,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    String msb_snapshot_import(@u_int64_t long cancelId,
                               String archive,
                               String dest,
                               @Out Pointer buf,
                               @u_int64_t long bufLen);

    // ── Agent ──────────────────────────────────────────────────────────────

    String msb_agent_socket_path(String name,
                                 @Out Pointer buf,
                                 @u_int64_t long bufLen);

    String msb_agent_open_sandbox(@u_int64_t long cancelId,
                                  String name,
                                  @u_int64_t long timeoutMs,
                                  @Out Pointer outHandle);

    String msb_agent_open_path(@u_int64_t long cancelId,
                               String path,
                               @u_int64_t long timeoutMs,
                               @Out Pointer outHandle);

    String msb_agent_request(@u_int64_t long cancelId,
                             @In Pointer agentHandle,
                             int flags,
                             @In Pointer bodyPtr,
                             @u_int64_t long bodyLen,
                             @Out Pointer outId,
                             @Out Pointer outFlags,
                             @Out Pointer outBodyPtr,
                             @Out Pointer outBodyLen);

    String msb_agent_stream_open(@u_int64_t long cancelId,
                                 @In Pointer agentHandle,
                                 int flags,
                                 @In Pointer bodyPtr,
                                 @u_int64_t long bodyLen,
                                 @Out Pointer outId,
                                 @Out Pointer outStreamHandle);

    String msb_agent_stream_next(@u_int64_t long cancelId,
                                 @In Pointer agentHandle,
                                 @In Pointer streamHandle,
                                 @Out Pointer outPresent,
                                 @Out Pointer outId,
                                 @Out Pointer outFlags,
                                 @Out Pointer outBodyPtr,
                                 @Out Pointer outBodyLen);

    String msb_agent_stream_close(@u_int64_t long cancelId,
                                  @In Pointer agentHandle,
                                  @In Pointer streamHandle);

    String msb_agent_send(@u_int64_t long cancelId,
                          @In Pointer agentHandle,
                          int id,
                          int flags,
                          @In Pointer bodyPtr,
                          @u_int64_t long bodyLen);

    String msb_agent_ready_bytes(@In Pointer agentHandle,
                                 @Out Pointer outBodyPtr,
                                 @Out Pointer outBodyLen);

    String msb_agent_close(@u_int64_t long cancelId,
                           @In Pointer agentHandle);

    void msb_agent_free_bytes(@In Pointer ptr,
                              @u_int64_t long len);
}