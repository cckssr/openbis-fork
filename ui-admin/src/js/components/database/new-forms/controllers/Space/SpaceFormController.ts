import React, { useEffect, useState } from 'react';
import openbis from '@srcV3/openbis.esm';
import { Form, FormFieldDataType, findFormFieldById, FormSection } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormController } from '@src/js/components/database/new-forms/controllers/FormController.ts';

function adaptSpaceDtoToForm(dto: any): Form {
  return {
    entityPermId: dto.permId.permId,
    entityType: 'SPACE',
    title: `Space: ${dto.code}`,
    version: dto.version || 1,
    entityKind: 'SPACE',
    meta: {},
    fields: [
      { id: 'code', label: 'Code', value: dto.code, dataType: FormFieldDataType.VARCHAR, isMandatory: true, isMultiValue: false, isEditable: false, section: FormSection.IDENTIFICATION_INFO, meta: [] },
      { id: 'description', label: 'Description', value: dto.description, dataType: FormFieldDataType.MULTILINE_VARCHAR, isMandatory: false, isMultiValue: false, isEditable: true, section: FormSection.GENERAL, meta: [] },
      { id: 'registrator', label: 'Registrator', value: dto.registrator.userId, dataType: FormFieldDataType.VARCHAR, isMandatory: false, isMultiValue: false, isEditable: false, section: FormSection.IDENTIFICATION_INFO, meta: [] },
      { id: 'registrationDate', label: 'Registration Date', value: dto.registrationDate ? new Date(dto.registrationDate).toLocaleDateString() : '', 
        dataType: FormFieldDataType.TIMESTAMP, isMandatory: false, isMultiValue: false, isEditable: false, section: FormSection.IDENTIFICATION_INFO, meta: [] }
    ]
  };
}


export class SpaceFormController implements FormController {
  private openbisFacade: openbis.openbis;
  private user: string;

  constructor(openbisFacade: openbis.openbis, user: string) {
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
    // Add more fetch options as needed
    const result = await this.openbisFacade.getSpaces([id], fetchOptions);

    const spaceDto = result[permId];
    console.log(spaceDto);
    if (!spaceDto) throw new Error(`Space with permId ${permId} not found`);
    return adaptSpaceDtoToForm(spaceDto);
  }

  async save(form: Form): Promise<number> {
    const { SpacePermId, SpaceUpdate } = this.openbisFacade;
    const spaceUpdate = new SpaceUpdate()
    spaceUpdate.setSpaceId(new SpacePermId(form.entityPermId));
    spaceUpdate.setDescription(findFormFieldById(form.fields, 'description').value)

    const result = await this.openbisFacade.updateSpaces([spaceUpdate]);
    console.log(result)
    return Promise.resolve(form.version + 1);
  }

  async checkPermissions(form: Form) {
    const { SpacePermId, RightsFetchOptions, ProjectIdentifier } = this.openbisFacade;
    const objId = form.entityPermId;
    const right = await this.openbisFacade.getRightsByIds([new SpacePermId(objId)], new RightsFetchOptions())
    console.log({ right })

    const roles = await this.getUserRole(false, form.entityPermId);
    console.log({roles});

    var dummyId = new ProjectIdentifier("/DEFAULT/__DUMMY_FOR_RIGHTS_CALCULATION__");
    const dummyRights = await this.openbisFacade.getRights([objId, dummyId], new RightsFetchOptions());
    console.log({dummyRights})

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

  async getUserRole(isAdmin: boolean, space: string, project?: string): string[] {
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
				.then(roleAssignments => {
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