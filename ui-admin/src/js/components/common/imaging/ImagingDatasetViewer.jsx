import React from 'react';
import ImagingDatasetViewerContainer from '@src/js/components/common/imaging/components/viewer/ImagingDatasetViewerContainer.jsx';
import { ImagingDataProvider } from '@src/js/components/common/imaging/components/viewer/ImagingDataContext.jsx'

//Component needed as uniqe access point for the context provider, to avoid changing DatabaseComponent.jsx

const ImagingDataSetViewer = ({onUnsavedChanges, objId, objType, extOpenbis}) => {
	return (
		<ImagingDataProvider onUnsavedChanges={onUnsavedChanges}
			objId={objId}
			objType={objType}
			extOpenbis={extOpenbis} >
			<ImagingDatasetViewerContainer />
		</ImagingDataProvider>
	);
};

export default ImagingDataSetViewer;
