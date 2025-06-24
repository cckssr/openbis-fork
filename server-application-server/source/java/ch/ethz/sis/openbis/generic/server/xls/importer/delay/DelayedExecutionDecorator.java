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
package ch.ethz.sis.openbis.generic.server.xls.importer.delay;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IEntityType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.interfaces.IPropertiesHolder;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.update.IObjectUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.create.PluginCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.fetchoptions.PluginFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.IPluginId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.update.PluginUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.IProjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.update.ProjectUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.DataType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.IPropertyAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.IPropertyTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.update.PropertyTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.update.SpaceUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.create.VocabularyCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.create.VocabularyTermCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.IVocabularyId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.IVocabularyTermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.update.VocabularyTermUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.update.VocabularyUpdate;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

public class DelayedExecutionDecorator
{
    private String sessionToken;

    private IApplicationServerApi v3;

    Set<IObjectId> ids;

    Map<IdentifierVariable, IObjectId> resolvedVariables;

    Map<IObjectId, List<DelayedExecution>> delayedExecutions;

    Map<String, PropertyType> propertyTypeCache;

    public boolean isSystem()
    {
        return sessionToken.startsWith("system");
    }

    public DelayedExecutionDecorator(String sessionToken, IApplicationServerApi v3)
    {
        this.sessionToken = sessionToken;
        this.v3 = v3;
        this.ids = new LinkedHashSet<>();
        this.resolvedVariables = new HashMap<>();
        this.delayedExecutions =
                new LinkedHashMap<>(); // The only reason this is a linked Hash Map is so the list of error messages is on the same order as the delayed executions are added.
        this.propertyTypeCache = new HashMap<>();
    }

    private void addIdsAndExecuteDelayed(IObjectId id, ImportTypes importTypes, String variable)
    {
        switch (importTypes)
        {
            case PROJECT:
                id = getProject((IProjectId) id, new ProjectFetchOptions()).getIdentifier();
                break;
            case EXPERIMENT:
                id = getExperiment((IExperimentId) id, new ExperimentFetchOptions()).getIdentifier();
                break;
            case SAMPLE:
                id = getSample((ISampleId) id, new SampleFetchOptions()).getIdentifier();
                break;
        }

        this.ids.add(id);

        if (variable != null)
        {
            IdentifierVariable identifierVariable = new IdentifierVariable(variable);
            this.resolvedVariables.put(identifierVariable, id);
            resolveDependencies(identifierVariable);
        }

        resolveDependencies(id);
    }

    private void addDelayedExecution(DelayedExecution delayedExecution)
    {
        for (IObjectId id : delayedExecution.getDependencies())
        {
            List<DelayedExecution> delayedExecutionsForId = delayedExecutions.get(id);
            if (delayedExecutionsForId == null)
            {
                delayedExecutionsForId = new ArrayList<>();
                delayedExecutions.put(id, delayedExecutionsForId);
            }
            delayedExecutionsForId.add(delayedExecution);
        }
    }

    public void hasFinished()
    {
        if (!delayedExecutions.isEmpty())
        {
            List<String> errors = new ArrayList<>();
            Set<DelayedExecution> delayedExecutionsAsList =
                    new LinkedHashSet<>(); // The only reason this is a linked Hash Set is so the list of error messages is on the same order as the delayed executions are added.
            for (List<DelayedExecution> valueList : delayedExecutions.values())
            {
                delayedExecutionsAsList.addAll(valueList);
            }
            for (DelayedExecution delayedExecution : delayedExecutionsAsList)
            {
                errors.add("sheet: " + (delayedExecution.getPage() + 1) + " line: " + (delayedExecution.getLine() + 1) + " message: Entity "
                        + delayedExecution.getDependencies() + " could not be found. Either you forgot to register it or mistyped the identifier.");
            }
            throw new UserFailureException(errors.toString());
        }
    }

    private void resolveDependencies(IObjectId id)
    {
        List<DelayedExecution> executableDelays = new ArrayList<>();

        if (delayedExecutions.containsKey(id))
        {
            List<DelayedExecution> delayedExecutionsForId = delayedExecutions.get(id);
            delayedExecutions.remove(id);
            for (DelayedExecution delayedExecution : delayedExecutionsForId)
            {
                delayedExecution.getDependencies().remove(id);
                if (delayedExecution.getDependencies().isEmpty())
                {
                    executableDelays.add(delayedExecution);
                }
            }

            for (DelayedExecution executableDelayed : executableDelays)
            {
                executeDelayed(executableDelayed);
            }
        }
    }

    private void executeDelayed(DelayedExecution delayedExecution)
    {
        Class clazz = delayedExecution.getCreationOrUpdate().getClass();

        if (clazz == ProjectCreation.class)
        {
            createProject((ProjectCreation) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == ProjectUpdate.class)
        {
            updateProject((ProjectUpdate) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == ExperimentCreation.class)
        {
            createExperiment((ExperimentCreation) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == ExperimentUpdate.class)
        {
            updateExperiment((ExperimentUpdate) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == ExperimentTypeCreation.class)
        {
            createExperimentType((ExperimentTypeCreation) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(),
                    delayedExecution.getLine());
        } else if (clazz == ExperimentTypeUpdate.class)
        {
            updateExperimentType((ExperimentTypeUpdate) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(),
                    delayedExecution.getLine());
        } else if (clazz == SampleCreation.class)
        {
            createSample(delayedExecution.getVariable(), (SampleCreation) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(),
                    delayedExecution.getLine());
        } else if (clazz == SampleUpdate.class)
        {
            updateSample(delayedExecution.getVariable(), (SampleUpdate) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(),
                    delayedExecution.getLine());
        } else if (clazz == SampleTypeCreation.class)
        {
            createSampleType((SampleTypeCreation) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == SampleTypeUpdate.class)
        {
            updateSampleType((SampleTypeUpdate) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == DataSetTypeCreation.class)
        {
            createDataSetType((DataSetTypeCreation) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == SampleTypeUpdate.class)
        {
            updateDataSetType((DataSetTypeUpdate) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == PropertyTypeCreation.class)
        {
            createPropertyType((PropertyTypeCreation) delayedExecution.getCreationOrUpdate(), delayedExecution.getPage(), delayedExecution.getLine());
        } else if (clazz == PropertyTypeUpdate.class)
        {
            // no delay for PropertyTypeUpdate
        }
    }

    public Set<IObjectId> getIds()
    {
        return ids;
    }

    private <E> Collection<E> safe(Collection<E> list)
    {
        if (list == null)
        {
            return Collections.EMPTY_LIST;
        } else
        {
            return list;
        }
    }

    private boolean isKeySamplePropertyCode(String propertyCode)
    {
        if (!propertyTypeCache.containsKey(propertyCode))
        {
            PropertyType propertyType = getPropertyType(new PropertyTypePermId(propertyCode));
            propertyTypeCache.put(propertyCode, propertyType);
        }

        return propertyTypeCache.get(propertyCode).getDataType() == DataType.SAMPLE;
    }

    //
    // SPACE
    //

    public Space getSpace(ISpaceId spaceId, SpaceFetchOptions fetchOptions)
    {
        return v3.getSpaces(this.sessionToken, List.of(spaceId), fetchOptions).getOrDefault(spaceId, null);
    }

    public void createSpace(SpaceCreation newSpace)
    {
        addIdsAndExecuteDelayed(v3.createSpaces(this.sessionToken, List.of(newSpace)).get(0), ImportTypes.SPACE, null);
    }

    public void updateSpace(SpaceUpdate spaceUpdate)
    {
        v3.updateSpaces(this.sessionToken, List.of(spaceUpdate));
        this.ids.add(spaceUpdate.getSpaceId());
    }

    //
    // PROJECT
    //

    public Project getProject(IProjectId projectId, ProjectFetchOptions fetchOptions)
    {
        return v3.getProjects(this.sessionToken, List.of(projectId), fetchOptions).getOrDefault(projectId, null);
    }

    public void createProject(ProjectCreation projectCreation, int page, int line)
    {
        ISpaceId spaceId = projectCreation.getSpaceId();
        if (getSpace(spaceId, new SpaceFetchOptions()) == null)
        { // Delay
            ProjectIdentifier projectIdentifer =
                    new ProjectIdentifier("/" + projectCreation.getSpaceId().toString() + "/" + projectCreation.getCode());
            DelayedExecution delayedExecution = new DelayedExecution(null, projectIdentifer, projectCreation, page, line);
            delayedExecution.addDependencies(List.of(spaceId));
            addDelayedExecution(delayedExecution);
        } else
        { // Execute
            addIdsAndExecuteDelayed(v3.createProjects(this.sessionToken, List.of(projectCreation)).get(0), ImportTypes.PROJECT, null);
        }
    }

    public void updateProject(ProjectUpdate projectUpdate, int page, int line)
    {
        if (projectUpdate.getSpaceId().getValue() != null)
        { // Updating space
            ISpaceId spaceId = projectUpdate.getSpaceId().getValue();
            if (getSpace(spaceId, new SpaceFetchOptions()) == null)
            { // Delay
                IProjectId projectId = projectUpdate.getProjectId();
                DelayedExecution delayedExecution = new DelayedExecution(null, projectId, projectUpdate, page, line);
                delayedExecution.addDependencies(List.of(spaceId));
                addDelayedExecution(delayedExecution);
            } else
            { // Execute
                v3.updateProjects(sessionToken, List.of(projectUpdate));
                this.ids.add(projectUpdate.getProjectId());
            }
        } else
        { // Execute
            v3.updateProjects(sessionToken, List.of(projectUpdate));
            this.ids.add(projectUpdate.getProjectId());
        }
    }

    //
    // EXPERIMENT
    //

    public Experiment getExperiment(IExperimentId experimentId, ExperimentFetchOptions fetchOptions)
    {
        return v3.getExperiments(this.sessionToken, List.of(experimentId), fetchOptions).getOrDefault(experimentId, null);
    }

    public void createExperiment(ExperimentCreation experimentCreation, int page, int line)
    {
        ExperimentIdentifier experimentIdentifier =
                new ExperimentIdentifier(experimentCreation.getProjectId().toString() + "/" + experimentCreation.getCode());

        // Manage Sample properties cyclical dependencies
        resolveAndScheduleAssignmentOfSampleProperties(experimentIdentifier,
                experimentCreation, page, line);
        //

        // check project
        IProjectId projectId = experimentCreation.getProjectId();
        if (getProject(projectId, new ProjectFetchOptions()) == null)
        {// Delay
            DelayedExecution delayedExecution = new DelayedExecution(null, experimentIdentifier, experimentCreation, page, line);
            delayedExecution.addDependencies(List.of(projectId));
            addDelayedExecution(delayedExecution);

        } else
        { // Execute
            addIdsAndExecuteDelayed(v3.createExperiments(this.sessionToken, List.of(experimentCreation)).get(0), ImportTypes.EXPERIMENT, null);
        }
    }

    public void updateExperiment(ExperimentUpdate experimentUpdate, int page, int line)
    {
        // Manage Sample properties cyclical dependencies
        resolveAndScheduleAssignmentOfSampleProperties(experimentUpdate.getExperimentId(),
                experimentUpdate, page, line);
        //

        IExperimentId experimentId = experimentUpdate.getExperimentId();
        IProjectId projectId = experimentUpdate.getProjectId().getValue();
        if (projectId != null && getProject(projectId, new ProjectFetchOptions()) == null)
        { // Delay
            DelayedExecution delayedExecution = new DelayedExecution(null, experimentId, experimentUpdate, page, line);
            delayedExecution.addDependencies(List.of(projectId));
            addDelayedExecution(delayedExecution);
        } else
        {// Execute
            v3.updateExperiments(this.sessionToken, List.of(experimentUpdate));
            this.ids.add(experimentUpdate.getExperimentId());
        }
    }

    public ExperimentType getExperimentType(IEntityTypeId experimentTypeId, ExperimentTypeFetchOptions fetchOptions)
    {
        return v3.getExperimentTypes(this.sessionToken, List.of(experimentTypeId), fetchOptions).getOrDefault(experimentTypeId, null);
    }

    public void createExperimentType(ExperimentTypeCreation experimentTypeCreation, int page, int line)
    {
        if (!safe(experimentTypeCreation.getPropertyAssignments()).isEmpty())
        {
            throw new IllegalStateException("XLS Parser - createExperimentType called with properties.");
        }
        addIdsAndExecuteDelayed(v3.createExperimentTypes(this.sessionToken, List.of(experimentTypeCreation)).get(0), ImportTypes.EXPERIMENT_TYPE,
                null);
    }

    public void updateExperimentType(ExperimentTypeUpdate experimentTypeUpdate, int page, int line)
    {
        List<IPropertyTypeId> dependencies = new ArrayList<>();

        // Collect possible dependencies
        Set<IPropertyTypeId> possibleDependencies = new LinkedHashSet<>();
        for (PropertyAssignmentCreation propertyAssignmentCreation : safe(experimentTypeUpdate.getPropertyAssignments().getAdded()))
        {
            possibleDependencies.add(propertyAssignmentCreation.getPropertyTypeId());
        }
        for (PropertyAssignmentCreation propertyAssignmentCreation : safe(experimentTypeUpdate.getPropertyAssignments().getSet()))
        {
            possibleDependencies.add(propertyAssignmentCreation.getPropertyTypeId());
        }
        for (IPropertyAssignmentId propertyAssignmentId : safe(experimentTypeUpdate.getPropertyAssignments().getRemoved()))
        {
            possibleDependencies.add(((PropertyAssignmentPermId) propertyAssignmentId).getPropertyTypeId());
        }

        // Check if they have been created already
        for (IPropertyTypeId propertyTypeId : possibleDependencies)
        {
            PropertyType propertyType = getPropertyType(propertyTypeId);
            if (propertyType == null)
            {
                dependencies.add(propertyTypeId);
            }
        }

        if (!dependencies.isEmpty())
        {
            // Delay
            DelayedExecution delayedExecution = new DelayedExecution(null, experimentTypeUpdate.getTypeId(), experimentTypeUpdate, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        } else
        {
            v3.updateExperimentTypes(this.sessionToken, List.of(experimentTypeUpdate));
            this.ids.add(experimentTypeUpdate.getTypeId());
        }
    }

    //
    // SAMPLE
    //

    public Sample getSample(ISampleId sampleId, SampleFetchOptions fetchOptions)
    {
        return v3.getSamples(this.sessionToken, List.of(sampleId), fetchOptions).getOrDefault(sampleId, null);
    }

    public void createSample(String variable, SampleCreation sampleCreation, int page, int line)
    {
        ISampleId sampleId = ImportUtils.buildSampleIdentifier(variable, sampleCreation);
        if (sampleId instanceof IdentifierVariable && variable == null)
        {
            variable = ((IdentifierVariable) sampleId).getVariable();
        }

        List<IObjectId> dependencies = new ArrayList<>();

        ISpaceId spaceId = sampleCreation.getSpaceId();
        IExperimentId experimentId = sampleCreation.getExperimentId();
        IProjectId projectId = sampleCreation.getProjectId();

        // check space
        if (spaceId != null && getSpace(spaceId, new SpaceFetchOptions()) == null)
        {
            dependencies.add(spaceId);
        }

        // check experiment
        if (experimentId != null && getExperiment(experimentId, new ExperimentFetchOptions()) == null)
        {
            dependencies.add(experimentId);
        }

        // check project

        if (projectId != null && getProject(projectId, new ProjectFetchOptions()) == null)
        {
            dependencies.add(projectId);
        }

        // Manage Sample properties cyclical dependencies
        resolveAndScheduleAssignmentOfSampleProperties(sampleId, sampleCreation, page, line);
        //

        // parents/children variable substitution
        createSampleResolvingAndScheduleAssignmentOfSampleParentChildDependencies(sampleId, sampleCreation, page, line);
        //

        if (!dependencies.isEmpty())
        {
            DelayedExecution delayedExecution = new DelayedExecution(variable, sampleId, sampleCreation, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        } else
        {
            addIdsAndExecuteDelayed(v3.createSamples(sessionToken, List.of(sampleCreation)).get(0), ImportTypes.SAMPLE, variable);
        }
    }

    private void createSampleResolvingAndScheduleAssignmentOfSampleParentChildDependencies(ISampleId sampleId, SampleCreation sampleCreation,
            int page, int line)
    {
        List<ISampleId> dependencies = new ArrayList<>();
        dependencies.add(sampleId);

        SampleUpdate sampleUpdate = new SampleUpdate();
        sampleUpdate.setSampleId(sampleId);

        for (ISampleId id : safe(sampleCreation.getParentIds()))
        {
            if (id instanceof IdentifierVariable)
            {
                IdentifierVariable idVariable = (IdentifierVariable) id;
                if (!resolvedVariables.containsKey(idVariable))
                {
                    dependencies.add(id);
                    sampleUpdate.getParentIds().add(id);
                } else // Variable substitution
                {
                    ISampleId identifier = (ISampleId) resolvedVariables.get(idVariable);
                    sampleCreation.getParentIds().remove(idVariable);
                    ((List<ISampleId>) sampleCreation.getParentIds()).add(identifier);
                }
            } else
            {
                if (getSample(id, new SampleFetchOptions()) == null)
                {
                    dependencies.add(id);
                    sampleUpdate.getParentIds().add(id);
                }
            }
        }

        for (ISampleId id : safe(sampleCreation.getChildIds()))
        {
            if (id instanceof IdentifierVariable)
            {
                IdentifierVariable idVariable = (IdentifierVariable) id;
                if (!resolvedVariables.containsKey(idVariable))
                {
                    dependencies.add(id);
                    sampleUpdate.getChildIds().add(id);
                } else // Variable substitution
                {
                    ISampleId identifier = (ISampleId) resolvedVariables.get(idVariable);
                    sampleCreation.getChildIds().remove(idVariable);
                    ((List<ISampleId>) sampleCreation.getChildIds()).add(identifier);
                }
            } else
            {
                if (getSample(id, new SampleFetchOptions()) == null)
                {
                    dependencies.add(id);
                    sampleUpdate.getChildIds().add(id);
                }
            }
        }

        if (!safe(sampleCreation.getParentIds()).isEmpty() && !dependencies.isEmpty())
        {
            for (ISampleId id : dependencies)
            {
                sampleCreation.getParentIds().remove(id);
            }
        }
        if (!safe(sampleCreation.getChildIds()).isEmpty() && !dependencies.isEmpty())
        {
            for (ISampleId id : dependencies)
            {
                sampleCreation.getChildIds().remove(id);
            }
        }

        if (!dependencies.isEmpty())
        {
            String variable = null;
            if (sampleId instanceof IdentifierVariable)
            {
                variable = ((IdentifierVariable) sampleId).getVariable();
            }

            DelayedExecution delayedExecution =
                    new DelayedExecution(variable, sampleId, sampleUpdate, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        }
    }

    private void samplePropertiesVariableReplacer(IPropertiesHolder sampleOrExperimentDTO)
    {
        Map<String, Serializable> properties = sampleOrExperimentDTO.getProperties();
        Map<String, Serializable> sampleProperties = new HashMap<>();
        for (String propertyCode : properties.keySet())
        {
            if (isKeySamplePropertyCode(propertyCode))
            {
                List<String> propertyValues = new ArrayList<>();
                for (String propertyValue : getProperties(properties.get(propertyCode)))
                {
                    ISampleId sampleId = ImportUtils.buildSampleIdentifier(propertyValue);
                    if (sampleId instanceof IdentifierVariable && resolvedVariables.containsKey(sampleId))
                    {
                        sampleId = (ISampleId) resolvedVariables.get(sampleId);
                        if (sampleId instanceof SampleIdentifier)
                        {
                            propertyValue = ((SampleIdentifier) sampleId).getIdentifier();
                        } else if (sampleId instanceof SamplePermId)
                        {
                            propertyValue = ((SamplePermId) sampleId).getPermId();
                        }
                    } else
                    {
                        // Do nothing
                    }
                    propertyValues.add(propertyValue);
                }
                sampleProperties.put(propertyCode, propertyValues.toArray(new String[0]));
            }
        }

        // Update sample properties on the DTO
        for (String samplePropertyKey : sampleProperties.keySet())
        {
            sampleOrExperimentDTO.setProperty(samplePropertyKey, sampleProperties.get(samplePropertyKey));
        }
    }

    private void resolveAndScheduleAssignmentOfSampleProperties(IObjectId objectId, IPropertiesHolder sampleOrExperimentDTO, int page, int line)
    {
        samplePropertiesVariableReplacer(sampleOrExperimentDTO); // Update properties before deciding to schedule anything

        Map<String, Serializable> properties = sampleOrExperimentDTO.getProperties();
        Map<String, Serializable> sampleProperties = new HashMap<>();
        List<IObjectId> dependencies = new ArrayList<>();

        for (String propertyCode : properties.keySet())
        {
            if (isKeySamplePropertyCode(propertyCode))
            {
                boolean dependencyFound = false;
                for (String propertyValue : getProperties(properties.get(propertyCode)))
                {
                    if (propertyValue != null)
                    {
                        ISampleId sampleId = ImportUtils.buildSampleIdentifier(propertyValue);
                        // 1. Check if they are dependencies or not
                        if (sampleId instanceof IdentifierVariable && !resolvedVariables.containsKey(sampleId))
                        {
                            // Not resolved variable => Dependency
                            dependencies.add(sampleId);
                            dependencyFound = true;
                        } else if (sampleId instanceof SampleIdentifier && getSample(sampleId, new SampleFetchOptions()) == null)
                        {
                            // Not found sample => Dependency
                            dependencies.add(sampleId);
                            dependencyFound = true;
                        } else
                        {
                            // Not a dependency
                        }
                    }
                }

                if (dependencyFound)
                { // We store all sample properties with dependencies to create the update
                    sampleProperties.put(propertyCode, properties.get(propertyCode));
                }
            }
        }

        if (dependencies.isEmpty() == false)
        { // We only create an update if dependencies are not resolved
            IObjectUpdate entityToUpdate = null;
            if (sampleOrExperimentDTO instanceof SampleCreation || sampleOrExperimentDTO instanceof SampleUpdate)
            {
                SampleUpdate entityUpdate = new SampleUpdate();
                entityUpdate.setSampleId((ISampleId) objectId);
                entityUpdate.getProperties().putAll(sampleProperties);
                entityToUpdate = entityUpdate;
            } else if (sampleOrExperimentDTO instanceof ExperimentCreation || sampleOrExperimentDTO instanceof ExperimentUpdate)
            {
                ExperimentUpdate entityUpdate = new ExperimentUpdate();
                entityUpdate.setExperimentId((IExperimentId) objectId);
                entityUpdate.getProperties().putAll(sampleProperties);
                entityToUpdate = entityUpdate;
            }

            // Remove all sample properties scheduled for update
            for (String samplePropertyKey : sampleProperties.keySet())
            {
                sampleOrExperimentDTO.getProperties().remove(samplePropertyKey);
            }

            // If is a creation the entity is a dependency
            if (sampleOrExperimentDTO instanceof SampleCreation ||
                    sampleOrExperimentDTO instanceof ExperimentCreation)
            {
                dependencies.add(objectId);
            }
            String variable = null;
            if (objectId instanceof IdentifierVariable)
            {
                variable = ((IdentifierVariable) objectId).getVariable();
            }

            DelayedExecution delayedExecution = new DelayedExecution(variable, objectId, (Serializable) entityToUpdate, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        }
    }

    private String[] getProperties(Serializable propertyValue)
    {
        if (propertyValue == null)
        {
            return new String[0];
        }
        if (propertyValue.getClass().isArray())
        {
            Serializable[] values = (Serializable[]) propertyValue;
            return Stream.of(values)
                    .map(Serializable::toString)
                    .toArray(String[]::new);
        } else
        {
            return new String[] { propertyValue.toString() };
        }
    }

    public void updateSample(String variable, SampleUpdate sampleUpdate, int page, int line)
    {
        List<IObjectId> dependencies = new ArrayList<>();

        ISpaceId spaceId = sampleUpdate.getSpaceId().getValue();
        IExperimentId experimentId = sampleUpdate.getExperimentId().getValue();
        IProjectId projectId = sampleUpdate.getProjectId().getValue();

        // check for self cyclical dependency using variable
        if (sampleUpdate.getSampleId() instanceof IdentifierVariable)
        {
            if (resolvedVariables.containsKey((IdentifierVariable) sampleUpdate.getSampleId()))
            {
                sampleUpdate.setSampleId((SampleIdentifier) resolvedVariables.get((IdentifierVariable) sampleUpdate.getSampleId()));
            }
        }

        // check space
        if (spaceId != null && getSpace(spaceId, new SpaceFetchOptions()) == null)
        {
            dependencies.add(spaceId);
        }

        // check experiment
        if (experimentId != null && getExperiment(experimentId, new ExperimentFetchOptions()) == null)
        {
            dependencies.add(experimentId);
        }

        // check project
        if (projectId != null && getProject(projectId, new ProjectFetchOptions()) == null)
        {
            dependencies.add(projectId);
        }

        // Manage Sample properties cyclical dependencies
        resolveAndScheduleAssignmentOfSampleProperties(sampleUpdate.getSampleId(),
                sampleUpdate, page, line);
        //

        // parents/children variable substitution
        List<ISampleId> parentIdsAdded = new ArrayList<>();
        for (ISampleId id : sampleUpdate.getParentIds().getAdded())
        {
            if (id instanceof IdentifierVariable)
            {
                IdentifierVariable idVariable = (IdentifierVariable) id;
                if (!resolvedVariables.containsKey(idVariable))
                {
                    dependencies.add(id);
                    parentIdsAdded.add(id);
                } else
                {
                    ISampleId identifier = (ISampleId) resolvedVariables.get(idVariable);
                    parentIdsAdded.add(identifier);
                }
            } else
            {
                if (getSample(id, new SampleFetchOptions()) == null)
                {
                    dependencies.add(id);
                }
                parentIdsAdded.add(id);
            }
        }
        Collection<ISampleId> parentIdsRemoved = sampleUpdate.getParentIds().getRemoved();

        sampleUpdate.getParentIds().clearActions();
        sampleUpdate.getParentIds().add(parentIdsAdded.toArray(new ISampleId[0]));
        sampleUpdate.getParentIds().remove(parentIdsRemoved.toArray(new ISampleId[0]));

        List<ISampleId> childIdsAdded = new ArrayList<>();
        for (ISampleId id : sampleUpdate.getChildIds().getAdded())
        {
            if (id instanceof IdentifierVariable)
            {
                IdentifierVariable idVariable = (IdentifierVariable) id;
                if (!resolvedVariables.containsKey(idVariable))
                {
                    dependencies.add(id);
                    childIdsAdded.add(id);
                } else
                {
                    ISampleId identifier = (ISampleId) resolvedVariables.get(idVariable);
                    childIdsAdded.add(identifier);
                }
            } else
            {
                if (getSample(id, new SampleFetchOptions()) == null)
                {
                    dependencies.add(id);
                }
                childIdsAdded.add(id);
            }
        }
        Collection<ISampleId> childIdsRemoved = sampleUpdate.getChildIds().getRemoved();

        sampleUpdate.getChildIds().clearActions();
        sampleUpdate.getChildIds().add(childIdsAdded.toArray(new ISampleId[0]));
        sampleUpdate.getChildIds().remove(childIdsRemoved.toArray(new ISampleId[0]));

        if (!dependencies.isEmpty())
        {
            ISampleId sampleId = sampleUpdate.getSampleId();
            DelayedExecution delayedExecution = new DelayedExecution(variable, sampleId, sampleUpdate, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        } else
        { // Execute
            v3.updateSamples(sessionToken, List.of(sampleUpdate));
            this.ids.add(sampleUpdate.getSampleId());
        }
    }

    public SampleType getSampleType(IEntityTypeId sampleTypeId, SampleTypeFetchOptions fetchOptions)
    {
        return v3.getSampleTypes(this.sessionToken, List.of(sampleTypeId), fetchOptions).getOrDefault(sampleTypeId, null);
    }

    public void createSampleType(SampleTypeCreation sampleTypeCreation, int page, int line)
    {
        if (!safe(sampleTypeCreation.getPropertyAssignments()).isEmpty())
        {
            throw new IllegalStateException("XLS Parser - createSampleType called with properties.");
        }
        addIdsAndExecuteDelayed(v3.createSampleTypes(this.sessionToken, List.of(sampleTypeCreation)).get(0), ImportTypes.SAMPLE_TYPE, null);
    }

    public void updateSampleType(SampleTypeUpdate sampleTypeUpdate, int page, int line)
    {
        List<IPropertyTypeId> dependencies = new ArrayList<>();

        // Collect possible dependencies
        Set<IPropertyTypeId> possibleDependencies = new LinkedHashSet<>();
        for (PropertyAssignmentCreation propertyAssignmentCreation : safe(sampleTypeUpdate.getPropertyAssignments().getAdded()))
        {
            possibleDependencies.add(propertyAssignmentCreation.getPropertyTypeId());
        }
        for (PropertyAssignmentCreation propertyAssignmentCreation : safe(sampleTypeUpdate.getPropertyAssignments().getSet()))
        {
            possibleDependencies.add(propertyAssignmentCreation.getPropertyTypeId());
        }
        for (IPropertyAssignmentId propertyAssignmentId : safe(sampleTypeUpdate.getPropertyAssignments().getRemoved()))
        {
            possibleDependencies.add(((PropertyAssignmentPermId) propertyAssignmentId).getPropertyTypeId());
        }

        // Check if they have been created already
        for (IPropertyTypeId propertyTypeId : possibleDependencies)
        {
            PropertyType propertyType = getPropertyType(propertyTypeId);
            if (propertyType == null)
            {
                dependencies.add(propertyTypeId);
            }
        }

        if (!dependencies.isEmpty())
        {
            // Delay
            DelayedExecution delayedExecution = new DelayedExecution(null, sampleTypeUpdate.getTypeId(), sampleTypeUpdate, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        } else
        {
            v3.updateSampleTypes(this.sessionToken, List.of(sampleTypeUpdate));
            this.ids.add(sampleTypeUpdate.getTypeId());
        }
    }

    //
    // DATASET TYPES
    //

    public DataSetType getDataSetType(IEntityTypeId dataSetTypeId, DataSetTypeFetchOptions fetchOptions)
    {
        return v3.getDataSetTypes(this.sessionToken, List.of(dataSetTypeId), fetchOptions).getOrDefault(dataSetTypeId, null);
    }

    public void createDataSetType(DataSetTypeCreation dataSetTypeCreation, int page, int line)
    {
        if (!safe(dataSetTypeCreation.getPropertyAssignments()).isEmpty())
        {
            throw new IllegalStateException("XLS Parser - createDataSetType called with properties.");
        }
        addIdsAndExecuteDelayed(v3.createDataSetTypes(this.sessionToken, List.of(dataSetTypeCreation)).get(0), ImportTypes.DATASET_TYPE, null);
    }

    public void updateDataSetType(DataSetTypeUpdate dataSetTypeUpdate, int page, int line)
    {
        List<IPropertyTypeId> dependencies = new ArrayList<>();

        // Collect possible dependencies
        Set<IPropertyTypeId> possibleDependencies = new LinkedHashSet<>();
        for (PropertyAssignmentCreation propertyAssignmentCreation : safe(dataSetTypeUpdate.getPropertyAssignments().getAdded()))
        {
            possibleDependencies.add(propertyAssignmentCreation.getPropertyTypeId());
        }
        for (PropertyAssignmentCreation propertyAssignmentCreation : safe(dataSetTypeUpdate.getPropertyAssignments().getSet()))
        {
            possibleDependencies.add(propertyAssignmentCreation.getPropertyTypeId());
        }
        for (IPropertyAssignmentId propertyAssignmentId : safe(dataSetTypeUpdate.getPropertyAssignments().getRemoved()))
        {
            possibleDependencies.add(((PropertyAssignmentPermId) propertyAssignmentId).getPropertyTypeId());
        }

        // Check if they have been created already
        for (IPropertyTypeId propertyTypeId : possibleDependencies)
        {
            PropertyType propertyType = getPropertyType(propertyTypeId);
            if (propertyType == null)
            {
                dependencies.add(propertyTypeId);
            }
        }

        if (!dependencies.isEmpty())
        {
            // Delay
            DelayedExecution delayedExecution = new DelayedExecution(null, dataSetTypeUpdate.getTypeId(), dataSetTypeUpdate, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        } else
        {
            v3.updateDataSetTypes(this.sessionToken, List.of(dataSetTypeUpdate));
            this.ids.add(dataSetTypeUpdate.getTypeId());
        }
    }

    //
    // PROPERTY
    //

    Map<IPropertyTypeId, PropertyType> propertyTypeCache2 = new ConcurrentHashMap<>();

    public PropertyType getPropertyType(IPropertyTypeId typeId)
    {
        if (!propertyTypeCache2.containsKey(typeId))
        {
            PropertyTypeFetchOptions fetchOptions = new PropertyTypeFetchOptions();
            fetchOptions.withVocabulary().withTerms().withVocabulary();
            fetchOptions.withSampleType();

            PropertyType propertyType = v3.getPropertyTypes(this.sessionToken, List.of(typeId), fetchOptions).getOrDefault(typeId, null);
            if (propertyType != null)
            {
                propertyTypeCache2.put(typeId, propertyType);
            }
        }
        return propertyTypeCache2.get(typeId);
    }

    public void createPropertyType(PropertyTypeCreation newPropertyType, int page, int line)
    {
        List<IObjectId> dependencies = new ArrayList<>();

        //TODO
        //  IF PROPERTY TYPE EXISTS - DUE TO DE DELAY IN EXECUTION OF A CYCLICAL DEPENDENCY
        //      IGNORE CREATION, ALL CREATIONS LOOK THE SAME AND ALREADY HAPPENED, IT WILL BE ASSIGNED AS NEXT STEP
        //
        if (getPropertyType(new PropertyTypePermId(newPropertyType.getCode())) != null)
        {
            return;
        }

        // check sample type
        if (newPropertyType.getDataType().equals(DataType.SAMPLE))
        {
            IEntityTypeId sampleTypeId = newPropertyType.getSampleTypeId();
            if (sampleTypeId != null && getSampleType(sampleTypeId, new SampleTypeFetchOptions()) == null)
            {
                dependencies.add(sampleTypeId);
            }
        }

        // check vocabulary
        if (newPropertyType.getVocabularyId() != null)
        {
            IVocabularyId vocabularyId = newPropertyType.getVocabularyId();
            VocabularyFetchOptions fetchOptions = new VocabularyFetchOptions();
            fetchOptions.withTerms().withVocabulary();
            if (getVocabulary(vocabularyId, fetchOptions) == null)
            {
                dependencies.add(vocabularyId);
            }
        }

        if (!dependencies.isEmpty())
        { // Delay
            PropertyTypePermId propertyTypePermId = new PropertyTypePermId(newPropertyType.getCode());
            DelayedExecution delayedExecution = new DelayedExecution(null, propertyTypePermId, newPropertyType, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        } else
        { // Execute
            addIdsAndExecuteDelayed(v3.createPropertyTypes(sessionToken, List.of(newPropertyType)).get(0), ImportTypes.PROPERTY_TYPE, null);
        }
    }

    public void updatePropertyType(PropertyTypeUpdate propertyTypeUpdate, int page, int line)
    {
        // Data type and vocabulary can't be updated. That is mean - no delay.
        v3.updatePropertyTypes(this.sessionToken, List.of(propertyTypeUpdate));
        this.ids.add(propertyTypeUpdate.getTypeId());
    }

    //
    // PLUGIN
    //

    public Plugin getPlugin(IPluginId plugin, PluginFetchOptions fetchOptions)
    {
        return v3.getPlugins(this.sessionToken, List.of(plugin), fetchOptions).getOrDefault(plugin, null);
    }

    public void createPlugin(PluginCreation newPlugin)
    {
        addIdsAndExecuteDelayed(v3.createPlugins(this.sessionToken, List.of(newPlugin)).get(0), ImportTypes.SCRIPT, null);
    }

    public void updatePlugin(PluginUpdate pluginUpdate)
    {
        v3.updatePlugins(this.sessionToken, List.of(pluginUpdate));
        this.ids.add(pluginUpdate.getPluginId());
    }

    //
    // VOCABULARY
    //
    public Vocabulary getVocabulary(IVocabularyId vocabularyId, VocabularyFetchOptions fetchOptions)
    {
        return v3.getVocabularies(this.sessionToken, List.of(vocabularyId), fetchOptions).getOrDefault(vocabularyId, null);
    }

    public void createVocabulary(VocabularyCreation newVocabulary)
    {
        addIdsAndExecuteDelayed(v3.createVocabularies(this.sessionToken, List.of(newVocabulary)).get(0), ImportTypes.VOCABULARY_TYPE, null);
    }

    public void updateVocabulary(VocabularyUpdate vocabularyUpdate)
    {
        v3.updateVocabularies(this.sessionToken, List.of(vocabularyUpdate));
        this.ids.add(vocabularyUpdate.getVocabularyId());
    }

    public VocabularyTerm getVocabularyTerm(IVocabularyTermId vocabularyTerm, VocabularyTermFetchOptions fetchOptions)
    {
        return v3.getVocabularyTerms(this.sessionToken, List.of(vocabularyTerm), fetchOptions).getOrDefault(vocabularyTerm, null);
    }

    public void createVocabularyTerm(VocabularyTermCreation newVocabularyTerm)
    {
        addIdsAndExecuteDelayed(v3.createVocabularyTerms(this.sessionToken, List.of(newVocabularyTerm)).get(0), ImportTypes.VOCABULARY_TERM, null);
    }

    public void updateVocabularyTerm(VocabularyTermUpdate vocabularyTermUpdate)
    {
        v3.updateVocabularyTerms(this.sessionToken, List.of(vocabularyTermUpdate));
        this.ids.add(vocabularyTermUpdate.getVocabularyTermId());
    }

    //
    // SEMANTIC ANNOTATIONS
    //

    public SemanticAnnotation getSemanticAnnotation(SemanticAnnotationSearchCriteria criteria, SemanticAnnotationFetchOptions fetchOptions)
    {
        SearchResult<SemanticAnnotation> semanticAnnotationSearchResult = v3.searchSemanticAnnotations(this.sessionToken, criteria, fetchOptions);

        if (semanticAnnotationSearchResult.getTotalCount() > 0)
        {
            return semanticAnnotationSearchResult.getObjects().get(0);
        } else
        {
            return null;
        }
    }

    public void createSemanticAnnotation(SemanticAnnotationCreation creation, int page, int line)
    {
        List<IObjectId> dependencies = new ArrayList<>();
        EntityTypePermId entityTypePermId = null;
        PropertyTypePermId propertyTypePermId = null;

        if (creation.getPropertyAssignmentId() != null)
        {
            PropertyAssignmentPermId propertyAssignmentPermId = (PropertyAssignmentPermId) creation.getPropertyAssignmentId();
            entityTypePermId = (EntityTypePermId) propertyAssignmentPermId.getEntityTypeId();
            propertyTypePermId = (PropertyTypePermId) propertyAssignmentPermId.getPropertyTypeId();
        }
        if (creation.getEntityTypeId() != null)
        {
            entityTypePermId = (EntityTypePermId) creation.getEntityTypeId();
        }
        if (creation.getPropertyTypeId() != null)
        {
            propertyTypePermId = (PropertyTypePermId) creation.getPropertyTypeId();
        }

        if (entityTypePermId != null)
        {
            IEntityType entityType = null;
            switch (entityTypePermId.getEntityKind())
            {
                case EXPERIMENT:
                    entityType = getExperimentType(entityTypePermId, new ExperimentTypeFetchOptions());
                    break;
                case SAMPLE:
                    entityType = getSampleType(entityTypePermId, new SampleTypeFetchOptions());
                    break;
                case DATA_SET:
                    entityType = getDataSetType(entityTypePermId, new DataSetTypeFetchOptions());
                    break;
            }
            if (entityType == null)
            {
                dependencies.add(entityTypePermId);
            }
        }

        if (propertyTypePermId != null)
        {
            PropertyType propertyType = getPropertyType(propertyTypePermId);
            if (propertyType == null)
            {
                dependencies.add(propertyTypePermId);
            }
        }

        if (!dependencies.isEmpty())
        { // Delay
            DelayedExecution delayedExecution = new DelayedExecution(null, null, creation, page, line);
            delayedExecution.addDependencies(dependencies);
            addDelayedExecution(delayedExecution);
        } else
        { // Execute
            v3.createSemanticAnnotations(sessionToken, List.of(creation));
        }
    }

}
