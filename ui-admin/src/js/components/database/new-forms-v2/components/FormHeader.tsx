import React from 'react';
import { FormMode, Permissions } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

interface FormHeaderProps {
  title: string;
  mode: FormMode;
  isDirty: boolean;
  isValid: boolean;
  onModeChange: (mode: FormMode) => void;
  permissions?: Permissions;
}

export const FormHeader: React.FC<FormHeaderProps> = ({
  title,
  mode,
  isDirty,
  isValid,
  onModeChange,
  permissions,
}) => {
  const getModeBadge = () => {
    switch (mode) {
      case FormMode.VIEW:
        return <span className="mode-badge view">View Mode</span>;
      case FormMode.EDIT:
        return <span className="mode-badge edit">Edit Mode</span>;
      case FormMode.CREATE:
        return <span className="mode-badge create">Create Mode</span>;
      default:
        return null;
    }
  };

  const getStatusIndicators = () => {
    const indicators = [];
    
    if (isDirty) {
      indicators.push(<span key="dirty" className="status-indicator dirty">Unsaved Changes</span>);
    }
    
    if (!isValid) {
      indicators.push(<span key="invalid" className="status-indicator invalid">Validation Errors</span>);
    }

    return indicators;
  };

  return (
    <div className="form-header">
      <div className="header-main">
        <h1 className="form-title">{title}</h1>
        <div className="header-actions">
          {getModeBadge()}
          {getStatusIndicators()}
        </div>
      </div>
      
      {mode === FormMode.VIEW && permissions?.canEdit && (
        <div className="header-edit">
          <button
            className="edit-button"
            onClick={() => onModeChange(FormMode.EDIT)}
          >
            Edit
          </button>
        </div>
      )}
    </div>
  );
};
