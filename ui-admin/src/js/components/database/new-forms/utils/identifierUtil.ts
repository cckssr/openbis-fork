export const guid = (): string => {
	const s4 = () => {
		return Math.floor((1 + Math.random()) * 0x10000)
			.toString(16)
			.substring(1);
	}

	return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
		s4() + '-' + s4() + s4() + s4();
};

export const createDummySampleIdentifier = (spaceCode: string, projectCode?: string): string => {
   if(projectCode) {
       return "/" + spaceCode + "/" + projectCode + "/__DUMMY_SAMPLE_F_R_C__" + guid();
   }
   return "/" + spaceCode + "/__DUMMY_SAMPLE_F_R_C__" + guid();
}

export const getSpaceCodeFromIdentifier = (identifier: string): string | undefined => {
   const identifierParts = identifier.split('/');
   let spaceCode: string | undefined;
   if(identifierParts.length > 2) { // If has fewer parts, it is a shared sample
      spaceCode = identifierParts[1];
   }
   return spaceCode;
};

export const getProjectCodeFromExperimentIdentifier = (experimentIdentifier: string): string => {
   console.log(experimentIdentifier.split('/'))
   return experimentIdentifier.split('/')[2];
};


export const getProjectIdentifier = (spaceCode: string, projectCode: string): string => {
   return '/' + spaceCode + '/' + projectCode;
}


export const getProjectIdentifierFromSampleIdentifier = (sampleIdentifier: string): string | undefined => {
	const spaceCode = getSpaceCodeFromIdentifier(sampleIdentifier);
	const projectCode = getProjectCodeFromExperimentIdentifier(sampleIdentifier);
	if (spaceCode && projectCode) {
		return getProjectIdentifier(spaceCode, projectCode);
	}
	return undefined;
}

export const getProjectIdentifierFromExperimentIdentifier = (experimentIdentifier: string): string | undefined => {
   const spaceCode = getSpaceCodeFromIdentifier(experimentIdentifier);
   const projectCode = getProjectCodeFromExperimentIdentifier(experimentIdentifier);
   if (spaceCode && projectCode) {
       return getProjectIdentifier(spaceCode, projectCode);
   }
   return undefined;
}

export const createDummyDataSetIdentifierFromExperimentIdentifier = (experimentIdentifier: string): string | undefined => {
   const spaceCode = getSpaceCodeFromIdentifier(experimentIdentifier);
   const projectCode = getProjectCodeFromExperimentIdentifier(experimentIdentifier);
   if (spaceCode && projectCode) {
       return "/" + spaceCode + "/" + projectCode + "/__DUMMY_DATA_SET_F_R_C__" + guid();
   }
   return undefined;
}

export const createDummyDataSetIdentifierFromSampleIdentifier = (sampleIdentifier: string): string | undefined => {
   const spaceCode = getSpaceCodeFromIdentifier(sampleIdentifier);
   const projectCode = getProjectCodeFromExperimentIdentifier(sampleIdentifier);
   if (spaceCode && projectCode) {
       return "/" + spaceCode + "/" + projectCode + "/__DUMMY_DATA_SET_F_R_C__" + guid();
   }
   return undefined;
}

export const createDummySampleIdentifierFromSampleIdentifier = (sampleIdentifier: string): string | undefined => {
   const spaceCode = getSpaceCodeFromIdentifier(sampleIdentifier);
   const projectCode = getProjectCodeFromExperimentIdentifier(sampleIdentifier);
   if (spaceCode && projectCode) {
       return "/" + spaceCode + "/" + projectCode + "/__DUMMY_SAMPLE_F_R_C__" + guid();
   }
   return undefined;
}

export const createDummyExperimentIdentifierFromProjectIdentifier = (projectIdentifier: string): string => {
   return projectIdentifier + "/__DUMMY_EXPERIMENT_F_R_C__" + guid();
}

export const createDummySampleIdentifierFromProjectIdentifier = (projectIdentifier: string): string => {
   return projectIdentifier + "/__DUMMY_SAMPLE_F_R_C__" + guid();
}