import React from 'react'
import { Grid2, useMediaQuery } from '@mui/material';
import Exporter from '@src/js/components/common/imaging/components/viewer/Exporter.jsx';
import constants from '@src/js/components/common/imaging/constants.js';
import ImageListItemSection
	from '@src/js/components/common/imaging/components/common/ImageListItemSection.js';
import messages from '@src/js/common/messages.js'
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';

const ImageSection = ({ images, activeImageIdx, configExports, onActiveItemChange, handleExport }) => {
	const isTablet = useMediaQuery('(max-width:820px)');

	const renderExportButton = () => {
		return configExports.length > 0 && <Exporter handleExport={handleExport} config={configExports} />
	}

	return (<CollapsableSection 
		isCollapsed={images.length <= 1}
		title={messages.get(messages.IMAGES)} 
		renderActions={renderExportButton}>
			<Grid2 container spacing={1} direction={isTablet ? 'column' : 'row'} sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
				<Grid2 size={{ xs: 12, sm: 10 }}>
					<ImageListItemSection
						cols={3} rowHeight={150}
						type={constants.IMAGE_TYPE}
						items={images}
						activeImageIdx={activeImageIdx}
						onActiveItemChange={onActiveItemChange} />
				</Grid2>
				<Grid2 size={{ xs: 3, sm: 2 }} container direction='column' sx={{ justifyContent: 'space-around' }}>
					{configExports.length > 0 &&
						<Exporter handleExport={handleExport} config={configExports} />}
				</Grid2>
			</Grid2>
		</CollapsableSection>
	);
}

export default ImageSection;