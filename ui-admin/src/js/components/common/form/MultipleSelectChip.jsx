import * as React from 'react';
import { Theme, useTheme } from '@mui/material/styles';
import Box from '@mui/material/Box';
import OutlinedInput from '@mui/material/OutlinedInput';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select, { SelectChangeEvent } from '@mui/material/Select';
import Chip from '@mui/material/Chip';
import ListItemText from '@mui/material/ListItemText';
import Checkbox from '@mui/material/Checkbox';
import SelectField from '@src/js/components/common/form/SelectField.jsx';
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx'

const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;
const MenuProps = {
	PaperProps: {
		style: {
			maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
			width: 250,
		},
	},
};

function getStyles(name, selectedOptions, theme) {
	return {
		fontWeight: selectedOptions.includes(name)
			? theme.typography.fontWeightMedium
			: theme.typography.fontWeightRegular,
	};
}

export default function MultipleSelectChip({ options, label, mode, value, ...props }) {
	const theme = useTheme();
	const [selectedOptions, setSelectedOptions] = React.useState([]);

	const handleChange = (event) => {
		const {
			target: { value },
		} = event;
		setSelectedOptions(
			// On autofill we get a stringified value.
			typeof value === 'string' ? value.split(',') : value,
		);
	};

	const renderEdit = () => {
		return (
			<div>
				<SelectField
					label={label}
					value={selectedOptions}
					options={options}
					onChange={handleChange}
				/>
				<FormControl sx={{ m: 1, width: 300 }}>
					<InputLabel id="multiple-select-chip-label">{label}</InputLabel>
					<Select
						labelId="multiple-select-chip-label"
						id="multiple-select-chip"
						multiple
						value={selectedOptions}
						onChange={handleChange}
						input={<OutlinedInput id="multiple-select-chip" label={label} />}
						renderValue={(selected) => (
							<Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
								{selected.map((value) => (
									<Chip key={value} label={value} />
								))}
							</Box>
						)}
						MenuProps={MenuProps}
					>
						{options.map((option) => (
							<MenuItem
								key={option.id}
								value={option.code}
								style={getStyles(option.code, selectedOptions, theme)}
							>
								<Checkbox checked={selectedOptions.includes(option.code)} />
								<ListItemText primary={option.label} />
							</MenuItem>
						))}
					</Select>
				</FormControl>
			</div>
		)
	}

	const renderView = () => {
		return <FormFieldView label={label} value={getOptionText(value)} disableUnderline={true}/>
	}

	const getOptionText = (option) => {
		return (
			<div>
				{value?.map((option) => (
					<Chip key={option.id} label={option.code} />
				))}
			</div>
		)
	}

	return (
		<div>
			{mode === 'view' && renderView()}
			{mode === 'edit' && renderEdit()}
			{mode !== 'view' && mode !== 'edit' && <div>Unsupported mode: {mode}</div>}
		</div>
	);
}