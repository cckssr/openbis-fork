// SpaceFormFooter - Space-specific form footer component

import React from 'react';
import { FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

interface SpaceFormFooterProps {
  spaceId: string;
  mode: FormMode;
  onSave?: (result: any) => void;
  onCancel?: () => void;
  onDelete?: (entityId: string) => void;
}

export const SpaceFormFooter: React.FC<SpaceFormFooterProps> = ({
  spaceId,
  mode,
  onSave,
  onCancel,
  onDelete
}) => {
  const getFooterActions = () => {
    switch (mode) {
      case FormMode.CREATE:
        return (
          <div className="footer-actions">
            <button 
              className="btn btn-secondary"
              onClick={onCancel}
            >
              <i className="fa fa-times" /> Cancel
            </button>
            <button 
              className="btn btn-primary"
              onClick={onSave}
            >
              <i className="fa fa-save" /> Create Space
            </button>
          </div>
        );
        
      case FormMode.EDIT:
        return (
          <div className="footer-actions">
            <button 
              className="btn btn-secondary"
              onClick={onCancel}
            >
              <i className="fa fa-times" /> Cancel
            </button>
            <button 
              className="btn btn-primary"
              onClick={onSave}
            >
              <i className="fa fa-save" /> Save Changes
            </button>
          </div>
        );
        
      case FormMode.VIEW:
        return (
          <div className="footer-actions">
            <button 
              className="btn btn-secondary"
              onClick={onCancel}
            >
              <i className="fa fa-arrow-left" /> Back
            </button>
            <button 
              className="btn btn-warning"
              onClick={() => {/* Navigate to edit mode */}}
            >
              <i className="fa fa-edit" /> Edit
            </button>
            {onDelete && (
              <button 
                className="btn btn-danger"
                onClick={() => onDelete(spaceId)}
              >
                <i className="fa fa-trash" /> Delete
              </button>
            )}
          </div>
        );
        
      case FormMode.DELETE:
        return (
          <div className="footer-actions">
            <button 
              className="btn btn-secondary"
              onClick={onCancel}
            >
              <i className="fa fa-times" /> Cancel
            </button>
            <button 
              className="btn btn-danger"
              onClick={() => onDelete?.(spaceId)}
            >
              <i className="fa fa-trash" /> Confirm Delete
            </button>
          </div>
        );
        
      default:
        return null;
    }
  };

  const getFooterInfo = () => {
    switch (mode) {
      case FormMode.CREATE:
        return (
          <div className="footer-info">
            <i className="fa fa-info-circle" />
            <span>Creating a new space will allow you to organize projects and data.</span>
          </div>
        );
        
      case FormMode.EDIT:
        return (
          <div className="footer-info">
            <i className="fa fa-info-circle" />
            <span>Changes will be saved immediately when you click Save.</span>
          </div>
        );
        
      case FormMode.VIEW:
        return (
          <div className="footer-info">
            <i className="fa fa-info-circle" />
            <span>View mode - no changes can be made to the form.</span>
          </div>
        );
        
      case FormMode.DELETE:
        return (
          <div className="footer-info warning">
            <i className="fa fa-exclamation-triangle" />
            <span>Deleting a space will remove all associated projects and data. This action cannot be undone.</span>
          </div>
        );
        
      default:
        return null;
    }
  };

  return (
    <div className="space-form-footer">
      <div className="footer-content">
        {getFooterInfo()}
        {getFooterActions()}
      </div>
    </div>
  );
};

export default SpaceFormFooter;
