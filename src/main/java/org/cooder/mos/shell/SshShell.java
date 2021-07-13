/*
 * This file is part of MOS <p> Copyright (c) 2021 by cooder.org <p> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */
package org.cooder.mos.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.cooder.mos.MosSystem;
import org.cooder.mos.Utils;
import org.cooder.mos.ssh.MosSshServer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;

public class SshShell extends Shell implements org.apache.sshd.server.command.Command {
    private static AtomicInteger shellCount = new AtomicInteger(0);
    private OutputStream out;
    private OutputStream err;
    private InputStream in;
    private ExitCallback callback;
    private ChannelSession channel;
    private Environment env;

    public SshShell(String path) {
        super(MosSystem.fileSystem().find(new String[] {path}));
    }

    /**
     * 引导提示
     * 
     * @return
     */
    private String prompt() {
        return String.format("root@mos-jason: %s $ ", currentPath());
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    /**
     * 循环任务
     */
    @Override
    public void loop() {
        try {
            LineReader reader = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.builder().streams(in, out).encoding(StandardCharsets.UTF_8).build()).build();
            while (true) {
                String cmd = reader.readLine(prompt());
                if ("exit".equals(cmd)) {
                    Utils.printMsg(out, "bye~");
                    Utils.writeNewLine(out);
                    break;
                } else if (cmd.length() == 0) {
                    continue;
                }

                try {
                    if (cmd.startsWith("scp")) {
                        channel.getSession().getFactoryManager().getCommandFactory().createCommand(channel, cmd)
                            .start(channel, env);
                    } else {
                        String[] as = Utils.parseArgs(cmd);
                        new MosCommandLine(this, out, err).execute(as);
                    }
                } catch (Exception e) {
                    Utils.printlnErrorMsg(err, e.getMessage());
                }
            }
        } catch (IOException e) {
            Utils.printlnError(out, e);
            callback.onExit(-1, e.getMessage());
            return;
        }
        callback.onExit(0);
    }

    @Override
    public void start(ChannelSession channel, Environment env) {
        this.channel = channel;
        this.env = env;

        // 检查连接数
        if (shellCount.getAndIncrement() >= MosSshServer.MAX_COUNT) {
            Utils.printlnErrorMsg(err, "shell连接过多！");
            throw new RuntimeException();
        }
        try {
            // 异步执行
            MosSshServer.EXECUTOR_SERVICE.execute(this);
        } catch (RejectedExecutionException e) {
            Utils.printlnErrorMsg(err, "shell连接过多!!");
            throw e;
        }
    }

    @Override
    public void destroy(ChannelSession channel) {
        // 停止循环
        shellCount.decrementAndGet();
    }

    @Override
    public InputStream getIn() {
        return in;
    }

    @Override
    public OutputStream getOut() {
        return out;
    }

    @Override
    public OutputStream getErr() {
        return err;
    }
}
