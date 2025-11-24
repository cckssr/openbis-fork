import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import objectType from "@src/js/common/consts/objectType";
import { EntityKind } from "@src/js/components/database/new-forms/types/formEnums.ts";

export function findFormFieldById(fields: FormField[], permId: string, label: string, onlyValue: boolean = false): FormField | string | null {
	const field = fields.find(field => field.id === permId + '-' + label);
	if (!field) return null;
	if (onlyValue) return field.value;
	return field;
}

export function findFormFieldByLabel(fields: FormField[], label: string, onlyValue: boolean = false): FormField | string | null {
	const field = fields.find(field => field.label === label);
	if (!field) return null;
	if (onlyValue) return field.value;
	return field;
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

type FieldKeyMapper = (field: FormField) => string | null;

const defaultFieldKeyMapper: FieldKeyMapper = (field: FormField) => field.name || null;

function areValuesEqual(valueA: any, valueB: any): boolean {
	if (valueA === valueB) {
		return true;
	}

	const isObject =
		typeof valueA === 'object' && valueA !== null && typeof valueB === 'object' && valueB !== null;

	if (isObject) {
		try {
			return JSON.stringify(valueA) === JSON.stringify(valueB);
		} catch (error) {
			console.warn('[FormFieldUtils.areValuesEqual] Failed to compare values', { error });
		}
	}

	return false;
}

interface ChangedFieldsOptions {
	includeReadOnly?: boolean;
	mapFieldToKey?: FieldKeyMapper;
}

export function getChangedEditableFieldValues(
	form: Form,
	options: ChangedFieldsOptions = {}
): Record<string, any> {
	const { includeReadOnly = false, mapFieldToKey = defaultFieldKeyMapper } = options;
	const changedValues: Record<string, any> = {};

	form.fields.forEach(field => {
		if (!includeReadOnly && field.readOnly) {
			return;
		}

		const key = mapFieldToKey(field);
		if (!key) {
			return;
		}

		const initialValue = field.initialValue !== undefined ? field.initialValue : field.value;

		if (!areValuesEqual(field.value, initialValue)) {
			changedValues[key] = field.value;
		}
	});

	return changedValues;
}


