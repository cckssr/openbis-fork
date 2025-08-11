/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.xls.export;

import ch.ethz.sis.openbis.generic.server.xls.export.helper.*;
import org.apache.poi.ss.usermodel.Workbook;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;

public class ExportHelperFactory
{

    private final XLSSampleTypeExportHelper sampleTypeExportHelper;

    private final XLSExperimentTypeExportHelper experimentTypeExportHelper;

    private final XLSDataSetTypeExportHelper dataSetTypeExportHelper;

    private final XLSVocabularyExportHelper vocabularyExportHelper;

    private final XLSSpaceExportHelper spaceExportHelper;

    private final XLSProjectExportHelper projectExportHelper;

    private final XLSExperimentExportHelper experimentExportHelper;

    private final XLSSampleExportHelper sampleExportHelper;

    private final XLSDataSetExportHelper dataSetExportHelper;

    private final XLSTypeGroupExportHelper typeGroupExportHelper;

    final Workbook wb;

    ExportHelperFactory(final Workbook wb)
    {
        this.wb = wb;

        sampleTypeExportHelper = new XLSSampleTypeExportHelper(wb);
        experimentTypeExportHelper = new XLSExperimentTypeExportHelper(wb);
        dataSetTypeExportHelper = new XLSDataSetTypeExportHelper(wb);
        vocabularyExportHelper = new XLSVocabularyExportHelper(wb);
        spaceExportHelper = new XLSSpaceExportHelper(wb);
        projectExportHelper = new XLSProjectExportHelper(wb);
        experimentExportHelper = new XLSExperimentExportHelper(wb);
        sampleExportHelper = new XLSSampleExportHelper(wb);
        dataSetExportHelper = new XLSDataSetExportHelper(wb);
        typeGroupExportHelper = new XLSTypeGroupExportHelper(wb);
    }

    IXLSExportHelper<? extends IEntityType> getHelper(final ExportableKind exportableKind)
    {
        switch (exportableKind)
        {
            case SAMPLE_TYPE:
            {
                return sampleTypeExportHelper;
            }
            case EXPERIMENT_TYPE:
            {
                return experimentTypeExportHelper;
            }
            case DATASET_TYPE:
            {
                return dataSetTypeExportHelper;
            }
            case VOCABULARY_TYPE:
            {
                return vocabularyExportHelper;
            }
            case SPACE:
            {
                return spaceExportHelper;
            }
            case PROJECT:
            {
                return projectExportHelper;
            }
            case EXPERIMENT:
            {
                return experimentExportHelper;
            }
            case SAMPLE:
            {
                return sampleExportHelper;
            }
            case DATASET:
            {
                return dataSetExportHelper;
            }
            case TYPE_GROUP:
            {
                return typeGroupExportHelper;
            }
            default:
            {
                throw new IllegalArgumentException(String.format("Not supported exportable kind %s.", exportableKind));
            }
        }
    }

}
