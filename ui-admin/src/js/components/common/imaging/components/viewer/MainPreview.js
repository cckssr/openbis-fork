import React from 'react'
import { Box, Grid2, Typography } from '@mui/material';
import messages from '@src/js/common/messages.js'
import makeStyles from '@mui/styles/makeStyles';
import constants from "@src/js/components/common/imaging/constants.js";
import Message from '@src/js/components/common/form/Message.jsx'

const useStyles = makeStyles((theme) => ({
	imgContainer: {
		maxHeight: '800px',
		minHeight: '400px',
		overflow: 'auto',
		justifyContent: 'space-around',
		alignItems: 'center'
	}
}));

const MainPreview = ({ activePreview, resolution, isChanged, isUploadedPreview }) => {
	const classes = useStyles();

	return (
		<Grid2 size={{ sm: 12, md: 8 }}>
			<Grid2 container direction={'row'} sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
				<Grid2 size={{ md: 'auto' }}>
					<Typography variant='h6'>Selected Preview</Typography>
				</Grid2>
				<Grid2 size={{ md: 'auto' }}>
					{isChanged && !isUploadedPreview && (
						<Message type='info'>
							{messages.get(messages.UPDATE_CHANGES)}
						</Message>
					)}
				</Grid2>
			</Grid2>
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
		</Grid2 >
	);
};

export default MainPreview;