# Microsandbox Java SDK

[![JDK 8+](https://img.shields.io/badge/JDK-8%2B-blue.svg)](https://openjdk.org/projects/jdk/8/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-0.6.18-orange.svg)](https://github.com/microsandbox/microsandbox-java-sdk)
[![Platform](https://img.shields.io/badge/Platform-Linux%20%7C%20macOS%20%7C%20Windows-lightgrey.svg)]()

> **安全、轻量、极速的 Java 微虚拟机（microVM）沙箱管理 SDK**

Microsandbox Java SDK 让你在 Java/JVM 应用中通过编程方式创建和管理基于 **KVM/Cloud Hypervisor** 的 microVM 隔离沙箱环境。兼具**毫秒级启动速度**和**硬件级安全隔离**，是 AI Agent、在线代码执行、CI/CD、多租户 SaaS 等场景的理想选择。

---

## 目录

- [核心特性](#核心特性)
- [架构总览](#架构总览)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [API 详解](#api-详解)
  - [沙箱生命周期管理](#1-沙箱生命周期管理)
  - [命令执行](#2-命令执行)
  - [文件系统操作](#3-文件系统操作)
  - [SSH 操作](#4-ssh-操作)
  - [端口映射与网络](#5-端口映射与网络)
  - [镜像管理](#6-镜像管理)
  - [快照管理](#7-快照管理)
  - [日志与监控](#8-日志与监控)
  - [Agent 协议](#9-agent-协议)
- [高级配置](#高级配置)
- [异常处理](#异常处理)
- [示例应用](#示例应用)
- [项目结构](#项目结构)
- [与其他方案对比](#与其他方案对比)
- [贡献指南](#贡献指南)
- [License](#license)

---

## 核心特性

| 特性 | 描述 |
|---|---|
| 🔒 **硬件级隔离** | 基于 KVM/Cloud Hypervisor 的 microVM，提供比容器更强的安全边界 |
| ⚡ **毫秒级启动** | microVM 启动速度远超传统虚拟机，接近容器水平 |
| 🐳 **OCI 镜像兼容** | 直接使用 Docker Hub / OCI 标准镜像，无需特殊格式 |
| 📂 **客户机文件系统操作** | 无需 SSH 即可读写虚拟机内部文件 |
| 🔌 **内建 SSH 支持** | 客户端/服务端 SSH、SFTP、端口转发开箱即用 |
| 📸 **快照与恢复** | 一键创建/恢复沙箱快照，支持完整性校验 |
| 📊 **实时监控** | 流式日志订阅和指标采集 |
| 🏗️ **JDK 8 兼容** | 兼容 Java 8 及以上版本，适合企业遗留系统 |
| 📦 **零外部依赖** | 除 JNR-FFI 外无额外第三方库依赖 |
| 🌍 **跨平台** | 支持 Linux、macOS、Windows |

---

## 架构总览

```
┌─────────────────────────────────────────────────────┐
│               Java Application                       │
│         Microsandbox.createSandbox(...)              │
├─────────────────────────────────────────────────────┤
│            Microsandbox Java SDK                     │
│   Sandbox · SandboxFs · SandboxSSH · Snapshot       │
│   Image · Volume · Agent · LogStream · MetricsStream │
├─────────────────────────────────────────────────────┤
│            NativeBridge (JNR-FFI)                    │
│   Thread-safe singleton · Cancellation support       │
├─────────────────────────────────────────────────────┤
│     libmicrosandbox_go_ffi (Go/Rust native lib)      │
├─────────────────────────────────────────────────────┤
│     microVM Runtime (KVM / Cloud Hypervisor)          │
└─────────────────────────────────────────────────────┘
```

---

## 环境要求

- **JDK**: 8 或更高版本
- **操作系统**: Linux（KVM）/ macOS（VZ）/ Windows（WSL2）
- **原生运行时**: SDK 支持自动安装，也可手动部署

| 组件 | 说明 |
|---|---|
| `msb` | Microsandbox CLI 二进制文件 |
| `libmicrosandbox_go_ffi` | FFI 共享库（`.so` / `.dylib` / `.dll`） |

SDK 会在首次调用时自动检测并下载所需组件到 `~/.microsandbox/` 目录。

---

## 快速开始

### 1. 添加依赖

将 SDK JAR 和 JNR-FFI 依赖添加到你的项目中：

**Maven:**

```xml
<dependency>
    <groupId>com.microsandbox</groupId>
    <artifactId>microsandbox-java-sdk</artifactId>
    <version>0.6.18</version>
</dependency>
<dependency>
    <groupId>com.github.jnr</groupId>
    <artifactId>jnr-ffi</artifactId>
    <version>2.2.16</version>
</dependency>
```

**Gradle:**

```groovy
implementation 'com.microsandbox:microsandbox-java-sdk:0.6.18'
implementation 'com.github.jnr:jnr-ffi:2.2.16'
```

### 2. 最小示例

```java
import com.microsandbox.sdk.Microsandbox;
import com.microsandbox.sdk.Sandbox;
import com.microsandbox.sdk.model.ExecOutput;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // 确保运行时已安装
        Microsandbox.ensureInstalled();

        // 创建一个 Alpine 沙箱
        Sandbox sandbox = Microsandbox.createSandbox("hello",
            SandboxCreateOptions.builder()
                .image("alpine:3.19")
                .memory(256)
                .cpus(1)
                .build());

        // 在沙箱中执行命令
        ExecOutput result = sandbox.shell("echo 'Hello from microVM!'");
        System.out.println(result.getStdout());

        // 关闭沙箱
        sandbox.close();
    }
}
```

### 3. 运行

```bash
java -cp your-app.jar:libs/* QuickStart
```

---

## API 详解

### 1. 沙箱生命周期管理

#### 创建沙箱

```java
// 使用 Builder 模式
Sandbox sandbox = Microsandbox.createSandbox("my-sandbox",
    SandboxCreateOptions.builder()
        .image("python:3.12-slim")
        .memory(512)                   // 内存限制 (MiB)
        .cpus(2)                       // CPU 核心数
        .maxMemory(1024)               // 最大可热插拔内存 (MiB)
        .maxCpus(4)                    // 最大可热插拔 CPU 数
        .detached(true)                // 后台运行，不阻塞当前进程
        .replace(true)                 // 同名沙箱存在时替换重建
        .ephemeral(false)              // 是否临时沙箱
        .user("root")                  // 客户机用户
        .hostname("worker-01")         // 客户机主机名
        .workdir("/app")               // 初始工作目录
        .shell("/bin/bash")            // 默认 shell
        .pullPolicy("if-missing")      // 镜像拉取策略: always / if-missing / never
        .env("APP_ENV", "production")  // 环境变量
        .env("LOG_LEVEL", "debug")
        .entrypoint(new String[]{"/bin/sh", "-c"})
        .cmd(new String[]{"echo hello"})
        .build());

// 或使用已有的 SandboxConfig 对象
SandboxConfig config = SandboxConfig.builder()
    .image("node:20")
    .memory(1024)
    .build();
Sandbox sandbox = Microsandbox.createSandbox("node-sandbox", config);
```

#### 连接或创建

```java
// 如果同名沙箱已存在则连接，否则创建新沙箱
Sandbox sandbox = Microsandbox.connectOrCreateSandbox("my-sandbox",
    SandboxCreateOptions.builder()
        .image("alpine:3.19")
        .build());
```

#### 查找沙箱

```java
// 获取轻量级句柄（不连接）
SandboxHandle handle = Microsandbox.getSandbox("my-sandbox");
System.out.println("Status: " + handle.getStatus());
```

#### 列出所有沙箱

```java
List<SandboxHandle> sandboxes = Microsandbox.listSandboxes();
for (SandboxHandle sb : sandboxes) {
    System.out.println(sb.getName() + " -> " + sb.getStatus());
}
```

#### 启动 / 停止 / 销毁

```java
// 启动已停止的沙箱
Sandbox sandbox = Microsandbox.startSandbox("my-sandbox");

// 启动为 detached 模式（释放句柄后 VM 继续运行）
Sandbox sandbox = Microsandbox.startSandboxDetached("my-sandbox");

// 优雅停止（等待超时后强制终止）
sandbox.stop(30, TimeUnit.SECONDS);

// 请求停止（立即返回，异步执行）
sandbox.requestStop();

// 强制终止
sandbox.kill(5, TimeUnit.SECONDS);

// 请求强制终止（立即返回）
sandbox.requestKill();

// 优雅排空
sandbox.drain();

// 从管理中分离
sandbox.detach();

// 销毁沙箱
Microsandbox.removeSandbox("my-sandbox");
```

#### 生命周期探测

```java
// 检查沙箱是否存活
boolean alive = sandbox.ping();

// 保持沙箱活跃（防止超时回收）
sandbox.touch();

// 等待沙箱停止
sandbox.waitUntilStopped();

// 检查当前句柄是否拥有 VM 生命周期
boolean owns = sandbox.ownsLifecycle();
```

#### 自动资源管理

```java
// 实现 AutoCloseable，支持 try-with-resources
try (Sandbox sandbox = Microsandbox.createSandbox("temp-sandbox",
        SandboxCreateOptions.builder().image("alpine:3.19").build())) {
    ExecOutput out = sandbox.shell("echo 'auto-closed'");
    System.out.println(out.getStdout());
}  // sandbox.close() 自动调用
```

---

### 2. 命令执行

#### 同步执行

```java
// 执行指定命令
ExecOutput result = sandbox.exec("python", "--version");
System.out.println("stdout: " + result.getStdout());
System.out.println("stderr: " + result.getStderr());
System.out.println("exit code: " + result.getExitCode());

// 使用 shell 便捷方法
ExecOutput result = sandbox.shell("ls -la /tmp && echo done");
```

#### 带选项执行

```java
ExecOutput result = sandbox.exec("make", "build",
    ExecOptions.builder()
        .workdir("/project")
        .env("CC", "gcc")
        .env("CFLAGS", "-O2")
        .timeout(60, TimeUnit.SECONDS)
        .user("builder")
        .build());
```

#### 流式执行

```java
// 实时获取命令输出流
ExecStream stream = sandbox.execStream("tail", "-f", "/var/log/app.log");
String line;
while ((line = stream.nextLine()) != null) {
    System.out.println("[LOG] " + line);
}
stream.close();
```

#### 交互式终端

```java
// 启动交互式 PTY 会话
int exitCode = sandbox.attach("/bin/bash");
System.out.println("Shell exited with code: " + exitCode);

// 启动默认 shell
int exitCode = sandbox.attachShell();
```

---

### 3. 文件系统操作

通过 `SandboxFs` 直接操作虚拟机内部的文件系统，无需 SSH。

```java
SandboxFs fs = sandbox.fs();
```

#### 读写文件

```java
// 写入文件（字符串）
fs.writeString("/tmp/config.json", "{\"key\": \"value\"}");

// 写入文件（字节）
fs.writeBytes("/tmp/binary.dat", byteArray);

// 读取文件（字符串）
String content = fs.readString("/etc/hostname");

// 读取文件（字节）
byte[] data = fs.readBytes("/tmp/config.json");
```

#### 目录操作

```java
// 列出目录
List<String> entries = fs.list("/var/log");
for (String entry : entries) {
    System.out.println(entry);
}

// 创建目录（自动创建父目录）
fs.mkdir("/app/src/utils");

// 创建多级目录
fs.mkdirp("/data/databases/postgres/backup");

// 检查路径是否存在
boolean exists = fs.exists("/etc/passwd");

// 删除文件
fs.remove("/tmp/old-file.txt");

// 删除目录（递归）
fs.removeDir("/tmp/build-cache");
```

---

### 4. SSH 操作

#### SSH 客户端

```java
SandboxSSH ssh = sandbox.ssh();

// 打开 SSH 客户端会话
try (SandboxSSH.SSHClient client = ssh.openClient(null)) {
    // 执行远程命令
    String response = client.exec("whoami && hostname");
    System.out.println(response);

    // 带选项执行
    String response = client.exec("ls -la",
        "{\"workdir\": \"/home/user\", \"timeout\": 30}");
}
```

#### SSH 服务端

```java
// 准备可复用的 SSH 服务端端点
SandboxSSH.SSHServer server = ssh.prepareServer(null);

// 获取服务端地址信息
String endpoint = server.getEndpoint();
System.out.println("SSH endpoint: " + endpoint);

server.close();
```

---

### 5. 端口映射与网络

#### 端口映射

```java
// 宿主机 8080 -> 沙箱内 8000
Map<Integer, Integer> ports = new HashMap<>();
ports.put(8080, 8000);
ports.put(3306, 3306);

Sandbox sandbox = Microsandbox.createSandbox("web-app",
    SandboxCreateOptions.builder()
        .image("nginx:alpine")
        .ports(ports)
        .build());

// 或使用 Builder 链式调用
Sandbox sandbox = Microsandbox.createSandbox("web-app",
    SandboxCreateOptions.builder()
        .image("nginx:alpine")
        .port(8080, 8000)
        .port(3306, 3306)
        .build());
```

#### 网络配置

```java
NetworkConfig network = new NetworkConfig();
// 配置网络策略...

Sandbox sandbox = Microsandbox.createSandbox("sandbox",
    SandboxCreateOptions.builder()
        .image("alpine:3.19")
        .network(network)
        .build());
```

---

### 6. 镜像管理

```java
// 列出所有缓存的镜像
List<ImageInfo> images = Image.list();
for (ImageInfo img : images) {
    System.out.println(img.getReference() + " (" + img.getSize() + " bytes)");
}

// 获取单个镜像信息
ImageInfo info = Image.get("alpine:3.19");

// 从本地归档导入镜像
List<ImageInfo> imported = Image.load("/path/to/image.tar", "my-tag:latest");

// 导出镜像到归档
Image.save(new String[]{"alpine:3.19"}, "/tmp/alpine-export.tar", "docker");

// 删除缓存镜像
Image.remove("alpine:3.19");             // 安全删除（有沙箱引用时失败）
Image.remove("alpine:3.19", true);       // 强制删除

// 清理未使用的镜像
String pruned = Image.prune();
```

---

### 7. 快照管理

```java
// 从已停止的沙箱创建快照
String snapshotJson = Snapshot.create("v1.0", "my-sandbox",
    new Snapshot.CreateOptions()
        .force(true)
        .recordIntegrity(true)
        .labels(Map.of("version", "1.0", "env", "prod")));

// 从沙箱句柄创建快照
String result = Snapshot.capture("my-sandbox", "v1.0");

// 查看快照
String info = Snapshot.get("v1.0");

// 列出所有快照
String list = Snapshot.list();

// 验证快照完整性
String verification = Snapshot.verify("v1.0");

// 导出快照（包含父快照和镜像数据）
Snapshot.save("v1.0", "/tmp/snapshot-v1.0.tar", true, true, false);

// 导入快照
String imported = Snapshot.load("/tmp/snapshot-v1.0.tar", "/snapshots/");

// 删除快照
Snapshot.remove("v1.0");
Snapshot.remove("v1.0", true);  // 强制删除

// 重建快照索引
long count = Snapshot.reindex("/snapshots/dir");
```

---

### 8. 日志与监控

#### 读取日志

```java
// 读取历史日志
List<LogEntry> logs = sandbox.logs();
for (LogEntry entry : logs) {
    System.out.println("[" + entry.getTimestamp() + "] " + entry.getMessage());
}

// 带选项读取
List<LogEntry> logs = sandbox.logs("{\"tail\": 100, \"follow\": false}");
```

#### 流式日志

```java
// 实时订阅日志流
LogStream logStream = sandbox.logStream();
LogEntry entry;
while ((entry = logStream.next()) != null) {
    System.out.println("[LIVE] " + entry.getMessage());
}
logStream.close();
```

#### 实时指标

```java
// 获取当前指标
Metrics metrics = sandbox.metrics();
System.out.println("CPU: " + metrics.getCpuPercent());
System.out.println("Memory: " + metrics.getMemoryUsageBytes());

// 流式指标订阅（每 5 秒采集一次）
MetricsStream stream = sandbox.metricsStream(5000);
Metrics m;
while ((m = stream.next()) != null) {
    System.out.printf("CPU: %.1f%%, Mem: %d MB%n",
        m.getCpuPercent(), m.getMemoryUsageBytes() / 1024 / 1024);
}
stream.close();

// 获取所有沙箱的指标
Map<String, Metrics> allMetrics = Microsandbox.allMetrics();
for (Map.Entry<String, Metrics> entry : allMetrics.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
```

---

### 9. Agent 协议

底层 Agent 协议用于与沙箱内的 `agentd` 守护进程通信。

```java
// 按沙箱名称连接
Agent.AgentClient client = Agent.connectSandbox("my-sandbox");

// 按 socket 路径连接
Agent.AgentClient client = Agent.connectPath("/path/to/relay.sock");

// 获取 socket 路径
String path = Agent.socketPath("my-sandbox");

// 发送请求帧（CBOR 编码）
byte[] cborBody = ...;
Agent.RawFrame response = client.request(Agent.FLAG_SESSION_START, cborBody);
System.out.println("Response flags: " + response.getFlags());
System.out.println("Response body: " + response.getBody());

// 打开流式会话
Agent.AgentStream stream = client.stream(Agent.FLAG_TERMINAL, cborBody);
Agent.RawFrame frame;
while ((frame = stream.next()) != null) {
    // 处理帧...
}

// 发送后续帧
client.send(response.getId(), Agent.FLAG_TERMINAL, followUpBody);

// 获取握手就绪字节
byte[] readyBytes = client.readyBytes();

// 关闭
stream.close();
client.close();
```

---

## 高级配置

### 挂载卷

```java
Map<String, MountConfig> mounts = new HashMap<>();
mounts.put("/data", new MountConfig("/host/data", "rw"));
mounts.put("/config", new MountConfig("/host/config", "ro"));

Sandbox sandbox = Microsandbox.createSandbox("sandbox",
    SandboxCreateOptions.builder()
        .image("alpine:3.19")
        .mount("/data", new MountConfig("/host/data", "rw"))
        .mount("/config", new MountConfig("/host/config", "ro"))
        .build());
```

### 热插拔资源

```java
Sandbox sandbox = Microsandbox.createSandbox("sandbox",
    SandboxCreateOptions.builder()
        .image("alpine:3.19")
        .memory(256)       // 初始内存
        .maxMemory(2048)   // 最大可扩展到 2GB
        .cpus(1)           // 初始 1 核
        .maxCpus(8)        // 最大可扩展到 8 核
        .build());

// 动态修改资源
sandbox.modify("{\"memory\": 1024}");
```

### 版本信息

```java
// 运行时版本
String version = Microsandbox.version();

// SDK 版本
String sdkVersion = Microsandbox.sdkVersion();

// 默认后端信息
String backendInfo = Microsandbox.defaultBackendInfo();
```

### 手动安装管理

```java
// 检查是否已安装
boolean installed = Microsandbox.isInstalled();

// 强制安装（带进度回调）
Setup.ensureInstalled(progress -> System.out.println(progress));
```

---

## 异常处理

SDK 提供了结构化的异常体系，通过 `ErrorKind` 枚举支持程序化错误处理：

```java
import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.exception.ErrorKind;

try {
    Sandbox sandbox = Microsandbox.createSandbox("my-sandbox", config);
    ExecOutput result = sandbox.exec("some-command");
} catch (MicrosandboxException e) {
    switch (e.getKind()) {
        case SANDBOX_NOT_FOUND:
            System.err.println("沙箱不存在");
            break;
        case SANDBOX_ALREADY_EXISTS:
            System.err.println("沙箱已存在，请使用 replace 选项");
            break;
        case EXEC_TIMEOUT:
            System.err.println("命令执行超时");
            break;
        case RUNTIME_NOT_INSTALLED:
            System.err.println("运行时未安装，请调用 ensureInstalled()");
            break;
        case FFI_LOAD_ERROR:
            System.err.println("原生库加载失败: " + e.getMessage());
            break;
        case NETWORK_POLICY:
            System.err.println("网络策略违规");
            break;
        case IMAGE_NOT_FOUND:
            System.err.println("镜像不存在: " + e.getMessage());
            break;
        case SANDBOX_NOT_RUNNING:
            System.err.println("沙箱未运行");
            break;
        case INVALID_HANDLE:
            System.err.println("句柄已关闭或无效");
            break;
        case BUFFER_TOO_SMALL:
            System.err.println("FFI 响应超出缓冲区限制");
            break;
        default:
            System.err.println("未预期的错误 [" + e.getKind() + "]: " + e.getMessage());
    }
    e.printStackTrace();
}
```

### 完整错误类型列表

| ErrorKind | 说明 |
|---|---|
| `SANDBOX_NOT_FOUND` | 请求的沙箱不存在 |
| `SANDBOX_NOT_RUNNING` | 沙箱存在但未运行 |
| `SANDBOX_ALREADY_EXISTS` | 同名沙箱已存在 |
| `SANDBOX_STILL_RUNNING` | 沙箱仍在运行，无法删除 |
| `SANDBOX_REPLACED` | 沙箱已被新配置替换 |
| `EXEC_TIMEOUT` | 命令执行超时 |
| `EXEC_FAILED` | 命令执行失败 |
| `FILESYSTEM` | 客户机文件系统操作失败 |
| `PATH_NOT_FOUND` | 文件路径不存在 |
| `IMAGE_NOT_FOUND` | OCI 镜像引用无法解析 |
| `IMAGE_IN_USE` | 镜像仍被沙箱引用，无法删除 |
| `IMAGE_PULL_FAILED` | 镜像拉取失败 |
| `SNAPSHOT_NOT_FOUND` | 快照不存在 |
| `SNAPSHOT_ALREADY_EXISTS` | 快照目标已存在 |
| `SNAPSHOT_SANDBOX_RUNNING` | 无法对运行中的沙箱创建快照 |
| `SNAPSHOT_INTEGRITY` | 快照完整性校验失败 |
| `NETWORK_POLICY` | 网络策略配置或运行时违规 |
| `VOLUME_NOT_FOUND` | 卷不存在 |
| `VOLUME_ALREADY_EXISTS` | 卷已存在 |
| `RUNTIME_NOT_INSTALLED` | 运行时未安装 |
| `FFI_LOAD_ERROR` | FFI 共享库加载失败 |
| `FFI_ERROR` | FFI 层内部错误 |
| `CANCELLED` | 操作被取消 |
| `TIMEOUT` | 操作超时 |
| `INVALID_CONFIG` | 配置被运行时拒绝 |
| `INVALID_ARGUMENT` | FFI 参数格式错误 |
| `INVALID_HANDLE` | 句柄无效或已关闭 |
| `BUFFER_TOO_SMALL` | FFI 响应超出缓冲区 |
| `IO` | 宿主机 I/O 错误 |
| `NO_DEFAULT_COMMAND` | 入口点/CMD 不可执行 |
| `UNSUPPORTED_OPERATION` | 运行时版本不支持该特性 |
| `INTERNAL` | 内部错误 |

---

## 示例应用

### 在 Python HTTP 服务器中使用端口映射

```java
public class WebServerDemo {
    public static void main(String[] args) throws Exception {
        Microsandbox.ensureInstalled();

        // 创建带端口映射的沙箱
        Sandbox sandbox = Microsandbox.createSandbox("web-server",
            SandboxCreateOptions.builder()
                .image("python:3.12-slim")
                .memory(512)
                .cpus(1)
                .detached(true)
                .replace(true)
                .port(8080, 8000)
                .build());

        System.out.println("[sandbox] started with port mapping 8080 -> 8000");

        // 准备首页文件
        sandbox.exec("sh", "-c",
            "mkdir -p /tmp/www && " +
            "echo '<html><body><h1>Hello from microVM!</h1></body></html>' " +
            "> /tmp/www/index.html");

        // 启动 Python HTTP 服务器
        ExecOutput start = sandbox.exec("sh", "-c",
            "cd /tmp/www && " +
            "nohup python -m http.server 8000 --bind 0.0.0.0 " +
            "> /tmp/http.log 2>&1 & echo $!");
        System.out.println("[start] pid = " + start.getStdout().trim());

        // 自检
        Thread.sleep(1500);
        ExecOutput check = sandbox.exec("sh", "-c",
            "python -c \"import urllib.request;" +
            "print(urllib.request.urlopen('http://127.0.0.1:8000/').read().decode())\"");
        System.out.println("[check] " + check.getStdout());

        System.out.println("\n=== 从宿主机访问 ===");
        System.out.println("浏览器: http://localhost:8080/");
    }
}
```

### 安全执行用户代码（AI Agent 场景）

```java
public class AgentSandbox {
    public static void main(String[] args) throws Exception {
        Microsandbox.ensureInstalled();

        try (Sandbox sandbox = Microsandbox.createSandbox("agent-session",
                SandboxCreateOptions.builder()
                    .image("python:3.12-slim")
                    .memory(256)
                    .cpus(1)
                    .ephemeral(true)     // 用完即销毁
                    .build())) {

            // 用户提交的代码
            String userCode = "print(sum(range(100)))";

            // 将代码写入文件
            sandbox.fs().writeString("/tmp/user_script.py", userCode);

            // 在隔离环境中执行
            ExecOutput result = sandbox.exec("python", "/tmp/user_script.py");

            System.out.println("执行结果: " + result.getStdout());
            // 输出: 执行结果: 4950
        }  // 沙箱自动销毁
    }
}
```

### 一键部署开发环境

```java
public class DevEnvironment {
    public static void main(String[] args) throws Exception {
        Microsandbox.ensureInstalled();

        Sandbox sandbox = Microsandbox.createSandbox("dev-env",
            SandboxCreateOptions.builder()
                .image("ubuntu:22.04")
                .memory(2048)
                .cpus(4)
                .port(3000, 3000)
                .env("NODE_ENV", "development")
                .env("DEBIAN_FRONTEND", "noninteractive")
                .build());

        // 安装开发工具链
        sandbox.shell("apt-get update && apt-get install -y curl git build-essential");

        // 安装 Node.js
        sandbox.shell("curl -fsSL https://deb.nodesource.com/setup_20.x | bash -");
        sandbox.shell("apt-get install -y nodejs");

        // 拉取项目代码
        sandbox.shell("git clone https://github.com/user/project.git /app");

        // 启动开发服务器
        sandbox.execStream("sh", "-c", "cd /app && npm install && npm run dev");

        System.out.println("开发环境就绪: http://localhost:3000");
    }
}
```

---

## 项目结构

```
src/
└── com/microsandbox/
    ├── Main.java                          # 示例应用入口
    └── sdk/
        ├── Microsandbox.java              # SDK 主入口（静态工具类）
        ├── Sandbox.java                   # 沙箱实例（命令执行、文件系统、生命周期）
        ├── SandboxCreateOptions.java      # 沙箱创建选项（Builder 模式）
        ├── SandboxHandle.java             # 轻量级沙箱句柄
        ├── SandboxFs.java                 # 客户机文件系统操作
        ├── SandboxSSH.java                # SSH 客户端/服务端
        ├── Image.java                     # OCI 镜像管理
        ├── Snapshot.java                  # 快照管理
        ├── Volume.java                    # 卷管理
        ├── Agent.java                     # Agent 协议客户端
        ├── Setup.java                     # 运行时自动安装
        ├── ExecStream.java                # 命令执行流
        ├── LogStream.java                 # 日志流
        ├── MetricsStream.java             # 指标流
        ├── exception/
        │   ├── MicrosandboxException.java # 结构化异常
        │   └── ErrorKind.java             # 错误类型枚举
        ├── ffi/
        │   ├── MicrosandboxNative.java    # JNR-FFI 原生接口定义
        │   ├── NativeBridge.java          # FFI 桥接层（单例）
        │   └── LibraryLoader.java         # 共享库加载器
        └── model/
            ├── SandboxConfig.java         # 沙箱配置模型
            ├── SandboxInfo.java           # 沙箱信息模型
            ├── SandboxStatus.java         # 沙箱状态枚举
            ├── ExecOptions.java           # 执行选项
            ├── ExecOutput.java            # 执行输出
            ├── ExecEvent.java             # 执行事件
            ├── Metrics.java               # 资源指标
            ├── LogEntry.java              # 日志条目
            ├── LogOptions.java            # 日志选项
            ├── LogStreamOptions.java       # 日志流选项
            ├── ImageInfo.java             # 镜像信息
            ├── VolumeInfo.java            # 卷信息
            ├── MountConfig.java           # 挂载配置
            ├── NetworkConfig.java         # 网络配置
            └── ...
```

---

## 与其他方案对比

| 特性 | Microsandbox Java SDK | Docker Java SDK | 传统虚拟机 |
|---|---|---|---|
| **隔离级别** | 🔒 硬件级 (KVM) | 进程级 (namespace/cgroup) | 🔒 硬件级 |
| **启动速度** | ⚡ 毫秒级 | 秒级 | 🐢 分钟级 |
| **内存开销** | 极低 (~10MB) | 低 (~50MB) | 高 (~512MB+) |
| **OCI 镜像兼容** | ✅ | ✅ | ❌ |
| **Java 原生 SDK** | ✅ 专为 JVM 设计 | 第三方封装 | 需调用 CLI |
| **内置 SSH** | ✅ | 需额外配置 | 需额外配置 |
| **快照管理** | ✅ 一等公民 | 需 Docker commit | 支持但笨重 |
| **热插拔资源** | ✅ | ❌ | 部分支持 |
| **Agent 通信** | ✅ 内置协议 | ❌ | ❌ |
| **内核依赖** | 需要 KVM/VZ 支持 | 需要 Docker daemon | 无 |

---

## 贡献指南

我们欢迎社区贡献！请遵循以下步骤：

1. **Fork** 本仓库
2. **创建** 功能分支：`git checkout -b feature/amazing-feature`
3. **提交** 更改：`git commit -m 'Add amazing feature'`
4. **推送** 到分支：`git push origin feature/amazing-feature`
5. **创建** Pull Request

### 开发规范

- 保持 **JDK 8 兼容性**，不要使用 Java 8 以上的语法特性
- 所有公开 API 必须添加 **Javadoc** 注释
- Handle 类必须实现 `AutoCloseable` 接口
- 使用结构化的 `MicrosandboxException` 进行错误处理
- 提交前确保代码通过所有现有测试

### 报告问题

请通过 [GitHub Issues](https://github.com/microsandbox/microsandbox-java-sdk/issues) 报告问题，包含：

- 运行环境（OS、JDK 版本）
- 复现步骤
- 期望行为与实际行为
- 相关日志/堆栈信息

---

## License

本项目基于 [Apache License 2.0](LICENSE) 开源。

```
Copyright 2024 Microsandbox Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<p align="center">
  <strong>Microsandbox</strong> — 安全沙箱，无限可能 🚀
</p>
