import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/entities/FormController.ts';
import { createDummySampleIdentifier } from '@src/js/components/database/new-forms/utils/IdentifierUtil.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { fetchRights, getUserRole } from '@src/js/components/database/new-forms/entities/AuthorizationService.ts';
import { adaptSpaceDtoToForm } from '@src/js/components/database/new-forms/adapters/entity.adapter.ts';
import { groupFieldsBySection } from '@src/js/components/database/new-forms/utils/Utils.ts';
import { FormAction } from '@src/js/components/database/new-forms/types/form.types.ts';

export class SpaceFormController implements FormController {
  private openbisFacade: any;
  private user: string;

  constructor(openbisFacade: any, user: string) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
    this.user = user;
  }

  async load(permId: string): Promise<Form> {
    const { SpacePermId, SpaceFetchOptions } = this.openbisFacade;
    const id = new SpacePermId(permId);
    const fetchOptions = new SpaceFetchOptions();
    fetchOptions.withProjects && fetchOptions.withProjects();
    fetchOptions.withRegistrator && fetchOptions.withRegistrator();
    fetchOptions.withSamples && fetchOptions.withSamples();
    const result = await this.openbisFacade.getSpaces([id], fetchOptions);

    const spaceDto = result[permId];

    const roles = await getUserRole(this.openbisFacade, false, permId);
    console.log({roles});
    console.log(spaceDto);
    if (!spaceDto) throw new Error(`Space with permId ${permId} not found`);
    return adaptSpaceDtoToForm(spaceDto);
  }

  async save(form: Form): Promise<number> {
    const { SpacePermId, SpaceUpdate } = this.openbisFacade;
    const spaceUpdate = new SpaceUpdate()
    spaceUpdate.setSpaceId(new SpacePermId(form.entityPermId));
    spaceUpdate.setDescription(findFormFieldById(form.fields, 'description')?.value)

    const result = await this.openbisFacade.updateSpaces([spaceUpdate]);
    console.log(result)
    return Promise.resolve(form.version ? form.version + 1 : 1);
  }

  async checkPermissions(form: Form) {
    const { SpacePermId, ProjectIdentifier, SampleIdentifier } = this.openbisFacade;
    const spaceCode = form.entityPermId;
    const spacePermId = new SpacePermId(spaceCode);
    const dummyProjectId = new ProjectIdentifier(createDummySampleIdentifier(spaceCode));
    const dummySampleId = new SampleIdentifier(createDummySampleIdentifier(spaceCode));
    console.log({dummyProjectId, dummySampleId})
    const ids = [spacePermId, dummyProjectId, dummySampleId];
    const { editable, deletable } = await fetchRights(this.openbisFacade, spaceCode, ids);
		console.log({editable, deletable})
		//return { canEdit: editable, canDelete: deletable, canMove: true };
    return { canEdit: true, canDelete: true, canMove: true };
  }

  edit(form: Form): void {
    // Implement edit logic as needed
  }

  delete(form: Form): void {
    // Implement delete logic as needed
  }

  move(form: Form): void {
    // Implement move logic as needed
  }

  async getUserRole(isAdmin: boolean, space: string, project?: string): Promise<string[]> {
    if (isAdmin) {
      return ["ADMIN"];
    } else {
      const { RoleAssignmentSearchCriteria, RoleAssignmentFetchOptions } = this.openbisFacade;
      const criteria = new RoleAssignmentSearchCriteria();
      criteria.withSpace().withCode().thatEquals(space);
      //if (form.user) {
      criteria.withOrOperator();
      criteria.withUser().withUserId().thatEquals('admin');
      criteria.withAuthorizationGroup().withUser().withUserId().thatEquals('admin');
      //}
      const fetchOptions = new RoleAssignmentFetchOptions();
      fetchOptions.withSpace();
      fetchOptions.withProject();
      fetchOptions.withUser();
      fetchOptions.withAuthorizationGroup();
      const roles = await this.openbisFacade.searchRoleAssignments(criteria, fetchOptions)
        .then((roleAssignments: any) => {
          var roles = [];
          console.log({ roleAssignments });
          for (let i = 0; i < roleAssignments.getObjects().length; i++) {
            const ra = roleAssignments.getObjects()[i];
            if (ra.space && ra.space.code === space && roles.indexOf(ra.role) < 0) {
              roles.push(ra.role);
            }
            if (project && ra.project && ra.project.code === project && roles.indexOf(ra.role) < 0) {
              roles.push(ra.role);
            }
          }
          return roles;
        })
        .catch((errorResult: any) => {
          console.error("Error searching role assignments:", errorResult);
          return [];
        })
      return roles;
    }
  }
}