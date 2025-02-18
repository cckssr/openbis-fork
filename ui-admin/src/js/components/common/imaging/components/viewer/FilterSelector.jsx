import React, { useState, useEffect } from 'react';
import { IconButton, Typography, List, ListItem, ListItemText, Divider, Grid2 } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import SaveIcon from '@mui/icons-material/Save';
import EditIcon from '@mui/icons-material/Edit';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import InputSlider from '@src/js/components/common/imaging/components/common/InputSlider.jsx';
import Button from '@src/js/components/common/form/Button.jsx';
import { isObjectEmpty } from '@src/js/components/common/imaging/utils.js';


const FilterSelector = ({ configFilters, onAddFilter, historyFilters }) => {
	const [selectedFilter, setSelectedFilter] = useState('');
	const [history, setHistory] = useState([]);
	const [sliderValues, setSliderValues] = useState({});
	const [editingIndex, setEditingIndex] = useState(null);

	useEffect(() => {
		if (historyFilters && historyFilters.length) {
			setHistory(historyFilters);
		}
	}, [historyFilters]);

	const handleSelect = (event) => {
		setSelectedFilter(event.target.value);
		setSliderValues({});
	};

	const handleSliderChange = (label, value) => {
		setSliderValues((prev) => ({ ...prev, [label]: value }));
	};

	const formatHistoryItem = (filterName, values) => {
		return {
			name: filterName,
			parameters: values
		};
	};

	const addToHistory = () => {
		if (selectedFilter) {
			const selectedControls = configFilters[selectedFilter] || [];
			const values = selectedControls.reduce((acc, control) => {
				acc[control.label] = sliderValues[control.label] || control.range[0];
				return acc;
			}, {});
			const newHistoryItem = formatHistoryItem(selectedFilter, values);
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
		setSelectedFilter(history[index].name);
		setSliderValues(history[index].parameters);
	};

	const applyEdits = () => {
		if (editingIndex !== null) {
			const updatedHistory = [...history];
			updatedHistory[editingIndex] = { name: selectedFilter, parameters: sliderValues };
			setHistory(updatedHistory);
			setEditingIndex(null);
			onAddFilter(updatedHistory);
		}
	};

	const renderFilterControls = () => {
		if (!selectedFilter) return null;
		if (!configFilters[selectedFilter].length) return <Typography px={1}>No adjustable parameters for {selectedFilter}</Typography>;

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
					}}>
					<Grid2 container mt={2} size={12}>
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
					<Grid2 size={12} height='68%'>
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
									<ListItemText primary={`${item.name} - ${Object.entries(item.parameters).map(([key, value]) => `${key}: ${value}`).join(', ')}`} />
									<IconButton edge='end' onClick={() => startEditing(index)} color='inherit'>
										<EditIcon />
									</IconButton>
									<IconButton edge='end' onClick={() => removeFromHistory(index)} color='inherit'>
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
