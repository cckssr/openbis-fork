// SpaceErrorDisplay - Space-specific error display component

import React from 'react';

interface SpaceErrorDisplayProps {
  error: string;
  spaceId: string;
  onRetry: () => void;
  onCancel?: () => void;
}

export const SpaceErrorDisplay: React.FC<SpaceErrorDisplayProps> = ({
  error,
  spaceId,
  onRetry,
  onCancel
}) => {
  return (
    <div className="space-error-container">
      <div className="error-content">
        <div className="error-icon">
          <i className="fa fa-exclamation-triangle" />
        </div>
        <h2>Space Error</h2>
        <p>
          Failed to load space <strong>"{spaceId}"</strong>
        </p>
        <div className="error-details">
          <p><strong>Error:</strong> {error}</p>
        </div>
        <div className="error-actions">
          <button 
            className="btn btn-primary" 
            onClick={onRetry}
          >
            <i className="fa fa-refresh" /> Retry
          </button>
          {onCancel && (
            <button 
              className="btn btn-secondary" 
              onClick={onCancel}
            >
              <i className="fa fa-arrow-left" /> Back
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default SpaceErrorDisplay;
