import React, { useState } from 'react';
import { Select, MenuItem, IconButton, Typography, Box, List, ListItem, ListItemText, Slider, Divider } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import InputSlider from '@src/js/components/common/imaging/components/common/InputSlider.jsx';
import Button from '@src/js/components/common/form/Button.jsx'
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';

function flattenObject(obj) {
	return Object.entries(obj).flatMap(([key, items]) =>
		items.map(item => ({ section: key, ...item }))
	);
}

const FilterSelector = ({ data, onApplyFilter }) => {
	const flattenedData = data ? flattenObject(data) : null;

	const [selectedFilter, setSelectedFilter] = useState(null);
	const [selectedValue, setSelectedValue] = useState(null);
	const [history, setHistory] = useState([]);

	const handleSelect = (event) => {
		console.log(event);
		const filter = flattenedData.find(item => item['@id'] === parseInt(event.target.value));
		setSelectedFilter(filter);
		if (filter && filter.type === "Slider") {
			setSelectedValue(parseFloat(filter.range[0]));
		} else {
			setSelectedValue(null);
		}
	};

	const addToHistory = () => {
		if (selectedFilter) {
			const newHistory = [...history, { ...selectedFilter, value: selectedValue }];
			setHistory(newHistory);
			onApplyFilter(newHistory);
		}
	};

	const removeFromHistory = (index) => {
		const newHistory = history.filter((_, i) => i !== index);
		setHistory(newHistory);
		onApplyFilter(newHistory);
	};



	return (
		<Box mt={2}>
			{flattenedData === null ? <Typography sx={{ mt: 1, mb: 1 }} variant='h6' >
				No Filters
			</Typography> :
				<>
					<Dropdown key='filters-dropdown'
						label='Select a Filter'
						initValue={selectedFilter ? selectedFilter['@id'] : ""}
						values={flattenedData}
						isMulti={false}
						disabled={false}
						onSelectChange={handleSelect}
						mappingItemsCallback={(values, label) => {
							const menuItems = []
							menuItems.push(<MenuItem value="" disabled>Select a filter</MenuItem>)
							menuItems.push(values.map(item => (
								<MenuItem key={item['@id']} value={item['@id']}>
									({item.section}) {item.label}
								</MenuItem>
							)))
							return menuItems
						}} />
					<Box mt={2}>
						{selectedFilter && selectedFilter.type === "Slider" && (
							<InputSlider key={`filters-slider-${selectedFilter.label}`}
								label={selectedFilter.label}
								initValue={selectedValue}
								range={selectedFilter.range}
								unit={selectedFilter.unit}
								playable={selectedFilter.playable}
								speeds={selectedFilter.speeds}
								disabled={false}
								onChange={(_, newValue) => setSelectedValue(newValue)} />
						)}
					</Box>

					<Button label='Apply'
						fullWidth
						variant='outlined'
						color='inherit'
						startIcon={<AddIcon />}
						onClick={addToHistory}
						disabled={!selectedFilter} />
				</>
			}
			<Typography sx={{ mt: 1, mb: 1 }} variant='h6' >
				History
			</Typography>
			<Divider></Divider>
			<List>
				{history.map((item, index) => (
					<ListItem key={index} secondaryAction={
						<IconButton edge="end" onClick={() => removeFromHistory(index)} color="secondary">
							<RemoveIcon />
						</IconButton>
					}>
						<ListItemText primary={`${item.label} (${item.section}) - Value: ${item.value}`} />
					</ListItem>
				))}
			</List>
		</Box>
	);
};

export default FilterSelector;
