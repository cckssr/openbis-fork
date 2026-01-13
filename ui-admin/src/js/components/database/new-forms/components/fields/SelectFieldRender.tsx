import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import SelectField from '@src/js/components/common/form/SelectField.jsx';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';

export const SelectFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
	const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;
	return (<SelectField
		reference={field}
		id={field.id}
		name={field.label}
		mandatory={field.required}
		label={field.label}
		mode={isEditing && !field.readOnly ? FormMode.EDIT : FormMode.VIEW}
		disabled={isEditing && field.readOnly}
		value={field.value}
		onChange={(e: React.ChangeEvent<HTMLInputElement>) => onFieldChange(field.id, e.target.value)}
		description={field.meta?.helpText}
		emptyOption={null}
		disableUnderline={true}
		options={field.options}
	/>
	);
}