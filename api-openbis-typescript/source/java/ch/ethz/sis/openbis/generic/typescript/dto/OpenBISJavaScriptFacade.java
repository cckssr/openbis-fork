package ch.ethz.sis.openbis.generic.typescript.dto;

import java.util.List;
import java.util.Map;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroupAssignment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupAssignmentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupAssignmentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.delete.TypeGroupDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupAssignmentId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.TypeGroupAssignmentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.search.TypeGroupSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.update.TypeGroupUpdate;
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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.typescript.TypeScriptMethod;
import ch.ethz.sis.openbis.generic.typescript.TypeScriptObject;

@TypeScriptObject("openbis")
public class OpenBISJavaScriptFacade implements IApplicationServerApi
{

    public OpenBISJavaScriptFacade()
    {
    }

    public OpenBISJavaScriptFacade(String openbisUrl)
    {
    }

    public OpenBISJavaScriptFacade(String openbisUrl, String afsUrl)
    {
    }

    @TypeScriptMethod(sessionToken = false, async = false)
    public OpenBISJavaScriptDSSFacade getDataStoreFacade()
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false, async = false)
    public OpenBISJavaScriptAFSFacade getAfsServerFacade()
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false, async = false)
    public OpenBISJavaScriptDSSFacade getDataStoreFacade(String[] dataStoreCodes)
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false)
    @Override public String login(final String userId, final String password)
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false)
    @Override public String loginAs(final String userId, final String password, final String asUserId)
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false)
    @Override public String loginAsAnonymousUser()
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void logout(final String sessionToken)
    {
    }

    @TypeScriptMethod(sessionToken = false, async = false)
    public void setSessionToken(String sessionToken)
    {
    }

    @TypeScriptMethod(sessionToken = false, async = false)
    public void setInteractiveSessionKey(String interactiveSessionKey)
    {
    }

    @TypeScriptMethod(sessionToken = false)
    public String beginTransaction()
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false)
    public void commitTransaction()
    {
    }

    @TypeScriptMethod(sessionToken = false)
    public void rollbackTransaction()
    {
    }

    @TypeScriptMethod
    @Override public SessionInformation getSessionInformation(final String sessionToken)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public boolean isSessionActive(final String sessionToken)
    {
        return false;
    }

    @TypeScriptMethod
    @Override public List<SpacePermId> createSpaces(final String sessionToken, final List<SpaceCreation> newSpaces)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<ProjectPermId> createProjects(final String sessionToken, final List<ProjectCreation> newProjects)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<ExperimentPermId> createExperiments(final String sessionToken, final List<ExperimentCreation> newExperiments)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<EntityTypePermId> createExperimentTypes(final String sessionToken, final List<ExperimentTypeCreation> newExperimentTypes)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<SamplePermId> createSamples(final String sessionToken, final List<SampleCreation> newSamples)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<EntityTypePermId> createSampleTypes(final String sessionToken, final List<SampleTypeCreation> newSampleTypes)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<DataSetPermId> createDataSets(final String sessionToken, final List<DataSetCreation> newDataSets)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<EntityTypePermId> createDataSetTypes(final String sessionToken, final List<DataSetTypeCreation> newDataSetTypes)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<MaterialPermId> createMaterials(final String sessionToken, final List<MaterialCreation> newMaterials)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<EntityTypePermId> createMaterialTypes(final String sessionToken, final List<MaterialTypeCreation> newMaterialTypes)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<PropertyTypePermId> createPropertyTypes(final String sessionToken, final List<PropertyTypeCreation> newPropertyTypes)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<PluginPermId> createPlugins(final String sessionToken, final List<PluginCreation> newPlugins)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<VocabularyPermId> createVocabularies(final String sessionToken, final List<VocabularyCreation> newVocabularies)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<VocabularyTermPermId> createVocabularyTerms(final String sessionToken,
            final List<VocabularyTermCreation> newVocabularyTerms)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<TagPermId> createTags(final String sessionToken, final List<TagCreation> newTags)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<AuthorizationGroupPermId> createAuthorizationGroups(final String sessionToken,
            final List<AuthorizationGroupCreation> newAuthorizationGroups)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<RoleAssignmentTechId> createRoleAssignments(final String sessionToken,
            final List<RoleAssignmentCreation> newRoleAssignments)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<PersonPermId> createPersons(final String sessionToken, final List<PersonCreation> newPersons)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<ExternalDmsPermId> createExternalDataManagementSystems(final String sessionToken,
            final List<ExternalDmsCreation> newExternalDataManagementSystems)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<QueryTechId> createQueries(final String sessionToken, final List<QueryCreation> newQueries)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<SemanticAnnotationPermId> createSemanticAnnotations(final String sessionToken,
            final List<SemanticAnnotationCreation> newAnnotations)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<PersonalAccessTokenPermId> createPersonalAccessTokens(final String sessionToken,
            final List<PersonalAccessTokenCreation> newPersonalAccessTokens)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<TypeGroupId> createTypeGroups(final String sessionToken, final List<TypeGroupCreation> newTypeGroups)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void updateSpaces(final String sessionToken, final List<SpaceUpdate> spaceUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateProjects(final String sessionToken, final List<ProjectUpdate> projectUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateExperiments(final String sessionToken, final List<ExperimentUpdate> experimentUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateExperimentTypes(final String sessionToken, final List<ExperimentTypeUpdate> experimentTypeUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateSamples(final String sessionToken, final List<SampleUpdate> sampleUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateSampleTypes(final String sessionToken, final List<SampleTypeUpdate> sampleTypeUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateDataSets(final String sessionToken, final List<DataSetUpdate> dataSetUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateDataSetTypes(final String sessionToken, final List<DataSetTypeUpdate> dataSetTypeUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateMaterials(final String sessionToken, final List<MaterialUpdate> materialUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateMaterialTypes(final String sessionToken, final List<MaterialTypeUpdate> materialTypeUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateExternalDataManagementSystems(final String sessionToken, final List<ExternalDmsUpdate> externalDmsUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updatePropertyTypes(final String sessionToken, final List<PropertyTypeUpdate> propertyTypeUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updatePlugins(final String sessionToken, final List<PluginUpdate> pluginUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateVocabularies(final String sessionToken, final List<VocabularyUpdate> vocabularyUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateVocabularyTerms(final String sessionToken, final List<VocabularyTermUpdate> vocabularyTermUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateTags(final String sessionToken, final List<TagUpdate> tagUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateAuthorizationGroups(final String sessionToken, final List<AuthorizationGroupUpdate> authorizationGroupUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updatePersons(final String sessionToken, final List<PersonUpdate> personUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateOperationExecutions(final String sessionToken, final List<OperationExecutionUpdate> executionUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateSemanticAnnotations(final String sessionToken, final List<SemanticAnnotationUpdate> annotationUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updateQueries(final String sessionToken, final List<QueryUpdate> queryUpdates)
    {

    }

    @TypeScriptMethod
    @Override public void updatePersonalAccessTokens(final String sessionToken, final List<PersonalAccessTokenUpdate> personalAccessTokenUpdates)
    {

    }

    @TypeScriptMethod
    @Override public Map<IObjectId, Rights> getRights(final String sessionToken, final List<? extends IObjectId> ids,
            final RightsFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<ISpaceId, Space> getSpaces(final String sessionToken, final List<? extends ISpaceId> spaceIds,
            final SpaceFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IProjectId, Project> getProjects(final String sessionToken, final List<? extends IProjectId> projectIds,
            final ProjectFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IExperimentId, Experiment> getExperiments(final String sessionToken, final List<? extends IExperimentId> experimentIds,
            final ExperimentFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IEntityTypeId, ExperimentType> getExperimentTypes(final String sessionToken,
            final List<? extends IEntityTypeId> experimentTypeIds,
            final ExperimentTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<ISampleId, Sample> getSamples(final String sessionToken, final List<? extends ISampleId> sampleIds,
            final SampleFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IEntityTypeId, SampleType> getSampleTypes(final String sessionToken, final List<? extends IEntityTypeId> sampleTypeIds,
            final SampleTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IDataSetId, DataSet> getDataSets(final String sessionToken, final List<? extends IDataSetId> dataSetIds,
            final DataSetFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IEntityTypeId, DataSetType> getDataSetTypes(final String sessionToken, final List<? extends IEntityTypeId> dataSetTypeIds,
            final DataSetTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IMaterialId, Material> getMaterials(final String sessionToken, final List<? extends IMaterialId> materialIds,
            final MaterialFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IEntityTypeId, MaterialType> getMaterialTypes(final String sessionToken, final List<? extends IEntityTypeId> materialTypeIds,
            final MaterialTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IPropertyTypeId, PropertyType> getPropertyTypes(final String sessionToken, final List<? extends IPropertyTypeId> typeIds,
            final PropertyTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IPluginId, Plugin> getPlugins(final String sessionToken, final List<? extends IPluginId> pluginIds,
            final PluginFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IVocabularyId, Vocabulary> getVocabularies(final String sessionToken, final List<? extends IVocabularyId> vocabularyIds,
            final VocabularyFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IVocabularyTermId, VocabularyTerm> getVocabularyTerms(final String sessionToken,
            final List<? extends IVocabularyTermId> vocabularyTermIds, final VocabularyTermFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<ITagId, Tag> getTags(final String sessionToken, final List<? extends ITagId> tagIds, final TagFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IAuthorizationGroupId, AuthorizationGroup> getAuthorizationGroups(final String sessionToken,
            final List<? extends IAuthorizationGroupId> groupIds, final AuthorizationGroupFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IRoleAssignmentId, RoleAssignment> getRoleAssignments(final String sessionToken, final List<? extends IRoleAssignmentId> ids,
            final RoleAssignmentFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IPersonId, Person> getPersons(final String sessionToken, final List<? extends IPersonId> ids,
            final PersonFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IExternalDmsId, ExternalDms> getExternalDataManagementSystems(final String sessionToken,
            final List<? extends IExternalDmsId> externalDmsIds, final ExternalDmsFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<ISemanticAnnotationId, SemanticAnnotation> getSemanticAnnotations(final String sessionToken,
            final List<? extends ISemanticAnnotationId> annotationIds, final SemanticAnnotationFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IOperationExecutionId, OperationExecution> getOperationExecutions(final String sessionToken,
            final List<? extends IOperationExecutionId> executionIds, final OperationExecutionFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IQueryId, Query> getQueries(final String sessionToken, final List<? extends IQueryId> queryIds,
            final QueryFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IQueryDatabaseId, QueryDatabase> getQueryDatabases(final String sessionToken,
            final List<? extends IQueryDatabaseId> queryDatabaseIds,
            final QueryDatabaseFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<IPersonalAccessTokenId, PersonalAccessToken> getPersonalAccessTokens(final String sessionToken,
            final List<? extends IPersonalAccessTokenId> personalAccessTokenIds, final PersonalAccessTokenFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Space> searchSpaces(final String sessionToken, final SpaceSearchCriteria searchCriteria,
            final SpaceFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Project> searchProjects(final String sessionToken, final ProjectSearchCriteria searchCriteria,
            final ProjectFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Experiment> searchExperiments(final String sessionToken, final ExperimentSearchCriteria searchCriteria,
            final ExperimentFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<ExperimentType> searchExperimentTypes(final String sessionToken, final ExperimentTypeSearchCriteria searchCriteria,
            final ExperimentTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Sample> searchSamples(final String sessionToken, final SampleSearchCriteria searchCriteria,
            final SampleFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<SampleType> searchSampleTypes(final String sessionToken, final SampleTypeSearchCriteria searchCriteria,
            final SampleTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<DataSet> searchDataSets(final String sessionToken, final DataSetSearchCriteria searchCriteria,
            final DataSetFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<DataSetType> searchDataSetTypes(final String sessionToken, final DataSetTypeSearchCriteria searchCriteria,
            final DataSetTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Material> searchMaterials(final String sessionToken, final MaterialSearchCriteria searchCriteria,
            final MaterialFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<ExternalDms> searchExternalDataManagementSystems(final String sessionToken,
            final ExternalDmsSearchCriteria searchCriteria,
            final ExternalDmsFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<MaterialType> searchMaterialTypes(final String sessionToken, final MaterialTypeSearchCriteria searchCriteria,
            final MaterialTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Plugin> searchPlugins(final String sessionToken, final PluginSearchCriteria searchCriteria,
            final PluginFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Vocabulary> searchVocabularies(final String sessionToken, final VocabularySearchCriteria searchCriteria,
            final VocabularyFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<VocabularyTerm> searchVocabularyTerms(final String sessionToken, final VocabularyTermSearchCriteria searchCriteria,
            final VocabularyTermFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Tag> searchTags(final String sessionToken, final TagSearchCriteria searchCriteria,
            final TagFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<AuthorizationGroup> searchAuthorizationGroups(final String sessionToken,
            final AuthorizationGroupSearchCriteria searchCriteria,
            final AuthorizationGroupFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<RoleAssignment> searchRoleAssignments(final String sessionToken, final RoleAssignmentSearchCriteria searchCriteria,
            final RoleAssignmentFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Person> searchPersons(final String sessionToken, final PersonSearchCriteria searchCriteria,
            final PersonFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<CustomASService> searchCustomASServices(final String sessionToken,
            final CustomASServiceSearchCriteria searchCriteria,
            final CustomASServiceFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<SearchDomainService> searchSearchDomainServices(final String sessionToken,
            final SearchDomainServiceSearchCriteria searchCriteria, final SearchDomainServiceFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<AggregationService> searchAggregationServices(final String sessionToken,
            final AggregationServiceSearchCriteria searchCriteria,
            final AggregationServiceFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<ReportingService> searchReportingServices(final String sessionToken,
            final ReportingServiceSearchCriteria searchCriteria,
            final ReportingServiceFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<ProcessingService> searchProcessingServices(final String sessionToken,
            final ProcessingServiceSearchCriteria searchCriteria,
            final ProcessingServiceFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<ObjectKindModification> searchObjectKindModifications(final String sessionToken,
            final ObjectKindModificationSearchCriteria searchCriteria, final ObjectKindModificationFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<GlobalSearchObject> searchGlobally(final String sessionToken, final GlobalSearchCriteria searchCriteria,
            final GlobalSearchObjectFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<OperationExecution> searchOperationExecutions(final String sessionToken,
            final OperationExecutionSearchCriteria searchCriteria,
            final OperationExecutionFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<DataStore> searchDataStores(final String sessionToken, final DataStoreSearchCriteria searchCriteria,
            final DataStoreFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<SemanticAnnotation> searchSemanticAnnotations(final String sessionToken,
            final SemanticAnnotationSearchCriteria searchCriteria,
            final SemanticAnnotationFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<PropertyType> searchPropertyTypes(final String sessionToken, final PropertyTypeSearchCriteria searchCriteria,
            final PropertyTypeFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<PropertyAssignment> searchPropertyAssignments(final String sessionToken,
            final PropertyAssignmentSearchCriteria searchCriteria,
            final PropertyAssignmentFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Query> searchQueries(final String sessionToken, final QuerySearchCriteria searchCriteria,
            final QueryFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<QueryDatabase> searchQueryDatabases(final String sessionToken, final QueryDatabaseSearchCriteria searchCriteria,
            final QueryDatabaseFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void deleteSpaces(final String sessionToken, final List<? extends ISpaceId> spaceIds, final SpaceDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteProjects(final String sessionToken, final List<? extends IProjectId> projectIds,
            final ProjectDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public IDeletionId deleteExperiments(final String sessionToken, final List<? extends IExperimentId> experimentIds,
            final ExperimentDeletionOptions deletionOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public IDeletionId deleteSamples(final String sessionToken, final List<? extends ISampleId> sampleIds,
            final SampleDeletionOptions deletionOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public IDeletionId deleteDataSets(final String sessionToken, final List<? extends IDataSetId> dataSetIds,
            final DataSetDeletionOptions deletionOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void deleteMaterials(final String sessionToken, final List<? extends IMaterialId> materialIds,
            final MaterialDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deletePlugins(final String sessionToken, final List<? extends IPluginId> pluginIds,
            final PluginDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deletePropertyTypes(final String sessionToken, final List<? extends IPropertyTypeId> propertyTypeIds,
            final PropertyTypeDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteVocabularies(final String sessionToken, final List<? extends IVocabularyId> ids,
            final VocabularyDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteVocabularyTerms(final String sessionToken, final List<? extends IVocabularyTermId> termIds,
            final VocabularyTermDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteExperimentTypes(final String sessionToken, final List<? extends IEntityTypeId> experimentTypeIds,
            final ExperimentTypeDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteSampleTypes(final String sessionToken, final List<? extends IEntityTypeId> sampleTypeIds,
            final SampleTypeDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteDataSetTypes(final String sessionToken, final List<? extends IEntityTypeId> dataSetTypeIds,
            final DataSetTypeDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteMaterialTypes(final String sessionToken, final List<? extends IEntityTypeId> materialTypeIds,
            final MaterialTypeDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteExternalDataManagementSystems(final String sessionToken, final List<? extends IExternalDmsId> externalDmsIds,
            final ExternalDmsDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteTags(final String sessionToken, final List<? extends ITagId> tagIds, final TagDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteAuthorizationGroups(final String sessionToken, final List<? extends IAuthorizationGroupId> groupIds,
            final AuthorizationGroupDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteRoleAssignments(final String sessionToken, final List<? extends IRoleAssignmentId> assignmentIds,
            final RoleAssignmentDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteOperationExecutions(final String sessionToken, final List<? extends IOperationExecutionId> executionIds,
            final OperationExecutionDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteSemanticAnnotations(final String sessionToken, final List<? extends ISemanticAnnotationId> annotationIds,
            final SemanticAnnotationDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteQueries(final String sessionToken, final List<? extends IQueryId> queryIds,
            final QueryDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deletePersons(final String sessionToken, final List<? extends IPersonId> personIds,
            final PersonDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deletePersonalAccessTokens(final String sessionToken, final List<? extends IPersonalAccessTokenId> personalAccessTokenIds,
            final PersonalAccessTokenDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public SearchResult<Deletion> searchDeletions(final String sessionToken, final DeletionSearchCriteria searchCriteria,
            final DeletionFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<Event> searchEvents(final String sessionToken, final EventSearchCriteria searchCriteria,
            final EventFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<PersonalAccessToken> searchPersonalAccessTokens(final String sessionToken,
            final PersonalAccessTokenSearchCriteria searchCriteria, final PersonalAccessTokenFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<SessionInformation> searchSessionInformation(final String sessionToken,
            final SessionInformationSearchCriteria searchCriteria,
            final SessionInformationFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void revertDeletions(final String sessionToken, final List<? extends IDeletionId> deletionIds)
    {

    }

    @TypeScriptMethod
    @Override public void confirmDeletions(final String sessionToken, final List<? extends IDeletionId> deletionIds)
    {

    }

    @TypeScriptMethod
    @Override public Object executeCustomASService(final String sessionToken, final ICustomASServiceId serviceId,
            final CustomASServiceExecutionOptions options)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<SearchDomainServiceExecutionResult> executeSearchDomainService(final String sessionToken,
            final SearchDomainServiceExecutionOptions options)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public TableModel executeAggregationService(final String sessionToken, final IDssServiceId serviceId,
            final AggregationServiceExecutionOptions options)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public TableModel executeReportingService(final String sessionToken, final IDssServiceId serviceId,
            final ReportingServiceExecutionOptions options)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void executeProcessingService(final String sessionToken, final IDssServiceId serviceId,
            final ProcessingServiceExecutionOptions options)
    {

    }

    @TypeScriptMethod
    @Override public TableModel executeQuery(final String sessionToken, final IQueryId queryId, final QueryExecutionOptions options)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public TableModel executeSql(final String sessionToken, final String sql, final SqlExecutionOptions options)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public PluginEvaluationResult evaluatePlugin(final String sessionToken, final PluginEvaluationOptions options)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void archiveDataSets(final String sessionToken, final List<? extends IDataSetId> dataSetIds, final DataSetArchiveOptions options)
    {

    }

    @TypeScriptMethod
    @Override public void unarchiveDataSets(final String sessionToken, final List<? extends IDataSetId> dataSetIds,
            final DataSetUnarchiveOptions options)
    {

    }

    @TypeScriptMethod
    @Override public void lockDataSets(final String sessionToken, final List<? extends IDataSetId> dataSetIds, final DataSetLockOptions options)
    {

    }

    @TypeScriptMethod
    @Override public void unlockDataSets(final String sessionToken, final List<? extends IDataSetId> dataSetIds, final DataSetUnlockOptions options)
    {

    }

    @TypeScriptMethod
    @Override public IOperationExecutionResults executeOperations(final String sessionToken, final List<? extends IOperation> operations,
            final IOperationExecutionOptions options)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<String, String> getServerInformation(final String sessionToken)
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false)
    @Override public Map<String, String> getServerPublicInformation()
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<String> createPermIdStrings(final String sessionToken, final int count)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public List<String> createCodes(final String sessionToken, final String prefix, final EntityKind entityKind, final int count)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public ImportResult executeImport(final String sessionToken, final ImportData importData, final ImportOptions importOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public ExportResult executeExport(final String sessionToken, final ExportData exportData, final ExportOptions exportOptions)
    {
        return null;
    }

    @TypeScriptMethod(sessionToken = false)
    public void uploadToSessionWorkspace(final Object file)
    {
    }

    @TypeScriptMethod @Override public List<TypeGroupAssignmentId> createTypeGroupAssignments(String sessionToken,
            List<TypeGroupAssignmentCreation> newTypeGroupAssignments)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void updateTypeGroups(String sessionToken, List<TypeGroupUpdate> typeGroupUpdates)
    {

    }

    @TypeScriptMethod
    @Override public Map<ITypeGroupId, TypeGroup> getTypeGroups(String sessionToken,
            List<? extends ITypeGroupId> typeGroupIds, TypeGroupFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public Map<ITypeGroupAssignmentId, TypeGroupAssignment> getTypeGroupAssignments(
            String sessionToken, List<? extends ITypeGroupAssignmentId> ids,
            TypeGroupAssignmentFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<TypeGroup> searchTypeGroups(String sessionToken,
            TypeGroupSearchCriteria searchCriteria, TypeGroupFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public SearchResult<TypeGroupAssignment> searchTypeGroupAssignments(String sessionToken,
            TypeGroupAssignmentSearchCriteria searchCriteria,
            TypeGroupAssignmentFetchOptions fetchOptions)
    {
        return null;
    }

    @TypeScriptMethod
    @Override public void deleteTypeGroups(String sessionToken, List<? extends ITypeGroupId> typeGroupIds,
            TypeGroupDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod
    @Override public void deleteTypeGroupAssignments(String sessionToken,
            List<? extends ITypeGroupAssignmentId> typeGroupAssignmentIds,
            TypeGroupAssignmentDeletionOptions deletionOptions)
    {

    }

    @TypeScriptMethod(sessionToken = false)
    @Override public int getMajorVersion()
    {
        return 0;
    }

    @TypeScriptMethod(sessionToken = false)
    @Override public int getMinorVersion()
    {
        return 0;
    }

}
