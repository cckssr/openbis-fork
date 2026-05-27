import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import SelectField from '@src/js/components/common/form/SelectField.jsx';
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx';
import MultiValueFieldEditor from './MultiValueFieldEditor.tsx';

export const SwitchFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
	const isEditing = mode === 'edit' || mode === 'create';

	if (field.isMultiValue && !isEditing) {
		const values: any[] = Array.isArray(field.value) ? field.value : [];
		const lines = values.map((v, i) => {
			const text = v === 'true' || v === true ? 'Yes' : v === 'false' || v === false ? 'No' : '';
			return <div key={i}>{text}</div>;
		});
		return (
			<FormFieldView
				label={field.label}
				value={lines.length > 0 ? <>{lines}</> : undefined}
				disableUnderline={true}
			/>
		);
	} else if (field.isMultiValue && isEditing && !field.readOnly) {
		return (
			<MultiValueFieldEditor
				label={field.label}
				required={field.required}
				values={Array.isArray(field.value) ? field.value : []}
				onChange={(vals) => onFieldChange(field.id, vals)}
				renderInput={(val, onChange) => (
					<SelectField
						reference={field}
						options={[{label: '', value: null}, {label: 'Yes', value: 'true'},
							{label: 'No', value: 'false'}]}
						id={field.id}
						name={field.label}
						mandatory={field.required}
						mode="edit"
						disabled={field.readOnly}
						value={val ?? ''}
						onChange={(e: React.ChangeEvent<HTMLInputElement>) => onChange(e.target.value || null)}
						description={field.meta?.helpText}
						emptyOption={field.meta?.emptyOption}
						hiddenLabel={true}
						disableUnderline={true}
					/>
				)}
				isEmpty={(v) => !v}
			/>
		);
	} else {
		return (<SelectField
				reference={field}
				options={[{label: '', value: null}, {label: 'Yes', value: 'true'}, {label: 'No', value: 'false'}]}
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
				disableUnderline={true}
			/>
		);
	}
}