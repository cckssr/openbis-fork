/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server;

import ch.systemsx.cisd.common.multiplexer.IMultiplexer;
import ch.systemsx.cisd.openbis.generic.server.business.IDataStoreServiceFactory;
import ch.systemsx.cisd.openbis.generic.server.business.IEntityOperationChecker;
import ch.systemsx.cisd.openbis.generic.server.business.IRelationshipService;
import ch.systemsx.cisd.openbis.generic.server.business.IServiceConversationClientManagerLocal;
import ch.systemsx.cisd.openbis.generic.server.business.bo.*;
import ch.systemsx.cisd.openbis.generic.server.business.bo.datasetlister.DatasetLister;
import ch.systemsx.cisd.openbis.generic.server.business.bo.datasetlister.IDatasetLister;
import ch.systemsx.cisd.openbis.generic.server.business.bo.materiallister.IMaterialLister;
import ch.systemsx.cisd.openbis.generic.server.business.bo.materiallister.MaterialLister;
import ch.systemsx.cisd.openbis.generic.server.business.bo.samplelister.ISampleLister;
import ch.systemsx.cisd.openbis.generic.server.business.bo.samplelister.SampleLister;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.server.jython.api.v1.impl.IMasterDataScriptRegistrationRunner;
import ch.systemsx.cisd.openbis.generic.shared.IJythonEvaluatorPool;
import ch.systemsx.cisd.openbis.generic.shared.dto.Session;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;
import ch.systemsx.cisd.openbis.generic.shared.managed_property.IManagedPropertyEvaluatorFactory;
import ch.systemsx.cisd.openbis.generic.shared.managed_property.api.IEntityInformationProvider;

/**
 * The unique {@link ICommonBusinessObjectFactory} implementation.
 *
 * @author Tomasz Pylak
 */
public final class CommonBusinessObjectFactory extends AbstractBusinessObjectFactory implements
        ICommonBusinessObjectFactory
{

    private final IJythonEvaluatorPool jythonEvaluatorPool;

    private EntityHistoryCreator historyCreator;

    private IEntityInformationProvider entityInformationProvider;

    public CommonBusinessObjectFactory(IDAOFactory daoFactory, IDataStoreServiceFactory dssFactory,
            IRelationshipService relationshipService,
            IEntityOperationChecker entityOperationChecker,
            IServiceConversationClientManagerLocal conversationClient,
            IEntityInformationProvider entityInformationProvider,
            IManagedPropertyEvaluatorFactory managedPropertyEvaluatorFactory,
            IMultiplexer multiplexer,
            IJythonEvaluatorPool jythonEvaluatorPool,
            EntityHistoryCreator historyCreator)
    {
        super(daoFactory, dssFactory, relationshipService, entityOperationChecker,
                conversationClient, managedPropertyEvaluatorFactory, multiplexer);
        this.entityInformationProvider = entityInformationProvider;
        this.jythonEvaluatorPool = jythonEvaluatorPool;
        this.historyCreator = historyCreator;
    }

    @Override
    public final IAttachmentBO createAttachmentBO(final Session session)
    {
        return new AttachmentBO(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public final ISpaceBO createSpaceBO(final Session session)
    {
        return new SpaceBO(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public final IScriptBO createScriptBO(final Session session)
    {
        return new ScriptBO(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService, jythonEvaluatorPool, configurer.getResolvedProps());
    }

    @Override
    public final IRoleAssignmentTable createRoleAssignmentTable(final Session session)
    {
        return new RoleAssignmentTable(getDaoFactory(), session,
                getManagedPropertyEvaluatorFactory(), dataSetTypeWithoutExperimentChecker,
                relationshipService);
    }

    @Override
    public final ISampleTable createSampleTable(final Session session)
    {
        return new SampleTable(getDaoFactory(), session, getRelationshipService(),
                getEntityOperationChecker(), entityInformationProvider, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker);
    }

    @Override
    public ISampleLister createSampleLister(Session session)
    {
        return SampleLister.create(getDaoFactory(), session.getBaseIndexURL(), session
                .tryGetPerson().getId());
    }

    @Override
    public ISampleLister createSampleLister(Session session, Long userId)
    {
        return SampleLister.create(getDaoFactory(), session.getBaseIndexURL(), userId);
    }

    @Override
    public IDatasetLister createDatasetLister(Session session)
    {
        return DatasetLister.create(getDaoFactory(), session.getBaseIndexURL(), session
                .tryGetPerson().getId());
    }

    @Override
    public IDatasetLister createDatasetLister(Session session, Long userId)
    {
        return DatasetLister.create(getDaoFactory(), session.getBaseIndexURL(), userId);
    }

    @Override
    public IMaterialLister createMaterialLister(Session session)
    {
        return MaterialLister.create(getDaoFactory(), session.getBaseIndexURL(), session
                .tryGetPerson().getId());
    }

    @Override
    public final ISampleBO createSampleBO(final Session session)
    {
        return new SampleBO(getDaoFactory(), session, getRelationshipService(),
                getEntityOperationChecker(), entityInformationProvider, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker);
    }

    @Override
    public IDataBO createDataBO(Session session)
    {
        return new DataBO(getDaoFactory(), session, getRelationshipService(),
                getConversationClient(), entityInformationProvider, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker);
    }

    @Override
    public final IDataSetTable createDataSetTable(final Session session)
    {
        return new DataSetTable(getDaoFactory(), getDSSFactory(), session,
                getRelationshipService(), getConversationClient(),
                entityInformationProvider, getManagedPropertyEvaluatorFactory(), getMultiplexer(),
                dataSetTypeWithoutExperimentChecker);
    }

    @Override
    public ISearchDomainSearcher createSearchDomainSearcher(Session session)
    {
        return new SearchDomainSearcher(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService, getDSSFactory());
    }

    @Override
    public IDeletedDataSetTable createDeletedDataSetTable(Session session)
    {
        return new DeletedDataSetTable(getDaoFactory(), getDSSFactory(), session,
                getRelationshipService(), getConversationClient(),
                entityInformationProvider, getManagedPropertyEvaluatorFactory());
    }

    @Override
    public IExperimentTable createExperimentTable(final Session session)
    {
        return new ExperimentTable(getDaoFactory(), session, getRelationshipService(),
                entityInformationProvider, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker);
    }

    @Override
    public IMaterialTable createMaterialTable(final Session session)
    {
        return new MaterialTable(getDaoFactory(), session, entityInformationProvider, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public final IExperimentBO createExperimentBO(final Session session)
    {
        return new ExperimentBO(getDaoFactory(), session, getRelationshipService(),
                entityInformationProvider, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker);
    }

    @Override
    public final IPropertyTypeTable createPropertyTypeTable(final Session session)
    {
        return new PropertyTypeTable(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public final IPropertyTypeBO createPropertyTypeBO(final Session session)
    {
        return new PropertyTypeBO(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public final IVocabularyBO createVocabularyBO(Session session)
    {
        return new VocabularyBO(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public final IVocabularyTermBO createVocabularyTermBO(Session session)
    {
        return new VocabularyTermBO(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public IEntityTypePropertyTypeBO createEntityTypePropertyTypeBO(Session session,
            EntityKind entityKind)
    {
        return new EntityTypePropertyTypeBO(getDaoFactory(), session, entityKind,
                entityInformationProvider, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public IProjectBO createProjectBO(Session session)
    {
        return new ProjectBO(getDaoFactory(), session, getRelationshipService(),
                getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker,
                historyCreator);
    }

    @Override
    public IEntityTypeBO createEntityTypeBO(Session session)
    {
        return new EntityTypeBO(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public IMaterialBO createMaterialBO(Session session)
    {
        return new MaterialBO(getDaoFactory(), session, entityInformationProvider, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService, historyCreator);
    }

    @Override
    public IAuthorizationGroupBO createAuthorizationGroupBO(Session session)
    {
        return new AuthorizationGroupBO(getDaoFactory(), session,
                getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public IGridCustomFilterOrColumnBO createGridCustomFilterBO(Session session)
    {
        return new GridCustomFilterBO(getDaoFactory(), session,
                getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public IGridCustomFilterOrColumnBO createGridCustomColumnBO(Session session)
    {
        return new GridCustomColumnBO(getDaoFactory(), session,
                getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public ITrashBO createTrashBO(Session session)
    {
        return new TrashBO(getDaoFactory(), this, session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public IDeletionTable createDeletionTable(Session session)
    {
        return new DeletionTable(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public ICorePluginTable createCorePluginTable(Session session,
            IMasterDataScriptRegistrationRunner masterDataScriptRunner)
    {
        return new CorePluginTable(getDaoFactory(), session, masterDataScriptRunner,
                getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public IDataStoreBO createDataStoreBO(Session session)
    {
        return new DataStoreBO(getDaoFactory(), session, getDSSFactory());
    }

    @Override
    public IMetaprojectBO createMetaprojectBO(Session session)
    {
        return new MetaprojectBO(getDaoFactory(), createExperimentBO(session),
                createSampleBO(session), createDataBO(session), createMaterialBO(session), session,
                getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public ITypeGroupBO createTypeGroupBO(Session session)
    {
        return new TypeGroupBO(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }

    @Override
    public ITypeGroupAssignmentTable createTypeGroupAssignmentTable(Session session)
    {
        return new TypeGroupAssignmentTable(getDaoFactory(), session, getManagedPropertyEvaluatorFactory(),
                dataSetTypeWithoutExperimentChecker, relationshipService);
    }
}
