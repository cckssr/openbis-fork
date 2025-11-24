import openbis from '@srcV3/openbis.esm';
import { Form, } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { EntityKind, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { fetchRights } from '@src/js/components/database/new-forms/utils/authorizationServiceUtil.ts';
import { createDummyExperimentIdentifierFromProjectIdentifier, createDummySampleIdentifierFromProjectIdentifier } from '@src/js/components/database/new-forms/utils/identifierUtil.ts';
import { findFormFieldById, findFormFieldByLabel } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';
import { ProjectFormModel } from '@src/js/components/database/new-forms/entities/Project/ProjectFormModel.ts';

export class ProjectFormController implements IFormController {
  private openbisFacade: openbis.openbis;
  private spacePermId: string = '';

  constructor(openbisFacade: openbis.openbis) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
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
    console.log({projectIdentifier})
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
    
    const dependentDeletions = await this.checkExistingDeletions(projectIdentifier);
    if (dependentDeletions.length > 0) {
      const errorMessage = this.formatDeletionError(dependentDeletions);
      throw new Error(errorMessage);
    }
    
    // If this is just a check, return early
    if (context?.checkOnly) {
      return;
    }
    
    // Get dependent entities if not provided in context
    let dependentEntities = context?.dependentEntities;
    if (!dependentEntities) {
      dependentEntities = await this.getDependentEntities(form);
    }
    
    console.log('ProjectFormController.dependentEntities:', dependentEntities);
    
    // Get delete reason from context or use default
    const deleteReason = context?.deleteReason || 'delete via ng-ui';
    
    // Delete dependent entities first if they exist
    if (dependentEntities.experiments.length > 0 || dependentEntities.samples.length > 0) {
      await this.deleteDependentEntities(deleteReason, dependentEntities);
    }
    
    // Then delete the main project
    const { ProjectIdentifier, ProjectDeletionOptions, DeletionSearchCriteria, DeletionFetchOptions } = this.openbisFacade;
    const projectId = new ProjectIdentifier(projectIdentifier);

    const criteria = new DeletionSearchCriteria();
    const fetchOptions = new DeletionFetchOptions();
    fetchOptions.withDeletedObjects();
    const deletions = await this.openbisFacade.searchDeletions(criteria, fetchOptions);

    console.log('ProjectFormController.deletions:', deletions);
    const deletionOptions = new ProjectDeletionOptions();
    deletionOptions.setReason(deleteReason);
    const result = await this.openbisFacade.deleteProjects([projectId], deletionOptions);
    console.log('ProjectFormController.delete result:', result);
    return
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

  async checkExistingDeletions(projectIdentifier: string): Promise<any[]> {
    console.log('ProjectFormController.checkExistingDeletions', {projectIdentifier});
    
    const { DeletionSearchCriteria, DeletionFetchOptions } = this.openbisFacade;
    const criteria = new DeletionSearchCriteria();
    const fetchOptions = new DeletionFetchOptions();
    fetchOptions.withDeletedObjects();
    
    const deletions = await this.openbisFacade.searchDeletions(criteria, fetchOptions);
    console.log('ProjectFormController.allDeletions:', {deletions});
    
    const dependentDeletions: any[] = [];
    
    // Check each deletion for dependent entities
    // The searchDeletions method returns an array or object with objects property
    const deletionList = deletions.getObjects() || deletions;
    if (Array.isArray(deletionList)) {
      deletionList.forEach((deletion: any) => {
        const deletedObjects = deletion.getDeletedObjects();
        for (let idx = 0; idx < deletedObjects.length; idx++) {
          const deletedObject = deletedObjects[idx];
          const kind = deletedObject.entityKind;
          if (kind === "EXPERIMENT" || kind === "SAMPLE") {
            const splitted = deletedObject.identifier.split("/");
            if (splitted.length > 3 && ("/" + splitted[1] + "/" + splitted[2]) === projectIdentifier) {
              dependentDeletions.push(deletion);
              break;
            }
          }
        }
      });
    }
    
    console.log('ProjectFormController.dependentDeletions:', {dependentDeletions});
    return dependentDeletions;
  }

  formatDeletionError(dependentDeletions: any[]): string {
    let text = "This project can only be deleted if the following deletion sets in Trashcan are deleted permanently:\n";
    dependentDeletions.forEach((deletion: any) => {
      const deletionDate = new Date(deletion.deletionDate);
      const formattedDate = deletionDate.toLocaleDateString() + " " + deletionDate.toLocaleTimeString();
      text += `${formattedDate} (reason: ${deletion.reason}) \n`;
    });
    return text;
  }

  async deleteDependentEntities(reason: string, dependentEntities: any): Promise<void> {
    console.log('ProjectFormController.deleteDependentEntities', reason, dependentEntities);
    
    const { ExperimentIdentifier, SampleIdentifier, ExperimentDeletionOptions, SampleDeletionOptions } = this.openbisFacade;
    
    // Delete experiments first
    if (dependentEntities.experiments.length > 0) {
      const experimentIds = dependentEntities.experiments.map((exp: any) => new ExperimentIdentifier(exp.getIdentifier().getIdentifier()));
      const experimentDeletionOptions = new ExperimentDeletionOptions();
      experimentDeletionOptions.setReason(reason);
      await this.openbisFacade.deleteExperiments(experimentIds, experimentDeletionOptions);
      console.log('ProjectFormController.deleted experiments:', experimentIds.length);
    }
    
    // Then delete independent samples (samples not associated with experiments we just deleted)
    if (dependentEntities.samples.length > 0) {
      const experimentIds = new Set(dependentEntities.experiments.map((exp: any) => exp.getPermId()));
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
    console.log('ProjectFormController.update', form);
    const { ProjectPermId, ProjectUpdate } = this.openbisFacade;
    const projectUpdate = new ProjectUpdate();
    projectUpdate.setProjectId(new ProjectPermId(form.entityPermId));
    projectUpdate.setDescription(findFormFieldByLabel(form.fields, 'Description', true));
    const result = await this.openbisFacade.updateProjects([projectUpdate]);
    console.log('ProjectFormController.update', result);
    return Promise.resolve(form.version ? form.version + 1 : 1);
  }
}
