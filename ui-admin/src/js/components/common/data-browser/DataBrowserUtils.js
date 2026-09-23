
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

const sizeToString = (bytes) => {
    if (!bytes) {
      return null
    }

    if (typeof bytes == 'string') {
      bytes = parseInt(bytes)
    }

    let size
    let unit
    const kbytes = bytes / 1024.0
    const mbytes = kbytes / 1024.0
    const gbytes = mbytes / 1024.0
    if (gbytes > 1.0) {
      size = gbytes
      unit = 'GB'
    } else if (mbytes > 1.0) {
      size = mbytes
      unit = 'MB'
    } else if (kbytes > 1.0) {
      size = kbytes
      unit = 'kB'
    } else {
      size = bytes
      unit = 'bytes'
    }
    return size.toFixed(1) + '\xa0' + unit
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
      if(dataSet.getDataStore() !== null && dataSet.getDataStore().getCode() === "AFS") {
          if (dataSet.getExperiment() !== null) {
              return dataSet.getExperiment().getImmutableDataDate() !== null
          } else if (dataSet.getSample() !== null) {
              return dataSet.getSample().getImmutableDataDate() !== null
          }
      } else {
          // old data set
          return true;
      }
  }
  return false
}

  export { getFileNameFromPath, isUserAbortedError, isArchived, isFrozen, timeToString, sizeToString };