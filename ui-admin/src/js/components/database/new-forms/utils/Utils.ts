import objectType from "@src/js/common/consts/objectType";
import { EntityKind, FormField } from "@src/js/components/database/new-forms/types/form.types.ts";
import { ProjectCreationForm } from "@src/js/components/database/new-forms/entities/Project/ProjectCreationForm.tsx";
import CollectionFormView from "@src/js/components/database/new-forms/entities/Collection/CollectionFormView.tsx";
import SpaceFormView from "@src/js/components/database/new-forms/entities/Space/SpaceFormView.tsx";
import ProjectFormView from "@src/js/components/database/new-forms/entities/Project/ProjectFormView.tsx";
import ObjectFormView from "@src/js/components/database/new-forms/entities/Object/ObjectFormView.tsx";
import DatasetFormView from "@src/js/components/database/new-forms/entities/Dataset/DatasetFormView.tsx";

export function findFormFieldById(fields: FormField[], fieldId: string): FormField | undefined {
	return fields.find(field => field.id === fieldId);
}

export const getFormatedDate = (date: Date): string => {
	const day = String(date.getDate()).padStart(2, '0');
	const month = String(date.getMonth() + 1).padStart(2, '0');
	const year = date.getFullYear();
	const hour = String(date.getHours()).padStart(2, '0');
	const minute = String(date.getMinutes()).padStart(2, '0');
	const second = String(date.getSeconds()).padStart(2, '0');
	return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
}

// Map admin UI objectTypes to form EntityKind enum
export const objectTypeToEntityKindMap = {
	[objectType.SPACE]: EntityKind.SPACE,
	[objectType.PROJECT]: EntityKind.PROJECT,
	[objectType.NEW_PROJECT]: EntityKind.NEW_PROJECT,
	[objectType.OBJECT]: EntityKind.SAMPLE,
	[objectType.COLLECTION]: EntityKind.COLLECTION,
	[objectType.DATA_SET]: EntityKind.DATASET,
};

export const entityKindToFormComponentMap = {
	[EntityKind.SPACE]: SpaceFormView,
	[EntityKind.PROJECT]: ProjectFormView,
	[EntityKind.NEW_PROJECT]: ProjectCreationForm,
	[EntityKind.SAMPLE]: ObjectFormView,
	[EntityKind.COLLECTION]: CollectionFormView,
	[EntityKind.DATASET]: DatasetFormView
};

export function groupFieldsBySection(fields: FormField[]): { section: string; fields: FormField[] }[] {
  const sections: { [key: string]: FormField[] } = {};
  fields.forEach(field => {
    const section = field.section || 'General';
    if (!sections[section]) sections[section] = [];
    sections[section].push(field);
  });
  return Object.entries(sections).map(([section, fields]) => ({ section, fields }));
}