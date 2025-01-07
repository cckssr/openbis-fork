import React from 'react'
import { Box, Grid2, Typography } from '@mui/material';
import messages from '@src/js/common/messages.js'
import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles((theme) => ({
	imgContainer: {
		maxHeight: '800px',
		minHeight: '400px',
		overflow: 'auto',
		justifyContent: 'space-around',
		alignItems: 'center'
	}
}));

const MainPreview = ({ activePreview, resolution }) => {
	const classes = useStyles();

	return (
		<Grid2 container size={{ sm: 12, md: 8 }} className={classes.imgContainer}>
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
	);
};

export default MainPreview;