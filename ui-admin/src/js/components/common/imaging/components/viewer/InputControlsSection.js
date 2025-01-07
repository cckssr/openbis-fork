import React from 'react'
import { Typography } from '@mui/material';
import Dropdown from '@src/js/components/common/imaging/components/common/Dropdown.jsx';
import InputSlider from '@src/js/components/common/imaging/components/common/InputSlider.jsx';
import InputRangeSlider
	from '@src/js/components/common/imaging/components/common/InputRangeSlider.jsx';
import ColorMap from '@src/js/components/common/imaging/components/viewer/ColorMap.jsx';
import constants from '@src/js/components/common/imaging/constants.js';


const InputControlsSection = ({ sectionKey, imageDatasetControlList, inputValues, isUploadedPreview }) => {

	const renderType = (imageDatasetControl, idx) => {
		switch (imageDatasetControl.type) {
			case constants.DROPDOWN:
				return <Dropdown key={`InputsPanel-${imageDatasetControl.type}-${idx}`}
					label={imageDatasetControl.label}
					initValue={inputValues[imageDatasetControl.label]}
					values={imageDatasetControl.values}
					isMulti={imageDatasetControl.multiselect}
					disabled={isUploadedPreview}
					onSelectChange={(event) => onChangeActConf(event.target.name, event.target.value)} />;
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
				return <ColorMap key={`InputsPanel-${imageDatasetControl.type}-${idx}`}
					values={imageDatasetControl.values}
					disabled={isUploadedPreview}
					initValue={inputValues[imageDatasetControl.label]}
					label={imageDatasetControl.label}
					onSelectChange={(event) => onChangeActConf(event.target.name, event.target.value)} />;
		}
	}

	return (<>
		<Typography sx={{m: '8px 0 8px 0'}}><strong>Section:</strong> {sectionKey ? sectionKey : '-'} </Typography>
		{imageDatasetControlList.map((imageDatasetControl, idx) => renderType(imageDatasetControl, idx))}
	</>);
}

export default InputControlsSection;