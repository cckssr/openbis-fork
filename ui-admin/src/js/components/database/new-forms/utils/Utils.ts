import { FormField } from "@src/js/components/database/new-forms/types/form.types.ts";

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
