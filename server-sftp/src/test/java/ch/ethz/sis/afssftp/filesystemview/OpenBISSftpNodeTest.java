package ch.ethz.sis.afssftp.filesystemview;

import junit.framework.TestCase;

import java.util.List;

public class OpenBISSftpNodeTest extends TestCase {
    public void testGetJoinedAfsFilePath() {
        OpenBISSftpNode afsNode1 = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.AFS_FILE)
                .build();
        assertEquals("/", afsNode1.getJoinedAfsFilePath());

        OpenBISSftpNode afsNode2 = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1"))
                .build();
        assertEquals("/dir1", afsNode2.getJoinedAfsFilePath());

        OpenBISSftpNode afsNode3 = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1", "file2"))
                .build();
        assertEquals("/dir1/file2", afsNode3.getJoinedAfsFilePath());

        OpenBISSftpNode afsNode4 = OpenBISSftpNode.builder()
                .type(OpenBISSftpNode.Type.AFS_FILE)
                .afsFilePath(List.of("dir1", "dir2", "file3"))
                .build();
        assertEquals("/dir1/dir2/file3", afsNode4.getJoinedAfsFilePath());
    }
}