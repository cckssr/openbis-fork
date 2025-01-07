import React from 'react'
import { Grid2, useMediaQuery } from '@mui/material';
import PaperBox from '@src/js/components/common/imaging/components/common/PaperBox.js';
import InputFileUpload
    from '@src/js/components/common/imaging/components/viewer/InputFileUpload.js';
import AlertDialog from '@src/js/components/common/imaging/components/common/AlertDialog.jsx';
import constants from '@src/js/components/common/imaging/constants.js';
import ImageListItemSection
    from '@src/js/components/common/imaging/components/common/ImageListItemSection.js';

import AddToQueueIcon from '@mui/icons-material/AddToQueue';
import SaveIcon from '@mui/icons-material/Save';
import DeleteIcon from '@mui/icons-material/Delete';

import messages from '@src/js/common/messages.js'
import Message from '@src/js/components/common/form/Message.jsx'
import Button from '@src/js/components/common/form/Button.jsx'

const PreviewsSection = ({ previews, activeImageIdx, activePreviewIdx, isSaved, onActiveItemChange, onMove,
	onClickSave,
	onHandleYes,
	onClickNew,
	onInputFile 
}) => {
	const nPreviews = previews.length;
	const isTablet = useMediaQuery('(max-width:820px)');
	return (
		<PaperBox>
			<Grid2 container direction={isTablet ? 'column':'row'} spacing={1} sx={{ overflow: 'auto', justifyContent: 'space-between', alignItems: 'center' }}>
				<Grid2 size={{ sm: 12, md: 10 }}>
					<ImageListItemSection title={messages.get(messages.PREVIEWS)}
						cols={5} rowHeight={200}
						type={constants.PREVIEW_TYPE}
						items={previews}
						activeImageIdx={activeImageIdx}
						activePreviewIdx={activePreviewIdx}
						onActiveItemChange={onActiveItemChange}
						onMove={onMove} />
				</Grid2>
				<Grid2 size={{ xs: 3, md: 2 }} container direction='column' sx={{ justifyContent: 'space-around' }}>
					{!isSaved && (
						<Message type='warning'>
							{messages.get(messages.UNSAVED_CHANGES)}
						</Message>
					)}
					<Button name='btn-save-preview'
						label={messages.get(messages.SAVE)}
						variant='outlined'
						type='final'
						color='inherit'
						startIcon={<SaveIcon />}
						disabled={isSaved}
						onClick={onClickSave} />

					<AlertDialog label={messages.get(messages.REMOVE)} icon={<DeleteIcon />}
						title={messages.get(messages.CONFIRMATION_REMOVE, 'current preview')}
						content={messages.get(messages.CONTENT_REMOVE_PREVIEW)}
						disabled={nPreviews === 1}
						onHandleYes={onHandleYes} />

					<Button
						name='btn-new-preview'
						label={messages.get(messages.NEW)}
						type='final'
						color='inherit'
						variant='outlined'
						startIcon={<AddToQueueIcon />}
						onClick={onClickNew} />

					<InputFileUpload onInputFile={onInputFile} />
				</Grid2>
			</Grid2>
		</PaperBox>
	);
};

export default PreviewsSection;