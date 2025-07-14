import React, { useState } from 'react';
import { FormField, FormFieldDataType, FormMode, FormSection } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormFieldRenderer } from '@src/js/components/database/new-forms/components/FormFieldRenderer.tsx';
import { useEntityForm } from '@src/js/components/database/new-forms/components/EntityFormContainer.tsx';

interface ProjectCreationFormProps {

}

export const ProjectCreationForm: React.FC<ProjectCreationFormProps> = () => {
  const [formData, setFormData] = useState({
    code: '',
    description: ''
  });
  const { controller } = useEntityForm();

  console.log('ProjectCreationForm', { formData });
  const fields: FormField[] = [
    {
      id: 'code',
      label: 'Code',
      value: formData.code,
      dataType: FormFieldDataType.VARCHAR,
      required: true,
      isMultiValue: false,
      readOnly: false,
      section: FormSection.IDENTIFICATION_INFO,
      column: 'left',
      meta: []
    },
    {
      id: 'description',
      label: 'Description',
      value: formData.description,
      dataType: FormFieldDataType.MULTILINE_VARCHAR,
      required: false,
      isMultiValue: false,
      readOnly: false,
      section: FormSection.GENERAL,
      column: 'center',
      meta: []
    }
  ];

  const handleFieldUpdate = (fieldId: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [fieldId]: value
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    controller.create(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="project-creation-form">
      <div className="form-section">
        <h3>Identification Information</h3>
        <div className="form-fields">
          {fields.map(field => (
            <FormFieldRenderer
              key={field.id}
              field={field}
              onUpdate={handleFieldUpdate}
              isEditing={true}
              mode={FormMode.EDIT}
            />
          ))}
        </div>
      </div>
      <div className="form-actions">
        <button type="submit" className="primary-button">Create Project</button>
      </div>
    </form>
  );
};
