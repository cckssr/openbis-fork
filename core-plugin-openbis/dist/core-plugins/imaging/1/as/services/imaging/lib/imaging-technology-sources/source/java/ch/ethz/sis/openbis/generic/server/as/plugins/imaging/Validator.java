/*
 *  Copyright ETH 2023 - 2024 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.server.as.plugins.imaging;

import ch.ethz.sis.openbis.generic.imagingapi.v3.dto.ImagingDataSetExportConfig;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.io.Serializable;
import java.util.Map;

class Validator
{
    private Validator() {}


    static void validateInputParams(Map<String, Object> params)
    {
        if (!params.containsKey("type"))
        {
            throw new UserFailureException("Missing type!");
        }
    }


    static void validateExportConfig(ImagingDataSetExportConfig exportConfig) {
        if(exportConfig == null) {
            throw new UserFailureException("Export config can not be empty!");
        }
        if(exportConfig.getInclude() == null || exportConfig.getInclude().isEmpty())
        {
            throw new UserFailureException("Include option of config can not be empty!");
        }
        if(exportConfig.getImageFormat() == null || exportConfig.getImageFormat().trim().isEmpty())
        {
            throw new UserFailureException("Image format of config can not be empty!");
        }
        if(exportConfig.getArchiveFormat() == null || exportConfig.getArchiveFormat().trim().isEmpty())
        {
            throw new UserFailureException("Archive format of config can not be empty!");
        }
        if(exportConfig.getResolution() == null || exportConfig.getResolution().trim().isEmpty())
        {
            throw new UserFailureException("resolution option of config can not be empty!");
        }
    }


}
