import React from 'react'
import { Grid2 } from '@mui/material';
import messages from '@src/js/common/messages.js'
import Message from '@src/js/components/common/form/Message.jsx'

import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';
import { useImagingDataContext } from '@src/js/components/common/imaging/components/viewer/ImagingDataContext.jsx';
import { makeStyles } from '@mui/styles';
import MainPreview from '@src/js/components/common/imaging/components/viewer/MainPreview.js';
import MainPreviewInputControls from '@src/js/components/common/imaging/components/viewer/MainPreviewInputControls.js';

const useStyles = makeStyles(theme => ({
	gridDirection: {
		flexDirection: "row",
		[theme.breakpoints.down('md')]: {
			flexDirection: "column",
		},
	},
}));

const PreviewSection = ({ activeImage, activePreview }) => {
	const classes = useStyles();

	const { state } = useImagingDataContext();

	const { isSaved } = state;

	const renderPreviewChanges = (isSaved) => {
		return !isSaved && (<Message type='warning'>
			{messages.get(messages.UNSAVED_CHANGES)}
		</Message>
		)
	}

	return <CollapsableSection title='Preview' isCollapsed={false} renderWarnings={renderPreviewChanges(isSaved)}>
		<Grid2 container className={classes.gridDirection}>
			<MainPreview activePreview={activePreview}
				previews={activeImage.previews} />
			<MainPreviewInputControls activePreview={activePreview}
				configInputs={activeImage.config.inputs}
				configFilters={activeImage.config.filters}
				configResolutions={activeImage.config.resolutions}
			/>
		</Grid2>
	</CollapsableSection>;
}

export default PreviewSection;