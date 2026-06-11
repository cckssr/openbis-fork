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
package ch.ethz.sis.openbis.generic.server.xls.importer.utils;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.IProjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.IdentifierVariable;
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider;

import java.util.UUID;

public class ImportUtils
{
    private static final boolean isProjectSamplesEnabled;

    static {
        isProjectSamplesEnabled = CommonServiceProvider.getCommonServer().isProjectSamplesEnabled(null);
    }

    public static boolean isTrue(String property)
    {
        return property != null && !property.trim().isEmpty() && Boolean.parseBoolean(property);
    }

    public static ISampleId buildSampleIdentifier(String id)
    {
        if (id == null || id.isEmpty())
        {
            return null;
        }

        if (id.startsWith(PropertyTypeSearcher.VARIABLE_PREFIX)) { // Variable
            return new IdentifierVariable(id);
        } else if (id.startsWith("/")) { // Identifier
            if (isProjectSamplesEnabled == false) // If a project code is found => remove it
            {
                String[] identifierParts = id.split("/");
                if (identifierParts.length == 4) { // If is a project sample
                    String spaceCode = identifierParts[1];
                    String projectCode = identifierParts[2];
                    String sampleCode = identifierParts[3];
                    id = "/" + spaceCode + "/" + sampleCode;
                }
            }
            return new SampleIdentifier(id);
        } else {
            return new SamplePermId(id);
        }
    }

    public static ISampleId buildSampleIdentifier(String code, String space, String project, String experiment)
    {
        if (code == null || code.isEmpty())
        {
            return null;
        }

        if (isProjectSamplesEnabled)
        {
            if (project != null && !project.trim().isEmpty())
            {
                project = project.split("/")[2];
            }

            if (experiment != null && !experiment.trim().isEmpty())
            {
                project = experiment.split("/")[2];
            }
        } else { // If isProjectSamplesEnabled disabled => remove it
            project = null;
        }

        return new SampleIdentifier(space, project, null, code);
    }

    public static ISampleId buildSampleIdentifier(String variable, SampleCreation sampleCreation)
    {
        if (sampleCreation.getCode() == null)
        {
            if (variable != null)
            {
                return new IdentifierVariable(variable);
            } else
            {
                return new IdentifierVariable(UUID.randomUUID().toString());
            }
        } else
        {
            String space = null;
            ISpaceId spaceId = sampleCreation.getSpaceId();
            if (spaceId != null)
            {
                space = ((SpacePermId) spaceId).getPermId();
            }
            String project = null;
            if (isProjectSamplesEnabled)
            {
                IProjectId projectId = sampleCreation.getProjectId();
                IExperimentId experimentId = sampleCreation.getExperimentId();
                if (projectId != null)
                {
                    project = ((ProjectIdentifier) projectId).getIdentifier().split("/")[2];
                } else if(experimentId != null) {
                    project = ((ExperimentIdentifier) experimentId).getIdentifier().split("/")[2];
                }
            }
            return new SampleIdentifier(space, project, null, sampleCreation.getCode());
        }
    }

    private static String getScriptName(String ownerCode, String pathToScript)
    {
        if (pathToScript.contains("/"))
        {
            pathToScript = pathToScript.substring(pathToScript.lastIndexOf("/") + 1);
        }
        if (pathToScript.contains("."))
        {
            pathToScript = pathToScript.substring(0, pathToScript.lastIndexOf("."));
        }
        return ownerCode + "." + pathToScript;
    }

    public static PluginPermId getScriptId(String ownerCode, String script, Plugin existingPlugin)
    {
        if (script != null && !script.isEmpty())
        {
            if (script.equals("--DELETE--") || script.equals("__DELETE__"))
            {
                return null;
            } else {
                return new PluginPermId(ImportUtils.getScriptName(ownerCode, script));
            }
        } else if (existingPlugin != null){
            return existingPlugin.getPermId();
        }

        return null;
    }
}
