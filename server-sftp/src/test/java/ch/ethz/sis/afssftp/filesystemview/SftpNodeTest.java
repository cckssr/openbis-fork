package ch.ethz.sis.afssftp.filesystemview;

import junit.framework.TestCase;

import java.util.List;

public class SftpNodeTest extends TestCase {
    public void testGetJoinedAfsFilePath() {
        SftpNode afsNode1 = SftpNode.builder()
                .type(SftpNode.Type.AFS_FILE)
                .build();
        assertEquals("/", afsNode1.getJoinedAfsFilePath());

        SftpNode afsNode2 = SftpNode.builder()
                .type(SftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1"))
                .build();
        assertEquals("/dir1", afsNode2.getJoinedAfsFilePath());

        SftpNode afsNode3 = SftpNode.builder()
                .type(SftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1", "file2"))
                .build();
        assertEquals("/dir1/file2", afsNode3.getJoinedAfsFilePath());

        SftpNode afsNode4 = SftpNode.builder()
                .type(SftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1", "dir2", "file3"))
                .build();
        assertEquals("/dir1/dir2/file3", afsNode4.getJoinedAfsFilePath());
    }
}