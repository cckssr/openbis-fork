// SpaceFormModel - Space-specific form model with lazy field registration

import { BaseFormModel } from '@src/js/components/database/new-forms-v2/entities/base/BaseFormModel.ts';
import { FormSchema, FormContext, ValidationResult, FormData } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

export class SpaceFormModel extends BaseFormModel {
  entityType = 'SPACE';
  
  baseSchema: FormSchema = {
    entityType: 'SPACE',
    sections: [
      {
        id: 'identification',
        title: 'Identification',
        description: 'Basic space identification information',
        fields: ['code', 'description'],
        collapsible: false,
        collapsed: false,
        order: 1,
      },
      {
        id: 'metadata',
        title: 'Metadata',
        description: 'Additional space metadata',
        fields: ['identifier', 'permId'],
        collapsible: true,
        collapsed: true,
        order: 2,
      },
    ],
    fields: {
      code: {
        id: 'code',
        type: 'text',
        label: 'Space Code',
        required: true,
        placeholder: 'Enter space code (e.g., MY_SPACE)',
        validation: {
          required: true,
          pattern: '^[A-Z0-9_-]+$',
        },
        order: 1,
        section: 'identification',
      },
      description: {
        id: 'description',
        type: 'textarea',
        label: 'Description',
        required: false,
        placeholder: 'Enter space description',
        validation: {
          maxLength: 1000,
        },
        order: 2,
        section: 'identification',
      },
      identifier: {
        id: 'identifier',
        type: 'text',
        label: 'Identifier',
        required: false,
        disabled: true,
        placeholder: 'Auto-generated identifier',
        order: 1,
        section: 'metadata',
      },
      permId: {
        id: 'permId',
        type: 'text',
        label: 'Permanent ID',
        required: false,
        disabled: true,
        placeholder: 'Auto-generated permanent ID',
        order: 2,
        section: 'metadata',
      },
    },
  };

  /**
   * Register Space-specific fields
   */
  async registerFields(): Promise<void> {
    // This would register Space-specific field components
    // For now, we'll use the base schema fields
    console.log('Registering Space-specific fields...');
  }

  /**
   * Enhance schema with Space-specific logic
   */
  enhanceSchema(baseSchema: FormSchema, context: FormContext): FormSchema {
    const enhancedSchema = { ...baseSchema };
    
    // Enhance fields based on context
    Object.keys(enhancedSchema.fields).forEach(fieldId => {
      const field = enhancedSchema.fields[fieldId];
      
      // Set field visibility
      field.hidden = !this.isFieldVisible(fieldId, context);
      
      // Set field enabled state
      field.disabled = !this.isFieldEnabled(fieldId, context);
      
      // Set field required state
      field.required = this.isFieldRequired(fieldId, context);
      
      // Set field validation
      field.validation = {
        ...field.validation,
        ...this.getFieldValidation(fieldId, context),
      };
    });

    // Enhance sections based on context
    enhancedSchema.sections = enhancedSchema.sections
      .filter(section => this.isSectionVisible(section.id, context))
      .map(section => ({
        ...section,
        order: this.getSectionOrder(section.id, context),
      }))
      .sort((a, b) => a.order - b.order);

    return enhancedSchema;
  }

  /**
   * Validate Space form data
   */
  validate(data: FormData): ValidationResult {
    const errors: { [fieldId: string]: string } = {};
    let isValid = true;

    // Validate required fields
    if (!data.code || data.code.trim() === '') {
      errors.code = 'Space code is required';
      isValid = false;
    } else if (!/^[A-Z0-9_-]+$/.test(data.code)) {
      errors.code = 'Space code must contain only uppercase letters, numbers, underscores, and hyphens';
      isValid = false;
    }

    // Validate description length
    if (data.description && data.description.length > 1000) {
      errors.description = 'Description must be less than 1000 characters';
      isValid = false;
    }

    return { isValid, errors };
  }

  /**
   * Check if field should be visible based on context
   */
  protected isFieldVisible(fieldId: string, context: FormContext): boolean {
    switch (fieldId) {
      case 'identifier':
      case 'permId':
        // Show metadata fields only for existing spaces
        return context.mode !== 'CREATE';
      default:
        return true;
    }
  }

  /**
   * Check if field should be enabled based on context
   */
  protected isFieldEnabled(fieldId: string, context: FormContext): boolean {
    switch (fieldId) {
      case 'identifier':
      case 'permId':
        // Metadata fields are always disabled (read-only)
        return false;
      case 'code':
        // Code can only be edited in CREATE mode
        return context.mode === 'CREATE';
      default:
        return true;
    }
  }

  /**
   * Check if field is required based on context
   */
  protected isFieldRequired(fieldId: string, context: FormContext): boolean {
    switch (fieldId) {
      case 'code':
        return true;
      case 'description':
        return false;
      default:
        return false;
    }
  }

  /**
   * Get field validation rules based on context
   */
  protected getFieldValidation(fieldId: string, context: FormContext): any {
    switch (fieldId) {
      case 'code':
        return {
          required: true,
          pattern: '^[A-Z0-9_-]+$',
        };
      case 'description':
        return {
          maxLength: 1000,
        };
      default:
        return {};
    }
  }

  /**
   * Check if section should be visible based on context
   */
  protected isSectionVisible(sectionId: string, context: FormContext): boolean {
    switch (sectionId) {
      case 'metadata':
        // Show metadata section only for existing spaces
        return context.mode !== 'CREATE';
      default:
        return true;
    }
  }

  /**
   * Get section order based on context
   */
  protected getSectionOrder(sectionId: string, context: FormContext): number {
    switch (sectionId) {
      case 'identification':
        return 1;
      case 'metadata':
        return 2;
      default:
        return 999;
    }
  }
}