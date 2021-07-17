package org.cooder.mos.ssh;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.common.util.threads.SshThreadPoolExecutor;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.scp.ScpModuleProperties;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.cooder.mos.fs.MosScpFileOpener;
import org.cooder.mos.shell.factory.MosShellFactory;

/**
 * <信息描述>
 *
 * @author lihaitao on 2021/6/5
 */
public class MosSshServer {

    // 支持的最大连接数
    public static final int MAX_COUNT = 3;
    public static final CloseableExecutorService EXECUTOR_SERVICE = initExecutor().get();
    private static final SshServer SERVER = SshServer.setUpDefaultServer();

    static {
        // 用scp包裹command factory
        ScpCommandFactory scpCommandFactory = new ScpCommandFactory.Builder()
            .withDelegateShellFactory(new MosShellFactory()).withFileOpener(new MosScpFileOpener()).build();

        ScpModuleProperties.PROP_AUTO_SYNC_FILE_ON_WRITE.set(SERVER, true);
        CoreModuleProperties.WINDOW_TIMEOUT.set(SERVER, Duration.ofSeconds(5));
        scpCommandFactory.setExecutorServiceProvider(initExecutor());
        // scp encoding默认编码集是utf-8
        ScpModuleProperties.NAME_ENCODING_CHARSET.set(SERVER, StandardCharsets.UTF_8);

        SERVER.setPort(21013);
        SERVER.setShellFactory(scpCommandFactory);
        SERVER.setCommandFactory(scpCommandFactory);
        SERVER.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);
        SERVER.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("key.ser").toPath()));
    }

    public static void start() throws IOException {
        SERVER.start();
        // 启动服务后 需要保持运行
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
        }
    }

    public static void close() {
        try {
            SERVER.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化线程池
     *
     * @return
     */
    private static Supplier<? extends CloseableExecutorService> initExecutor() {
        return () -> new SshThreadPoolExecutor(MAX_COUNT, MAX_COUNT, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("shell-thread-%d");
                return thread;
            });
    }
}
