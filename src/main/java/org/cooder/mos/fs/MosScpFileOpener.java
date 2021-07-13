package org.cooder.mos.fs;

import static org.cooder.mos.Utils.getFileByPath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.apache.sshd.common.session.Session;
import org.apache.sshd.scp.common.ScpFileOpener;
import org.apache.sshd.scp.common.ScpSourceStreamResolver;
import org.apache.sshd.scp.common.ScpTargetStreamResolver;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;
import org.cooder.mos.api.FileInputStream;
import org.cooder.mos.api.FileOutputStream;
import org.cooder.mos.api.MosDirectoryStream;
import org.cooder.mos.api.MosFile;

/**
 * <实现自己的scp file opener>
 *
 * @author lihaitao on 2021/6/26
 */
public class MosScpFileOpener implements ScpFileOpener {

    @Override
    public InputStream openRead(Session session, Path file, long size, Set<PosixFilePermission> permissions,
        OpenOption... options) throws IOException {
        MosFile mosFile = getFileByPath(file);
        return new FileInputStream(mosFile);
    }

    @Override
    public ScpSourceStreamResolver createScpSourceStreamResolver(Session session, Path path) throws IOException {
        return new MosScpSourceStreamResolver(path, this);
    }

    @Override
    public OutputStream openWrite(Session session, Path file, long size, Set<PosixFilePermission> permissions,
        OpenOption... options) throws IOException {
        MosFile mosFile = getFileByPath(file);
        return new FileOutputStream(mosFile, FileSystem.WRITE);
    }

    @Override
    public ScpTargetStreamResolver createScpTargetStreamResolver(Session session, Path path) throws IOException {
        return new MosScpTargetStreamResolver(path, this);
    }

    @Override
    public boolean sendAsRegularFile(Session session, Path path, LinkOption... options) throws IOException {
        MosFile mosFile = getFileByPath(path);
        return mosFile.exist() && !mosFile.isDir();
    }

    @Override
    public boolean sendAsDirectory(Session session, Path path, LinkOption... options) throws IOException {
        MosFile mosFile = getFileByPath(path);
        return mosFile.exist() && mosFile.isDir();
    }

    @Override
    public Path resolveOutgoingFilePath(Session session, Path localPath, LinkOption... options) throws IOException {
        MosFile mosFile = getFileByPath(localPath);
        if (!mosFile.exist()) {
            throw new IOException(localPath + ": no such file or directory");
        }

        return localPath;
    }

    @Override
    public Path resolveIncomingFilePath(Session session, Path localPath, String name, boolean preserve,
        Set<PosixFilePermission> permissions, ScpTimestampCommandDetails time) throws IOException {
        MosFile fileByPath = getFileByPath(localPath);
        boolean status = fileByPath.exist();

        Path file = null;
        if (status && fileByPath.isDir()) {
            String localName = name.replace('/', File.separatorChar);
            file = localPath.resolve(localName);
        } else if (!status) {
            Path parent = localPath.getParent();
            MosFile parentFile = getFileByPath(parent);
            if (parentFile.exist() && parentFile.isDir()) {
                file = localPath;
            }
        }

        if (file == null) {
            throw new IOException("Cannot write to " + localPath);
        }

        MosFile mosFile = getFileByPath(file);

        if (!(mosFile.exist() && mosFile.isDir())) {
            mosFile.mkdir();
        }

        /* 更新时间
        if (preserve) {
            updateFileProperties(file, permissions, time);
        }
        */
        return file;
    }

    @Override
    public Set<PosixFilePermission> getLocalFilePermissions(Session session, Path path, LinkOption... options)
        throws IOException {
        return new HashSet<>();
    }

    @Override
    public DirectoryStream<Path> getLocalFolderChildren(Session session, Path path) throws IOException {
        return new MosDirectoryStream(path);
    }
}
