package org.cooder.mos.ssh;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.common.util.threads.SshThreadPoolExecutor;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.cooder.mos.shell.factory.MosShellFactory;

/**
 * <信息描述>
 *
 * @author lihaitao on 2021/6/5
 */
public class MosSshServer {

    private static final SshServer SERVER = SshServer.setUpDefaultServer();

    // 支持的最大连接数
    public static final int MAX_COUNT = 3;
    public static final CloseableExecutorService EXECUTOR_SERVICE = initExecutor();

    static {
        SERVER.setPort(21013);
        SERVER.setShellFactory(new MosShellFactory());
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
    private static CloseableExecutorService initExecutor() {
        return new SshThreadPoolExecutor(MAX_COUNT, MAX_COUNT, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread thread = new Thread(r);
            thread.setName("shell-thread-%d");
            return thread;
        });
    }
}
