import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import TextAreaField from '@src/js/components/common/form/TextAreaField.jsx';
import { FormFieldDataType } from '@src/js/components/database/new-forms/types/formEnums.ts';

export const TextAreaFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
	const isEditing = mode === 'edit' || mode === 'create';
	return (<TextAreaField id={field.id}
		name={field.label}
		mandatory={field.required}
		label={field.label}
		mode={isEditing && !field.readOnly ? 'edit' : 'view'}
		disabled={isEditing && field.readOnly}
		value={field.value}
		onChange={(e: React.ChangeEvent<HTMLInputElement>) => onFieldChange(field.id, e.target.value)}
		disableUnderline={true}
		description={field.meta?.helpText}
		styles={field.dataType === FormFieldDataType.MONOSPACE_FONT ? { monospaceFont: true } : {}}
	/>
	);
}