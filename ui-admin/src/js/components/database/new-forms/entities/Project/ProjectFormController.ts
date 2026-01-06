import openbis from '@srcV3/openbis.esm';
import { Form, } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { createDummyExperimentIdentifierFromProjectIdentifier, createDummySampleIdentifierFromProjectIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldById, findFormFieldByLabel } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { ProjectFormModel } from '@src/js/components/database/new-forms/entities/Project/ProjectFormModel.ts';
import { DeleteService } from '@src/js/components/database/new-forms/services/DeleteService.ts';

export class ProjectFormController implements IFormController {
  private openbisFacade: openbis.openbis;
  private spacePermId: string = '';
  private deleteService: DeleteService;

  constructor(openbisFacade: openbis.openbis) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
    this.deleteService = new DeleteService({ openbisFacade: this.openbisFacade });
  }

  async load(permId: string, entityKind?: string, params?: any): Promise<Form> {
    console.log('ProjectFormController.load', permId, entityKind);
    if (entityKind === EntityKind.NEW_PROJECT) {
      return ProjectFormModel.adaptNewProjectDtoToForm(permId, params);
    }
    const { ProjectPermId, ProjectFetchOptions, ExperimentIdentifier, RightsFetchOptions } = this.openbisFacade;
    const id = new ProjectPermId(permId);
    const fetchOptions = new ProjectFetchOptions();
    fetchOptions.withSpace();
    fetchOptions.withModifier();
	fetchOptions.withRegistrator();
    const result = await this.openbisFacade.getProjects([id], fetchOptions);

    const projectDto = result[permId];
    console.log({ projectDto });
    if (!projectDto) throw new Error(`Project with permId ${permId} not found`);
    /* const spaceCode = projectDto.getSpace().getCode();
    const projectCode = projectDto.getCode();
    
    const sessionInfo = await this.openbisFacade.getSessionInformation();
    console.log({ sessionInfo })
    console.log({ spaceCode }, { projectCode });
    const roles = await getUserRole(this.openbisFacade, false, spaceCode, projectCode);
    console.log({roles}); */
    return ProjectFormModel.adaptProjectDtoToForm(projectDto);
  }

  async save(form: Form, mode: FormMode): Promise<any> {
    if (mode === FormMode.CREATE) {
      return this._createProject(form);
    } else if (mode === FormMode.EDIT) {
      return this._updateProject(form);
    } else {
      throw new Error(`Invalid form mode: ${mode}`);
    }
  }

  async checkPermissions(form: Form) {
    const { ProjectPermId, ExperimentIdentifier, SampleIdentifier } = this.openbisFacade;
    const projectCode = form.entityPermId;
    const projectPermId = new ProjectPermId(projectCode);
    const projectIdentifierField = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);
    if (!projectIdentifierField || typeof projectIdentifierField !== 'string') throw new Error('Project identifier not found');
    const projectIdentifier = projectIdentifierField;
    const dummyExperimentId = new ExperimentIdentifier(createDummyExperimentIdentifierFromProjectIdentifier(projectIdentifier));
    const dummySampleId = new SampleIdentifier(createDummySampleIdentifierFromProjectIdentifier(projectIdentifier));
    const ids = [projectPermId, dummyExperimentId, dummySampleId];
		const { editable, deletable } = await fetchRights(this.openbisFacade, projectCode, ids);
		return { canEdit: editable, canDelete: deletable, canMove: true };
    //return { canEdit: true, canDelete: true, canMove: true };
  }

  async delete(form: Form, context?: any): Promise<void> {
    console.log('ProjectFormController.delete', form, context);
    
    // Check for existing deletions in trashcan before proceeding
    const projectIdentifier = findFormFieldById(form.fields, form.entityPermId, 'identifier', true);

    if (!projectIdentifier || typeof projectIdentifier !== 'string') throw new Error('Project identifier not found');
    
    const dependentDeletions = await this.deleteService.checkExistingDeletions(
      projectIdentifier,
      'PROJECT',
      ['EXPERIMENT', 'SAMPLE']
    );
    if (dependentDeletions.length > 0) {
      const errorMessage = this.deleteService.formatDeletionError(dependentDeletions, 'project');
      throw new Error(errorMessage);
    }
    
    // If this is just a check, return early
    if (context?.checkOnly) {
      return;
    }
    
    // Get dependent entities if not provided in context
    // Use rawDependentEntities from context if available (from normalized structure)
    let dependentEntities = context?.rawDependentEntities || context?.dependentEntities;
    if (!dependentEntities) {
      dependentEntities = await this.getDependentEntities(form);
    }
    
    console.log('ProjectFormController.dependentEntities:', dependentEntities);
    
    // Get delete reason from context or use default
    const deleteReason = context?.deleteReason || 'delete via ng-ui';
    
    // Check if project is empty
    const isEmpty = (!dependentEntities.experiments || dependentEntities.experiments.length === 0) &&
                    (!dependentEntities.samples || dependentEntities.samples.length === 0);
    
    if (!isEmpty) {
      // Non-empty project: Move all entities to trashcan and STOP (don't delete the project)
      // Move dependent entities to trashcan using DeleteService
      let movedCount = 0;
      if (dependentEntities.experiments && dependentEntities.experiments.length > 0) {
        const result = await this.deleteService.moveExperimentsToTrashcan(dependentEntities.experiments, deleteReason);
        if (!result.success) {
          throw new Error(result.error || 'Failed to move experiments to trashcan');
        }
        movedCount += result.count;
      }
      
      if (dependentEntities.samples && dependentEntities.samples.length > 0) {
        const result = await this.deleteService.moveSamplesToTrashcan(dependentEntities.samples, deleteReason);
        if (!result.success) {
          throw new Error(result.error || 'Failed to move samples to trashcan');
        }
        movedCount += result.count;
      }
      
      // Return early - don't delete the project itself
      // Return object to indicate deletion was skipped
      return Promise.resolve({
        skipped: true,
        message: `Successfully moved ${movedCount} entit${movedCount > 1 ? 'ies' : 'y'} to trashcan`
      });
    }
    
    // Empty project: Move the project itself to trashcan using DeleteService
    const result = await this.deleteService.moveProjectsToTrashcan(
      [{ identifier: projectIdentifier }],
      deleteReason
    );
    if (!result.success) {
      throw new Error(result.error || 'Failed to move project to trashcan');
    }
    console.log('ProjectFormController.delete result:', result);
    return Promise.resolve();
  }

  async getDependentEntities(form: Form): Promise<any> {
    const { ProjectPermId, ProjectFetchOptions } = this.openbisFacade;
    const id = new ProjectPermId(form.entityPermId);
    const fetchOptions = new ProjectFetchOptions();
    fetchOptions.withExperiments();
    fetchOptions.withSamples().withExperiment();
    const result = await this.openbisFacade.getProjects([id], fetchOptions);
    const project = result[id];
    console.log('ProjectFormController.dependentEntities:', result);
    return { experiments: project.getExperiments(), samples: project.getSamples() };
  }


  /**
   * @deprecated Use moveEntitiesToTrashcan instead. This method actually deletes entities.
   * Kept for backward compatibility but should not be used for non-empty projects.
   */
  async deleteDependentEntities(reason: string, dependentEntities: any): Promise<void> {
    console.log('ProjectFormController.deleteDependentEntities', reason, dependentEntities);
    
    const { ExperimentIdentifier, SampleIdentifier, ExperimentDeletionOptions, SampleDeletionOptions } = this.openbisFacade;
    
    // Delete experiments first
    if (dependentEntities.experiments && dependentEntities.experiments.length > 0) {
      const experimentIds = dependentEntities.experiments.map((exp: any) => new ExperimentIdentifier(exp.getIdentifier().getIdentifier()));
      const experimentDeletionOptions = new ExperimentDeletionOptions();
      experimentDeletionOptions.setReason(reason);
      await this.openbisFacade.deleteExperiments(experimentIds, experimentDeletionOptions);
      console.log('ProjectFormController.deleted experiments:', experimentIds.length);
    }
    
    // Then delete independent samples (samples not associated with experiments we just deleted)
    if (dependentEntities.samples && dependentEntities.samples.length > 0) {
      const experimentIds = new Set((dependentEntities.experiments || []).map((exp: any) => exp.getPermId()));
      const independentSamples = dependentEntities.samples
        .filter((sample: any) => {
          const experiment = sample.getExperiment();
          return !experiment || !experimentIds.has(experiment.getPermId());
        })
        .map((sample: any) => new SampleIdentifier(sample.getIdentifier().getIdentifier()));
      
      if (independentSamples.length > 0) {
        const sampleDeletionOptions = new SampleDeletionOptions();
        sampleDeletionOptions.setReason(reason);
        await this.openbisFacade.deleteSamples(independentSamples, sampleDeletionOptions);
        console.log('ProjectFormController.deleted independent samples:', independentSamples.length);
      }
    }
  }

  async move(form: Form, context?: any, params?: any): Promise<void> {
    const { ProjectPermId, ProjectUpdate, SpacePermId } = this.openbisFacade;
    const projectPermId = new ProjectPermId(form.entityPermId);
    const projectUpdate = new ProjectUpdate();
    console.log('ProjectFormController.move', params);
    projectUpdate.setProjectId(projectPermId);
    projectUpdate.setSpaceId(params.target.getPermId());
    const result = await this.openbisFacade.updateProjects([projectUpdate]);
    console.log('ProjectFormController.move', result);
    return result;
  }

  async _createProject(form: Form): Promise<any> {
    console.log('ProjectFormController.create', form);
    const { ProjectCreation, SpacePermId } = this.openbisFacade;
    const creation = new ProjectCreation();
    creation.setCode(findFormFieldByLabel(form.fields, 'Code', true));
    creation.setSpaceId(new SpacePermId(form.meta.spacePermId));
    creation.setDescription(findFormFieldByLabel(form.fields, 'Description', true));
    const result = await this.openbisFacade.createProjects([creation]);
    console.log('ProjectFormController.create', result);
    return result[0].getPermId();
  }

  async _updateProject(form: Form): Promise<any> {
    const { ProjectPermId, ProjectUpdate } = this.openbisFacade;
    const projectUpdate = new ProjectUpdate();
    projectUpdate.setProjectId(new ProjectPermId(form.entityPermId));
    projectUpdate.setDescription(findFormFieldByLabel(form.fields, 'Description', true));
    const result = await this.openbisFacade.updateProjects([projectUpdate]);
    console.log('ProjectFormController.update', result);
    return Promise.resolve(form.version ? form.version + 1 : 1);
  }
}
