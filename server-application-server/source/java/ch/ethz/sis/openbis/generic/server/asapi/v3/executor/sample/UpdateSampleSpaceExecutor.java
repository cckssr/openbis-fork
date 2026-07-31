/*
 * Copyright ETH 2014 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic.server.asapi.v3.executor.sample;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.update.FieldUpdateValue;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.IProjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.IOperationContext;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.AbstractUpdateEntityToOneRelationExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.project.IMapProjectByIdExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.space.IMapSpaceByIdExecutor;
import ch.ethz.sis.openbis.generic.server.asapi.v3.helper.common.batch.MapBatch;
import ch.systemsx.cisd.openbis.generic.shared.dto.ProjectPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SamplePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.SpacePE;

/**
 * @author pkupczyk
 */
@Component
public class UpdateSampleSpaceExecutor extends AbstractUpdateEntityToOneRelationExecutor<SampleUpdate, SamplePE, ISpaceId, SpacePE> implements
        IUpdateSampleSpaceExecutor
{

    private static final String PROJECTS_MAP_ATTRIBUTE = UpdateSampleSpaceExecutor.class.getName() + "-projects-map";

    @Autowired
    private IMapSpaceByIdExecutor mapSpaceByIdExecutor;

    @Autowired
    private IMapProjectByIdExecutor mapProjectByIdExecutor;

    @Override
    protected String getRelationName()
    {
        return "sample-space";
    }

    @Override
    protected ISpaceId getRelatedId(SpacePE related)
    {
        return new SpacePermId(related.getCode());
    }

    @Override
    protected SpacePE getCurrentlyRelated(SamplePE entity)
    {
        return entity.getSpace();
    }

    @Override
    protected FieldUpdateValue<ISpaceId> getRelatedUpdate(SampleUpdate update)
    {
        return update.getSpaceId();
    }

    @Override
    protected Map<ISpaceId, SpacePE> map(IOperationContext context, List<ISpaceId> relatedIds)
    {
        return mapSpaceByIdExecutor.map(context, relatedIds);
    }

    @Override public void update(final IOperationContext context, final MapBatch<SampleUpdate, SamplePE> batch)
    {
        try
        {
            // load all necessary projects at once instead of loading them separately for each updated sample
            final Map<IProjectId, ProjectPE> projectsMap = loadProjects(context, batch);
            context.setAttribute(PROJECTS_MAP_ATTRIBUTE, projectsMap);

            super.update(context, batch);
        } finally
        {
            context.setAttribute(PROJECTS_MAP_ATTRIBUTE, null);
        }
    }

    private Map<IProjectId, ProjectPE> loadProjects(final IOperationContext context, final MapBatch<SampleUpdate, SamplePE> batch)
    {
        Set<IProjectId> projectIds = new HashSet<>();

        for (SampleUpdate update : batch.getObjects().keySet())
        {
            if (update.getProjectId() != null && update.getProjectId().isModified() && update.getProjectId().getValue() != null)
            {
                projectIds.add(update.getProjectId().getValue());
            }
        }

        return mapProjectByIdExecutor.map(context, projectIds);
    }

    @Override
    protected void check(IOperationContext context, SamplePE entity, ISpaceId relatedId, SpacePE related)
    {
        // checks are done in the update method
    }

    @Override protected void update(final IOperationContext context, final SamplePE entity, final SampleUpdate update, final SpacePE related)
    {
        Map<IProjectId, ProjectPE> projectsMap = (Map<IProjectId, ProjectPE>) context.getAttribute(PROJECTS_MAP_ATTRIBUTE);

        if (related == null)
        {
            relationshipService.shareSample(context.getSession(), entity);
        } else
        {
            if (entity.getSpace() == null)
            {
                relationshipService.unshareSample(context.getSession(), entity, related);
            } else
            {
                if (update.getProjectId() != null && update.getProjectId().isModified() && update.getProjectId().getValue() != null)
                {
                    ProjectPE projectPE = projectsMap.get(update.getProjectId().getValue());

                    if (projectPE != null && related.equals(projectPE.getSpace()))
                    {
                        relationshipService.assignSampleToProject(context.getSession(), entity, projectPE);
                        entity.setSpace(related);
                    } else
                    {
                        relationshipService.assignSampleToSpace(context.getSession(), entity, related);
                    }
                } else
                {
                    relationshipService.assignSampleToSpace(context.getSession(), entity, related);
                }
            }
        }
    }
}
