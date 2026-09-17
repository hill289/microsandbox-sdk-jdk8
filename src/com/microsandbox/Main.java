package com.microsandbox;

import com.microsandbox.sdk.Microsandbox;
import com.microsandbox.sdk.Sandbox;
import com.microsandbox.sdk.SandboxHandle;
import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.model.ExecOutput;
import com.microsandbox.sdk.model.SandboxConfig;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            // 1. 端口映射：宿主机 8080 -> 沙箱内 8000
            Map<Integer, Integer> ports = new HashMap<>();
            ports.put(8080, 8000);

            // 2. 构建沙箱配置
            SandboxConfig config = SandboxConfig.builder()
                    .image("docker.m.daocloud.io/library/python")
                    .memory(512)
                    .cpus(1)
                    .detached(true)       // 后台运行，不阻塞
                    .replace(true)        // 已存在同名沙箱时按新配置重建，端口映射才会生效
                    .ports(ports)
                    .build();

            // 3. 创建沙箱（首次会按上面的配置创建；replace=true 时会重建）
            Sandbox sandbox = Microsandbox.createSandbox("my-sandbox", config);
            System.out.println("[sandbox] started with port mapping 8080 -> 8000");

            // 4. 准备首页文件
            ExecOutput prep = sandbox.exec("sh", "-c",
                    "mkdir -p /tmp/www && " +
                    "printf '%s' '<html><body><h1>Hello from Python http.server in a microVM!</h1></body></html>' " +
                    "> /tmp/www/index.html");
            if (!prep.getStderr().isEmpty()) {
                System.err.println("[prep-err] " + prep.getStderr());
            }

            // 5. 后台启动 Python HTTP 服务器
            //    --bind 0.0.0.0 是必须的，否则端口映射无法从宿主机转发进来
            ExecOutput start = sandbox.exec("sh", "-c",
                    "cd /tmp/www && " +
                    "nohup python -m http.server 8000 --bind 0.0.0.0 " +
                    "> /tmp/http.log 2>&1 & echo $!");
            System.out.println("[start] python http.server pid = " + start.getStdout().trim());

            // 6. 等服务器起来，然后在沙箱内自检
            Thread.sleep(1500);
            ExecOutput check = sandbox.exec("sh", "-c",
                    "python -c \"import urllib.request;" +
                    "print(urllib.request.urlopen('http://127.0.0.1:8000/').read().decode())\"");
            System.out.println("[check] " + check.getStdout());
            if (!check.getStderr().isEmpty()) {
                System.err.println("[check-err] " + check.getStderr());
            }

            // 7. 打印宿主机访问方式
            System.out.println();
            System.out.println("=== 从宿主机访问 ===");
            System.out.println("浏览器: http://localhost:8080/");
            System.out.println("curl   : curl http://localhost:8080/");

        } catch (MicrosandboxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}