import React from 'react';

interface FormErrorBoundaryProps {
  error?: Error;
  resetError?: () => void;
}

export const FormErrorBoundary: React.FC<FormErrorBoundaryProps> = ({
  error,
  resetError,
}) => {
  return (
    <div className="form-error-boundary">
      <div className="error-content">
        <h3>Something went wrong</h3>
        <p>An error occurred while rendering the form.</p>
        
        {error && (
          <details className="error-details">
            <summary>Error Details</summary>
            <pre>{error.message}</pre>
            {error.stack && (
              <pre className="error-stack">{error.stack}</pre>
            )}
          </details>
        )}
        
        {resetError && (
          <button 
            className="retry-button"
            onClick={resetError}
          >
            Try Again
          </button>
        )}
      </div>
    </div>
  );
};
