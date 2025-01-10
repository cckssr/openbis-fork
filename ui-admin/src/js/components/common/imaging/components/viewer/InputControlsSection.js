import React from 'react'
import { MenuItem, Typography } from '@mui/material';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import InputSlider from '@src/js/components/common/imaging/components/common/InputSlider.jsx';
import InputRangeSlider
	from '@src/js/components/common/imaging/components/common/InputRangeSlider.jsx';
import constants from '@src/js/components/common/imaging/constants.js';

const ColorItem = ({ colorMapValue }) => {
	return (
		<span style={{ background: `linear-gradient(90deg, ${constants.DEFAULT_COLORMAP[colorMapValue]})`, width: '70%', height: '15px', marginLeft: '10px' }} />
	)
}

const InputControlsSection = ({ sectionKey, imageDatasetControlList, inputValues, isUploadedPreview, onChangeActConf }) => {

	const renderColorMapItems = (values, label) => {
		return values.map((v, i) => <MenuItem key={'select-' + label + '-menuitem-' + i} value={v}>
			<span style={{ width: '30%' }}>{v}</span> <ColorItem colorMapValue={v} />
		</MenuItem>)
	}

	const renderType = (imageDatasetControl, idx) => {
		switch (imageDatasetControl.type) {
			case constants.DROPDOWN:
				return <Dropdown key={`InputsPanel-${imageDatasetControl.type}-${idx}`}
					label={imageDatasetControl.label}
					initValue={inputValues[imageDatasetControl.label]}
					values={imageDatasetControl.values}
					isMulti={imageDatasetControl.multiselect}
					disabled={isUploadedPreview}
					onSelectChange={(event) => onChangeActConf(event.target.name, event.target.value)}
					mappingItemsCallback={null} />;
			case constants.SLIDER:
				return <InputSlider key={`InputsPanel-${imageDatasetControl.type}-${idx}`}
					label={imageDatasetControl.label}
					initValue={inputValues[imageDatasetControl.label]}
					range={imageDatasetControl.range}
					unit={imageDatasetControl.unit}
					playable={imageDatasetControl.playable && !isUploadedPreview}
					speeds={imageDatasetControl.speeds}
					disabled={isUploadedPreview}
					onChange={(name, value, update) => onChangeActConf(name, value, update)} />;
			case constants.RANGE:
				return <InputRangeSlider key={`InputsPanel-${imageDatasetControl.type}-${idx}`}
					label={imageDatasetControl.label}
					initValue={inputValues[imageDatasetControl.label]}
					range={imageDatasetControl.range}
					disabled={isUploadedPreview || imageDatasetControl.range.findIndex(n => n === 'nan') !== -1}
					unit={imageDatasetControl.unit}
					playable={imageDatasetControl.playable && !isUploadedPreview}
					speeds={imageDatasetControl.speeds}
					onChange={(name, value, update) => onChangeActConf(name, value, update)} />;
			case constants.COLORMAP:
				return <Dropdown key={`InputsPanel-${imageDatasetControl.type}-${idx}`}
					label={imageDatasetControl.label}
					initValue={inputValues[imageDatasetControl.label]}
					values={imageDatasetControl.values}
					isMulti={imageDatasetControl.multiselect}
					disabled={isUploadedPreview}
					onSelectChange={(event) => onChangeActConf(event.target.name, event.target.value)}
					mappingItemsCallback={renderColorMapItems} />;
		}
	}

	return (<>
		<Typography component='legend' sx={{ m: '8px 0 8px 0', /* borderBottom: '1px solid rgb(0, 0, 0, 0.12)' */ width: '100%' }}>{sectionKey ? sectionKey : '-'}</Typography>
		{imageDatasetControlList.map((imageDatasetControl, idx) => renderType(imageDatasetControl, idx))}
	</>);
}

export default InputControlsSection;