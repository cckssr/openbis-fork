import { EntityKind } from "@src/js/components/database/new-forms/types/form.enums.ts";
import { Form } from "@src/js/components/database/new-forms/types/form.types.ts";
import { getIdentifierField, getPathField, getSpaceField, getCodeField, getDescriptionField, getRegistratorField, getRegistrationDateField, getModifierField, getModificationDateField } from "@src/js/components/database/new-forms/entities/formFieldGetters.ts";

export class ProjectFormModel {
	static adaptProjectDtoToForm(projectDto: any): Form {
		const permId = projectDto.permId.permId;
		const fields = [
			getIdentifierField(projectDto),
			getPathField(projectDto),
			getSpaceField(projectDto),
			getCodeField(projectDto),
			getDescriptionField(projectDto),
			getRegistratorField(projectDto),
			getRegistrationDateField(projectDto),
			getModifierField(projectDto),
			getModificationDateField(projectDto),
		];
		return {
			entityPermId: permId,
			entityType: EntityKind.PROJECT,
			title: `Project: ${projectDto.code}`,
			version: projectDto.version || 1,
			entityKind: EntityKind.PROJECT,
			fields,
			isDirty: false,
			isValid: true,
			meta: {},
			actions: [],
		};
	}
}