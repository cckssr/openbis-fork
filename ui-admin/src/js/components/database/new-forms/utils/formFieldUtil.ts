import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import objectType from "@src/js/common/consts/objectType";
import { EntityKind } from "@src/js/components/database/new-forms/types/formEnums.ts";
import { setPropertyValue } from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';

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
	dto?: any,
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

			// Apply to DTO using datatype-specific setter when possible.
			// We only do this when the key matches the property code (`field.name`).
			if (dto && field.name && key === field.name) {
				setPropertyValue(dto, key, field.value, field.dataType, field.isMultiValue);
			}
		}
	});

	return changedValues;
}


