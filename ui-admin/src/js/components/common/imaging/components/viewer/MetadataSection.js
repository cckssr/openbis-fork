import React from 'react'
import { Typography, Box, TextField, Autocomplete, Checkbox } from '@mui/material';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';
import { isObjectEmpty } from '@src/js/components/common/imaging/utils.js';
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';
import EditableMetadataField from "@src/js/components/common/imaging/components/gallery/EditableMetadataField.jsx";
import { useImagingDataContext } from '@src/js/components/common/imaging/components/viewer/ImagingDataContext.jsx';

const MetadataSection = ({ activePreview, activeImage, imagingTags, onEditComment }) => {

	const currPreviewMetadata = activePreview.metadata;
	const currPreviewTags = activePreview.tags;
	const currPreviewComment = activePreview.comment;
	const { handleTagImage } = useImagingDataContext();

	// State for tags autocomplete
	const [tags, setTags] = React.useState([]);
	const [inputValue, setInputValue] = React.useState('');

	// Update tags when preview tags change
	React.useEffect(() => {
		if (currPreviewTags && currPreviewTags.length > 0) {
			const transformedTags = [];
			for (const activePreviewTag of currPreviewTags) {
				const matchTag = imagingTags.find(imagingTag => imagingTag.value === activePreviewTag);
				if (matchTag) {
					transformedTags.push(matchTag);
				}
			}
			setTags(transformedTags);
			setInputValue(transformedTags.map(t => t.label).join(', '));
		} else {
			setTags([]);
			setInputValue('');
		}
	}, [currPreviewTags, imagingTags]);

	const renderParameters = () => {
		// Use preview metadata dynamically, as it was implemented before
		if (!currPreviewMetadata || isObjectEmpty(currPreviewMetadata)) {
			return null;
		}

		return (
			<CollapsableSection title='Parameters' span={true} isCollapsed={false}>
				<Box sx={{ py: 1 }}>
					{Object.entries(currPreviewMetadata).map(([key, value]) => (
						<Typography 
							key={key}
							variant='body2'
							component='div'
							sx={{
								color: 'textSecondary',
								mb: 0.5
							}}
						>
							<strong>{key}:</strong> {value}
						</Typography>
					))}
				</Box>
			</CollapsableSection>
		);
	};

	const handleTagsChange = (event, newTags) => {
		setTags(newTags);
		const tagsArray = newTags.map(tag => tag.value);
		handleTagImage(false, tagsArray);
	};

	const renderTags = () => {

		return (
			<CollapsableSection title='Tags' span={true} isCollapsed={false}>
				<Box sx={{ py: 1 }}>
					<Autocomplete
						multiple
						id='tags-autocomplete'
						options={imagingTags || []}
						disableCloseOnSelect
						getOptionLabel={(option) => option.label || option}
						inputValue={inputValue}
						value={tags}
						onInputChange={(event, newInputValue) => {
							setInputValue(newInputValue);
						}}
						renderInput={(params) => (
							<TextField variant='standard' label='Tags' {...params} placeholder='Search Tag' />
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
									{option.label || option}
								</li>
							);
						}}
						onChange={handleTagsChange}
						size='small'
					/>
				</Box>
			</CollapsableSection>
		);
	};

	const renderComments = () => {
		return (
			<CollapsableSection title='Comments' span={true} isCollapsed={false}>
				<Box sx={{ py: 1 }}>
					<EditableMetadataField 
						keyProp={"Comment"}
						valueProp={currPreviewComment}
						onEdit={newVal => onEditComment(newVal)} 
					/>
				</Box>
			</CollapsableSection>
		);
	};

	return (
		<CollapsableSection title='Metadata' isCollapsed={false}>
			{renderParameters()}
			{renderTags()}
			{renderComments()}
		</CollapsableSection>
	);
};

export default MetadataSection;