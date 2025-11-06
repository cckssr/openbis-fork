
const isUserAbortedError = (error) => {
    if (!error || typeof error.message !== "string") {
        return false;
    }

    return (
        error.message.includes("aborted") ||
        error.message.includes("Request aborted") ||
        error.message.includes("The user aborted a request") || 
        error.name === "AbortError"
    );
};

const getFileNameFromPath = (filePath) => {
    if (typeof filePath !== "string") {
      throw new Error("Invalid filePath: expected a string.");
    }    
    
    const parts = filePath.split('/').filter(Boolean);    
    
    return parts.length ? parts[parts.length - 1] : '';
  };

const timeToString = (time) => {
    return new Date(time).toLocaleString()
  }

const isArchived = (dataSet) => {
  if (dataSet !== null && dataSet.getPhysicalData() !== null) {
    let archivingStatus = dataSet.getPhysicalData().getStatus()
    return archivingStatus === "ARCHIVED" || archivingStatus === "ARCHIVE_PENDING" || archivingStatus === "UNARCHIVE_PENDING";
  } else {
    return false
  }
}

const isFrozen = (dataSet) => {
  if (dataSet !== null) {
    if (dataSet.getExperiment() !== null) {
      return dataSet.getExperiment().getImmutableDataDate() !== null
    } else if(dataSet.getSample() !== null) {
      return dataSet.getSample().getImmutableDataDate() !== null
    }
  }
  return false
}

  export { getFileNameFromPath, isUserAbortedError, isArchived, isFrozen, timeToString };