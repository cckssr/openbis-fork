class RetryCaller {
    constructor(config = {}) {
        this.maxRetries = config.maxRetries ?? 3
        this.initialWaitTime = config.initialWaitTime ?? 1000 
        this.waitFactor = config.waitFactor ?? 2        
        this.logger = config.logger ?? console
        this.abortFn = null
    }

    async callWithRetry(callFunction, onRetry = undefined) {
        let attempts = 0;
        let waitTime = this.initialWaitTime;

        while (attempts < this.maxRetries) {
            try {
                const { promise, abortFn } = callFunction()            
                this.abortFn = abortFn
                return await promise
            } catch (error) {
                if (this.isRetryableError(error)) {
                    attempts++
                    if (onRetry) {
                        onRetry(attempts, this.maxRetries, waitTime, error);
                    }
                    if (attempts < this.maxRetries) {
                        //this.logger.warn(`Attempt ${attempts} failed - retrying in ${waitTime}ms`)
                        await this.wait(waitTime)
                        waitTime *= this.waitFactor
                    } else {
                        //this.logger.warn(`Attempt ${attempts} failed - no more retries`)
                        throw error
                    }
                } else {
                    throw error
                }
            } finally {
                this.abortFn = null
            }
        }
    }

    isRetryableError(error) {
        return (
            error.code === 'ECONNABORTED' || 
            error.message.includes('timeout') || 
            error.message.includes('network error') || 
            error.message.includes('Unable to reach the server') || 
            error.message.includes('Request timed out') || 
            error.message.includes('Failed to fetch') ||  
            error.message.includes('NetworkError when attempting to fetch resource') 
        );
    }
    

    async wait(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    abort() {
        if (this.abortFn) {
            this.abortFn(); 
            this.abortFn = null;
        }
    }
}

export default RetryCaller;
