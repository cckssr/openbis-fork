export const guid = (): string => {
	const s4 = () => {
		return Math.floor((1 + Math.random()) * 0x10000)
			.toString(16)
			.substring(1);
	}

	return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
		s4() + '-' + s4() + s4() + s4();
};

export const getForcedSpaceIdentifier = (spaceCode: string): string => {
   return '/' + spaceCode;
}

export const getMaterialIdentifier = (materialTypeCode: string, materialCode: string): string => {
   return '/' + materialTypeCode + '/' + materialCode;
}


export const getExperimentIdentifier = (spaceCode: string, projectCode: string, experimentCode: string): string => {
   return '/' + spaceCode + '/' + projectCode + '/' + experimentCode;
}

export const getSampleIdentifier = (spaceCode: string, projectCodeOrNull: string | null, sampleCode: string): string => {
   const projectPart = (projectCodeOrNull && isProjectSamplesEnabled) ? projectCodeOrNull + '/' : '';
   return '/' + spaceCode + '/' + projectPart + sampleCode;
}

//
// All Identifier Parsing
//



export const getCodeFromIdentifier = (identifier: string): string => {
   const identifierParts = identifier.split('/');
   return identifierParts[identifierParts.length - 1];
}

//
// Sample Identifier Parsing
//

export const getProjectCodeFromSampleIdentifier = (sampleIdentifier: string): string | undefined => {
   let projectCode: string | undefined;
   const sampleIdentifierParts = sampleIdentifier.split('/');
   if(sampleIdentifierParts.length === 4) {
      projectCode = sampleIdentifierParts[2];
   }
   return projectCode;
}

export const getContainerSampleIdentifierFromContainedSampleIdentifier = (sampleIdentifier: string): string | undefined => {
   let containerSampleIdentifier: string | undefined;
   const containerIdentifierEnd = sampleIdentifier.lastIndexOf(':');
   if(containerIdentifierEnd !== -1) {
      containerSampleIdentifier = sampleIdentifier.substring(0, containerIdentifierEnd);
   }
   return containerSampleIdentifier;
}

//
// Experiment Identifier Parsing
//




export const createDummySampleIdentifier = (spaceCode: string, projectCode?: string): string => {
    if(projectCode) {
        return "/" + spaceCode + "/" + projectCode + "/__DUMMY_SAMPLE_F_R_C__" + guid();
    }
    return "/" + spaceCode + "/__DUMMY_SAMPLE_F_R_C__" + guid();
}

export const createDummyDataSetIdentifier = (spaceCode: string, projectCode?: string): string => {
    if(projectCode) {
        return "/" + spaceCode + "/" + projectCode + "/__DUMMY_DATA_SET_F_R_C__" + guid();
    }
    return "/" + spaceCode + "/__DUMMY_DATA_SET_F_R_C__" + guid();
}

export const createDummySampleIdentifierFromExperimentIdentifier = (experimentIdentifier: string): string | undefined => {
    const spaceCode = getSpaceCodeFromIdentifier(experimentIdentifier);
    const projectCode = getProjectCodeFromExperimentIdentifier(experimentIdentifier);
     if (spaceCode && projectCode) {
        return "/" + spaceCode + "/" + projectCode + "/__DUMMY_SAMPLE_F_R_C__" + guid();
    }
    return undefined;
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