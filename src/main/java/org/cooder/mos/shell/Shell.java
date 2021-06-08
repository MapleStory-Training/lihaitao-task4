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
import org.cooder.mos.fs.FileDescriptor;
import org.cooder.mos.fs.IFileSystem;
import org.cooder.mos.shell.command.*;
import org.cooder.mos.ssh.MosSshServer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "", subcommands = {HelpCommand.class, Mkdir.class, ListCommand.class, Cat.class, Echo.class, Pwd.class,
    Remove.class, Touch.class})
public class Shell implements Runnable, org.apache.sshd.server.command.Command {
    public InputStream in;
    public OutputStream out;
    public OutputStream err;
    private FileDescriptor current;
    private ExitCallback callback;
    private static AtomicInteger shellCount = new AtomicInteger(0);

    public Shell(String path) {
        current = MosSystem.fileSystem().find(new String[] {path});
    }

    public String currentPath() {
        return current.isRoot() ? current.getName() : current.getPath();
    }

    /**
     * 引导提示
     * 
     * @return
     */
    private String prompt() {
        return String.format("root@mos-jason: %s $ ", currentPath());
    }

    @Command(name = "format", hidden = true)
    public void format() throws IOException {
        MosSystem.fileSystem().format();
        Utils.printMsgNotFlush(out, "disk format success.");
        Utils.writeNewLineNotFlush(out);
    }

    @Command(name = "cd", description = "切换工作目录")
    public void cd(@Parameters(paramLabel = "<path>") String path) {
        String[] paths = null;
        if (path.equals("/")) {
            current = MosSystem.fileSystem().find(new String[] {"/"});
            return;
        }

        if (path.equals("..")) {
            if (current.isRoot()) {
                return;
            }
            String[] ps = Utils.normalizePath(current.getParentPath());
            current = MosSystem.fileSystem().find(ps);
            return;
        }

        paths = absolutePath(path);
        FileDescriptor node = MosSystem.fileSystem().find(paths);
        if (node == null) {
            Utils.printlnErrorMsg(err, path + ": No such file or directory");
            return;
        }

        if (!node.isDir()) {
            Utils.printlnErrorMsg(err, path + ": Not a directory");
            return;
        }

        current = node;
    }

    public String[] absolutePath(String path) {
        if (path.equals(".")) {
            path = current.getPath();
        } else if (path.equals("..")) {
            if (current.isRoot()) {
                path = current.getPath();
            } else {
                path = current.getParentPath();
            }
        } else if (!path.startsWith("/")) {
            path = current.getPath() + IFileSystem.separator + path;
        }
        return Utils.normalizePath(path);
    }

    @Override
    public void run() {
        loop();
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

    // 循环任务
    private void loop() {
        try {
            LineReader reader = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.builder().streams(in, out).encoding(StandardCharsets.UTF_8).build()).build();
            while (true) {
                String cmd = reader.readLine(prompt());
                if ("exit".equals(cmd)) {
                    Utils.printMsgNotFlush(out, "bye~");
                    Utils.writeNewLineNotFlush(out);
                    break;
                } else if (cmd.length() == 0) {
                    continue;
                }

                try {
                    String[] as = Utils.parseArgs(cmd);
                    new MosCommandLine(this, out, err).execute(as);
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
}
