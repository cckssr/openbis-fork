import React from 'react';
import { FormSchema, FormData, FormContext, FormField } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

interface FormRendererProps {
  schema: FormSchema;
  data: FormData;
  context: FormContext;
  onFieldChange: (fieldId: string, value: any) => void;
  getFieldRenderer: (fieldType: string) => React.ComponentType<any> | undefined;
  loadWidget: (widgetId: string) => Promise<React.ComponentType<any> | null>;
  isFieldVisible: (fieldId: string) => boolean;
  getFieldErrors: (fieldId: string) => any[];
}

export const FormRenderer: React.FC<FormRendererProps> = ({
  schema,
  data,
  context,
  onFieldChange,
  getFieldRenderer,
  loadWidget,
  isFieldVisible,
  getFieldErrors,
}) => {
  // Render a single field
  const renderField = (field: FormField) => {
    if (!isFieldVisible(field.id)) {
      return null;
    }

    const FieldComponent = getFieldRenderer(field.type);
    if (!FieldComponent) {
      return (
        <div key={field.id} className="field-error">
          <p>Field renderer not found for type: {field.type}</p>
        </div>
      );
    }

    return (
      <FieldComponent
        key={field.id}
        field={field}
        value={data[field.id]}
        onChange={onFieldChange}
        error={getFieldErrors(field.id)}
        context={context}
      />
    );
  };

  // Render a section
  const renderSection = (section: any) => {
    const sectionFields = schema.fields.filter(field => 
      section.fields.includes(field.id)
    );

    return (
      <div key={section.id} className="form-section">
        <h3 className="section-title">{section.title}</h3>
        <div className="section-fields">
          {sectionFields.map(renderField)}
        </div>
      </div>
    );
  };

  // Render widgets (simplified for now - widgets will be implemented later)
  const renderWidgets = () => {
    if (!schema.widgets || schema.widgets.length === 0) {
      return null;
    }

    return (
      <div className="form-widgets">
        {schema.widgets.map((widget) => (
          <div key={widget.id} className="widget-placeholder">
            <p>Widget: {widget.id} (not implemented yet)</p>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="form-renderer">
      <div className="form-sections">
        {schema.sections.map(renderSection)}
      </div>
      {renderWidgets()}
    </div>
  );
};
