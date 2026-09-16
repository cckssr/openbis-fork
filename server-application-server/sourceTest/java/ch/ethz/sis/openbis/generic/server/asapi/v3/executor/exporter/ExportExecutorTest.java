/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.exporter;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ExportExecutorTest
{

    private static final String NEXT_ZIP_ENTRY_DATA_PROVIDER = "nextZipEntryData";

    private static final String ERRONEOUS_NEXT_ZIP_ENTRY_DATA_PROVIDER = "erroneousNextZipEntryData";

    private static final String FOLDER_NAME_DATA_PROVIDER = "folderNameData";

    private static final String ERRONEOUS_FOLDER_NAME_DATA_PROVIDER = "erroneousFolderNameData";

    private static final String HSL_TO_HEX_DATA_PROVIDER = "hslToHexData";

    private static final String SPACE_CODE = "TEST_SPACE";

    private static final String PROJECT_CODE = "TEST_PROJECT";

    private static final String EXPERIMENT_CODE = "TEST_EXPERIMENT";

    private static final String EXPERIMENT_NAME = "Test experiment name";

    private static final String SAMPLE_CODE = "TEST_SAMPLE";

    private static final String SAMPLE_NAME = "Test sample name";

    private static final String EXTENSION = ".pdf";

    private static final String DATA_SET_CODE = "TEST_DATA_SET";

    public static final String UUID_SUFFIX = "#123e4567-e89b-12d3-a456-426614174000";

    private static final Object[][] NEXT_ZIP_ENTRY_DATA = {
            {
                    null, null, null, null, null, null, null, null, null, "/"
            },
            {
                    SPACE_CODE, null, null, null, null, null, null, null, null,
                    String.format("%s/", SPACE_CODE)
            },
            {
                    SPACE_CODE, null, null, null, null, null, null, null, EXTENSION,
                    String.format("%s/%s%s", SPACE_CODE, SPACE_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, null, null, null, null, SAMPLE_CODE, null, null, null,
                    String.format("%s/%s/", SPACE_CODE, SAMPLE_CODE)
            },
            {
                    SPACE_CODE, null, null, null, null, SAMPLE_CODE, null, null, EXTENSION,
                    String.format("%s/%s/%s%s", SPACE_CODE, SAMPLE_CODE, SAMPLE_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, null, null, null, null, SAMPLE_CODE, SAMPLE_NAME, null, null,
                    String.format("%s/%s (%s)/", SPACE_CODE, SAMPLE_NAME, SAMPLE_CODE)
            },
            {
                    SPACE_CODE, null, null, null, null, SAMPLE_CODE, SAMPLE_NAME, null, EXTENSION,
                    String.format("%s/%s (%s)/%s (%s)%s", SPACE_CODE, SAMPLE_NAME, SAMPLE_CODE, SAMPLE_NAME, SAMPLE_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, null, null, null, null, SAMPLE_CODE, null, DATA_SET_CODE, null,
                    String.format("%s/%s/%s/", SPACE_CODE, SAMPLE_CODE, DATA_SET_CODE)
            },
            {
                    SPACE_CODE, null, null, null, null, SAMPLE_CODE, null, DATA_SET_CODE, EXTENSION,
                    String.format("%s/%s/%s%s", SPACE_CODE, SAMPLE_CODE, DATA_SET_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, null, null, null, null, SAMPLE_CODE, SAMPLE_NAME, DATA_SET_CODE, null,
                    String.format("%s/%s (%s)/%s/", SPACE_CODE, SAMPLE_NAME, SAMPLE_CODE, DATA_SET_CODE)
            },
            {
                    SPACE_CODE, null, null, null, null, SAMPLE_CODE, SAMPLE_NAME, DATA_SET_CODE, EXTENSION,
                    String.format("%s/%s (%s)/%s%s", SPACE_CODE, SAMPLE_NAME, SAMPLE_CODE, DATA_SET_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, null, null, null, null,
                    String.format("%s/%s/", SPACE_CODE, PROJECT_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, null, null, null, EXTENSION,
                    String.format("%s/%s/%s%s", SPACE_CODE, PROJECT_CODE, PROJECT_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, SAMPLE_CODE, SAMPLE_NAME, null, null,
                    String.format("%s/%s/%s (%s)/", SPACE_CODE, PROJECT_CODE, SAMPLE_NAME, SAMPLE_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, SAMPLE_CODE, SAMPLE_NAME, null, EXTENSION,
                    String.format("%s/%s/%s (%s)/%s (%s)%s", SPACE_CODE, PROJECT_CODE, SAMPLE_NAME, SAMPLE_CODE, SAMPLE_NAME, SAMPLE_CODE,
                            EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, SAMPLE_CODE, null, null, null,
                    String.format("%s/%s/%s/", SPACE_CODE, PROJECT_CODE, SAMPLE_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, SAMPLE_CODE, null, null, EXTENSION,
                    String.format("%s/%s/%s/%s%s", SPACE_CODE, PROJECT_CODE, SAMPLE_CODE, SAMPLE_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, SAMPLE_CODE, SAMPLE_NAME, DATA_SET_CODE, null,
                    String.format("%s/%s/%s (%s)/%s/", SPACE_CODE, PROJECT_CODE, SAMPLE_NAME, SAMPLE_CODE, DATA_SET_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, SAMPLE_CODE, SAMPLE_NAME, DATA_SET_CODE, EXTENSION,
                    String.format("%s/%s/%s (%s)/%s%s", SPACE_CODE, PROJECT_CODE, SAMPLE_NAME, SAMPLE_CODE, DATA_SET_CODE,
                            EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, SAMPLE_CODE, null, DATA_SET_CODE, null,
                    String.format("%s/%s/%s/%s/", SPACE_CODE, PROJECT_CODE, SAMPLE_CODE, DATA_SET_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, SAMPLE_CODE, null, DATA_SET_CODE, EXTENSION,
                    String.format("%s/%s/%s/%s%s", SPACE_CODE, PROJECT_CODE, SAMPLE_CODE, DATA_SET_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, null, null, null, null, null, null,
                    String.format("%s/%s/%s/", SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, null, null, null, null, null, EXTENSION,
                    String.format("%s/%s/%s/%s%s", SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, null, null, null, null,
                    String.format("%s/%s/%s (%s)/", SPACE_CODE, PROJECT_CODE, EXPERIMENT_NAME, EXPERIMENT_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, null, null, null, EXTENSION,
                    String.format("%s/%s/%s (%s)/%s (%s)%s", SPACE_CODE, PROJECT_CODE, EXPERIMENT_NAME, EXPERIMENT_CODE,
                            EXPERIMENT_NAME, EXPERIMENT_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, null, null, DATA_SET_CODE, null,
                    String.format("%s/%s/%s (%s)/%s/", SPACE_CODE, PROJECT_CODE, EXPERIMENT_NAME, EXPERIMENT_CODE, DATA_SET_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, null, null, DATA_SET_CODE, EXTENSION,
                    String.format("%s/%s/%s (%s)/%s%s", SPACE_CODE, PROJECT_CODE, EXPERIMENT_NAME, EXPERIMENT_CODE, DATA_SET_CODE,
                            EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, SAMPLE_CODE, SAMPLE_NAME, null, null,
                    String.format("%s/%s/%s (%s)/%s (%s)/", SPACE_CODE, PROJECT_CODE, EXPERIMENT_NAME, EXPERIMENT_CODE, SAMPLE_NAME,
                            SAMPLE_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, SAMPLE_CODE, SAMPLE_NAME, null, EXTENSION,
                    String.format("%s/%s/%s (%s)/%s (%s)/%s (%s)%s", SPACE_CODE, PROJECT_CODE, EXPERIMENT_NAME, EXPERIMENT_CODE,
                            SAMPLE_NAME, SAMPLE_CODE, SAMPLE_NAME, SAMPLE_CODE, EXTENSION)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, SAMPLE_CODE, SAMPLE_NAME, DATA_SET_CODE, null,
                    String.format("%s/%s/%s (%s)/%s (%s)/%s/", SPACE_CODE, PROJECT_CODE, EXPERIMENT_NAME, EXPERIMENT_CODE,
                            SAMPLE_NAME, SAMPLE_CODE, DATA_SET_CODE)
            },
            {
                    SPACE_CODE, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, SAMPLE_CODE, SAMPLE_NAME, DATA_SET_CODE, EXTENSION,
                    String.format("%s/%s/%s (%s)/%s (%s)/%s%s", SPACE_CODE, PROJECT_CODE, EXPERIMENT_NAME, EXPERIMENT_CODE,
                            SAMPLE_NAME, SAMPLE_CODE, DATA_SET_CODE, EXTENSION)
            },
            {
                    null, null, null, null, null, SAMPLE_CODE, null, null, null,
                    String.format("%s/", SAMPLE_CODE)
            },
            {
                    null, null, null, null, null, SAMPLE_CODE, SAMPLE_NAME, null, null,
                    String.format("%s (%s)/", SAMPLE_NAME, SAMPLE_CODE)
            },
            {
                    null, null, null, null, null, SAMPLE_CODE, null, null, EXTENSION,
                    String.format("%s/%s%s", SAMPLE_CODE, SAMPLE_CODE, EXTENSION)
            },
            {
                    null, null, null, null, null, SAMPLE_CODE, SAMPLE_NAME, null, EXTENSION,
                    String.format("%s (%s)/%s (%s)%s", SAMPLE_NAME, SAMPLE_CODE, SAMPLE_NAME, SAMPLE_CODE, EXTENSION)
            },
    };

    private static final Object[][] ERRONEOUS_NEXT_ZIP_ENTRY_DATA = {
            {
                    null, PROJECT_CODE, null, null, null, null, null, null, null
            },
            {
                    null, null, EXPERIMENT_CODE, null, null, null, null, null, null
            },
            {
                    null, null, null, null, null, null, null, DATA_SET_CODE, null
            },
            {
                    null, null, null, null, null, null, null, null, EXTENSION
            },
            {
                    null, PROJECT_CODE, EXPERIMENT_CODE, null, null, SAMPLE_CODE, null, DATA_SET_CODE, null
            },
            {
                    null, PROJECT_CODE, EXPERIMENT_CODE, EXPERIMENT_NAME, null, SAMPLE_CODE, SAMPLE_NAME, DATA_SET_CODE, EXTENSION
            },
            {
                    SPACE_CODE, null, EXPERIMENT_CODE, EXPERIMENT_NAME, null, null, null, null, EXTENSION
            },
            {
                    SPACE_CODE, null, null, null, null, null, null, DATA_SET_CODE, EXTENSION
            },
            {
                    SPACE_CODE, null, null, null, null, null, null, DATA_SET_CODE, null
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, null, null, DATA_SET_CODE, EXTENSION
            },
            {
                    SPACE_CODE, PROJECT_CODE, null, null, null, null, null, DATA_SET_CODE, null
            },
    };

    private static final Object[][] FOLDER_NAME_DATA = {
            {
                    'O', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", null, "OBJ1", "ANALYZED_DATA", null,
                    "O+DEFAULT_LAB_NOTEBOOK+DEFAULT_PROJECT+OBJ1+ANALYZED_DATA" + UUID_SUFFIX
            },
            {
                    'O', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", null, "OBJ1", "ANALYZED_DATA", "file",
                    "O+DEFAULT_LAB_NOTEBOOK+DEFAULT_PROJECT+OBJ1+ANALYZED_DATA"  + UUID_SUFFIX + "/file"
            },
            {
                    'O', "DEFAULT_LAB_NOTEBOOK", null, null, "OBJ1", "ANALYZED_DATA", "file",
                    "O+DEFAULT_LAB_NOTEBOOK+OBJ1+ANALYZED_DATA"  + UUID_SUFFIX + "/file"
            },
            {
                    'O', null, null, null, "OBJ1", "ANALYZED_DATA", "file",
                    "O+OBJ1+ANALYZED_DATA" + UUID_SUFFIX + "/file"
            },
            {
                    'E', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", null, "EXP1", "ANALYZED_DATA", "file",
                    "E+DEFAULT_LAB_NOTEBOOK+DEFAULT_PROJECT+EXP1+ANALYZED_DATA" + UUID_SUFFIX + "/file"
            },
            {
                    'O', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", "OBJ_CONTAINER", "OBJ1", "ANALYZED_DATA",
                    "file", "O+DEFAULT_LAB_NOTEBOOK+DEFAULT_PROJECT+OBJ_CONTAINER*OBJ1+ANALYZED_DATA" + UUID_SUFFIX + "/file"
            },
    };

    private static final Object[][] ERRONEOUS_FOLDER_NAME_DATA = {
            {
                    'P', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", null, "OBJ1", "ANALYZED_DATA", "file"
            },
            {
                    'O', null, "DEFAULT_PROJECT", null, "OBJ1", "ANALYZED_DATA", "file"
            },
            {
                    'O', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", null, null, "ANALYZED_DATA", "file"
            },
            {
                    'O', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", "OBJ_CONTAINER", null, "ANALYZED_DATA", "file"
            },
            {
                    'O', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", null, "OBJ1", null, "file"
            },
            {
                    // Experiments cannot have containers
                    'E', "DEFAULT_LAB_NOTEBOOK", "DEFAULT_PROJECT", "EXP_CONTAINER", "EXP1", "ANALYZED_DATA", "file"
            },
            {
                    // Experiments cannot be on the space level
                    'E', "DEFAULT_LAB_NOTEBOOK", null, null, "EXP1", "ANALYZED_DATA", "file"
            },
            {
                    // Experiments cannot be shared (w/o space)
                    'E', null, null, null, "EXP1", "ANALYZED_DATA", "file"
            },
    };

    private static final Object[][] HSL_TO_HEX_DATA = {
            {
                    // Empty document
                    "", "color", ""
            },
            {
                    // No colors
                    "<p>No colors here</p>", "color", "<p>No colors here</p>"
            },
            {
                    // The whole document is a single declaration
                    "color:hsl(0, 0%, 0%);", "color", "color: #000000;"
            },
            {
                    // Several declarations surrounded by text
                    "<span style=\"color:hsl(120, 100%, 50%);\">a</span><span style=\"color:hsl(240, 100%, 50%);\">b</span>", "color",
                    "<span style=\"color: #008000;\">a</span><span style=\"color: #000080;\">b</span>"
            },
            {
                    // Adjacent declarations
                    "color:hsl(0,0%,0%);color:hsl(30,40%,50%);", "color", "color: #000000;color: #80664D;"
            },
            {
                    // Characters outside of Latin-1 around the declaration
                    "\u00FC\uD83D\uDE00<b style=\"color:hsl(0, 75%, 60%);\">\u00FC\uD83D\uDE00</b>\u00FC\uD83D\uDE00", "color",
                    "\u00FC\uD83D\uDE00<b style=\"color: #992626;\">\u00FC\uD83D\uDE00</b>\u00FC\uD83D\uDE00"
            },
            {
                    // The replacements (23 characters) are shorter than the first two declarations (26 characters) and longer than the last two
                    // (19 characters): the third one fits into the space freed before it, the fourth one does not and shifts the rest to the right
                    "<p style=\"color:hsl(120, 100%, 50%);\">1</p><p style=\"color:hsl(240, 100%, 50%);\">2</p>"
                            + "<p style=\"color:hsl(0,0%,0%);\">3</p><p style=\"color:hsl(0,0%,0%);\">4</p>end",
                    "outline-color",
                    "<p style=\"outline-color: #008000;\">1</p><p style=\"outline-color: #000080;\">2</p>"
                            + "<p style=\"outline-color: #000000;\">3</p><p style=\"outline-color: #000000;\">4</p>end"
            },
    };

    @DataProvider
    private Object[][] nextZipEntryData()
    {
        return NEXT_ZIP_ENTRY_DATA;
    }

    @DataProvider
    private Object[][] erroneousNextZipEntryData()
    {
        return ERRONEOUS_NEXT_ZIP_ENTRY_DATA;
    }

    @DataProvider
    private Object[][] folderNameData()
    {
        return FOLDER_NAME_DATA;
    }

    @DataProvider
    private Object[][] erroneousFolderNameData()
    {
        return ERRONEOUS_FOLDER_NAME_DATA;
    }

    @DataProvider
    private Object[][] hslToHexData()
    {
        return HSL_TO_HEX_DATA;
    }

    @Test(dataProvider = NEXT_ZIP_ENTRY_DATA_PROVIDER)
    public void testGetNextDirectoryName(final String spaceCode, final String projectCode, final String experimentCode, final String experimentName,
             final String containerCode, final String sampleCode, final String sampleName, final String dataSetCode, final String extension,
            final String expectedResult)
    {
        assertEquals(ExportExecutor.getNextDocDirectoryName(spaceCode, projectCode, experimentCode, experimentName, containerCode, sampleCode,
                sampleName, dataSetCode, extension), expectedResult);
    }

    @Test(dataProvider = ERRONEOUS_NEXT_ZIP_ENTRY_DATA_PROVIDER, expectedExceptions = IllegalArgumentException.class)
    public void testGetNextDirectoryNameError(final String spaceCode, final String projectCode, final String experimentCode,
            final String experimentName, final String containerCode, final String sampleCode, final String sampleName, final String dataSetCode,
            final String extension)
    {
        ExportExecutor.getNextDocDirectoryName(spaceCode, projectCode, experimentCode, experimentName, containerCode, sampleCode, sampleName, dataSetCode,
                extension);
    }

    @Test(dataProvider = FOLDER_NAME_DATA_PROVIDER)
    public void testGetDataDirectoryName(final char prefix, final String spaceCode, final String projectCode,
            final String containerCode, final String entityCode, final String dataSetTypeCode,
            final String fileName, final String expectedResult)
    {
        assertEquals(ExportExecutor.getDataDirectoryName(prefix, spaceCode, projectCode, containerCode, entityCode, dataSetTypeCode,
                UUID_SUFFIX, fileName), expectedResult);
    }

    @Test(dataProvider = ERRONEOUS_FOLDER_NAME_DATA_PROVIDER, expectedExceptions = IllegalArgumentException.class)
    public void testGetDataDirectoryNameError(final char prefix, final String spaceCode, final String projectCode,
            final String containerCode, final String entityCode, final String dataSetTypeCode,
            final String fileName)
    {
        ExportExecutor.getDataDirectoryName(prefix, spaceCode, projectCode, containerCode, entityCode, dataSetTypeCode,
                UUID_SUFFIX, fileName);
    }

    @Test()
    public void testEscapeUnsafeCharacters()
    {
        final String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 $!#%'()+,-.;=@[]^_{}~\\/:*?\"<>|`";
        final String expectedOutput = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 $!#%'()+,-.;=@[]^_{}~__________";
        assertEquals(ExportExecutor.escapeUnsafeCharacters(input), expectedOutput);
    }

    @Test(dataProvider = HSL_TO_HEX_DATA_PROVIDER)
    public void testReplaceHSLToHex(final String html, final String cssProperty, final String expectedResult)
    {
        final StringBuilder builder = new StringBuilder(html);
        ExportPDFUtils.replaceHSLToHex(builder, cssProperty, ExportPDFUtils.HSL_COLOR_PATTERN);
        assertEquals(builder.toString(), expectedResult);
    }

}