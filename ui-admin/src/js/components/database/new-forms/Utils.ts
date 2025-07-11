import openbis from "@srcV3/openbis.esm";

export const getFormatedDate = (date: Date): string => {
	const day = String(date.getDate()).padStart(2, '0');
	const month = String(date.getMonth() + 1).padStart(2, '0');
	const year = date.getFullYear();
	const hour = String(date.getHours()).padStart(2, '0');
	const minute = String(date.getMinutes()).padStart(2, '0');
	const second = String(date.getSeconds()).padStart(2, '0');
	return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
  }

export const guid = (): string => {
	const s4 = () => {
	  return Math.floor((1 + Math.random()) * 0x10000)
				 .toString(16)
				 .substring(1);
	}
	
	return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
			 s4() + '-' + s4() + s4() + s4();
};

export const getSpaceCodeFromIdentifier = (identifier: string): string => {
	const identifierParts = identifier.split('/');
	let spaceCode;
	if(identifierParts.length > 2) { //If has less parts, is a shared sample
		spaceCode = identifierParts[1];
	}
	return spaceCode || '';
};

export const getProjectCodeFromExperimentIdentifier = (experimentIdentifier: string): string => {
	return experimentIdentifier.split('/')[2];
};

export const getProjectIdentifier = (spaceCode: string, projectCode: string): string => {
	return ('/' + spaceCode + '/' + projectCode);
}

export const getProjectIdentifierFromSampleIdentifier = (sampleIdentifier: string): string => {
	const spaceCode = getSpaceCodeFromIdentifier(sampleIdentifier);
	const projectCode = getProjectCodeFromExperimentIdentifier(sampleIdentifier);
	
	return getProjectIdentifier(spaceCode, projectCode);
}

export const getProjectIdentifierFromExperimentIdentifier = (experimentIdentifier: string): string => {
	const spaceCode = getSpaceCodeFromIdentifier(experimentIdentifier);
	const projectCode = getProjectCodeFromExperimentIdentifier(experimentIdentifier);
	
	return getProjectIdentifier(spaceCode, projectCode);
}

export const createDummyDataSetIdentifierFromExperimentIdentifier = (experimentIdentifier: string): string => {
	const spaceCode = getSpaceCodeFromIdentifier(experimentIdentifier);
	const projectCode = getProjectCodeFromExperimentIdentifier(experimentIdentifier);
	return "/" + spaceCode + "/" + projectCode + "/DUMMY_"+guid();
}