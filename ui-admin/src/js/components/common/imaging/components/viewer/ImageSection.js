import React from 'react'
import { Grid2, Accordion, AccordionSummary, AccordionDetails, Typography } from "@mui/material";
import Exporter from "@src/js/components/common/imaging/components/viewer/Exporter.jsx";
import constants from "@src/js/components/common/imaging/constants.js";
import ImageListItemSection
	from "@src/js/components/common/imaging/components/common/ImageListItemSection.js";
import messages from '@src/js/common/messages.js'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles(() => ({
	root: {
		padding: '8px',
		margin: '6px 0 12px 0',
		borderColor: '#ebebeb',
		borderStyle: 'solid',
		borderWidth: '1px 2px 2px 1px',
		backgroundColor: '#fff',
		'&:hover': {
			borderColor: '#dbdbdb'
		}
	},
	summaryButton: {
		width: '15%',
		marginRight: '16px'
	}
}));

const ImageSection = ({ images, activeImageIdx, configExports, onActiveItemChange, handleExport }) => {
	const [expanded, setExpanded] = React.useState(false);

	const handleExpansion = () => {
		setExpanded((prevExpanded) => !prevExpanded);
	};

	const classes = useStyles();

	return (
		<Accordion
			className={classes.root}
			expanded={expanded}
			onChange={handleExpansion}
			sx={{ '& .MuiAccordionSummary-root': { padding: '0px' },
					'& .MuiAccordionSummary-content': { margin: '0px', justifyContent: 'space-between' },
					'& .MuiAccordionDetails-root': { padding: '0px' }
				}}>
			<AccordionSummary
				expandIcon={<ExpandMoreIcon />}
				aria-controls="images-panel-control"
				id="images-panel-header"
			>
				<Typography variant='h6'>
					{messages.get(messages.IMAGES)}
				</Typography>
				{configExports.length > 0 && !expanded &&
					<Exporter styles={{ root: classes.summaryButton }} handleExport={handleExport} config={configExports} />}
			</AccordionSummary>
			<AccordionDetails>
				<Grid2 container direction='row' spacing={1} sx={{ justifyContent: "space-between", alignItems: "center" }}>
					<Grid2 size={{ xs: 9, sm: 10 }}>
						<ImageListItemSection
							cols={3} rowHeight={150}
							type={constants.IMAGE_TYPE}
							items={images}
							activeImageIdx={activeImageIdx}
							onActiveItemChange={onActiveItemChange} />
					</Grid2>
					<Grid2 size={{ xs: 3, sm: 2 }} container direction='column' sx={{ justifyContent: "space-around" }}>
						{configExports.length > 0 &&
							<Exporter handleExport={handleExport} config={configExports} />}
					</Grid2>
				</Grid2>
			</AccordionDetails>
		</Accordion>
	);
}

export default ImageSection;