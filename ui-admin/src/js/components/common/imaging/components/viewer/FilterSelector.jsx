import React from 'react';
import { IconButton, Typography, List, ListItem, ListItemText, Divider, Grid2 } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import SaveIcon from '@mui/icons-material/Save';
import EditIcon from '@mui/icons-material/Edit';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import InputSlider from '@src/js/components/common/imaging/components/common/InputSlider.jsx';
import Button from '@src/js/components/common/form/Button.jsx';
import { isObjectEmpty } from '@src/js/components/common/imaging/utils.js';
import { DragDropContext, Droppable, Draggable } from '@atlaskit/pragmatic-drag-and-drop-react-beautiful-dnd-migration';


const FilterSelector = ({ configFilters, onAddFilter, historyFilters }) => {
	const [selectedFilter, setSelectedFilter] = React.useState('');
    const [history, setHistory] = React.useState([]);
    const [sliderValues, setSliderValues] = React.useState({});
    const [editingIndex, setEditingIndex] = React.useState(null);

	const isEditing = editingIndex !== null; 

    React.useEffect(() => {
        if (historyFilters) {
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

    const transformParameters = React.useCallback((parameters) => {
        return Object.entries(parameters).reduce((acc, [key, value]) => {
            acc[key] = Array.isArray(value) && value.length === 1 ? value[0] : value;
            return acc;
        }, {});
    }, []);

	const getValuesFromSelectedFilter = () => {
		const selectedControls = configFilters[selectedFilter] || []; // Get the controls for the selected filter

		const values = selectedControls.reduce((acc, control) => { // use selectedControls here
			acc[control.label] = sliderValues[control.label] || control.range[0]; // Access by control.label
			return acc;
		}, {});

		return transformParameters(values);
	}

	const formatHistoryItem = (filterName, values) => {
		return {
			name: filterName,
			parameters: values
		};
	};

    const updateHistory = React.useCallback((newHistory) => {
        setHistory(newHistory);
        onAddFilter(newHistory);
    }, [onAddFilter]);



    const addToHistory = () => {
        if (selectedFilter) {
            const transformedValues = getValuesFromSelectedFilter();
            updateHistory([...history, formatHistoryItem(selectedFilter, transformedValues)]);
        }
    };

    const applyEdits = () => {
        if (editingIndex !== null) {
            const transformedValues = getValuesFromSelectedFilter();
            const updatedHistory = [...history];
            updatedHistory[editingIndex] = formatHistoryItem(selectedFilter, transformedValues);
            updateHistory(updatedHistory);
            setEditingIndex(null);
        }
    };

    const startEditing = (index) => {
        setEditingIndex(index);
        const item = history[index];
        setSelectedFilter(item.name);
        setSliderValues(item.parameters);
    };

    const removeFromHistory = (index) => {
        updateHistory(history.filter((_, i) => i !== index));
    };

	const onDragEnd = (result) => {
		if (!result.destination || isEditing) {
            return;
        }
		const items = Array.from(history);
		const [reorderedItem] = items.splice(result.source.index, 1);
		items.splice(result.destination.index, 0, reorderedItem);
		updateHistory(items);
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
						<DragDropContext onDragEnd={onDragEnd}>
							<Droppable droppableId='history-list'>
								{(provided) => (
									<List
										{...provided.droppableProps}
										ref={provided.innerRef}
										sx={{ height: '68%', overflow: 'auto' }}
									>
										{history.map((item, index) => (
											<Draggable key={index} draggableId={index.toString()} index={index} isDragDisabled={isEditing}>
												{(provided, snapshot) => (
													<ListItem
														{...provided.draggableProps}
														{...provided.dragHandleProps}
														ref={provided.innerRef}
														sx={{
															border: editingIndex === index ? '2px solid #039be5' : 'none',
															borderRadius: '5px',
															backgroundColor: snapshot.isDragging ? 'lightgray' : 'white', // Visual feedback while dragging
															transition: 'background-color 0.2s ease',
															display: 'flex', // Important for drag and drop to work correctly
															alignItems: 'center', // Vertically align items
															cursor: isEditing ? 'default' : (snapshot.isDragging ? 'grabbing' : 'grab'),
														}}
													>
														<ListItemText primary={`${item.name} - ${Object.entries(item.parameters).map(([key, value]) => `${key}: ${value}`).join(', ')}`} />
														<IconButton edge='end' onClick={() => startEditing(index)} color='inherit'>
															<EditIcon />
														</IconButton>
														<IconButton edge='end' onClick={() => removeFromHistory(index)} color='inherit'>
															<DeleteIcon />
														</IconButton>
													</ListItem>
												)}
											</Draggable>
										))}
										{provided.placeholder} {/* Important: This is needed for the drag and drop to work */}
									</List>
								)}
							</Droppable>
						</DragDropContext>
					</Grid2>
				</Grid2>
			) : (
				<Typography px={1} my={1} textAlign={'center'}>No filters configuration provided</Typography>
			)}
		</>
	);
};

export default FilterSelector;
