/*
 *  Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.openbis.generic;

import static ch.ethz.sis.afsclient.client.ChunkEncoderDecoder.EMPTY_ARRAY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.PathContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import ch.ethz.sis.afsapi.api.ClientAPI;
import ch.ethz.sis.afsapi.api.OperationsAPI;
import ch.ethz.sis.afsapi.api.PublicAPI;
import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsclient.client.AfsClient;
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.ITransactionCoordinatorApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.AuthorizationGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.create.AuthorizationGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.delete.AuthorizationGroupDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.fetchoptions.AuthorizationGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.id.AuthorizationGroupPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.id.IAuthorizationGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.search.AuthorizationGroupSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.authorizationgroup.update.AuthorizationGroupUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.TableModel;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSetType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.archive.DataSetArchiveOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.create.DataSetTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.delete.DataSetDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.delete.DataSetTypeDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.lock.DataSetLockOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.unarchive.DataSetUnarchiveOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.unlock.DataSetUnlockOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.update.DataSetUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.DataStore;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.fetchoptions.DataStoreFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.Deletion;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.fetchoptions.DeletionFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.id.IDeletionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.search.DeletionSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.EntityKind;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.IEntityTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.Event;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.fetchoptions.EventFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.event.search.EventSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.ExperimentType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.delete.ExperimentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.delete.ExperimentTypeDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.IExperimentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.update.ExperimentUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.ExportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.data.ExportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.exporter.options.ExportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.ExternalDms;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.create.ExternalDmsCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.delete.ExternalDmsDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.fetchoptions.ExternalDmsFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.id.ExternalDmsPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.id.IExternalDmsId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.search.ExternalDmsSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.externaldms.update.ExternalDmsUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.global.GlobalSearchObject;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.global.fetchoptions.GlobalSearchObjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.global.search.GlobalSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.ImportResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data.ImportData;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options.ImportOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.Material;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.MaterialType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.create.MaterialCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.create.MaterialTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.delete.MaterialDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.delete.MaterialTypeDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.fetchoptions.MaterialFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.fetchoptions.MaterialTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.id.IMaterialId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.id.MaterialPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.search.MaterialSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.search.MaterialTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.update.MaterialTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.material.update.MaterialUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.objectkindmodification.ObjectKindModification;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.objectkindmodification.fetchoptions.ObjectKindModificationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.objectkindmodification.search.ObjectKindModificationSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.IOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.IOperationExecutionResults;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.OperationExecution;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.delete.OperationExecutionDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.fetchoptions.OperationExecutionFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.id.IOperationExecutionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.search.OperationExecutionSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.update.OperationExecutionUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.PersonalAccessToken;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.create.PersonalAccessTokenCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.delete.PersonalAccessTokenDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.fetchoptions.PersonalAccessTokenFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.id.IPersonalAccessTokenId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.id.PersonalAccessTokenPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.search.PersonalAccessTokenSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.update.PersonalAccessTokenUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.Person;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.create.PersonCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.delete.PersonDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.fetchoptions.PersonFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.IPersonId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.search.PersonSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.update.PersonUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.Plugin;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.create.PluginCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.delete.PluginDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.evaluate.PluginEvaluationOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.evaluate.PluginEvaluationResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.fetchoptions.PluginFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.IPluginId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.id.PluginPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.search.PluginSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.plugin.update.PluginUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.delete.ProjectDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.IProjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.search.ProjectSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.update.ProjectUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.PropertyType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.create.PropertyTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.delete.PropertyTypeDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.fetchoptions.PropertyTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.IPropertyTypeId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyAssignmentPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.id.PropertyTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.search.PropertyAssignmentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.search.PropertyTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.property.update.PropertyTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.Query;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.QueryDatabase;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.create.QueryCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.delete.QueryDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.execute.QueryExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.execute.SqlExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.fetchoptions.QueryDatabaseFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.fetchoptions.QueryFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.id.IQueryDatabaseId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.id.IQueryId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.id.QueryTechId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.search.QueryDatabaseSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.search.QuerySearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.query.update.QueryUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.rights.Rights;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.rights.fetchoptions.RightsFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.RoleAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.create.RoleAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.delete.RoleAssignmentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.fetchoptions.RoleAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.id.IRoleAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.id.RoleAssignmentTechId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.roleassignment.search.RoleAssignmentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleTypeCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleTypeDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleTypeFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.ISampleId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SamplePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleTypeSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleTypeUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.SemanticAnnotation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.create.SemanticAnnotationCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.delete.SemanticAnnotationDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.fetchoptions.SemanticAnnotationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.id.ISemanticAnnotationId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.id.SemanticAnnotationPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.search.SemanticAnnotationSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.semanticannotation.update.SemanticAnnotationUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.server.ServerInformation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.AggregationService;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.CustomASService;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.CustomASServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.ProcessingService;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.ReportingService;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.SearchDomainService;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.SearchDomainServiceExecutionResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.execute.AggregationServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.execute.ProcessingServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.execute.ReportingServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.execute.SearchDomainServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.fetchoptions.AggregationServiceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.fetchoptions.CustomASServiceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.fetchoptions.ProcessingServiceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.fetchoptions.ReportingServiceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.fetchoptions.SearchDomainServiceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id.ICustomASServiceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id.IDssServiceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.search.AggregationServiceSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.search.CustomASServiceSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.search.ProcessingServiceSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.search.ReportingServiceSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.search.SearchDomainServiceSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.fetchoptions.SessionInformationFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.search.SessionInformationSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.create.SpaceCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.delete.SpaceDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.ISpaceId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.update.SpaceUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.Tag;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.create.TagCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.delete.TagDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.fetchoptions.TagFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.id.ITagId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.id.TagPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.search.TagSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.tag.update.TagUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.Vocabulary;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.create.VocabularyCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.create.VocabularyTermCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.delete.VocabularyDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.delete.VocabularyTermDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.IVocabularyId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.IVocabularyTermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.id.VocabularyTermPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularySearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.update.VocabularyTermUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.update.VocabularyUpdate;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.dataset.create.FullDataSetCreation;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.dataset.create.UploadedDataSetCreation;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fastdownload.FastDownloadSession;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fastdownload.FastDownloadSessionOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.IDataSetFileId;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.service.CustomDSSServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.service.id.ICustomDSSServiceId;
import ch.ethz.sis.openbis.generic.excel.v3.from.ExcelReader;
import ch.ethz.sis.openbis.generic.excel.v3.model.OpenBisModel;
import ch.ethz.sis.openbis.generic.excel.v3.to.ExcelWriter;
import ch.systemsx.cisd.common.exceptions.UserFailureException;
import ch.systemsx.cisd.common.http.JettyHttpClientFactory;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;

public class OpenBIS
{

    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 30000; //30 seconds

    private static final int CHUNK_SIZE = 1048576; // 1 MiB

    private final ITransactionCoordinatorApi transactionCoordinator;

    private final IApplicationServerApi asFacadeNoTransactions;

    private final IApplicationServerApi asFacadeWithTransactions;

    private final IDataStoreServerApi dssFacade;

    private final AfsClient afsClientNoTransactions;

    private final AfsClient afsClientWithTransactions;

    private String interactiveSessionKey;

    private String sessionToken;

    private UUID transactionId;

    private final String asURL;

    private final String dssURL;

    private final String afsURL;

    public OpenBIS(final String url)
    {
        this(url, DEFAULT_TIMEOUT_IN_MILLIS);
    }

    public OpenBIS(final String url, final int timeout)
    {
        this(url + "/openbis/openbis", url + "/datastore_server", url + "/afs_server", timeout);
    }

    public OpenBIS(final String asURL, final String dssURL, final String afsURL)
    {
        this(asURL, dssURL, afsURL, DEFAULT_TIMEOUT_IN_MILLIS);
    }

    public OpenBIS(final String asURL, final String dssURL, final String afsURL, final int timeout)
    {
        this(asURL, dssURL, afsURL, timeout, CHUNK_SIZE);
    }

    public OpenBIS(final String asURL, final String dssURL, final String afsURL, final int timeout, final int chunkSize)
    {
        this.transactionCoordinator =
                HttpInvokerUtils.createServiceStub(ITransactionCoordinatorApi.class, asURL + ITransactionCoordinatorApi.SERVICE_URL, timeout);
        this.asFacadeNoTransactions =
                HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, asURL + IApplicationServerApi.SERVICE_URL, timeout);
        this.asFacadeWithTransactions =
                createTransactionalProxy(ITransactionCoordinatorApi.APPLICATION_SERVER_PARTICIPANT_ID, IApplicationServerApi.class,
                        asFacadeNoTransactions);
        this.dssFacade = HttpInvokerUtils.createServiceStub(IDataStoreServerApi.class, dssURL + IDataStoreServerApi.SERVICE_URL, timeout);

        if (afsURL != null)
        {
            this.afsClientNoTransactions = new AfsClient(URI.create(afsURL), chunkSize, timeout);
            this.afsClientWithTransactions =
                    new AfsClient(createTransactionalProxy(ITransactionCoordinatorApi.AFS_SERVER_PARTICIPANT_ID, PublicAPI.class,
                            afsClientNoTransactions), chunkSize, timeout);
        } else
        {
            this.afsClientNoTransactions = null;
            this.afsClientWithTransactions = null;
        }

        this.asURL = asURL;
        this.dssURL = dssURL;
        this.afsURL = afsURL;
    }

    //
    // AS Facade methods
    //

    public String login(String userId, String password)
    {
        checkTransactionDoesNotExist();
        String sessionToken = asFacadeNoTransactions.login(userId, password);
        setSessionToken(sessionToken);
        return sessionToken;
    }

    public String loginAs(String userId, String password, String asUserId)
    {
        checkTransactionDoesNotExist();
        String sessionToken = asFacadeNoTransactions.loginAs(userId, password, asUserId);
        setSessionToken(sessionToken);
        return sessionToken;
    }

    public String loginAsAnonymousUser()
    {
        checkTransactionDoesNotExist();
        String sessionToken = asFacadeNoTransactions.loginAsAnonymousUser();
        setSessionToken(sessionToken);
        return sessionToken;
    }

    public void logout()
    {
        checkTransactionDoesNotExist();
        asFacadeNoTransactions.logout(sessionToken);
    }

    public SessionInformation getSessionInformation()
    {
        return asFacadeNoTransactions.getSessionInformation(sessionToken);
    }

    public boolean isSessionActive()
    {
        return asFacadeNoTransactions.isSessionActive(sessionToken);
    }

    public UUID beginTransaction()
    {
        checkTransactionDoesNotExist();

        if (sessionToken == null)
        {
            throw new IllegalStateException("Session token hasn't been set");
        }

        if (interactiveSessionKey == null)
        {
            throw new IllegalStateException("Interactive session token hasn't been set");
        }

        transactionId = UUID.randomUUID();
        transactionCoordinator.beginTransaction(transactionId, sessionToken, interactiveSessionKey);
        return transactionId;
    }

    public void commitTransaction()
    {
        checkTransactionExists();

        if (sessionToken == null)
        {
            throw new IllegalStateException("Session token hasn't been set");
        }

        if (interactiveSessionKey == null)
        {
            throw new IllegalStateException("Interactive session token hasn't been set");
        }

        transactionCoordinator.commitTransaction(transactionId, sessionToken, interactiveSessionKey);
        transactionId = null;
    }

    public void rollbackTransaction()
    {
        checkTransactionExists();

        if (sessionToken == null)
        {
            throw new IllegalStateException("Session token hasn't been set");
        }

        if (interactiveSessionKey == null)
        {
            throw new IllegalStateException("Interactive session token hasn't been set");
        }

        transactionCoordinator.rollbackTransaction(transactionId, sessionToken, interactiveSessionKey);
        transactionId = null;
    }

    public List<SpacePermId> createSpaces(List<SpaceCreation> newSpaces)
    {
        return asFacadeWithTransactions.createSpaces(sessionToken, newSpaces);
    }

    public List<ProjectPermId> createProjects(List<ProjectCreation> newProjects)
    {
        return asFacadeWithTransactions.createProjects(sessionToken, newProjects);
    }

    public List<ExperimentPermId> createExperiments(List<ExperimentCreation> newExperiments)
    {
        return asFacadeWithTransactions.createExperiments(sessionToken, newExperiments);
    }

    public List<EntityTypePermId> createExperimentTypes(List<ExperimentTypeCreation> newExperimentTypes)
    {
        return asFacadeWithTransactions.createExperimentTypes(sessionToken, newExperimentTypes);
    }

    public List<SamplePermId> createSamples(List<SampleCreation> newSamples)
    {
        return asFacadeWithTransactions.createSamples(sessionToken, newSamples);
    }

    public List<EntityTypePermId> createSampleTypes(List<SampleTypeCreation> newSampleTypes)
    {
        return asFacadeWithTransactions.createSampleTypes(sessionToken, newSampleTypes);
    }

    public List<DataSetPermId> createDataSetsAS(List<DataSetCreation> newDataSets)
    {
        return asFacadeWithTransactions.createDataSets(sessionToken, newDataSets);
    }

    public List<EntityTypePermId> createDataSetTypes(List<DataSetTypeCreation> newDataSetTypes)
    {
        return asFacadeWithTransactions.createDataSetTypes(sessionToken, newDataSetTypes);
    }

    public List<MaterialPermId> createMaterials(List<MaterialCreation> newMaterials)
    {
        return asFacadeWithTransactions.createMaterials(sessionToken, newMaterials);
    }

    public List<EntityTypePermId> createMaterialTypes(List<MaterialTypeCreation> newMaterialTypes)
    {
        return asFacadeWithTransactions.createMaterialTypes(sessionToken, newMaterialTypes);
    }

    public List<PropertyTypePermId> createPropertyTypes(List<PropertyTypeCreation> newPropertyTypes)
    {
        return asFacadeWithTransactions.createPropertyTypes(sessionToken, newPropertyTypes);
    }

    public List<PluginPermId> createPlugins(List<PluginCreation> newPlugins)
    {
        return asFacadeWithTransactions.createPlugins(sessionToken, newPlugins);
    }

    public List<VocabularyPermId> createVocabularies(List<VocabularyCreation> newVocabularies)
    {
        return asFacadeWithTransactions.createVocabularies(sessionToken, newVocabularies);
    }

    public List<VocabularyTermPermId> createVocabularyTerms(List<VocabularyTermCreation> newVocabularyTerms)
    {
        return asFacadeWithTransactions.createVocabularyTerms(sessionToken, newVocabularyTerms);
    }

    public List<TagPermId> createTags(List<TagCreation> newTags)
    {
        return asFacadeWithTransactions.createTags(sessionToken, newTags);
    }

    public List<AuthorizationGroupPermId> createAuthorizationGroups(List<AuthorizationGroupCreation> newAuthorizationGroups)
    {
        return asFacadeWithTransactions.createAuthorizationGroups(sessionToken, newAuthorizationGroups);
    }

    public List<RoleAssignmentTechId> createRoleAssignments(List<RoleAssignmentCreation> newRoleAssignments)
    {
        return asFacadeWithTransactions.createRoleAssignments(sessionToken, newRoleAssignments);
    }

    public List<PersonPermId> createPersons(List<PersonCreation> newPersons)
    {
        return asFacadeWithTransactions.createPersons(sessionToken, newPersons);
    }

    public List<ExternalDmsPermId> createExternalDataManagementSystems(List<ExternalDmsCreation> newExternalDataManagementSystems)
    {
        return asFacadeWithTransactions.createExternalDataManagementSystems(sessionToken, newExternalDataManagementSystems);
    }

    public List<QueryTechId> createQueries(List<QueryCreation> newQueries)
    {
        return asFacadeWithTransactions.createQueries(sessionToken, newQueries);
    }

    public List<SemanticAnnotationPermId> createSemanticAnnotations(List<SemanticAnnotationCreation> newAnnotations)
    {
        return asFacadeWithTransactions.createSemanticAnnotations(sessionToken, newAnnotations);
    }

    public List<PersonalAccessTokenPermId> createPersonalAccessTokens(List<PersonalAccessTokenCreation> newPersonalAccessTokens)
    {
        return asFacadeWithTransactions.createPersonalAccessTokens(sessionToken, newPersonalAccessTokens);
    }

    public List<TypeGroupId> createTypeGroups(String sessionToken, List<TypeGroupCreation> newTypeGroups)
    {
        return asFacadeWithTransactions.createTypeGroups(sessionToken, newTypeGroups);
    }

    public void updateSpaces(List<SpaceUpdate> spaceUpdates)
    {
        asFacadeWithTransactions.updateSpaces(sessionToken, spaceUpdates);
    }

    public void updateProjects(List<ProjectUpdate> projectUpdates)
    {
        asFacadeWithTransactions.updateProjects(sessionToken, projectUpdates);
    }

    public void updateExperiments(List<ExperimentUpdate> experimentUpdates)
    {
        asFacadeWithTransactions.updateExperiments(sessionToken, experimentUpdates);
    }

    public void updateExperimentTypes(List<ExperimentTypeUpdate> experimentTypeUpdates)
    {
        asFacadeWithTransactions.updateExperimentTypes(sessionToken, experimentTypeUpdates);
    }

    public void updateSamples(List<SampleUpdate> sampleUpdates)
    {
        asFacadeWithTransactions.updateSamples(sessionToken, sampleUpdates);
    }

    public void updateSampleTypes(List<SampleTypeUpdate> sampleTypeUpdates)
    {
        asFacadeWithTransactions.updateSampleTypes(sessionToken, sampleTypeUpdates);
    }

    public void updateDataSets(List<DataSetUpdate> dataSetUpdates)
    {
        asFacadeWithTransactions.updateDataSets(sessionToken, dataSetUpdates);
    }

    public void updateDataSetTypes(List<DataSetTypeUpdate> dataSetTypeUpdates)
    {
        asFacadeWithTransactions.updateDataSetTypes(sessionToken, dataSetTypeUpdates);
    }

    public void updateMaterials(List<MaterialUpdate> materialUpdates)
    {
        asFacadeWithTransactions.updateMaterials(sessionToken, materialUpdates);
    }

    public void updateMaterialTypes(List<MaterialTypeUpdate> materialTypeUpdates)
    {
        asFacadeWithTransactions.updateMaterialTypes(sessionToken, materialTypeUpdates);
    }

    public void updateExternalDataManagementSystems(List<ExternalDmsUpdate> externalDmsUpdates)
    {
        asFacadeWithTransactions.updateExternalDataManagementSystems(sessionToken, externalDmsUpdates);
    }

    public void updatePropertyTypes(List<PropertyTypeUpdate> propertyTypeUpdates)
    {
        asFacadeWithTransactions.updatePropertyTypes(sessionToken, propertyTypeUpdates);
    }

    public void updatePlugins(List<PluginUpdate> pluginUpdates)
    {
        asFacadeWithTransactions.updatePlugins(sessionToken, pluginUpdates);
    }

    public void updateVocabularies(List<VocabularyUpdate> vocabularyUpdates)
    {
        asFacadeWithTransactions.updateVocabularies(sessionToken, vocabularyUpdates);
    }

    public void updateVocabularyTerms(List<VocabularyTermUpdate> vocabularyTermUpdates)
    {
        asFacadeWithTransactions.updateVocabularyTerms(sessionToken, vocabularyTermUpdates);
    }

    public void updateTags(List<TagUpdate> tagUpdates)
    {
        asFacadeWithTransactions.updateTags(sessionToken, tagUpdates);
    }

    public void updateAuthorizationGroups(List<AuthorizationGroupUpdate> authorizationGroupUpdates)
    {
        asFacadeWithTransactions.updateAuthorizationGroups(sessionToken, authorizationGroupUpdates);
    }

    public void updatePersons(List<PersonUpdate> personUpdates)
    {
        asFacadeWithTransactions.updatePersons(sessionToken, personUpdates);
    }

    public void updateOperationExecutions(List<OperationExecutionUpdate> executionUpdates)
    {
        asFacadeWithTransactions.updateOperationExecutions(sessionToken, executionUpdates);
    }

    public void updateSemanticAnnotations(List<SemanticAnnotationUpdate> annotationUpdates)
    {
        asFacadeWithTransactions.updateSemanticAnnotations(sessionToken, annotationUpdates);
    }

    public void updateQueries(List<QueryUpdate> queryUpdates)
    {
        asFacadeWithTransactions.updateQueries(sessionToken, queryUpdates);
    }

    public void updatePersonalAccessTokens(List<PersonalAccessTokenUpdate> personalAccessTokenUpdates)
    {
        asFacadeWithTransactions.updatePersonalAccessTokens(sessionToken, personalAccessTokenUpdates);
    }

    public Map<IObjectId, Rights> getRights(List<? extends IObjectId> ids, RightsFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getRights(sessionToken, ids, fetchOptions);
    }

    public Map<ISpaceId, Space> getSpaces(List<? extends ISpaceId> spaceIds, SpaceFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getSpaces(sessionToken, spaceIds, fetchOptions);
    }

    public Map<IProjectId, Project> getProjects(List<? extends IProjectId> projectIds, ProjectFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getProjects(sessionToken, projectIds, fetchOptions);
    }

    public Map<IExperimentId, Experiment> getExperiments(List<? extends IExperimentId> experimentIds, ExperimentFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getExperiments(sessionToken, experimentIds, fetchOptions);
    }

    public Map<IEntityTypeId, ExperimentType> getExperimentTypes(List<? extends IEntityTypeId> experimentTypeIds,
            ExperimentTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getExperimentTypes(sessionToken, experimentTypeIds, fetchOptions);
    }

    public Map<ISampleId, Sample> getSamples(List<? extends ISampleId> sampleIds, SampleFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getSamples(sessionToken, sampleIds, fetchOptions);
    }

    public Map<IEntityTypeId, SampleType> getSampleTypes(List<? extends IEntityTypeId> sampleTypeIds, SampleTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getSampleTypes(sessionToken, sampleTypeIds, fetchOptions);
    }

    public Map<IDataSetId, DataSet> getDataSets(List<? extends IDataSetId> dataSetIds, DataSetFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getDataSets(sessionToken, dataSetIds, fetchOptions);
    }

    public Map<IEntityTypeId, DataSetType> getDataSetTypes(List<? extends IEntityTypeId> dataSetTypeIds, DataSetTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getDataSetTypes(sessionToken, dataSetTypeIds, fetchOptions);
    }

    public Map<IMaterialId, Material> getMaterials(List<? extends IMaterialId> materialIds, MaterialFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getMaterials(sessionToken, materialIds, fetchOptions);
    }

    public Map<IEntityTypeId, MaterialType> getMaterialTypes(List<? extends IEntityTypeId> materialTypeIds, MaterialTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getMaterialTypes(sessionToken, materialTypeIds, fetchOptions);
    }

    public Map<IPropertyTypeId, PropertyType> getPropertyTypes(List<? extends IPropertyTypeId> typeIds, PropertyTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getPropertyTypes(sessionToken, typeIds, fetchOptions);
    }

    public Map<IPluginId, Plugin> getPlugins(List<? extends IPluginId> pluginIds, PluginFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getPlugins(sessionToken, pluginIds, fetchOptions);
    }

    public Map<IVocabularyId, Vocabulary> getVocabularies(List<? extends IVocabularyId> vocabularyIds, VocabularyFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getVocabularies(sessionToken, vocabularyIds, fetchOptions);
    }

    public Map<IVocabularyTermId, VocabularyTerm> getVocabularyTerms(List<? extends IVocabularyTermId> vocabularyTermIds,
            VocabularyTermFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getVocabularyTerms(sessionToken, vocabularyTermIds, fetchOptions);
    }

    public Map<ITagId, Tag> getTags(List<? extends ITagId> tagIds, TagFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getTags(sessionToken, tagIds, fetchOptions);
    }

    public Map<IAuthorizationGroupId, AuthorizationGroup> getAuthorizationGroups(List<? extends IAuthorizationGroupId> groupIds,
            AuthorizationGroupFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getAuthorizationGroups(sessionToken, groupIds, fetchOptions);
    }

    public Map<IRoleAssignmentId, RoleAssignment> getRoleAssignments(List<? extends IRoleAssignmentId> ids, RoleAssignmentFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getRoleAssignments(sessionToken, ids, fetchOptions);
    }

    public Map<IPersonId, Person> getPersons(List<? extends IPersonId> ids, PersonFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getPersons(sessionToken, ids, fetchOptions);
    }

    public Map<IExternalDmsId, ExternalDms> getExternalDataManagementSystems(List<? extends IExternalDmsId> externalDmsIds,
            ExternalDmsFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getExternalDataManagementSystems(sessionToken, externalDmsIds, fetchOptions);
    }

    public Map<ISemanticAnnotationId, SemanticAnnotation> getSemanticAnnotations(List<? extends ISemanticAnnotationId> annotationIds,
            SemanticAnnotationFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getSemanticAnnotations(sessionToken, annotationIds, fetchOptions);
    }

    public Map<IOperationExecutionId, OperationExecution> getOperationExecutions(List<? extends IOperationExecutionId> executionIds,
            OperationExecutionFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getOperationExecutions(sessionToken, executionIds, fetchOptions);
    }

    public Map<IQueryId, Query> getQueries(List<? extends IQueryId> queryIds, QueryFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getQueries(sessionToken, queryIds, fetchOptions);
    }

    public Map<IQueryDatabaseId, QueryDatabase> getQueryDatabases(List<? extends IQueryDatabaseId> queryDatabaseIds,
            QueryDatabaseFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getQueryDatabases(sessionToken, queryDatabaseIds, fetchOptions);
    }

    public Map<IPersonalAccessTokenId, PersonalAccessToken> getPersonalAccessTokens(List<? extends IPersonalAccessTokenId> personalAccessTokenIds,
            PersonalAccessTokenFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.getPersonalAccessTokens(sessionToken, personalAccessTokenIds, fetchOptions);
    }

    public SearchResult<Space> searchSpaces(SpaceSearchCriteria searchCriteria, SpaceFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchSpaces(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Project> searchProjects(ProjectSearchCriteria searchCriteria, ProjectFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchProjects(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Experiment> searchExperiments(ExperimentSearchCriteria searchCriteria, ExperimentFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchExperiments(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<ExperimentType> searchExperimentTypes(ExperimentTypeSearchCriteria searchCriteria, ExperimentTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchExperimentTypes(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Sample> searchSamples(SampleSearchCriteria searchCriteria, SampleFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchSamples(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<SampleType> searchSampleTypes(SampleTypeSearchCriteria searchCriteria, SampleTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchSampleTypes(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<DataSet> searchDataSets(DataSetSearchCriteria searchCriteria, DataSetFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchDataSets(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<DataSetType> searchDataSetTypes(DataSetTypeSearchCriteria searchCriteria, DataSetTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchDataSetTypes(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Material> searchMaterials(MaterialSearchCriteria searchCriteria, MaterialFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchMaterials(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<ExternalDms> searchExternalDataManagementSystems(ExternalDmsSearchCriteria searchCriteria,
            ExternalDmsFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchExternalDataManagementSystems(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<MaterialType> searchMaterialTypes(MaterialTypeSearchCriteria searchCriteria, MaterialTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchMaterialTypes(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Plugin> searchPlugins(PluginSearchCriteria searchCriteria, PluginFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchPlugins(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Vocabulary> searchVocabularies(VocabularySearchCriteria searchCriteria, VocabularyFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchVocabularies(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<VocabularyTerm> searchVocabularyTerms(VocabularyTermSearchCriteria searchCriteria, VocabularyTermFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchVocabularyTerms(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Tag> searchTags(TagSearchCriteria searchCriteria, TagFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchTags(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<AuthorizationGroup> searchAuthorizationGroups(AuthorizationGroupSearchCriteria searchCriteria,
            AuthorizationGroupFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchAuthorizationGroups(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<RoleAssignment> searchRoleAssignments(RoleAssignmentSearchCriteria searchCriteria, RoleAssignmentFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchRoleAssignments(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Person> searchPersons(PersonSearchCriteria searchCriteria, PersonFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchPersons(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<CustomASService> searchCustomASServices(CustomASServiceSearchCriteria searchCriteria,
            CustomASServiceFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchCustomASServices(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<SearchDomainService> searchSearchDomainServices(SearchDomainServiceSearchCriteria searchCriteria,
            SearchDomainServiceFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchSearchDomainServices(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<AggregationService> searchAggregationServices(AggregationServiceSearchCriteria searchCriteria,
            AggregationServiceFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchAggregationServices(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<ReportingService> searchReportingServices(ReportingServiceSearchCriteria searchCriteria,
            ReportingServiceFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchReportingServices(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<ProcessingService> searchProcessingServices(ProcessingServiceSearchCriteria searchCriteria,
            ProcessingServiceFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchProcessingServices(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<ObjectKindModification> searchObjectKindModifications(ObjectKindModificationSearchCriteria searchCriteria,
            ObjectKindModificationFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchObjectKindModifications(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<GlobalSearchObject> searchGlobally(GlobalSearchCriteria searchCriteria, GlobalSearchObjectFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchGlobally(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<OperationExecution> searchOperationExecutions(OperationExecutionSearchCriteria searchCriteria,
            OperationExecutionFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchOperationExecutions(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<DataStore> searchDataStores(DataStoreSearchCriteria searchCriteria, DataStoreFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchDataStores(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<SemanticAnnotation> searchSemanticAnnotations(SemanticAnnotationSearchCriteria searchCriteria,
            SemanticAnnotationFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchSemanticAnnotations(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<PropertyType> searchPropertyTypes(PropertyTypeSearchCriteria searchCriteria, PropertyTypeFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchPropertyTypes(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<PropertyAssignment> searchPropertyAssignments(PropertyAssignmentSearchCriteria searchCriteria,
            PropertyAssignmentFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchPropertyAssignments(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Query> searchQueries(QuerySearchCriteria searchCriteria, QueryFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchQueries(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<QueryDatabase> searchQueryDatabases(QueryDatabaseSearchCriteria searchCriteria, QueryDatabaseFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchQueryDatabases(sessionToken, searchCriteria, fetchOptions);
    }

    public void deleteSpaces(List<? extends ISpaceId> spaceIds, SpaceDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteSpaces(sessionToken, spaceIds, deletionOptions);
    }

    public void deleteProjects(List<? extends IProjectId> projectIds, ProjectDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteProjects(sessionToken, projectIds, deletionOptions);
    }

    public IDeletionId deleteExperiments(List<? extends IExperimentId> experimentIds, ExperimentDeletionOptions deletionOptions)
    {
        return asFacadeWithTransactions.deleteExperiments(sessionToken, experimentIds, deletionOptions);
    }

    public IDeletionId deleteSamples(List<? extends ISampleId> sampleIds, SampleDeletionOptions deletionOptions)
    {
        return asFacadeWithTransactions.deleteSamples(sessionToken, sampleIds, deletionOptions);
    }

    public IDeletionId deleteDataSets(List<? extends IDataSetId> dataSetIds, DataSetDeletionOptions deletionOptions)
    {
        return asFacadeWithTransactions.deleteDataSets(sessionToken, dataSetIds, deletionOptions);
    }

    public void deleteMaterials(List<? extends IMaterialId> materialIds, MaterialDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteMaterials(sessionToken, materialIds, deletionOptions);
    }

    public void deletePlugins(List<? extends IPluginId> pluginIds, PluginDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deletePlugins(sessionToken, pluginIds, deletionOptions);
    }

    public void deletePropertyTypes(List<? extends IPropertyTypeId> propertyTypeIds, PropertyTypeDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deletePropertyTypes(sessionToken, propertyTypeIds, deletionOptions);
    }

    public void deleteVocabularies(List<? extends IVocabularyId> ids, VocabularyDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteVocabularies(sessionToken, ids, deletionOptions);
    }

    public void deleteVocabularyTerms(List<? extends IVocabularyTermId> termIds, VocabularyTermDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteVocabularyTerms(sessionToken, termIds, deletionOptions);
    }

    public void deleteExperimentTypes(List<? extends IEntityTypeId> experimentTypeIds, ExperimentTypeDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteExperimentTypes(sessionToken, experimentTypeIds, deletionOptions);
    }

    public void deleteSampleTypes(List<? extends IEntityTypeId> sampleTypeIds, SampleTypeDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteSampleTypes(sessionToken, sampleTypeIds, deletionOptions);
    }

    public void deleteDataSetTypes(List<? extends IEntityTypeId> dataSetTypeIds, DataSetTypeDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteDataSetTypes(sessionToken, dataSetTypeIds, deletionOptions);
    }

    public void deleteMaterialTypes(List<? extends IEntityTypeId> materialTypeIds, MaterialTypeDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteMaterialTypes(sessionToken, materialTypeIds, deletionOptions);
    }

    public void deleteExternalDataManagementSystems(List<? extends IExternalDmsId> externalDmsIds, ExternalDmsDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteExternalDataManagementSystems(sessionToken, externalDmsIds, deletionOptions);
    }

    public void deleteTags(List<? extends ITagId> tagIds, TagDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteTags(sessionToken, tagIds, deletionOptions);
    }

    public void deleteAuthorizationGroups(List<? extends IAuthorizationGroupId> groupIds, AuthorizationGroupDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteAuthorizationGroups(sessionToken, groupIds, deletionOptions);
    }

    public void deleteRoleAssignments(List<? extends IRoleAssignmentId> assignmentIds, RoleAssignmentDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteRoleAssignments(sessionToken, assignmentIds, deletionOptions);
    }

    public void deleteOperationExecutions(List<? extends IOperationExecutionId> executionIds, OperationExecutionDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteOperationExecutions(sessionToken, executionIds, deletionOptions);
    }

    public void deleteSemanticAnnotations(List<? extends ISemanticAnnotationId> annotationIds, SemanticAnnotationDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteSemanticAnnotations(sessionToken, annotationIds, deletionOptions);
    }

    public void deleteQueries(List<? extends IQueryId> queryIds, QueryDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deleteQueries(sessionToken, queryIds, deletionOptions);
    }

    public void deletePersons(List<? extends IPersonId> personIds, PersonDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deletePersons(sessionToken, personIds, deletionOptions);
    }

    public void deletePersonalAccessTokens(List<? extends IPersonalAccessTokenId> personalAccessTokenIds,
            PersonalAccessTokenDeletionOptions deletionOptions)
    {
        asFacadeWithTransactions.deletePersonalAccessTokens(sessionToken, personalAccessTokenIds, deletionOptions);
    }

    public SearchResult<Deletion> searchDeletions(DeletionSearchCriteria searchCriteria, DeletionFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchDeletions(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<Event> searchEvents(EventSearchCriteria searchCriteria, EventFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchEvents(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<PersonalAccessToken> searchPersonalAccessTokens(PersonalAccessTokenSearchCriteria searchCriteria,
            PersonalAccessTokenFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchPersonalAccessTokens(sessionToken, searchCriteria, fetchOptions);
    }

    public SearchResult<SessionInformation> searchSessionInformation(SessionInformationSearchCriteria searchCriteria,
            SessionInformationFetchOptions fetchOptions)
    {
        return asFacadeWithTransactions.searchSessionInformation(sessionToken, searchCriteria, fetchOptions);
    }

    public void revertDeletions(List<? extends IDeletionId> deletionIds)
    {
        asFacadeWithTransactions.revertDeletions(sessionToken, deletionIds);
    }

    public void confirmDeletions(List<? extends IDeletionId> deletionIds)
    {
        asFacadeWithTransactions.confirmDeletions(sessionToken, deletionIds);
    }

    public Object executeCustomASService(ICustomASServiceId serviceId, CustomASServiceExecutionOptions options)
    {
        return asFacadeWithTransactions.executeCustomASService(sessionToken, serviceId, options);
    }

    public SearchResult<SearchDomainServiceExecutionResult> executeSearchDomainService(SearchDomainServiceExecutionOptions options)
    {
        return asFacadeWithTransactions.executeSearchDomainService(sessionToken, options);
    }

    public TableModel executeAggregationService(IDssServiceId serviceId, AggregationServiceExecutionOptions options)
    {
        return asFacadeWithTransactions.executeAggregationService(sessionToken, serviceId, options);
    }

    public TableModel executeReportingService(IDssServiceId serviceId, ReportingServiceExecutionOptions options)
    {
        return asFacadeWithTransactions.executeReportingService(sessionToken, serviceId, options);
    }

    public void executeProcessingService(IDssServiceId serviceId, ProcessingServiceExecutionOptions options)
    {
        asFacadeWithTransactions.executeProcessingService(sessionToken, serviceId, options);
    }

    public TableModel executeQuery(IQueryId queryId, QueryExecutionOptions options)
    {
        return asFacadeWithTransactions.executeQuery(sessionToken, queryId, options);
    }

    public TableModel executeSql(String sql, SqlExecutionOptions options)
    {
        return asFacadeWithTransactions.executeSql(sessionToken, sql, options);
    }

    public PluginEvaluationResult evaluatePlugin(PluginEvaluationOptions options)
    {
        return asFacadeWithTransactions.evaluatePlugin(sessionToken, options);
    }

    public void archiveDataSets(List<? extends IDataSetId> dataSetIds, DataSetArchiveOptions options)
    {
        asFacadeWithTransactions.archiveDataSets(sessionToken, dataSetIds, options);
    }

    public void unarchiveDataSets(List<? extends IDataSetId> dataSetIds, DataSetUnarchiveOptions options)
    {
        asFacadeWithTransactions.unarchiveDataSets(sessionToken, dataSetIds, options);
    }

    public void lockDataSets(List<? extends IDataSetId> dataSetIds, DataSetLockOptions options)
    {
        asFacadeWithTransactions.lockDataSets(sessionToken, dataSetIds, options);
    }

    public void unlockDataSets(List<? extends IDataSetId> dataSetIds, DataSetUnlockOptions options)
    {
        asFacadeWithTransactions.unlockDataSets(sessionToken, dataSetIds, options);
    }

    public IOperationExecutionResults executeOperations(String sessionToken, List<? extends IOperation> operations,
            IOperationExecutionOptions options)
    {
        return asFacadeWithTransactions.executeOperations(sessionToken, operations, options);
    }

    public Map<String, String> getServerInformation()
    {
        return asFacadeWithTransactions.getServerInformation(sessionToken);
    }

    public Map<String, String> getServerPublicInformation()
    {
        return asFacadeWithTransactions.getServerPublicInformation();
    }

    public List<String> createPermIdStrings(int count)
    {
        return asFacadeWithTransactions.createPermIdStrings(sessionToken, count);
    }

    public List<String> createCodes(String prefix, EntityKind entityKind, int count)
    {
        return asFacadeWithTransactions.createCodes(sessionToken, prefix, entityKind, count);
    }

    public ImportResult executeImport(ImportData importData, ImportOptions importOptions)
    {
        return asFacadeWithTransactions.executeImport(sessionToken, importData, importOptions);
    }

    public ExportResult executeExport(ExportData exportData, ExportOptions exportOptions)
    {
        return asFacadeWithTransactions.executeExport(sessionToken, exportData, exportOptions);
    }

    public String uploadToSessionWorkspace(final Path fileOrFolder)
    {
        if (transactionId != null)
        {
            throw new IllegalStateException("AS session workspace SHOULD NOT be used during transactions.");
        }

        String uploadId = UUID.randomUUID().toString() + "/" + fileOrFolder.getFileName().toString();

        try
        {
            HttpClient httpClient = JettyHttpClientFactory.getHttpClient();

            MultiPartContentProvider multiPart = new MultiPartContentProvider();
            multiPart.addFieldPart("sessionKeysNumber", new StringContentProvider("1"), null);
            multiPart.addFieldPart("sessionKey_0", new StringContentProvider("openbis-file-upload"), null);
            multiPart.addFilePart("openbis-file-upload", uploadId, new PathContentProvider(fileOrFolder), null);
            multiPart.addFieldPart("keepOriginalFileName", new StringContentProvider("True"), null);
            multiPart.addFieldPart("sessionID", new StringContentProvider(this.sessionToken), null);
            multiPart.close();

            ContentResponse response = httpClient.newRequest(this.asURL + "/upload")
                    .method(HttpMethod.POST)
                    .content(multiPart)
                    .send();

            final int status = response.getStatus();
            if (status != 200)
            {
                throw new IOException(response.getContentAsString());
            }
        } catch (final IOException | TimeoutException | InterruptedException | ExecutionException e)
        {
            throw new RuntimeException(e);
        }

        return uploadId;
    }

    //
    // DSS Facade methods
    //

    public DataStoreFacade getDataStoreFacade()
    {
        return new DataStoreFacade();
    }

    public class DataStoreFacade
    {

        public SearchResult<DataSetFile> searchFiles(DataSetFileSearchCriteria searchCriteria, DataSetFileFetchOptions fetchOptions)
        {
            checkTransactionsNotSupported();
            return dssFacade.searchFiles(sessionToken, searchCriteria, fetchOptions);
        }

        public InputStream downloadFiles(List<? extends IDataSetFileId> fileIds, DataSetFileDownloadOptions downloadOptions)
        {
            checkTransactionsNotSupported();
            return dssFacade.downloadFiles(sessionToken, fileIds, downloadOptions);
        }

        public FastDownloadSession createFastDownloadSession(List<? extends IDataSetFileId> fileIds, FastDownloadSessionOptions options)
        {
            checkTransactionsNotSupported();
            return dssFacade.createFastDownloadSession(sessionToken, fileIds, options);
        }

        public DataSetPermId createUploadedDataSet(final UploadedDataSetCreation newDataSet)
        {
            checkTransactionsNotSupported();
            return dssFacade.createUploadedDataSet(sessionToken, newDataSet);
        }

        public List<DataSetPermId> createDataSets(List<FullDataSetCreation> newDataSets)
        {
            checkTransactionsNotSupported();
            return dssFacade.createDataSets(sessionToken, newDataSets);
        }

        public Object executeCustomDSSService(ICustomDSSServiceId serviceId, CustomDSSServiceExecutionOptions options)
        {
            checkTransactionsNotSupported();
            return dssFacade.executeCustomDSSService(sessionToken, serviceId, options);
        }

        public String uploadToSessionWorkspace(final Path fileOrFolder)
        {
            checkTransactionsNotSupported();
            String uploadId = uploadFileWorkspaceDSSEmptyDir(UUID.randomUUID().toString());
            uploadFileWorkspaceDSS(fileOrFolder.toFile(), uploadId);
            return uploadId;
        }

        //
        // Internal Helper Methods to upload files to DSS Session Workspace
        //

        /**
         * Upload file or folder to the DSS SessionWorkspaceFileUploadServlet and return the ID to be used by createUploadedDataSet
         * This method hides the complexities of uploading a folder with many files and does the uploads in chunks.
         */
        private String uploadFileWorkspaceDSS(final File fileOrFolder, final String parentsOrNull)
        {
            if (fileOrFolder.exists() == false)
            {
                throw new UserFailureException("Path doesn't exist: " + fileOrFolder);
            }
            String fileNameOrFolderName = "";
            if (parentsOrNull != null)
            {
                fileNameOrFolderName = parentsOrNull + "/";
            }
            fileNameOrFolderName += fileOrFolder.getName();

            if (fileOrFolder.isDirectory())
            {
                uploadFileWorkspaceDSSEmptyDir(fileNameOrFolderName);
                for (File file : fileOrFolder.listFiles())
                {
                    uploadFileWorkspaceDSS(file, fileNameOrFolderName);
                }
            } else
            {
                uploadFileWorkspaceDSSFile(fileNameOrFolderName, fileOrFolder);
            }
            return fileNameOrFolderName;
        }

        private String uploadFileWorkspaceDSSEmptyDir(String pathToDir)
        {
            final org.eclipse.jetty.client.HttpClient client = JettyHttpClientFactory.getHttpClient();
            final Request httpRequest = client.newRequest(dssURL + "/session_workspace_file_upload")
                    .method(HttpMethod.POST);
            httpRequest.param("sessionID", sessionToken);
            httpRequest.param("id", "1");
            httpRequest.param("filename", pathToDir);
            httpRequest.param("startByte", Long.toString(0));
            httpRequest.param("endByte", Long.toString(0));
            httpRequest.param("size", Long.toString(0));
            httpRequest.param("emptyFolder", Boolean.TRUE.toString());

            try
            {
                final ContentResponse response = httpRequest.send();
                final int status = response.getStatus();
                if (status != 200)
                {
                    throw new IOException(response.getContentAsString());
                }
            } catch (final IOException | TimeoutException | InterruptedException | ExecutionException e)
            {
                throw new RuntimeException(e);
            }
            return pathToDir;
        }

        private String uploadFileWorkspaceDSSFile(String pathToFile, File file)
        {
            try
            {
                long start = 0;
                for (byte[] chunk : streamFile(file, CHUNK_SIZE))
                {
                    final long end = start + chunk.length;

                    final org.eclipse.jetty.client.HttpClient client = JettyHttpClientFactory.getHttpClient();
                    final Request httpRequest = client.newRequest(dssURL + "/session_workspace_file_upload")
                            .method(HttpMethod.POST);
                    httpRequest.param("sessionID", sessionToken);
                    httpRequest.param("id", "1");
                    httpRequest.param("filename", pathToFile);
                    httpRequest.param("startByte", Long.toString(start));
                    httpRequest.param("endByte", Long.toString(end));
                    httpRequest.param("size", Long.toString(file.length()));
                    httpRequest.content(new BytesContentProvider(chunk));
                    final ContentResponse response = httpRequest.send();
                    final int status = response.getStatus();
                    if (status != 200)
                    {
                        throw new IOException(response.getContentAsString());
                    }
                    start += CHUNK_SIZE;
                }
            } catch (final IOException | TimeoutException | InterruptedException | ExecutionException e)
            {
                throw new RuntimeException(e);
            }
            return pathToFile;
        }

        private Iterable<byte[]> streamFile(final File file, final int chunkSize) throws FileNotFoundException
        {
            final InputStream inputStream = new FileInputStream(file);

            return new Iterable<byte[]>()
            {
                @Override
                public Iterator<byte[]> iterator()
                {
                    return new Iterator<byte[]>()
                    {
                        public boolean hasMore = true;

                        public boolean hasNext()
                        {
                            return hasMore;
                        }

                        public byte[] next()
                        {
                            try
                            {
                                byte[] bytes = inputStream.readNBytes(chunkSize);
                                if (bytes.length < chunkSize)
                                {
                                    hasMore = false;
                                    inputStream.close();
                                }
                                return bytes;
                            } catch (final IOException e)
                            {
                                try
                                {
                                    inputStream.close();
                                } catch (final IOException ex)
                                {
                                    throw new RuntimeException(ex);
                                }
                                throw new RuntimeException(e);
                            }
                        }
                    };
                }
            };
        }

        private void checkTransactionsNotSupported()
        {
            if (transactionId != null)
            {
                throw new IllegalStateException("Transactions are not supported for data store methods.");
            }
        }

    }

    //
    // AFS Server methods
    //

    public AfsServerFacade getAfsServerFacade()
    {
        if (this.afsURL != null)
        {
            return new AfsServerFacade();
        } else
        {
            throw new IllegalArgumentException("Please specify AFS server url");
        }
    }

    public class AfsServerFacade implements OperationsAPI, ClientAPI
    {

        private AfsServerFacade()
        {
        }

        public ch.ethz.sis.afsapi.dto.File[] list(String owner, String source, Boolean recursively)
        {
            try
            {
                return afsClientWithTransactions.list(owner, source, recursively);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Chunk[] read(Chunk[] chunks) throws Exception
        {
            try
            {
                return afsClientWithTransactions.read(chunks);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public byte[] read(String owner, String source, Long offset, Integer limit)
        {
            try
            {
                Chunk[] chunksRequest = new Chunk[] { new Chunk(owner, source, offset, limit, EMPTY_ARRAY) };
                Chunk[] chunksResponse = afsClientWithTransactions.read(chunksRequest);
                return chunksResponse[0].getData();
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Boolean write(Chunk[] chunks) throws Exception
        {
            try
            {
                return afsClientWithTransactions.write(chunks);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Boolean write(String owner, String source, Long offset, byte[] data)
        {
            try
            {
                Chunk[] chunksRequest = new Chunk[] { new Chunk(owner, source, offset, data.length, data) };
                return afsClientWithTransactions.write(chunksRequest);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Boolean delete(String owner, String source)
        {
            try
            {
                return afsClientWithTransactions.delete(owner, source);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Boolean copy(String sourceOwner, String source, String targetOwner, String target)
        {
            try
            {
                return afsClientWithTransactions.copy(sourceOwner, source, targetOwner, target);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Boolean move(String sourceOwner, String source, String targetOwner, String target)
        {
            try
            {
                return afsClientWithTransactions.move(sourceOwner, source, targetOwner, target);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Boolean create(String owner, String source, Boolean directory)
        {
            try
            {
                return afsClientWithTransactions.create(owner, source, directory);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public FreeSpace free(String owner, String source)
        {
            try
            {
                return afsClientWithTransactions.free(owner, source);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Boolean upload(Path sourcePath, String destinationOwner,
                final Path destinationPath, FileCollisionListener fileCollisionListener,
                TransferMonitorListener transferMonitorListener) throws Exception
        {
            try
            {
                return afsClientWithTransactions.upload(sourcePath, destinationOwner, destinationPath, fileCollisionListener,
                        transferMonitorListener);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public Boolean download(String sourceOwner, Path sourcePath,
                Path destinationPath, FileCollisionListener fileCollisionListener,
                TransferMonitorListener transferMonitorListener) throws Exception
        {
            try
            {
                return afsClientWithTransactions.download(sourceOwner, sourcePath, destinationPath, fileCollisionListener, transferMonitorListener);
            } catch (RuntimeException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

    }

    //
    // Facade only methods
    //

    public String getSessionToken()
    {
        return sessionToken;
    }

    public void setSessionToken(final String sessionToken)
    {
        checkTransactionDoesNotExist();
        this.sessionToken = sessionToken;

        if (afsClientNoTransactions != null)
        {
            this.afsClientNoTransactions.setSessionToken(sessionToken);
        }
        if (afsClientWithTransactions != null)
        {
            this.afsClientWithTransactions.setSessionToken(sessionToken);
        }
    }

    public void setInteractiveSessionKey(final String interactiveSessionKey)
    {
        this.interactiveSessionKey = interactiveSessionKey;
    }

    public String getInteractiveSessionKey()
    {
        return interactiveSessionKey;
    }

    public void setTransactionId(UUID transactionId)
    {
        this.transactionId = transactionId;
    }

    public UUID getTransactionId()
    {
        return transactionId;
    }

    /**
     * This utility method returns a well managed personal access token, creating one if no one is found and renews it if is close to expiration.
     * Requires are real session token since it uses other methods.
     *
     * @throws UserFailureException in case of any problems
     */
    public PersonalAccessTokenPermId getManagedPersonalAccessToken(String sessionName)
    {
        final int SECONDS_PER_DAY = 24 * 60 * 60;

        // Obtain servers renewal information
        Map<String, String> information = asFacadeWithTransactions.getServerInformation(sessionToken);
        int personalAccessTokensRenewalPeriodInSeconds =
                Integer.parseInt(information.get(ServerInformation.PERSONAL_ACCESS_TOKENS_VALIDITY_WARNING_PERIOD));
        int personalAccessTokensRenewalPeriodInDays = personalAccessTokensRenewalPeriodInSeconds / SECONDS_PER_DAY;
        int personalAccessTokensMaxValidityPeriodInSeconds =
                Integer.parseInt(information.get(ServerInformation.PERSONAL_ACCESS_TOKENS_MAX_VALIDITY_PERIOD));
        int personalAccessTokensMaxValidityPeriodInDays = personalAccessTokensMaxValidityPeriodInSeconds / SECONDS_PER_DAY;

        // Obtain user id
        SessionInformation sessionInformation = asFacadeWithTransactions.getSessionInformation(sessionToken);

        // Search for PAT for this user and application
        // NOTE: Standard users only get their PAT but admins get all, filtering with the user solves this corner case
        PersonalAccessTokenSearchCriteria personalAccessTokenSearchCriteria = new PersonalAccessTokenSearchCriteria();
        personalAccessTokenSearchCriteria.withSessionName().thatEquals(sessionName);
        personalAccessTokenSearchCriteria.withOwner().withUserId().thatEquals(sessionInformation.getPerson().getUserId());

        SearchResult<PersonalAccessToken> personalAccessTokenSearchResult =
                asFacadeWithTransactions.searchPersonalAccessTokens(sessionToken, personalAccessTokenSearchCriteria,
                        new PersonalAccessTokenFetchOptions());
        PersonalAccessToken bestTokenFound = null;
        PersonalAccessTokenPermId bestTokenFoundPermId = null;

        // Obtain longer lasting application token
        for (PersonalAccessToken personalAccessToken : personalAccessTokenSearchResult.getObjects())
        {
            if (personalAccessToken.getValidToDate().after(new Date()))
            {
                if (bestTokenFound == null)
                {
                    bestTokenFound = personalAccessToken;
                } else if (personalAccessToken.getValidToDate().after(bestTokenFound.getValidToDate()))
                {
                    bestTokenFound = personalAccessToken;
                }
            }
        }

        // If best token doesn't exist, create
        if (bestTokenFound == null)
        {
            bestTokenFoundPermId = createManagedPersonalAccessToken(sessionName, personalAccessTokensMaxValidityPeriodInDays);
        }

        // If best token is going to expire in less than the warning period, renew
        Calendar renewalDate = Calendar.getInstance();
        renewalDate.add(Calendar.DAY_OF_MONTH, personalAccessTokensRenewalPeriodInDays);
        if (bestTokenFound != null && bestTokenFound.getValidToDate().before(renewalDate.getTime()))
        {
            bestTokenFoundPermId = createManagedPersonalAccessToken(sessionName, personalAccessTokensMaxValidityPeriodInDays);
        }

        // If we have not created or renewed, return current
        if (bestTokenFoundPermId == null)
        {
            bestTokenFoundPermId = bestTokenFound.getPermId();
        }

        return bestTokenFoundPermId;
    }

    /**
     * This utility method provides a simplified API to create subject semantic annotations
     */
    public static SemanticAnnotationCreation getSemanticSubjectCreation(EntityKind subjectEntityKind,
            String subjectClass,
            String subjectClassOntologyId,
            String subjectClassOntologyVersion,
            String subjectClassId)
    {
        SemanticAnnotationCreation semanticAnnotationCreation = new SemanticAnnotationCreation();
        // Subject: Type matching an ontology class
        semanticAnnotationCreation.setEntityTypeId(new EntityTypePermId(subjectClass, subjectEntityKind));
        // Ontology URL
        semanticAnnotationCreation.setPredicateOntologyId(subjectClassOntologyId);
        // Ontology Version URL
        semanticAnnotationCreation.setPredicateOntologyVersion(subjectClassOntologyVersion);
        // Ontology Class URL
        semanticAnnotationCreation.setPredicateAccessionId(subjectClassId);
        return semanticAnnotationCreation;
    }

    /**
     * This utility method provides a simplified API to create predicate semantic annotations
     */
    public static SemanticAnnotationCreation getSemanticPredicateWithSubjectCreation(EntityKind subjectEntityKind,
            String subjectClass,
            String predicateProperty,
            String predicatePropertyOntologyId,
            String predicatePropertyOntologyVersion,
            String predicatePropertyId)
    {
        SemanticAnnotationCreation semanticAnnotationCreation = new SemanticAnnotationCreation();
        // Subject: Type matching an ontology class
        // Predicate: Property matching an ontology class property
        semanticAnnotationCreation.setPropertyAssignmentId(new PropertyAssignmentPermId(
                new EntityTypePermId(subjectClass, subjectEntityKind),
                new PropertyTypePermId(predicateProperty)));
        // Ontology URL
        semanticAnnotationCreation.setPredicateOntologyId(predicatePropertyOntologyId);
        // Ontology Version URL
        semanticAnnotationCreation.setPredicateOntologyVersion(predicatePropertyOntologyVersion);
        // Ontology Property URL
        semanticAnnotationCreation.setPredicateAccessionId(predicatePropertyId);
        return semanticAnnotationCreation;
    }

    /**
     * This utility method provides a simplified API to create predicate semantic annotations
     */
    public static SemanticAnnotationCreation getSemanticPredicateCreation(String predicateProperty,
            String predicatePropertyOntologyId,
            String predicatePropertyOntologyVersion,
            String predicatePropertyId)
    {
        SemanticAnnotationCreation semanticAnnotationCreation = new SemanticAnnotationCreation();
        // Predicate: Property matching an ontology class property
        semanticAnnotationCreation.setPropertyTypeId(new PropertyTypePermId(predicateProperty));
        // Ontology URL
        semanticAnnotationCreation.setPredicateOntologyId(predicatePropertyOntologyId);
        // Ontology Version URL
        semanticAnnotationCreation.setPredicateOntologyVersion(predicatePropertyOntologyVersion);
        // Ontology Property URL
        semanticAnnotationCreation.setPredicateAccessionId(predicatePropertyId);
        return semanticAnnotationCreation;
    }

    //
    // Internal Helper methods to create personal access tokens
    //

    private PersonalAccessTokenPermId createManagedPersonalAccessToken(String applicationName,
            int personalAccessTokensMaxValidityPeriodInDays)
    {
        final long SECONDS_PER_DAY = 24 * 60 * 60;
        final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000;

        PersonalAccessTokenCreation creation = new PersonalAccessTokenCreation();
        creation.setSessionName(applicationName);
        creation.setValidFromDate(new Date(System.currentTimeMillis() - MILLIS_PER_DAY));
        creation.setValidToDate(new Date(System.currentTimeMillis() + MILLIS_PER_DAY * personalAccessTokensMaxValidityPeriodInDays));
        List<PersonalAccessTokenPermId> personalAccessTokens = asFacadeWithTransactions.createPersonalAccessTokens(sessionToken, List.of(creation));
        return personalAccessTokens.get(0);
    }

    private void checkTransactionDoesNotExist()
    {
        if (transactionId != null)
        {
            throw new IllegalStateException(
                    "Operation cannot be executed. Expected no active transactions, but found transaction '" + transactionId + "'.");
        }
    }

    private void checkTransactionExists()
    {
        if (transactionId == null)
        {
            throw new IllegalStateException("Operation cannot be executed. No active transaction found.");
        }
    }

    private <T> T createTransactionalProxy(String transactionParticipantId, Class<T> serviceInterface, T service)
    {
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[] { serviceInterface },
                (proxy, method, args) ->
                {
                    if (transactionId != null)
                    {
                        return transactionCoordinator.executeOperation(transactionId, sessionToken, interactiveSessionKey,
                                transactionParticipantId, method.getName(), args);
                    } else
                    {
                        try
                        {
                            return method.invoke(service, args);
                        } catch (InvocationTargetException e)
                        {
                            throw e.getTargetException();
                        }
                    }
                });
    }

    OpenBisModel readModelFromExcel(ExcelReader.Format inputFormat, Path inputFile)
            throws IOException
    {
        return ExcelReader.convert(inputFormat, inputFile);
    }

    byte[] createExcel(ExcelWriter.Format outputFormat, OpenBisModel openBisModel)
    {
        return ExcelWriter.convert(outputFormat, openBisModel);
    }

}
