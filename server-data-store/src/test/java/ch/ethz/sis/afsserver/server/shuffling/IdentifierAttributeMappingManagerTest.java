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
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.junit.Before;
import org.junit.Test;

import ch.ethz.sis.afsserver.server.common.SimpleDataSetInformationDTO;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.systemsx.cisd.base.tests.AbstractFileSystemTestCase;
import ch.systemsx.cisd.common.filesystem.FileUtilities;

/**
 * @author Franz-Josef Elmer
 */
public class IdentifierAttributeMappingManagerTest extends AbstractFileSystemTestCase
{
    private File as1;

    private File as2;

    private File as3;

    private DatasetDescription dataSetDescription;

    private DatasetDescription bigDataSetDescription;

    private DatasetDescription smallDataSetDescription;

    private SimpleDataSetInformationDTO dataSet;

    private SimpleDataSetInformationDTO bigDataSet;

    private SimpleDataSetInformationDTO smallDataSet;

    @Before
    @Override
    public void setUp() throws IOException
    {
        // override to use JUnit @Before annotation instead of @BeforeMethod from TestNG
        super.setUp();
    }

    @Before
    public void prepareTestFiles() throws Exception
    {
        as1 = new File(workingDirectory, "a-s1");
        as2 = new File(workingDirectory, "a-s2");
        as3 = new File(workingDirectory, "a-s3");

        dataSet = createDataSet("S1", "P1", "E1", 100L);
        bigDataSet = createDataSet("S1", "P1", "E1", 1001L);
        smallDataSet = createDataSet("S1", "P1", "E1", 10L);

        dataSetDescription = createDataSetDescription(dataSet);
        bigDataSetDescription = createDataSetDescription(bigDataSet);
        smallDataSetDescription = createDataSetDescription(smallDataSet);
    }

    private SimpleDataSetInformationDTO createDataSet(String spaceCode, String projectCode, String experimentCode, long dataSetSize)
    {
        SimpleDataSetInformationDTO dto = new SimpleDataSetInformationDTO();
        dto.setDataStoreCode("DSS");
        dto.setDataSetType("MY-TYPE");
        dto.setDataSetSize(dataSetSize);
        dto.setSpaceCode(spaceCode);
        dto.setProjectCode(projectCode);
        dto.setExperimentCode(experimentCode);
        return dto;
    }

    private DatasetDescription createDataSetDescription(SimpleDataSetInformationDTO dto)
    {
        DatasetDescription description = new DatasetDescription();
        description.setDataStoreCode(dto.getDataStoreCode());
        description.setDatasetTypeCode(dto.getDataSetType());
        description.setDataSetSize(dto.getDataSetSize());
        description.setSpaceCode(dto.getSpaceCode());
        description.setProjectCode(dto.getProjectCode());
        description.setExperimentCode(dto.getExperimentCode());
        description.setExperimentIdentifier(new ExperimentIdentifier(dto.getSpaceCode(), dto.getProjectCode(), dto.getExperimentCode()).toString());
        return description;
    }

    @Test
    public void testGetArchiveFolder()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1\t2\t" + as1 + "\n"
                + "/S1/P1\t2\t" + as1 + "\n"
                + "/S1\t3\t\n"
                + "/S2\t4\t" + as2);
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), true, null);

        List<File> folders = new ArrayList<File>(mappingManager.getAllFolders());

        Collections.sort(folders);
        assertEquals("[" + as1 + ", " + as2 + "]", folders.toString());
    }

    @Test
    public void testGetArchiveFolderForBigDataSetWhenTwoFoldersConfigured()
    {
        testGetArchiveFolderWhenTwoFoldersConfigured(bigDataSetDescription, as1);
    }

    @Test
    public void testGetArchiveFolderForSmallDataSetWhenTwoFoldersConfigured()
    {
        testGetArchiveFolderWhenTwoFoldersConfigured(smallDataSetDescription, as2);
    }

    private void testGetArchiveFolderWhenTwoFoldersConfigured(DatasetDescription description, File expectedArchiveFolder)
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S2\t4\t" + as3 + "\n"
                + "/S1/P1\t2\t" + as1 + ", " + as2);
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), true, 1000L);

        List<File> folders = new ArrayList<File>(mappingManager.getAllFolders());

        Collections.sort(folders);
        assertEquals("[" + as1 + ", " + as2 + ", " + as3 + "]", folders.toString());

        File archiveFolder = mappingManager.getArchiveFolder(description, null);

        assertEquals(expectedArchiveFolder.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testGetArchiveFolderForBigDataSetWhenOneFolderConfigured()
    {
        testGetArchiveFolderWhenOneFolderConfigured(bigDataSetDescription, as1);
    }

    @Test
    public void testGetArchiveFolderForSmallDataSetWhenOneFolderConfigured()
    {
        testGetArchiveFolderWhenOneFolderConfigured(smallDataSetDescription, as1);
    }

    private void testGetArchiveFolderWhenOneFolderConfigured(DatasetDescription description, File expectedArchiveFolder)
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S2\t4\t" + as3 + "\n"
                + "/S1/P1\t2\t" + as1);
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), true, 1000L);

        List<File> folders = new ArrayList<File>(mappingManager.getAllFolders());

        Collections.sort(folders);
        assertEquals("[" + as1 + ", " + as3 + "]", folders.toString());

        File archiveFolder = mappingManager.getArchiveFolder(description, null);

        assertEquals(expectedArchiveFolder.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testCreateWhenTwoFoldersConfiguredButNoSmallDataSetSizeLimitDefined()
    {
        try
        {
            File mappingFile = new File(workingDirectory, "mapping.txt");
            FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                    + "/S2\t4\t" + as3 + "\n"
                    + "/S1/P1\t2\t" + as1 + ", " + as2);
            new IdentifierAttributeMappingManager(mappingFile.getPath(), true, null);
        } catch (IllegalArgumentException e)
        {
            assertEquals("Small data set size limit cannot be null", e.getMessage());
        }
    }

    @Test
    public void testCreateWhenMoreThanTwoFoldersConfigured()
    {
        try
        {
            File mappingFile = new File(workingDirectory, "mapping.txt");
            FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                    + "/S2\t4\t" + as3 + "\n"
                    + "/S1/P1\t2\t" + as1 + "," + as2 + ", " + as3);
            new IdentifierAttributeMappingManager(mappingFile.getPath(), true, null);
        } catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().startsWith("Found 3 archive folders"));
        }
    }

    @Test
    public void testCreateWithInvalidIdentifierRegex()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/s1/p[\t2\t" + as1 + "," + as2);
        try
        {
            new IdentifierAttributeMappingManager(mappingFile.getPath(), true, null);
        } catch (PatternSyntaxException ex)
        {
            //AssertionUtil.assertStarts("Unclosed character class", ex.getMessage());
        }
    }

    @Test
    public void testGetArchiveFolderFromExperimentMapping()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1/E1\t2\t" + as1 + "\n"
                + "/S1/P1/E2\t2\t" + as2 + "\n"
                + "/S1/P1\t2\t" + as2 + "\n"
                + "\n"
                + " \n"
                + "\t \n"
                + "\t\t\n"
                + "\t\t\t \n"
                + "/S1\t2\t" + as2 + "\n");
        as1.mkdirs();
        as2.mkdirs();
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);

        File archiveFolder = mappingManager.getArchiveFolder(dataSetDescription, as2);

        assertEquals(as1.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testGetArchiveFolderFromProjectMapping()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1\t2\t" + as1 + "\n"
                + "/S1/P2\t2\t" + as2 + "\n"
                + "/S1\t2\t" + as2 + "\n");
        as1.mkdirs();
        as2.mkdirs();
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);
        File archiveFolder = mappingManager.getArchiveFolder(dataSetDescription, as2);

        assertEquals(as1.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testGetArchiveFolderFromSpaceMapping()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1\t2\t" + as1);
        as1.mkdirs();
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);

        File archiveFolder = mappingManager.getArchiveFolder(dataSetDescription, as2);

        assertEquals(as1.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testGetArchiveFolderFromSpaceMappingArchiveFolderDoesNotExist()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1\t2\t" + as1);

        try
        {
            new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex)
        {
            assertEquals("Archive folder '" + as1 + "' doesn't exists or is a file.", ex.getMessage());
        }
    }

    @Test
    public void testGetArchiveFolderFromSpaceMappingCreateArchiveFolder()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1\t2\t" + as1 + "\n"
                + "/S2\t4\t" + as2);
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), true, null);

        File archiveFolder = mappingManager.getArchiveFolder(dataSetDescription, as2);

        assertEquals(as1.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testGetArchiveFolderFromSpaceMappingMissingFolder()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1\t2\t\n"
                + "/S2\t4\t" + as2);
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), true, null);

        File archiveFolder = mappingManager.getArchiveFolder(dataSetDescription, as2);

        assertEquals(as2.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testGetArchiveFolderFromSpaceMappingMissingEntry()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S2\t4\t" + as2);
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), true, null);

        File archiveFolder = mappingManager.getArchiveFolder(dataSetDescription, as2);

        assertEquals(as2.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testGetArchiveFolderFromNonexistingMappingFile()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");

        try
        {
            new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException ex)
        {
            assertEquals("Mapping file '" + mappingFile + "' does not exist.", ex.getMessage());
        }
    }

    @Test
    public void testGetArchiveFolderFromUndefinedMappingFile()
    {
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(null, true, null);

        File archiveFolder = mappingManager.getArchiveFolder(dataSetDescription, as2);

        assertEquals(as2.getPath(), archiveFolder.getPath());
    }

    @Test
    public void testGetShareIdsOnExperimentLevel()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1/E1\t2, 1,3\t\n"
                + "/S1/P1\t1,3\t\n"
                + "/S1\t2,3\t\n"
                + "/S2\t4,5\t");
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);

        List<String> shareIds = mappingManager.getShareIds(dataSet);

        assertEquals("[2, 1, 3]", shareIds.toString());
    }

    @Test
    public void testGetShareIdsOnExperimentLevelWithRegex()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1/E.*\t2, 1,3\t\n"
                + "/S1/P1\t1,3\t\n"
                + "/S1\t2,3\t\n"
                + "/S2\t4,5\t");
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);

        List<String> shareIds = mappingManager.getShareIds(dataSet);

        assertEquals("[2, 1, 3]", shareIds.toString());
    }

    @Test
    public void testGetShareIdsOnProjectLevel()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1/E2\t2, 1,3\t\n"
                + "/S1/P1\t1,3\t\n"
                + "/S1\t2,3\t\n"
                + "/S2\t4,5\t");
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);

        List<String> shareIds = mappingManager.getShareIds(dataSet);

        assertEquals("[1, 3]", shareIds.toString());
    }

    @Test
    public void testGetShareIdsOnProjectLevelWithRegex()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1/E2\t2, 1,3\t\n"
                + "/S1/P.*\t1,3\t\n"
                + "/S1\t2,3\t\n"
                + "/S2\t4,5\t");
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);

        List<String> shareIds = mappingManager.getShareIds(dataSet);

        assertEquals("[1, 3]", shareIds.toString());
    }

    @Test
    public void testGetShareIdsOnSpaceLevel()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1/E2\t2, 1,3\t\n"
                + "/S1/P2\t1,3\t\n"
                + "/S1\t2,3\t\n"
                + "/S2\t4,5\t");
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);

        List<String> shareIds = mappingManager.getShareIds(dataSet);

        assertEquals("[2, 3]", shareIds.toString());
    }

    @Test
    public void testGetShareIdsOnSpaceLevelWithRegex()
    {
        File mappingFile = new File(workingDirectory, "mapping.txt");
        FileUtilities.writeToFile(mappingFile, "Identifier\tShare ID\tArchive Folder\n"
                + "/S1/P1/E2\t2, 1,3\t\n"
                + "/S1/P2\t1,3\t\n"
                + "/S.*\t2,3\t\n"
                + "/S2\t4,5\t");
        IdentifierAttributeMappingManager mappingManager = new IdentifierAttributeMappingManager(mappingFile.getPath(), false, null);

        List<String> shareIds = mappingManager.getShareIds(dataSet);

        assertEquals("[2, 3]", shareIds.toString());
    }

}
