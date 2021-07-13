package org.cooder.mos.fs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.apache.sshd.common.file.util.MockPath;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.scp.common.ScpFileOpener;
import org.apache.sshd.scp.common.ScpTargetStreamResolver;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;

/**
 * <信息描述>
 *
 * @author lihaitao on 2021/6/27
 */
public class MosScpTargetStreamResolver implements ScpTargetStreamResolver {

    protected final Path path;
    protected final ScpFileOpener opener;

    public MosScpTargetStreamResolver(Path path, ScpFileOpener opener) {
        this.path = path;
        this.opener = opener;
    }

    @Override
    public OutputStream resolveTargetStream(Session session, String name, long length, Set<PosixFilePermission> perms,
        OpenOption... options) throws IOException {
        String newPath = path.toString() + path.getFileSystem().getSeparator() + name;
        return opener.openWrite(session, new MockPath(newPath), length, perms, options);
    }

    @Override
    public Path getEventListenerFilePath() {
        return path;
    }

    @Override
    public void postProcessReceivedData(String name, boolean preserve, Set<PosixFilePermission> perms,
        ScpTimestampCommandDetails time) throws IOException {
        // todo - 结束了

    }
}
