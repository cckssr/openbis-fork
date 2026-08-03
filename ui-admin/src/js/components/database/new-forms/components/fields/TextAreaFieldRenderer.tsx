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
		const lines = values.map((v, i) => (
			<pre
				key={i}
				style={{
					fontFamily: 'inherit',
					fontSize: 'inherit',
					whiteSpace: 'pre-wrap',
					margin: 0,
					marginBottom: i < values.length - 1 ? '8px' : 0,
				}}
			>
				{String(v ?? '')}
			</pre>
		));
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
				required={field.required}
				values={Array.isArray(field.value) ? field.value : []}
				onChange={(vals) => onFieldChange(field.id, vals)}
				renderInput={(val, index, onChange) => (
					<MuiTextField
						label={index === 0 ? field.label : null}
						variant="filled"
						size="small"
						fullWidth
						multiline
						hiddenLabel={index > 0}
						minRows={2}
						maxRows={10}
						value={val ?? ''}
						onChange={(e) => onChange(e.target.value)}
						margin="none"
						sx={{ '& .MuiInputBase-input': { fontSize: '0.875rem' }, '& .MuiInputLabel-root': { fontSize: '0.875rem' } }}
					/>
				)}
			/>
		);
	} else {
		return <MuiTextField
			label={field.label}
			variant="filled"
			size="small"
			fullWidth
			multiline
			hiddenLabel={false}
			minRows={2}
			maxRows={10}
			value={field.value}
			onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
				onFieldChange(field.id, e.target.value)}
			margin="none"
			sx={{ '& .MuiInputBase-input': { fontSize: '0.875rem' }, '& .MuiInputLabel-root': { fontSize: '0.875rem' } }}
		/>;
	}
}