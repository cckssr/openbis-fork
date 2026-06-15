package ch.ethz.sis.afssftp.filesystemview;

import lombok.Builder;
import lombok.Value;
import org.apache.sshd.sftp.server.DefaultGroupPrincipal;
import org.apache.sshd.sftp.server.DefaultUserPrincipal;

import java.nio.file.attribute.*;
import java.util.EnumSet;
import java.util.Set;

@Value
@Builder(toBuilder = true)
public class SftpFileAttributes implements PosixFileAttributes
{
    FileTime creationTime;
    FileTime modifiedTime;
    FileTime accessTime;

    boolean regularFile;
    boolean directory;

    long size;

    @Builder.Default
    Set<PosixFilePermission> permissions = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_EXECUTE
    );

    @Override
    public FileTime lastModifiedTime() {
        return modifiedTime;
    }

    @Override
    public FileTime lastAccessTime() {
        return accessTime;
    }

    @Override
    public FileTime creationTime() {
        return creationTime;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther()
    {
        return !(regularFile || directory);
    }

    @Override
    public long size() {
        return 0;
    }


    @Override
    public Object fileKey()
    {
        return null;
    }

    @Override
    public UserPrincipal owner()
    {
        return new DefaultUserPrincipal("openbis");
    }

    @Override
    public GroupPrincipal group()
    {
        return new DefaultGroupPrincipal("openbis");
    }

    @Override
    public Set<PosixFilePermission> permissions()
    {
        return permissions;
    }

}

