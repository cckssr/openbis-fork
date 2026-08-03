import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import SelectField from '@src/js/components/common/form/SelectField.jsx';
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx';
import MultiValueFieldEditor from './MultiValueFieldEditor.tsx';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';

const BOOLEAN_OPTIONS = [{ label: 'true', value: 'true' }, { label: 'false', value: 'false' }];

export const SwitchFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
	const isEditing = mode === 'edit' || mode === 'create';

	if (field.isMultiValue && !isEditing) {
		const values: any[] = Array.isArray(field.value) ? field.value : [];
		const lines = values.map((v, i) => {
			const text = v === 'true' || v === true ? 'true' : v === 'false' || v === false ? 'false' : '';
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
				required={field.required}
				values={Array.isArray(field.value) ? field.value : []}
				onChange={(vals) => onFieldChange(field.id, vals)}
				renderInput={(val, index, onChange) => {
					const selectedOption = BOOLEAN_OPTIONS.find(o => o.value === val) || null;
					return (
						<Autocomplete
							options={BOOLEAN_OPTIONS}
							value={selectedOption}
							getOptionLabel={(option) => option.label}
							isOptionEqualToValue={(option, v) => option.value === v.value}
							onChange={(_, newValue) => onChange(newValue ? newValue.value : null)}
							renderInput={(params) => (
								<TextField
									{...params}
									label={index === 0 ? field.label : null}
									hiddenLabel={index > 0}
									variant="filled"
									required={field.required && index === 0}
									sx={{ '& .MuiInputBase-input': { fontSize: '0.875rem' }, '& .MuiInputLabel-root': { fontSize: '0.875rem' } }}
								/>
							)}
						/>
					);
				}}
				isEmpty={(v) => !v}
			/>
		);
	} else if (isEditing && !field.readOnly) {
		const selectedOption = BOOLEAN_OPTIONS.find(o => o.value === field.value) || null;

		return (
			<Autocomplete
				options={BOOLEAN_OPTIONS}
				value={selectedOption}
				getOptionLabel={(option) => option.label}
				isOptionEqualToValue={(option, val) => option.value === val.value}
				onChange={(_, newValue) =>
					onFieldChange(field.id, newValue ? newValue.value : null)
				}
				renderInput={(params) => (
					<TextField
						{...params}
						label={field.label}
						variant="filled"
						required={field.required}
					/>
				)}
				sx={{ '& .MuiInputBase-input': { fontSize: '0.875rem' }, '& .MuiInputLabel-root': { fontSize: '0.875rem' } }}
			/>
		);
	} else {
		return (<SelectField
				reference={field}
				options={BOOLEAN_OPTIONS}
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