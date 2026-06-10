/**
 * Error formatting utility for consistent error handling across the form system
 * 
 * This utility ensures all errors are formatted consistently, making them:
 * - Easy to read for users
 * - Easy to debug for developers
 * - Safe to display (no [object Object] issues)
 */

export interface FormattedError {
  message: string;
  details?: string;
  stack?: string;
  code?: string | number;
  originalError?: any;
}

/**
 * Formats an error into a consistent structure
 * 
 * @param error - Error object, string, or any value
 * @param fallbackMessage - Default message if error cannot be parsed
 * @returns Formatted error object
 */
export function formatError(error: any, fallbackMessage: string = 'An unexpected error occurred'): FormattedError {
  // Handle null/undefined
  if (error == null) {
    return {
      message: fallbackMessage,
      originalError: error
    };
  }

  // Handle string errors
  if (typeof error === 'string') {
    return {
      message: error,
      originalError: error
    };
  }

  // Handle Error objects
  if (error instanceof Error) {
    return {
      message: error.message || fallbackMessage,
      details: error.name !== 'Error' ? error.name : undefined,
      stack: error.stack,
      originalError: error
    };
  }

  // Handle objects with message property
  if (typeof error === 'object') {
    const message = error.message
        || (typeof error.error === 'string' ? error.error : error.error?.message)
        || error.msg
        || error.toString();

    return {
      message: typeof message === 'string' ? message : fallbackMessage,
      details: error.details || error.description,
      code: error.code || error.status || error.statusCode,
      stack: error.stack,
      originalError: error
    };
  }

  // Fallback: convert to string
  try {
    const message = String(error);
    return {
      message: message !== '[object Object]' ? message : fallbackMessage,
      originalError: error
    };
  } catch {
    return {
      message: fallbackMessage,
      originalError: error
    };
  }
}

/**
 * Extracts a user-friendly error message from an error
 * 
 * @param error - Error object, string, or any value
 * @param fallbackMessage - Default message if error cannot be parsed
 * @returns User-friendly error message string
 */
export function getErrorMessage(error: any, fallbackMessage: string = 'An unexpected error occurred'): string {
  return formatError(error, fallbackMessage).message;
}

/**
 * Extracts error code/status from an error
 * 
 * @param error - Error object
 * @returns Error code or undefined
 */
export function getErrorCode(error: any): string | number | undefined {
  if (error == null) return undefined;
  
  if (typeof error === 'object') {
    return error.code || error.status || error.statusCode;
  }
  
  return undefined;
}

/**
 * Checks if an error is a specific HTTP status code
 * 
 * @param error - Error object
 * @param statusCode - HTTP status code to check
 * @returns True if error matches the status code
 */
export function isErrorStatus(error: any, statusCode: number): boolean {
  const code = getErrorCode(error);
  return code === statusCode;
}

/**
 * Checks if an error is a network/connection error
 * 
 * @param error - Error object
 * @returns True if error appears to be a network error
 */
export function isNetworkError(error: any): boolean {
  if (error == null) return false;
  
  const message = getErrorMessage(error).toLowerCase();
  const networkKeywords = ['network', 'fetch', 'connection', 'timeout', 'offline', 'failed to fetch'];
  
  return networkKeywords.some(keyword => message.includes(keyword));
}

/**
 * Formats error for console logging (includes full details)
 * 
 * @param error - Error object
 * @param context - Additional context about where error occurred
 * @returns Formatted string for console
 */
export function formatErrorForLogging(error: any, context?: string): string {
  const formatted = formatError(error);
  const parts = [];
  
  if (context) {
    parts.push(`[${context}]`);
  }
  
  parts.push(formatted.message);
  
  if (formatted.code) {
    parts.push(`(Code: ${formatted.code})`);
  }
  
  if (formatted.details) {
    parts.push(`Details: ${formatted.details}`);
  }
  
  return parts.join(' ');
}

