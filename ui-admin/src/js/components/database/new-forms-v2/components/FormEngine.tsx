// FormEngine - Simplified main form component for New Forms V2

import React from 'react';
import { useFormEngine } from '@src/js/components/database/new-forms-v2/core/useFormEngine.ts';
import { FormCallbacks } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

interface FormEngineProps extends FormCallbacks {
  formId: string;
  entityType?: string;
  entityId?: string;
  mode?: any;
  user?: any;
  openbisFacade?: any;
  showSpaceActions?: boolean;
  onNewProject?: (newObjectFormId: string) => void;
}

export const FormEngine: React.FC<FormEngineProps> = ({
  formId,
  onSave,
  onCancel,
  onDelete,
  onError,
  entityType,
  showSpaceActions = false,
  onNewProject,
}) => {
  // Use the simplified form engine hook
  const {
    form,
    controller,
    metadata,
    permissions,
    updateField,
    validateField,
    saveForm,
    deleteForm,
    isLoading,
    isDirty,
    isValid,
  } = useFormEngine(formId, {
    onSave,
    onCancel,
    onDelete,
    onError,
  });

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      await saveForm();
      onSave?.(form.data);
    } catch (error) {
      console.error('Form submission failed:', error);
      onError?.(error as Error);
    }
  };

  // Handle field changes
  const handleFieldChange = (fieldId: string, value: any) => {
    updateField(fieldId, value);
  };

  // Handle field blur (validation)
  const handleFieldBlur = (fieldId: string) => {
    validateField(fieldId);
  };

  if (!form) {
    return (
      <div className="form-engine-error">
        <p>Form not found or not initialized</p>
      </div>
    );
  }

  return (
    <div className="form-engine">
      <form onSubmit={handleSubmit} className="form-engine-form">
        {/* Render form sections */}
        {form.schema.sections.map(section => (
          <div key={section.id} className={`form-section ${section.collapsible ? 'collapsible' : ''}`}>
            <div className="section-header">
              <h3>{section.title}</h3>
              {section.description && (
                <p className="section-description">{section.description}</p>
              )}
            </div>
            
            <div className="section-content">
              {section.fields.map(fieldId => {
                const field = form.schema.fields[fieldId];
                if (!field || field.hidden) return null;

                return (
                  <div key={fieldId} className="form-field">
                    <label htmlFor={fieldId} className="field-label">
                      {field.label}
                      {field.required && <span className="required">*</span>}
                    </label>
                    
                    {renderField(field, form.data[fieldId], handleFieldChange, handleFieldBlur)}
                    
                    {form.validation[fieldId]?.error && (
                      <div className="field-error">
                        {form.validation[fieldId].error}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        ))}

        {/* Form status */}
        <div className="form-status">
          {isDirty && (
            <div className="status-dirty">
              <i className="fa fa-exclamation-circle" />
              <span>You have unsaved changes</span>
            </div>
          )}
          
          {!isValid && (
            <div className="status-invalid">
              <i className="fa fa-times-circle" />
              <span>Please fix validation errors</span>
            </div>
          )}
        </div>
      </form>
    </div>
  );
};

// Render individual field components
const renderField = (
  field: any,
  value: any,
  onChange: (fieldId: string, value: any) => void,
  onBlur: (fieldId: string) => void
) => {
  const fieldProps = {
    id: field.id,
    value: value || '',
    onChange: (e: any) => onChange(field.id, e.target.value),
    onBlur: () => onBlur(field.id),
    placeholder: field.placeholder,
    disabled: field.disabled,
    required: field.required,
  };

  switch (field.type) {
    case 'text':
      return <input type="text" {...fieldProps} />;
      
    case 'textarea':
      return <textarea {...fieldProps} rows={4} />;
      
    case 'email':
      return <input type="email" {...fieldProps} />;
      
    case 'password':
      return <input type="password" {...fieldProps} />;
      
    case 'number':
      return <input type="number" {...fieldProps} />;
      
    case 'date':
      return <input type="date" {...fieldProps} />;
      
    case 'datetime':
      return <input type="datetime-local" {...fieldProps} />;
      
    case 'select':
      return (
        <select {...fieldProps}>
          <option value="">Select an option</option>
          {/* Add options based on field configuration */}
        </select>
      );
      
    case 'checkbox':
      return (
        <input
          type="checkbox"
          checked={!!value}
          onChange={(e) => onChange(field.id, e.target.checked)}
          onBlur={() => onBlur(field.id)}
          disabled={field.disabled}
        />
      );
      
    case 'radio':
      return (
        <div className="radio-group">
          {/* Add radio options based on field configuration */}
        </div>
      );
      
    default:
      return <input type="text" {...fieldProps} />;
  }
};

export default FormEngine;
