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
public class MosSShServer {

    public static void start() throws IOException {
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(21012);
        sshd.setShellFactory(new MosShellFactory());
        sshd.setPasswordAuthenticator(
            (username, password, session) -> "mos".equals(username) && "mos123".equals(password));
        sshd.setHost("127.0.0.1");
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.start();
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
    }

}
