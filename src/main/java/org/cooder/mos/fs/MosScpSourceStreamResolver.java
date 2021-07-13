package org.cooder.mos.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import org.apache.sshd.common.file.util.MockPath;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.scp.common.ScpFileOpener;
import org.apache.sshd.scp.common.ScpSourceStreamResolver;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;
import org.cooder.mos.Utils;
import org.cooder.mos.api.MosFile;

/**
 * <信息描述>
 *
 * @author lihaitao on 2021/6/26
 */
public class MosScpSourceStreamResolver implements ScpSourceStreamResolver {

    private Path path;
    private MosFile file;
    private Collection<PosixFilePermission> perms;
    private long size;
    private ScpTimestampCommandDetails time;
    private ScpFileOpener opener;

    public MosScpSourceStreamResolver(Path path, ScpFileOpener opener) {
        Path filePath = path;
        if (path.getParent() != null) {
            String parentPath = path.getParent().toString();
            if (parentPath.endsWith("/")) {
                filePath = new MockPath(parentPath + path.toString());
            } else {
                filePath = new MockPath(parentPath + FileSystem.separator + path.toString());
            }
        }
        this.path = filePath;
        this.file = Utils.getFileByPath(filePath);
        this.perms = EnumSet.noneOf(PosixFilePermission.class);
        this.opener = opener;

        this.size = file.length();
        this.time = new ScpTimestampCommandDetails(file.lastModified(), file.lastModified());
    }

    @Override
    public String getFileName() throws IOException {
        return file.getName();
    }

    @Override
    public Path getEventListenerFilePath() {
        return path;
    }

    @Override
    public Collection<PosixFilePermission> getPermissions() throws IOException {
        return perms;
    }

    @Override
    public ScpTimestampCommandDetails getTimestamp() throws IOException {
        return time;
    }

    @Override
    public long getSize() throws IOException {
        return size;
    }

    @Override
    public InputStream resolveSourceStream(Session session, long fileSize, Set<PosixFilePermission> permissions,
        OpenOption... options) throws IOException {
        return opener.openRead(session, getEventListenerFilePath(), fileSize, permissions, options);
    }
}
