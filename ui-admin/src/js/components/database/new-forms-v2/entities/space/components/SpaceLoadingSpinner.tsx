// SpaceLoadingSpinner - Space-specific loading component

import React from 'react';

interface SpaceLoadingSpinnerProps {
  spaceId: string;
}

export const SpaceLoadingSpinner: React.FC<SpaceLoadingSpinnerProps> = ({
  spaceId
}) => {
  return (
    <div className="space-loading-container">
      <div className="loading-content">
        <div className="loading-spinner">
          <i className="fa fa-spinner fa-spin" />
        </div>
        <h3>Loading Space</h3>
        <p>
          Loading space <strong>"{spaceId}"</strong>...
        </p>
        <div className="loading-details">
          <p>Initializing space controller...</p>
          <p>Loading space data...</p>
          <p>Preparing form schema...</p>
        </div>
      </div>
    </div>
  );
};

export default SpaceLoadingSpinner;
