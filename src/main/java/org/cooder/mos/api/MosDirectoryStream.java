package org.cooder.mos.api;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import org.cooder.mos.Utils;
import org.cooder.mos.fs.MosPath;

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
        private Path path;

        public MosPathIterator(Path path) {
            Path filePath = Utils.getFilePath(path);
            this.mosFiles = Utils.getFileByPath(filePath).listFiles();
            this.path = filePath;
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
            String parentPath = path.toString();
            // String path = FileSystem.separator + StringUtils.join(mosFiles[index++].getPath(), FileSystem.separator);
            return new MosPath(mosFiles[index++].getName(), parentPath);
        }
    }
}
