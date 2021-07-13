package org.cooder.mos.api;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.file.util.MockPath;
import org.cooder.mos.Utils;
import org.cooder.mos.fs.FileSystem;

/**
 * <信息描述>
 *
 * @author lihaitao on 2021/7/13
 */
public class MosDirectoryStream implements DirectoryStream {

    private Iterator<Path> iterator;

    public MosDirectoryStream(Path path) {
        iterator = new MosPathIterator(path);
    }

    @Override
    public Iterator iterator() {
        return iterator;
    }

    @Override
    public void close() {
        iterator = null;
    }

    static class MosPathIterator implements Iterator<Path> {
        private int index;
        private MosFile[] mosFiles;

        public MosPathIterator(Path path) {
            this.mosFiles = Utils.getFileByPath(path).listFiles();
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return mosFiles != null && index < mosFiles.length;
        }

        @Override
        public Path next() {
            if (mosFiles == null || index >= mosFiles.length) {
                throw new IndexOutOfBoundsException();
            }
            String path = FileSystem.separator + StringUtils.join(mosFiles[index++].getPath(), FileSystem.separator);
            return new MockPath(path);
        }
    }
}
