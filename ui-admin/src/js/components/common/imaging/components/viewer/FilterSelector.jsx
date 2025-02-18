import React, { useState } from 'react';
import { IconButton, Typography, List, ListItem, ListItemText, Divider, Grid2 } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import InputSlider from '@src/js/components/common/imaging/components/common/InputSlider.jsx';
import Button from '@src/js/components/common/form/Button.jsx'
import { isObjectEmpty } from '@src/js/components/common/imaging/utils.js';
import SaveIcon from '@mui/icons-material/Save';
import EditIcon from '@mui/icons-material/Edit';

const FilterSelector = ({ configFilters, onAddFilter }) => {
	const [selectedFilter, setSelectedFilter] = useState('');
	const [history, setHistory] = useState([]);
	const [sliderValues, setSliderValues] = useState({});
	const [editingIndex, setEditingIndex] = useState(null);

	const handleSelect = (event) => {
		setSelectedFilter(event.target.value);
		setSliderValues({}); // Reset sliders when a new filter is selected
	};

	const handleSliderChange = (label, value) => {
		setSliderValues(prev => ({ ...prev, [label]: value }));
	};

	const addToHistory = () => {
		if (selectedFilter) {
			const selectedControls = configFilters[selectedFilter] || [];
			const values = selectedControls.map(control => ({
				label: control.label,
				value: sliderValues[control.label] || control.range[0]
			}));

			const newHistoryItem = { filter: selectedFilter, values };
			const newHistory = [...history, newHistoryItem];
			setHistory(newHistory);
			onAddFilter(newHistory);
		}
	};

	const removeFromHistory = (index) => {
		const newHistory = history.filter((_, i) => i !== index);
		setHistory(newHistory);
		onAddFilter(newHistory);
	};

	const startEditing = (index) => {
		setEditingIndex(index);
		setSelectedFilter(history[index].filter);
		setSliderValues(Object.fromEntries(history[index].values.map(v => [v.label, v.value])));
	};

	const applyEdits = () => {
		if (editingIndex !== null) {
			const updatedHistory = [...history];
			updatedHistory[editingIndex] = {
				filter: selectedFilter,
				values: Object.keys(sliderValues).map(label => ({ label, value: sliderValues[label] }))
			};
			setHistory(updatedHistory);
			setEditingIndex(null);
			onAddFilter(updatedHistory);
		}
	};

	const renderFilterControls = () => {
		if (!selectedFilter) return null;

		if (!configFilters[selectedFilter].length) return <Typography px={1}>No adjustable parameters for {selectedFilter}</Typography>;

		/* if (!selectedFilter || !configFilters[selectedFilter].length) {
			return <InputSlider key={`filters-noslider-${selectedFilter}`}
				label={selectedFilter}
				initValue={1}
				range={[1, 1, 1]}
				disabled={false}
				onChange={() => handleSliderChange(selectedFilter, 1)} />
		} */

		return configFilters[selectedFilter].map((control) => (
			<InputSlider
				key={`slider-${control.label}`}
				label={control.label}
				initValue={sliderValues[control.label] || control.range[0]}
				range={control.range.map(Number)}
				unit={control.unit}
				disabled={false}
				onChange={(_, value) => handleSliderChange(control.label, value)}
			/>
		));
	};

	return (
		<>
			{configFilters != null && !isObjectEmpty(configFilters) ? (
				<Grid2
					container
					direction='row'
					sx={{
						alignContent: 'space-between',
						height: '60vh',
					}} >
					<Grid2 container mt={2} size={12} >
						<Dropdown
							key='filters-dropdown'
							label='Select a Filter'
							initValue={selectedFilter}
							values={Object.keys(configFilters)}
							isMulti={false}
							disabled={false}
							onSelectChange={handleSelect}
						/>
						{renderFilterControls()}
					</Grid2>
					<Grid2 size={12} height='59%'>
						<Button
							label={editingIndex !== null ? 'Save Changes' : 'Add'}
							fullWidth
							variant='outlined'
							color='inherit'
							startIcon={editingIndex !== null ? <SaveIcon /> : <AddIcon />}
							onClick={editingIndex !== null ? applyEdits : addToHistory}
							disabled={!selectedFilter}
						/>
						<Typography sx={{ mt: 1, mb: 1 }} variant='h6'>History</Typography>
						<Divider />
						<List sx={{ height: '68%', overflow: 'auto' }}>
							{history.map((item, index) => (
								<ListItem key={index}>
									<ListItemText primary={`${item.filter} - ${item.values.map(v => `${v.label}: ${v.value}`).join(', ')}`} />
									<IconButton edge="end" onClick={() => startEditing(index)} color="inherit">
										<EditIcon />
									</IconButton>
									<IconButton edge="end" onClick={() => removeFromHistory(index)} color="inherit">
										<DeleteIcon />
									</IconButton>
								</ListItem>
							))}
						</List>
					</Grid2>
				</Grid2>
			) : (
				<Typography px={1} my={1} textAlign={'center'}>No filters configuration provided</Typography>
			)}
		</>
	);
};

export default FilterSelector;
