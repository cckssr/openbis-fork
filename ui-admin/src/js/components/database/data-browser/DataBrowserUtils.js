
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

  export { getFileNameFromPath, isUserAbortedError };