import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import SelectField from '@src/js/components/common/form/SelectField.jsx';
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import Checkbox from '@mui/material/Checkbox';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';

export const SelectFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
	const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;

	if (field.isMultiValue && isEditing && !field.readOnly) {
		const selectedCodes = Array.isArray(field.value) ? field.value : [];
		const selectedOptions = selectedCodes
			.map((code: string) => field.options?.find(o => o.value === code))
			.filter(Boolean) as { label: string; value: string }[];

		return (
			<Autocomplete
				multiple
				disableCloseOnSelect
				options={field.options || []}
				value={selectedOptions}
				getOptionLabel={(option) => option.label || option.value}
				isOptionEqualToValue={(option, val) => option.value === val.value}
				onChange={(_, newValue) =>
					onFieldChange(field.id, newValue.map((o) => o.value))
				}
				renderOption={(props, option, { selected }) => {
					const { key, ...optionProps } = props;
					return (
						<li key={key} {...optionProps}>
							<Checkbox
								icon={<CheckBoxOutlineBlankIcon fontSize="small" />}
								checkedIcon={<CheckBoxIcon fontSize="small" />}
								style={{ marginRight: 8 }}
								checked={selected}
							/>
							{option.label || option.value}
						</li>
					);
				}}
				renderInput={(params) => (
					<TextField
						{...params}
						label={field.label}
						variant="filled"
						required={field.required}
					/>
				)}
			/>
		);
	}

	if (field.isMultiValue && !isEditing) {
		const codes = Array.isArray(field.value) ? field.value : [];
		const lines = codes.map((code: string, i: number) => {
			const label = field.options?.find(o => o.value === code)?.label ?? code;
			return <div key={i}>{label}</div>;
		});
		return (
			<FormFieldView
				label={field.label}
				value={lines.length > 0 ? <>{lines}</> : undefined}
				description={field.meta?.helpText}
				disableUnderline={true}
			/>
		);
	}

	if (isEditing && !field.readOnly) {
		const selectedOption = field.options?.find(o => o.value === field.value) || null;

		return (
			<Autocomplete
				options={field.options || []}
				value={selectedOption}
				getOptionLabel={(option) => option.label || option.value}
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
			/>
		);
	} else {
		return (
			<SelectField
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
				options={field.options || []}
			/>
		);
	}
}