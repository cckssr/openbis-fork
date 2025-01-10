import React from 'react'
import { Divider, Grid2, TextField, Autocomplete, Checkbox, Typography, FormControlLabel, styled, Switch } from '@mui/material';
import { inRange, isObjectEmpty } from '@src/js/components/common/imaging/utils.js';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import constants from '@src/js/components/common/imaging/constants.js';
import CustomSwitch from '@src/js/components/common/imaging/components/common/CustomSwitch.jsx';
import RefreshIcon from '@mui/icons-material/Refresh';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import messages from '@src/js/common/messages.js'
import Message from '@src/js/components/common/form/Message.jsx'
import Button from '@src/js/components/common/form/Button.jsx'
import InputControlsSection from '@src/js/components/common/imaging/components/viewer/InputControlsSection.js';
import VisibilityIcon from '@mui/icons-material/Visibility';

const MainPreviewInputControls = ({ activePreview, configInputs, configResolutions, isUserGenerated, resolution, isChanged,
	onClickUpdate, onChangeShow, onSelectChangeRes, onChangeActConf, imagingTags, handleTagImage, datasetType }) => {
	const [tags, setTags] = React.useState([])
	const [inputValue, setInputValue] = React.useState('');

	React.useEffect(() => {
		if (isUserGenerated)
			onClickUpdate();
	}, [])

	React.useEffect(() => {
		if (activePreview && activePreview.tags != null) {
			var trasformedTags = []
			for (const activePreviewTag of activePreview.tags) {
				const matchTag = imagingTags.find(imagingTag => imagingTag.value === activePreviewTag);
				trasformedTags.push(matchTag);
			}
			setTags(trasformedTags);
			setInputValue(trasformedTags.join(', '));
		}
	}, [activePreview])

	const handleTagsChange = (event, newTags) => {
		setTags(newTags);
		const tagsArray = newTags.map(tag => tag.value);
		handleTagImage(false, tagsArray);
	}

	const createInitValues = (inputsConfig, activeConfig) => {
		const isActiveConfig = isObjectEmpty(activeConfig);
		return Object.fromEntries(inputsConfig.map(input => {
			switch (input.type) {
				case constants.DROPDOWN:
					if (!isActiveConfig)
						return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.multiselect ? [input.values[0]] : input.values[0]];
					else
						return [input.label, input.multiselect ? [input.values[0]] : input.values[0]];
				case constants.SLIDER:
					if (!isActiveConfig) {
						if (input.visibility) {
							for (const condition of input.visibility) {
								if (condition.values.includes(activeConfig[condition.label])) {
									input.range = condition.range;
									input.unit = condition.unit;
								}
							}
						}
						return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.range[0]];
					} else {
						if (input.visibility) {
							input.range = input.visibility[0].range[0];
						}
						return [input.label, input.range[0]];
					}
				case constants.RANGE:
					if (!isActiveConfig) {
						if (input.visibility) {
							for (const condition of input.visibility) {
								if (condition.values.includes(activeConfig[condition.label])) {
									input.range = condition.range;
									input.unit = condition.unit;
								}
							}
						}
						let rangeInitValue = [inRange(activeConfig[input.label][0], input.range[0], input.range[1]) ? activeConfig[input.label][0] : input.range[0],
						inRange(activeConfig[input.label][1], input.range[0], input.range[1]) ? activeConfig[input.label][1] : input.range[1]]
						return [input.label, rangeInitValue];
					} else {
						if (input.visibility) {
							return [input.label, [input.visibility[0].range[0], input.visibility[0].range[1]]];
						}
						return [input.label, [input.range[0], input.range[1]]];
					}
				case constants.COLORMAP:
					if (!isActiveConfig)
						return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.values[0]];
					else
						return [input.label, input.values[0]];
			}
		}));
	};

	const renderStaticUpdateControls = (isUploadedPreview,) => {
		return (<>
			<Grid2 container spacing={2} direction='row' sx={{ alignItems: 'center', mb: 1, justifyContent: 'space-between' }} size={{ xs: 12, sm: 12 }}>
				<Button label={messages.get(messages.UPDATE)}
					variant='outlined'
					color='primary'
					startIcon={<RefreshIcon />}
					onClick={onClickUpdate}
					disabled={!isChanged || isUploadedPreview} />

				{/* {isChanged && !isUploadedPreview && (
				<Message type='info'>
					{messages.get(messages.UPDATE_CHANGES)}
				</Message>
			)} */}

				<CustomSwitch labelPlacement='start'
					label={messages.get(messages.SHOW)}
					isChecked={activePreview.show}
					onChange={onChangeShow} />
			</Grid2>
			<Dropdown onSelectChange={onSelectChangeRes}
				label={messages.get(messages.RESOLUTIONS)}
				values={configResolutions}
				initValue={resolution.join('x')}
				isMulti={false}
				disabled={false}
				key={'InputsPanel-resolutions'} />

			<Grid2 sx={{ alignItems: 'center', mb: 1, px: 1 }} size={{ xs: 12, sm: 12 }}>
				<Autocomplete multiple
					id='tags-autocomplete'
					options={imagingTags}
					disableCloseOnSelect
					getOptionLabel={(option) => option.label}
					inputValue={inputValue}
					value={tags}
					onInputChange={(event, newInputValue) => {
						setInputValue(newInputValue);
					}}
					renderInput={(params) => (
						<TextField variant='standard' label='Preview Tags' {...params} placeholder='Search Tag' />
					)}
					renderOption={(props, option, { selected }) => {
						const { key, ...optionProps } = props;
						return (
							<li key={key} {...optionProps}>
								<Checkbox
									icon={<CheckBoxOutlineBlankIcon fontSize='small' />}
									checkedIcon={<CheckBoxIcon fontSize='small' />}
									style={{ marginRight: 8 }}
									checked={selected}
								/>
								{option.label}
							</li>
						);
					}}
					onChange={handleTagsChange}
				/>
			</Grid2>
		</>)
	}

	const renderDynamicControls = (configInputs) => {
		const sectionGroups = Map.groupBy(configInputs, imageDatasetControl => imageDatasetControl.section)
		var sectionsArray = []
		for (let [key, imageDatasetControlList] of sectionGroups) {
			sectionsArray.push(<InputControlsSection key={key} sectionKey={key} imageDatasetControlList={imageDatasetControlList} inputValues={inputValues} isUploadedPreview={isUploadedPreview} onChangeActConf={onChangeActConf} />)
		}
		return sectionsArray;
	}

	const inputValues = createInitValues(configInputs, activePreview.config);
	activePreview.config = inputValues;
	const currentMetadata = activePreview.metadata;
	const isUploadedPreview = datasetType === constants.USER_DEFINED_IMAGING_DATA ? true : isObjectEmpty(currentMetadata) ? false : ('file' in currentMetadata);
	return (
		<Grid2 container direction='row' size={{ sm: 12, md: 4 }} sx={{ padding: '8px', display: 'block' }}>

			<Grid2 container sx={{ justifyContent: 'space-between', maxHeight: '30%', width: '100%' }}>
				{(datasetType === constants.IMAGING_DATA || datasetType === constants.USER_DEFINED_IMAGING_DATA)
					&& renderStaticUpdateControls(isUploadedPreview)}

			</Grid2>
			<Grid2 container size={{ xs: 12, sm: 12 }}>
				<Divider variant='middle' sx={{ margin: '16px 0px 16px 0px', width: '100%', height: '2px' }} />
			</Grid2>
			<Grid2 container sx={{ justifyContent: 'space-between', overflow: 'auto', maxHeight: '70%', width: '100%', minHeight: '300px' }}>
				{configInputs !== null && configInputs.length > 0 ? renderDynamicControls(configInputs) : <Typography>Configuration inputs malformatted</Typography>}
			</Grid2>
		</Grid2>
	);
};

export default MainPreviewInputControls;