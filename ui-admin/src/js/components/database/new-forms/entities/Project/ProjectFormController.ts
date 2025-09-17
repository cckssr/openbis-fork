import openbis from '@srcV3/openbis.esm';
import { EntityKind, Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/types/FormController';
import { fetchRights } from '@src/js/components/database/new-forms/utils/AuthorizationService.ts';
import { createDummyExperimentIdentifierFromProjectIdentifier, createDummySampleIdentifierFromProjectIdentifier } from '@src/js/components/database/new-forms/utils/IdentifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { ProjectFormModel } from '@src/js/components/database/new-forms/entities/Project/ProjectFormModel.ts';

export class ProjectFormController implements FormController {
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
      console.log('ProjectFormController.save: CREATE');
      const { ProjectIdentifier, ProjectCreation, SpacePermId } = this.openbisFacade;
      const creation = new ProjectCreation();
      creation.setCode(form.fields.find(field => field.label === 'Code')?.value as string);
      creation.setSpaceId(new SpacePermId(form.meta.spacePermId));
      creation.setDescription(form.fields.find(field => field.label === 'Description')?.value as string);
      const result = await this.openbisFacade.createProjects([creation]);
      console.log('ProjectFormController.create', result);
      return result[0].getPermId();
      return Promise.resolve(form.version + 1);
    } else if (mode === FormMode.EDIT) {
      console.log('ProjectFormController.save: EDIT');
    } else {
      throw new Error(`Invalid form mode: ${mode}`);
    }
    //return Promise.resolve(form.version + 1);
  }

  async checkPermissions(form: Form) {
    const { ProjectPermId, ExperimentIdentifier, SampleIdentifier } = this.openbisFacade;
    const projectCode = form.entityPermId;
    const projectPermId = new ProjectPermId(projectCode);
    const projectIdentifier = findFormFieldById(form.fields, 'identifier')?.value;
    console.log({projectIdentifier})
    const dummyExperimentId = new ExperimentIdentifier(createDummyExperimentIdentifierFromProjectIdentifier(projectIdentifier));
    const dummySampleId = new SampleIdentifier(createDummySampleIdentifierFromProjectIdentifier(projectIdentifier));
    const ids = [projectPermId, dummyExperimentId, dummySampleId];
		const { editable, deletable } = await fetchRights(this.openbisFacade, projectCode, ids);
		return { canEdit: editable, canDelete: deletable, canMove: true };
    //return { canEdit: true, canDelete: true, canMove: true };
  }


  async delete(form: Form): Promise<void> {
    console.log('ProjectFormController.delete', form);
    const { ProjectIdentifier, ProjectDeletionOptions, DeletionSearchCriteria, DeletionFetchOptions } = this.openbisFacade;
    const projectIdentifier = new ProjectIdentifier(form.fields.find(field => field.id === form.entityPermId + '-identifier')?.value);

    const criteria = new DeletionSearchCriteria();
    const fetchOptions = new DeletionFetchOptions();
    fetchOptions.withDeletedObjects();
    const deletions = await this.openbisFacade.searchDeletions(criteria, fetchOptions);

    console.log({deletions});
    const deletionOptions = new ProjectDeletionOptions();
    deletionOptions.setReason('delete via ng-ui');
    const result = await this.openbisFacade.deleteProjects([projectIdentifier], deletionOptions);
    console.log({result});  
  }

/*   this.deleteProject = function(reason) {
    var _this = this;
    var projectIdentifier = this._projectFormModel.v3_project.identifier.identifier;
    mainController.serverFacade.listDeletions(function(deletions) {
        var dependentDeletions = [];
        deletions.forEach(function(deletion) {
            var deletedObjects = deletion.getDeletedObjects();
            for (var idx = 0; idx < deletedObjects.length; idx++) {
                var deletedObject = deletedObjects[idx];
                var kind = deletedObject.entityKind;
                if (kind == "EXPERIMENT" || kind == "SAMPLE") {
                    var splitted = deletedObject.identifier.split("/");
                    if (splitted.length > 3 && ("/" + splitted[1] + "/" + splitted[2]) == projectIdentifier) {
                        dependentDeletions.push(deletion);
                        break;
                    }
                }
            };
        });
        if (dependentDeletions.length > 0) {
            var text = "This project can only be deleted if the following deletions sets in Trashcan are deleted permanently:<br>";
            dependentDeletions.forEach(function(deletion) {
                text += Util.getFormatedDate(new Date(deletion.deletionDate)) + " (reason: " + deletion.reason + ")<br>";
            });
            Util.showInfo(text);
        } else {
            mainController.serverFacade.deleteProjects([_this._projectFormModel.project.id], reason, function(data) {
                Util.unblockUI()
                if(data.error) {
                    Util.showError(data.error.message);
                } else {
                    Util.showSuccess("Project Deleted");
                    mainController.sideMenu.deleteNodeByEntityPermId("PROJECT", _this._projectFormModel.project.permId, true);
                }
            });
        }
    });
} */

  move(form: Form): void {
    // Implement move logic as needed
  }

  create(form: Form): void {
    console.log('ProjectFormController.create', form);
    const { ProjectIdentifier, ProjectCreation } = this.openbisFacade;
    const creation = new ProjectCreation();
    creation.setCode(form.code);
    creation.setSpaceId(form.space);
    creation.setDescription(form.description);
    this.openbisFacade.createProjects([creation]).then(map => {
      console.log('ProjectFormController.create', map);
    });
  }
}
