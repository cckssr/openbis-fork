import React from 'react'
import { Chip, Divider, Typography } from "@mui/material";
import { isObjectEmpty } from "@src/js/components/common/imaging/utils.js";
import DefaultMetadataField from "@src/js/components/common/imaging/components/gallery/DefaultMetadataField.js";

const MetadataSection = ({ activePreview, activeImage, imagingTags }) => {

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
		return (<>
			<Typography gutterBottom variant='h6'>
				Preview Metadata
			</Typography>
			<Typography key={`preview-comment-${activePreview.index}`} variant="body2"
				component={'span'} sx={{
					color: "textSecondary"
				}}>
				{(currPreviewComment === null || currPreviewComment === "" ) 
					&& (currPreviewTags === null || currPreviewTags.length === 0) 
					&& isObjectEmpty(currPreviewMetadata) &&
					<p>No preview metadata to display</p>}

				{(currPreviewComment !== null && currPreviewComment !== "")  && 
					<DefaultMetadataField key={'preview-comment-metadata'} label='Preview Comment'
							value={currPreviewComment} />}
				
				{(currPreviewTags !== null && currPreviewTags.length > 0) && matchTagsToLabel()}
			</Typography>
			<Typography key={`preview-metadata-${activePreview.index}`} variant="body2"
				component={'span'} sx={{
					color: "textSecondary"
				}}>
				{!isObjectEmpty(currPreviewMetadata) &&
					Object.entries(currPreviewMetadata).map(([key, value], pos) =>
						<DefaultMetadataField key={'preview-property-' + pos} label={'(raw metadata) ' + key}
							value={value} />)
				}
			</Typography>
		</>);
	}

	const renderImageMetadata = () => {
		return (<>
			<Typography gutterBottom variant='h6'>
				Image Metadata
			</Typography>
			<Typography key={`image-metadata-${activeImage.index}`} variant="body2"
				component={'span'} sx={{
					color: "textSecondary"
				}}>
				{(currImageMetadata === null || isObjectEmpty(currImageMetadata)) ?
					<p>No image metadata to display</p>
					: Object.entries(currImageMetadata).map(([key, value], pos) =>
						<DefaultMetadataField key={'image-property-' + pos} label={key}
							value={value}/>)
				}
			</Typography>
		</>);
	}

	return (<>
		{renderPreviewMetadata()}
		<Divider variant='middle' />
		{renderImageMetadata()}
	</>
	);
};

export default MetadataSection;