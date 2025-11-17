import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/form.types.ts';
import SelectField from '@src/js/components/common/form/SelectField.jsx';

export const SwitchFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
	const isEditing = mode === 'edit' || mode === 'create';
	return (<SelectField
		reference={field}
		options={[{ label: '', value: null }, { label: 'Yes', value: true }, { label: 'No', value: false }]}
		id={field.id}
		name={field.label}
		mandatory={field.required}
		label={field.label}
		mode={isEditing && !field.readOnly ? 'edit' : 'view'}
		disabled={isEditing && field.readOnly}
		value={field.value}
		onChange={(e: React.ChangeEvent<HTMLInputElement>) => onFieldChange(field.id, e.target.value)}
		description={field.meta?.helpText}
		emptyOption={field.meta?.emptyOption}
	/>
	);
}