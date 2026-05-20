import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import TextAreaField from '@src/js/components/common/form/TextAreaField.jsx';
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx';
import MultiValueFieldEditor from './MultiValueFieldEditor.tsx';
import MuiTextField from '@mui/material/TextField';
import { FormFieldDataType } from '@src/js/components/database/new-forms/types/formEnums.ts';

export const TextAreaFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
	const isEditing = mode === 'edit' || mode === 'create';

	if (field.isMultiValue && !isEditing) {
		const values: any[] = Array.isArray(field.value) ? field.value : [];
		const lines = values.map((v, i) => <div key={i}>{String(v ?? '')}</div>);
		return (
			<FormFieldView
				label={field.label}
				value={lines.length > 0 ? <>{lines}</> : undefined}
				disableUnderline={true}
				description={field.meta?.helpText}
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
					<MuiTextField
						variant="filled"
						size="small"
						fullWidth
						multiline
						hiddenLabel
						minRows={2}
						value={val ?? ''}
						onChange={(e) => onChange(e.target.value)}
						margin="dense"
					/>
				)}
			/>
		);
	} else {
		return (<TextAreaField id={field.id}
							   name={field.label}
							   mandatory={field.required}
							   label={field.label}
							   mode={isEditing && !field.readOnly ? 'edit' : 'view'}
							   disabled={isEditing && field.readOnly}
							   value={field.value}
							   onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
								   onFieldChange(field.id, e.target.value)}
							   disableUnderline={true}
							   description={field.meta?.helpText}
							   styles={field.dataType === FormFieldDataType.MONOSPACE_FONT
								   ? {monospaceFont: true} : {}}
			/>
		);
	}
}