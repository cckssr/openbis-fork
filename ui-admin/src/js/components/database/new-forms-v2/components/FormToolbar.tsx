import React from 'react';
import { ActionConfig, FormContext, Permissions } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

interface FormToolbarProps {
  mode: string;
  actions: ActionConfig[];
  context: FormContext;
  onAction: (actionId: string) => void;
  isDirty: boolean;
  isValid: boolean;
  isLoading: boolean;
  permissions: Permissions;
}

export const FormToolbar: React.FC<FormToolbarProps> = ({
  mode,
  actions,
  context,
  onAction,
  isDirty,
  isValid,
  isLoading,
  permissions,
}) => {
  const getDefaultActions = () => {
    // Only add default actions if no actions are provided from FormModel
    if (actions && actions.length > 0) {
      return [];
    }

    const defaultActions: ActionConfig[] = [];

    switch (mode) {
      case 'VIEW':
        if (permissions.canEdit) {
          defaultActions.push({
            id: 'edit',
            type: 'button',
            label: 'Edit',
            permissions: ['canEdit'],
          });
        }
        if (permissions.canDelete) {
          defaultActions.push({
            id: 'delete',
            type: 'button',
            label: 'Delete',
            permissions: ['canDelete'],
          });
        }
        break;
      
      case 'EDIT':
        defaultActions.push({
          id: 'save',
          type: 'button',
          label: 'Save',
          permissions: ['canEdit'],
        });
        defaultActions.push({
          id: 'cancel',
          type: 'button',
          label: 'Cancel',
          permissions: ['canEdit'],
        });
        break;
      
      case 'CREATE':
        defaultActions.push({
          id: 'save',
          type: 'button',
          label: 'Create',
          permissions: ['canEdit'],
        });
        defaultActions.push({
          id: 'cancel',
          type: 'button',
          label: 'Cancel',
          permissions: ['canEdit'],
        });
        break;
    }

    return defaultActions;
  };

  const getVisibleActions = () => {
    const allActions = [...getDefaultActions(), ...actions];
    
    return allActions.filter(action => {
      // Check mode-based visibility
      const actionId = action.id.toLowerCase();
      
      // Show edit/delete actions only in VIEW mode
      if (mode === 'VIEW' && (actionId.includes('edit') || actionId.includes('delete'))) {
        // Allow these actions
      }
      // Show save/cancel actions only in EDIT/CREATE mode
      else if ((mode === 'EDIT' || mode === 'CREATE') && (actionId.includes('save') || actionId.includes('cancel'))) {
        // Allow these actions
      }
      // Show new-project action only in VIEW mode for spaces
      else if (mode === 'VIEW' && actionId.includes('new-project')) {
        // Allow this action
      }
      // Hide other actions that don't match the current mode
      else if (actionId.includes('edit') || actionId.includes('delete') || actionId.includes('save') || actionId.includes('cancel')) {
        return false;
      }

      // Check permissions - map FormModel permissions to FormToolbar permissions
      if (action.permissions && action.permissions.length > 0) {
        const hasPermission = action.permissions.some(permission => {
          switch (permission) {
            case 'edit':
              return permissions.canEdit;
            case 'delete':
              return permissions.canDelete;
            case 'create':
              return permissions.canCreate;
            case 'view':
              return permissions.canView;
            default:
              return true; // Allow unknown permissions for now
          }
        });
        if (!hasPermission) return false;
      }

      // Check visibility rules
      if (action.visibility) {
        // This would be enhanced with proper visibility logic
        return true;
      }

      return true;
    });
  };

  const renderAction = (action: ActionConfig) => {
    const isDisabled = 
      (action.id === 'save' && (!isDirty || !isValid)) ||
      isLoading;

    const getButtonClass = () => {
      const baseClass = 'toolbar-button';
      const typeClass = action.type ? `button-${action.type}` : '';
      const stateClass = isDisabled ? 'disabled' : '';
      const actionClass = action.id ? `action-${action.id}` : '';
      
      return [baseClass, typeClass, stateClass, actionClass].filter(Boolean).join(' ');
    };

    return (
      <button
        key={action.id}
        className={getButtonClass()}
        onClick={() => onAction(action.id)}
        disabled={isDisabled}
        title={action.label}
      >
        {action.label}
      </button>
    );
  };

  const visibleActions = getVisibleActions();

  if (visibleActions.length === 0) {
    return null;
  }

  return (
    <div className="form-toolbar">
      <div className="toolbar-actions">
        {visibleActions.map(renderAction)}
      </div>
      
      {isLoading && (
        <div className="toolbar-loading">
          <span>Processing...</span>
        </div>
      )}
    </div>
  );
};
