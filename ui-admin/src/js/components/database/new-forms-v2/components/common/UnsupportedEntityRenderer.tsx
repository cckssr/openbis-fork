// UnsupportedEntityRenderer - Displays error for unsupported entity types

import React from 'react';

interface UnsupportedEntityRendererProps {
  entityType: string;
  onCancel?: () => void;
}

export const UnsupportedEntityRenderer: React.FC<UnsupportedEntityRendererProps> = ({
  entityType,
  onCancel
}) => {
  return (
    <div className="unsupported-entity-container">
      <div className="error-content">
        <div className="error-icon">
          <i className="fa fa-exclamation-triangle" />
        </div>
        <h2>Unsupported Entity Type</h2>
        <p>
          The entity type <strong>"{entityType}"</strong> is not supported by the New Forms V2 system.
        </p>
        <p>
          Please contact your administrator or use the legacy form system for this entity type.
        </p>
        <div className="error-actions">
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

export default UnsupportedEntityRenderer;
