import React from 'react'
import { Divider, Typography } from "@mui/material";
import { isObjectEmpty } from "@src/js/components/common/imaging/utils.js";
import PaperBox from "@src/js/components/common/imaging/components/common/PaperBox.js";
import DefaultMetadaField
	from "@src/js/components/common/imaging/components/gallery/DefaultMetadaField.js";

const MetadataSection = ({ activePreview, activeImage }) => {

	const currImageMetadata = activeImage.metadata;
	const configMetadata = activeImage.config.metadata;
	const currPreviewMetadata = activePreview.metadata;
	const currPreviewTags = activePreview.tags;
	const currPreviewComment = activePreview.comment;

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
					<DefaultMetadaField key={'preview-comment-metadata'} keyProp='Preview Comment'
							valueProp={currPreviewComment} idx={activeImage.index} />}
				{(currPreviewTags !== null && currPreviewTags.length > 0) && <DefaultMetadaField key={'preview-tags-metadata'} keyProp='Preview Tags'
							valueProp={currPreviewTags} idx={activeImage.index} />}
			</Typography>
			<Typography key={`preview-metadata-${activePreview.index}`} variant="body2"
				component={'span'} sx={{
					color: "textSecondary"
				}}>
				{!isObjectEmpty(currPreviewMetadata) &&
					Object.entries(currPreviewMetadata).map(([key, value], pos) =>
						<DefaultMetadaField key={'preview-property-' + pos} keyProp={'(raw) ' + key}
							valueProp={value} idx={activeImage.index}
							pos={pos} />)
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
						<DefaultMetadaField key={'image-property-' + pos} keyProp={key}
							valueProp={value} idx={activePreview.index}
							pos={pos} />)
				}
			</Typography>
		</>);
	}

	const renderImageConfigMetadata = () => {
		return (<>
			<Typography gutterBottom variant='h6'>
				Config Metadata section
			</Typography>
			<Typography key={`config-metadata`} variant="body2"
				component={'span'} sx={{
					color: "textSecondary"
				}}>
				{isObjectEmpty(configMetadata) ?
					<p>No config metadata to display</p>
					: Object.entries(configMetadata).map(([key, value], pos) =>
						<DefaultMetadaField key={'config-property-' + pos} keyProp={key}
							valueProp={value} idx={activePreview.index}
							pos={pos} />)
				}
			</Typography>
		</>);
	}

	return (<PaperBox>
		{renderPreviewMetadata()}
		<Divider />
		{renderImageMetadata()}
		{/*<Divider />
		 renderImageConfigMetadata() */}
	</PaperBox>
	);
};

export default MetadataSection;