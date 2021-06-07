package org.cooder.mos.ssh;

import java.io.IOException;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.cooder.mos.shell.factory.MosShellFactory;

/**
 * <信息描述>
 *
 * @author lihaitao on 2021/6/5
 */
public class MosSshServer {

    private static final SshServer SERVER = SshServer.setUpDefaultServer();

    static {
        SERVER.setPort(21012);
        SERVER.setShellFactory(new MosShellFactory());
        SERVER.setPasswordAuthenticator(
            (username, password, session) -> "mos".equals(username) && "mos123".equals(password));
        SERVER.setHost("127.0.0.1");
        SERVER.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
    }

    public static void start() throws IOException {
        SERVER.start();
        // 启动服务后 需要保持运行
        while (true) {
            try {
                Thread.sleep(100);
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
}
