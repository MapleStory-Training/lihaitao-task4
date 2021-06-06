/*
 * This file is part of MOS <p> Copyright (c) 2021 by cooder.org <p> For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */
package org.cooder.mos.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.cooder.mos.MosSystem;
import org.cooder.mos.Utils;
import org.cooder.mos.fs.FileDescriptor;
import org.cooder.mos.fs.IFileSystem;
import org.cooder.mos.shell.command.*;
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
    private ExecutorService executorService;
    private boolean stop;
    private ExitCallback callback;

    public Shell() {}

    public String currentPath() {
        return current.isRoot() ? current.getName() : current.getPath();
    }

    // 循环任务
    private void loop() {
        try {
            LineReader reader = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.builder().streams(in, out).encoding(StandardCharsets.UTF_8).build()).build();
            while (!stop) {
                String cmd = reader.readLine(prompt());
                if ("exit".equals(cmd)) {
                    Utils.printMsgNotFlush(out, "bye~");
                    stop = true;
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
        } finally {
            callback.onExit(0);
        }
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

    @Override
    public void start(ChannelSession channel, Environment env) throws IOException {
        executorService = initExecutor();
        current = MosSystem.fileSystem().find(new String[] {"/"});
        executorService.submit(this);
    }

    /**
     * 初始化线程池
     *
     * @return
     */
    private ExecutorService initExecutor() {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), r -> {
            Thread thread = new Thread(r);
            thread.setName("shell-thread-%d");
            return thread;
        });

    }

    @Override
    public void destroy(ChannelSession channel) {
        // 停止循环
        this.stop = true;
        // 关闭线程池
        executorService.shutdown();
    }

    /*private void prompt() {
        try {
            // 先把之前的流刷干净
            out.flush();
            err.flush();
    
            // 换行输出新的提示符，输出到页面
            out.write("\r\n".getBytes());
            out.write(String.format("root@mos-jason: %s $ ", currentPath()).getBytes());
            out.flush();
        } catch (IOException e) {
            Utils.printMsgNotFlush(out, e.getMessage());
        }
    }
    
    private String readInput() throws IOException {
        Deque<Integer> deque = new ArrayDeque<>(1);
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        buffer.rewind();
        int byteData;
        int length = 0;
        while ((byteData = in.read()) > 0) {
            // 如果命令过长 提示
            if (buffer.position() > buffer.limit()) {
                out.write("command is too long".getBytes());
                return "";
            }
    
            // 如果是有效的回车
            if (deque.isEmpty() && byteData == (int)'\r') {
                // 如果之前什么都输入，直接返回
                if (buffer.position() == 0) {
                    return "";
                }
    
                // 将换行符也要输出
                buffer.rewind();
                byte[] data = new byte[length];
                buffer.get(data);
                out.write("\r\n".getBytes());
                return new String(data, StandardCharsets.UTF_8);
            }
    
            // 如果不处于字符串中, 检测是否有字符串的特殊字符
            if (deque.isEmpty()) {
                if (((int)'\"') == byteData || ((int)'\'') == byteData) {
                    deque.addLast(byteData);
                    out.write(byteData);
                    out.flush();
                    continue;
                }
            } else {
                // 如果处于字符串中，检测是不是字符串完成标志
                if (byteData == deque.getFirst()) {
                    deque.removeLast();
                    out.write(byteData);
                    out.flush();
                    continue;
                }
            }
    
            // 如果是处于字符串中的换行符，需要进行特殊处理，模拟mac os
            if (byteData == '\r') {
                newLine("dquote> ");
                buffer.put((byte)'\r');
                buffer.put((byte)'\n');
                length += 2;
                continue;
            }
            length++;
            buffer.put((byte)byteData);
            out.write(byteData);
            out.flush();
        }
        return "";
    }
    
    private void newLine(String prompt) throws IOException {
        out.write(("\r\n" + prompt).getBytes());
        out.flush();
    }*/
}
