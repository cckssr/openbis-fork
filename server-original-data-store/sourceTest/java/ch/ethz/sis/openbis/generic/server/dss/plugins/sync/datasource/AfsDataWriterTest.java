/*
 * Copyright ETH 2026 Zurich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.openbis.generic.server.dss.plugins.sync.datasource;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import ch.ethz.sis.afsapi.dto.File;

public class AfsDataWriterTest
{
    @Test
    public void testOmitsEmptySnapshotDirectories()
    {
        List<File> directories = List.of(
                directory("/Hello/.afs.snapshots"),
                directory("/Hello/.afs.snapshots/docker-desktop-amd64.deb"),
                directory("/Empty/EmptyInside"));
        List<File> files = List.of(file(
                "/.afs.trash/Hello/.afs.snapshots/docker-desktop-amd64.deb/2026_08_26_10_56_19_306"));

        List<File> emptyDirectories = AfsDataWriter.emptyDirectoriesAmong(directories, files);

        assertEquals(emptyDirectories.stream().map(File::getPath).toList(), List.of("/Empty/EmptyInside"));
    }

    private static File directory(String path)
    {
        return new File("owner", path, name(path), true, null, null);
    }

    private static File file(String path)
    {
        return new File("owner", path, name(path), false, 1L, null);
    }

    private static String name(String path)
    {
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
