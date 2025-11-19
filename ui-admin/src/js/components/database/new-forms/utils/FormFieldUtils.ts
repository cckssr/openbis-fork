import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';

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


