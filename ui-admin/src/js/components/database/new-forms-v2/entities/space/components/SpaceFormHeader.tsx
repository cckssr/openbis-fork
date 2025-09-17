// SpaceFormHeader - Space-specific form header component

import React from 'react';
import { FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

interface SpaceFormHeaderProps {
  spaceId: string;
  mode: FormMode;
  onNewProject?: () => void;
  onSettings?: () => void;
}

export const SpaceFormHeader: React.FC<SpaceFormHeaderProps> = ({
  spaceId,
  mode,
  onNewProject,
  onSettings
}) => {
  const getTitle = () => {
    switch (mode) {
      case FormMode.CREATE:
        return 'Create New Space';
      case FormMode.EDIT:
        return `Edit Space: ${spaceId}`;
      case FormMode.VIEW:
        return `Space: ${spaceId}`;
      case FormMode.DELETE:
        return `Delete Space: ${spaceId}`;
      default:
        return `Space: ${spaceId}`;
    }
  };

  const getSubtitle = () => {
    switch (mode) {
      case FormMode.CREATE:
        return 'Create a new space in the system';
      case FormMode.EDIT:
        return 'Modify space properties and settings';
      case FormMode.VIEW:
        return 'View space information and manage projects';
      case FormMode.DELETE:
        return 'Confirm space deletion';
      default:
        return '';
    }
  };

  return (
    <div className="space-form-header">
      <div className="header-content">
        <div className="header-title">
          <h1>{getTitle()}</h1>
          <p className="header-subtitle">{getSubtitle()}</p>
        </div>
        
        <div className="header-actions">
          {mode === FormMode.VIEW && onNewProject && (
            <button 
              className="btn btn-primary"
              onClick={onNewProject}
            >
              <i className="fa fa-plus" /> New Project
            </button>
          )}
          
          {onSettings && (
            <button 
              className="btn btn-secondary"
              onClick={onSettings}
            >
              <i className="fa fa-cog" /> Settings
            </button>
          )}
        </div>
      </div>
      
      <div className="header-breadcrumb">
        <span className="breadcrumb-item">Spaces</span>
        <span className="breadcrumb-separator">/</span>
        <span className="breadcrumb-item current">{spaceId}</span>
      </div>
    </div>
  );
};

export default SpaceFormHeader;
