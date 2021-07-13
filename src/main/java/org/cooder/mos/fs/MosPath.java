package org.cooder.mos.fs;

import java.nio.file.Path;

import org.apache.sshd.common.file.util.MockPath;

/**
 * <信息描述>
 *
 * @author lihaitao on 2021/7/13
 */
public class MosPath extends MockPath {
    private String parentPath;

    public MosPath(String path, String parentPath) {
        super(path);
        this.parentPath = parentPath;
    }

    @Override
    public Path getParent() {
        return new MosPath(parentPath, null);
    }
}
