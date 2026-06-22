import React from 'react'
import { Typography, Box } from '@mui/material';
import { isObjectEmpty } from '@src/js/components/common/imaging/utils.js';
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';
import EditableMetadataField from "@src/js/components/common/imaging/components/gallery/EditableMetadataField.jsx";
import { useImagingDataContext } from '@src/js/components/common/imaging/components/viewer/ImagingDataContext.jsx';
import TagsAutocomplete from '@src/js/components/common/imaging/components/viewer/TagsAutocomplete.jsx';

const MetadataSection = ({ activePreview, activeImage, onEditComment }) => {

	const currPreviewMetadata = activePreview.metadata;
	const currPreviewTags = activePreview.tags;
	const currPreviewComment = activePreview.comment;
	const currImageMetadata = activeImage.metadata;
	const configMetadata = activeImage.config.metadata;

	const { handleTagImage, state} = useImagingDataContext();
	const { imagingTags } = state;

	const renderImageParameters = () => {
		if (!currImageMetadata || isObjectEmpty(currImageMetadata)) {
			return null;
		}

		return (
			<Box sx={{ py: 1 }}>
				{Object.entries(currImageMetadata).map(([key, value]) => (
					<Typography key={key}
						variant='body2'
						component='div'
						sx={{color: 'textSecondary'}}
					>
						<strong>{key}:</strong> {value}
					</Typography>
				))}
			</Box>
		);
	};

	const renderPreviewParameters = () => {
		if (!currPreviewMetadata || isObjectEmpty(currPreviewMetadata)) {
			return null;
		}

		return (
			<Box sx={{ py: 1 }}>
				{
				Object.entries(currPreviewMetadata).map(([key, value]) => (
					<Typography key={key}
					            variant='body2'
					            component='div'
					            sx={{color: 'textSecondary'}}
					>
						<strong>{key}:</strong> {value}
					</Typography>
				))
				}
			</Box>
		);
	};

	const handleTagsChange = (event, tagsArray) => {
		handleTagImage(false, tagsArray);
	};

	const renderTags = () => {
		return (
			<Box sx={{ py: 1 }}>
				<TagsAutocomplete
					activePreviewTags={currPreviewTags}
					imagingTags={imagingTags}
					label='Preview Tags'
					size='small'
					onChange={handleTagsChange}
				/>
			</Box>
		);
	};

	const renderComments = () => {
		return (
			<Box sx={{ py: 1 }}>
				<EditableMetadataField
					key={`comment-${activePreview.index}`}
					keyProp={"Comment"}
					valueProp={currPreviewComment}
					onEdit={newVal => onEditComment(newVal)}
				/>
			</Box>
		);
	};

	return (
		<CollapsableSection title='Parameters' isCollapsed={false}>
			<div style={{ marginLeft: '32px' }}>
				{renderImageParameters()}
				{renderPreviewParameters()}
				{renderTags()}
				{renderComments()}
			</div>
		</CollapsableSection>
	);
};

export default MetadataSection;