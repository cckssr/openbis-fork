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
package ch.eth.sis.rocrate.parser.helper;

import ch.eth.sis.rocrate.parser.IAttribute;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.util.List;
import java.util.Map;

public abstract class BasicImportHelper extends AbstractImportHelper
{

    public BasicImportHelper()
    {
    }

    protected boolean isNewVersion(Map<String, Integer> header, List<String> values)
    {
        return true;
    }

    protected static boolean isNewVersionWithInternalNamespace(Map<String, Integer> header,
            List<String> values, Map<String, Integer> versions, boolean isSystem, String type,
            IAttribute versionAttribute, IAttribute codeAttribute, IAttribute internalAttribute)
    {
        String version = getValueByColumnName(header, values, versionAttribute);
        String code = getValueByColumnName(header, values, codeAttribute);

        if (code == null)
        {
            throw new UserFailureException("Mandatory field is missing or empty: " + codeAttribute);
        }

        String internal = getValueByColumnName(header, values, internalAttribute);
        boolean isInternalNamespace = false;
        boolean canUpdate = (isInternalNamespace == false) || isSystem;

        if (canUpdate == false)
        {
            return false;
        } else if (canUpdate && (version == null || version.isEmpty()))
        {
            return true;
        } else
        {
            return false;
        }
    }

    protected void updateVersion(Map<String, Integer> header, List<String> values)
    {
        // do nothing
    }

    protected abstract boolean isObjectExist(Map<String, Integer> header, List<String> values);

    protected abstract void createObject(Map<String, Integer> header, List<String> values, int page,
            int line);

    protected abstract void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line);

    public void importBlock(List<List<String>> page, int pageIndex, int start, int end)
    {
        int lineIndex = start;

        try
        {
            Map<String, Integer> header = parseHeader(page.get(lineIndex), true);
            lineIndex++;

            while (lineIndex < end)
            {
                validateLine(header, page.get(lineIndex));
                if (isNewVersion(header, page.get(lineIndex)))
                {
                    if (!isObjectExist(header, page.get(lineIndex)))
                    {
                        createObject(header, page.get(lineIndex), pageIndex, lineIndex);
                        updateVersion(header, page.get(lineIndex));
                    }
                }

                lineIndex++;
            }

        } catch (Exception e)
        {
            UserFailureException userFailureException = new UserFailureException(
                    "sheet: " + (pageIndex + 1) + " line: " + (lineIndex + 1) + " message: " + e.getMessage());
            userFailureException.setStackTrace(e.getStackTrace());
            throw userFailureException;
        }
    }
}
