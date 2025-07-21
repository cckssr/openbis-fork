import objectType from "@src/js/common/consts/objectType";
import { EntityKind, FormField } from "@src/js/components/database/new-forms/types/form.types.ts";

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