import React from 'react'
import { Grid2, Typography, useMediaQuery } from '@mui/material';
import messages from '@src/js/common/messages.js'
import makeStyles from '@mui/styles/makeStyles';
import constants from "@src/js/components/common/imaging/constants.js";
import Message from '@src/js/components/common/form/Message.jsx'
import Button from '@src/js/components/common/form/Button.jsx'

import InputFileUpload
	from '@src/js/components/common/imaging/components/viewer/InputFileUpload.js';
import AlertDialog from '@src/js/components/common/imaging/components/common/AlertDialog.jsx';
import ImageListItemSection
	from '@src/js/components/common/imaging/components/common/ImageListItemSection.js';

import AddToQueueIcon from '@mui/icons-material/AddToQueue';
import SaveIcon from '@mui/icons-material/Save';
import DeleteIcon from '@mui/icons-material/Delete';
import { useImagingDataContext } from '@src/js/components/common/imaging/components/viewer/ImagingDataContext.jsx';

const useStyles = makeStyles((theme) => ({
	imgContainer: {
		maxHeight: '600px',
		minHeight: '400px',
		overflow: 'auto',
		justifyContent: 'space-around',
		alignItems: 'center'
	}
}));

const MainPreview = ({ activePreview, previews }) => {
	const classes = useStyles();
	const { state, saveDataset, deletePreview, handleActivePreviewChange, onMove, createNewPreview, handleUpload } = useImagingDataContext();
	const { activeImageIdx, activePreviewIdx, resolution, isSaved } = state;
	
	const nPreviews = previews.length;
	const isTablet = useMediaQuery('(max-width:820px)');

	return (
		<Grid2 size={{ sm: 12, md: 8 }}>
			<Grid2 container className={classes.imgContainer}>
				{activePreview.bytes === null ?
					<Typography variant='body2'>
						{messages.get(messages.NO_PREVIEW)}
					</Typography>
					: <img alt={''}
						src={`data:image/${activePreview.format};base64,${activePreview.bytes}`}
						height={resolution[0]}
						width={resolution[1]}
					/>}
			</Grid2>
			<Grid2 container direction={isTablet ? 'column' : 'row'} spacing={1} sx={{ overflow: 'auto', justifyContent: 'space-between', alignItems: 'center' }}>
				<Grid2 size={{ md: 12 }}>
					<ImageListItemSection
						cols={20} rowHeight={200}
						type={constants.PREVIEW_TYPE}
						items={previews}
						activeImageIdx={activeImageIdx}
						activePreviewIdx={activePreviewIdx}
						onActiveItemChange={handleActivePreviewChange}
						onMove={onMove} />
				</Grid2>
				<Grid2 size={{ md: 12 }} sx={{ justifyContent: 'space-around', alignContent: 'center', height: '24px' }}>
					{!isSaved && (
						<Message type='warning'>
							{messages.get(messages.UNSAVED_CHANGES)}
						</Message>
					)}
				</Grid2>
				<Grid2 size={{ md: 12 }} container direction='row' spacing={2} sx={{ justifyContent: 'space-around' }}>

					<Button name='btn-save-preview'
						label={messages.get(messages.SAVE)}
						variant='outlined'
						type='final'
						color='inherit'
						startIcon={<SaveIcon />}
						disabled={isSaved}
						onClick={saveDataset} />

					<AlertDialog label={messages.get(messages.REMOVE)} 
						icon={<DeleteIcon />}
						title={messages.get(messages.CONFIRMATION_REMOVE, 'current preview')}
						content={messages.get(messages.CONTENT_REMOVE_PREVIEW)}
						disabled={nPreviews === 1}
						onHandleYes={deletePreview} />

					<Button
						name='btn-new-preview'
						label={messages.get(messages.NEW)}
						type='final'
						color='inherit'
						variant='outlined'
						startIcon={<AddToQueueIcon />}
						onClick={createNewPreview} />

					<InputFileUpload onInputFile={handleUpload} />
				</Grid2>
			</Grid2>
		</Grid2 >
	);
};

export default MainPreview;