/*
 * Copyright ETH 2013 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.afsserver.server.shuffling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.sis.afsserver.server.common.SimpleDataSetInformationDTO;
import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.common.filesystem.FileUtilities;
import ch.systemsx.cisd.common.filesystem.HostAwareFile;
import ch.systemsx.cisd.common.filesystem.IFreeSpaceProvider;

/**
 * @author Franz-Josef Elmer
 */
public class MappingBasedShareFinderTest extends AbstractFileSystemTestCase
{
    private File mappingFile;

    private Properties properties;

    private SimpleDataSetInformationDTO dataSetInfo;

    @Before
    public void initializeTestData()
    {
        mappingFile = new File(workingDirectory, "mapping.tsv");
        properties = new Properties();

        SimpleDataSetInformationDTO dto = new SimpleDataSetInformationDTO();
        dto.setDataSetCode("DS1");
        dto.setDataStoreCode("DSS");
        dto.setDataSetType("MY-TYPE");
        dto.setSpaceCode("S1");
        dto.setProjectCode("P1");
        dto.setExperimentCode("E1");
        dto.setDataSetSize(10 * FileUtils.ONE_KB);

        dataSetInfo = dto;
    }

    @Test
    public void test()
    {
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1/E1\t1,4,2,3\t\n"
                + "/S1/P1\t1,3\t\n"
                + "/S1\t3\t\n"
                + "/S2\t4,5\t");
        properties.setProperty(MappingBasedShareFinder.MAPPING_FILE_KEY, mappingFile.toString());
        IShareFinder shareFinder = new MappingBasedShareFinder(properties);

        Share share = shareFinder.tryToFindShare(dataSetInfo, shares(9, 11, 12));

        assertEquals("2", share.getShareId());
    }

    private List<Share> shares(int... sizes)
    {
        List<Share> shares = new ArrayList<Share>();
        for (int i = 0; i < sizes.length; i++)
        {
            String shareId = Integer.toString(i + 1);
            final long size = sizes[i];
            shares.add(new Share(new File(workingDirectory, shareId), 10, new IFreeSpaceProvider()
            {
                @Override
                public long freeSpaceKb(HostAwareFile path) throws IOException
                {
                    return size;
                }
            }));
        }
        return shares;
    }

}
