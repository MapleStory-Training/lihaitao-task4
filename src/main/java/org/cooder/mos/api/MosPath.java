package org.cooder.mos.api;

import java.nio.file.Path;

import org.apache.sshd.common.file.util.MockPath;
import org.cooder.mos.fs.FileSystem;

/**
 * <自己的路径实现>
 *
 * @author lihaitao on 2021/7/13
 */
public class MosPath extends MockPath {
    private final String path;

    public MosPath(String path) {
        super(path);
        this.path = path;
    }

    @Override
    public Path getParent() {
        return new MosPath(path.substring(0, path.lastIndexOf('/')));
    }

    @Override
    public Path getFileName() {
        if (path.equals("/")) {
            return new MosPath("/");
        }
        return new MosPath(path.substring(path.lastIndexOf('/') + 1));
    }

    @Override
    public Path resolve(String path) {
        if (path.equals("/")) {
            return new MosPath("/");
        }
        if (this.path.endsWith("/")) {
            return new MosPath(this.path + path);
        }
        return new MosPath(this.path + FileSystem.separator + path);
    }
}
