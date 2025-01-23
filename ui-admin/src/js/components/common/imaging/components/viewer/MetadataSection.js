import React from 'react'
import { Chip, Divider, Typography } from '@mui/material';
import { isObjectEmpty } from '@src/js/components/common/imaging/utils.js';
import DefaultMetadataField from '@src/js/components/common/imaging/components/gallery/DefaultMetadataField.js';
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';
import EditableMetadataField from "@src/js/components/common/imaging/components/gallery/EditableMetadataField.jsx";

const MetadataSection = ({ activePreview, activeImage, imagingTags, onEditComment }) => {

	const currImageMetadata = activeImage.metadata;
	const configMetadata = activeImage.config.metadata;
	const currPreviewMetadata = activePreview.metadata;
	const currPreviewTags = activePreview.tags;
	const currPreviewComment = activePreview.comment;

	const matchTagsToLabel = () => {
		var trasformedTags = []
		for (const activePreviewTag of currPreviewTags) {
			const matchTag = imagingTags.find(imagingTag => imagingTag.value === activePreviewTag);
			trasformedTags.push(matchTag.label);
		}
		return <DefaultMetadataField key={'property-tags'}
			label={'Preview Tags'}
			value={trasformedTags.map(item => (<Chip sx={{ mr: '4px' }} key={item}
				size='small'
				tabIndex={-1}
				label={item} />))} />
	}

	const renderPreviewMetadata = () => {
		return (<CollapsableSection title='Preview Metadata' span={true} isCollapsed={false}>

			<Typography key={`preview-comment-${activePreview.index}`} variant='body2'
				component={'span'} sx={{
					color: 'textSecondary'
				}}>
				{(currPreviewComment === null || currPreviewComment === '')
					&& (currPreviewTags === null || currPreviewTags.length === 0)
					&& isObjectEmpty(currPreviewMetadata) &&
					<p>No preview metadata to display</p>}
				{(currPreviewComment !== null && currPreviewComment !== '') &&
						<EditableMetadataField keyProp={"Preview Comment"}
            valueProp={currPreviewComment}
            onEdit={newVal => onEditComment(newVal)} />}

				{(currPreviewTags !== null && currPreviewTags.length > 0) && matchTagsToLabel()}
			</Typography>
			<Typography key={`preview-metadata-${activePreview.index}`} variant='body2'
				component={'span'} sx={{
					color: 'textSecondary'
				}}>
				{!isObjectEmpty(currPreviewMetadata) &&
					Object.entries(currPreviewMetadata).map(([key, value], pos) =>
						<DefaultMetadataField key={'preview-property-' + pos} label={'(raw metadata) ' + key}
							value={value} />)
				}
			</Typography>
		</CollapsableSection>);
	}

	const renderImageMetadata = () => {
		return (<CollapsableSection title='Image Metadata' span={true} isCollapsed={false}>
			<Typography key={`image-metadata-${activeImage.index}`} variant='body2'
				component={'span'} sx={{
					color: 'textSecondary'
				}}>
				{(currImageMetadata === null || isObjectEmpty(currImageMetadata)) ?
					<p>No image metadata to display</p>
					: Object.entries(currImageMetadata).map(([key, value], pos) =>
						<DefaultMetadataField key={'image-property-' + pos} label={key}
							value={value} />)
				}
			</Typography>
		</CollapsableSection>);
	}

	return (<>
		{renderPreviewMetadata()}
		{renderImageMetadata()}
	</>
	);
};

export default MetadataSection;