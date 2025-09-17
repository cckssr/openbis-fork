import { BaseFormModel } from '@src/js/components/database/new-forms-v2/entities/base/BaseFormModel.ts';
import { FormSchema, FormData, ValidationResult, FormContext, FormField, FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';
import { fieldRegistry } from '@src/js/components/database/new-forms-v2/core/utils/fieldRegistry.ts';
import { 
  getCodeField, 
  getDescriptionField, 
  getPermIdField,
  getIdentifierField,
  getPathField,
  getSpaceField,
  getRegistratorField, 
  getRegistrationDateField, 
  getModifierField, 
  getModificationDateField 
} from '@src/js/components/database/new-forms-v2/core/utils/fieldUtils.ts';

// Import field adapter components that bridge the interface differences
import { TextFieldAdapter } from '@src/js/components/database/new-forms-v2/components/fields/adapters/TextFieldAdapter.tsx';
import { TextAreaFieldAdapter } from '@src/js/components/database/new-forms-v2/components/fields/adapters/TextAreaFieldAdapter.tsx';
import { DateFieldAdapter } from '@src/js/components/database/new-forms-v2/components/fields/adapters/DateFieldAdapter.tsx';

export class ProjectFormModel extends BaseFormModel {
  readonly entityType = 'PROJECT';

  /**
   * Get base schema for Project entity
   */
  async getBaseSchema(): Promise<FormSchema> {
    return this.getSchema({
      entityType: 'PROJECT',
      entityId: 'new-project',
      mode: FormMode.VIEW,
      permissions: { canEdit: true, canDelete: true, canMove: false, canView: true, canCreate: true },
      user: 'system',
      openbisFacade: null,
    });
  }

  /**
   * Generate Project-specific schema
   */
  async getSchema(context: FormContext): Promise<FormSchema> {
    // Register fields first
    await this.registerFields();

    // Create mock DTO for field generation
    const mockDto = {
      permId: { permId: context.entityId || 'new-project' },
      code: context.entityId || 'NEW_PROJECT',
      description: '',
      identifier: { identifier: context.entityId ? `/${(context as any).meta?.spaceCode || 'SPACE'}/${context.entityId}` : '' },
      space: { code: (context as any).meta?.spaceCode || 'SPACE' },
      registrator: { userId: (typeof context.user === 'string' ? context.user : (context.user as any)?.userId) || 'system' },
      registrationDate: new Date().toISOString(),
      modifier: (typeof context.user === 'string' ? context.user : (context.user as any)?.userId) || 'system',
      modificationDate: new Date().toISOString(),
    };

    // Generate fields using utility functions
    const fields = [
      getPermIdField(mockDto),
      getIdentifierField(mockDto),
      getPathField(mockDto),
      getSpaceField(mockDto),
      getCodeField(mockDto),
      getRegistratorField(mockDto),
      getRegistrationDateField(mockDto),
      getModifierField(mockDto),
      getModificationDateField(mockDto),
      getDescriptionField(mockDto, { readOnly: context.mode === 'VIEW' }),
    ];

    // Create schema
    const schema: FormSchema = {
      entityType: this.entityType,
      title: context.entityId === 'NEW_PROJECT' ? 'New Project' : `Project: ${mockDto.code}`,
      sections: [
        {
          id: 'identification',
          title: 'Identification Info',
          fields: fields.filter(f => 
            f.name === 'permId' || f.name === 'identifier' || f.name === 'path' || 
            f.name === 'space' || f.name === 'code' || f.name === 'registrator' || 
            f.name === 'registrationDate' || f.name === 'modifier' || f.name === 'modificationDate'
          ).map(f => f.id),
        },
        {
          id: 'general',
          title: 'General',
          fields: fields.filter(f => f.name === 'description').map(f => f.id),
        },
      ],
      fields,
      widgets: [],
      actions: this.getActions(context),
    };

    return schema;
  }

  /**
   * Get actions based on context
   */
  private getActions(context: FormContext) {
    if (context.entityId === 'NEW_PROJECT') {
      // New project actions
      return [
        {
          id: 'save',
          type: 'button',
          label: 'Save',
          visibility: [],
          permissions: ['edit'],
        },
        {
          id: 'cancel',
          type: 'button',
          label: 'Cancel',
          visibility: [],
          permissions: [],
        },
      ];
    } else {
      // Existing project actions
      return [
        {
          id: 'save',
          type: 'button',
          label: 'Save',
          visibility: [],
          permissions: ['edit'],
        },
        {
          id: 'edit',
          type: 'button',
          label: 'Edit',
          visibility: [],
          permissions: ['edit'],
        },
        {
          id: 'cancel',
          type: 'button',
          label: 'Cancel',
          visibility: [],
          permissions: [],
        },
        {
          id: 'delete',
          type: 'button',
          label: 'Delete',
          visibility: [],
          permissions: ['delete'],
        },
      ];
    }
  }

  /**
   * Get default data for Project
   */
  getDefaultData(schema: FormSchema): FormData {
    const data: FormData = {};
    schema.fields.forEach(field => {
      data[field.id] = (field as any).value || '';
    });
    return data;
  }

  /**
   * Validate Project-specific data
   */
  async validate(data: FormData, context: FormContext): Promise<ValidationResult> {
    const errors: any[] = [];

    // Basic validation
    const codeField = Object.keys(data).find(key => key.endsWith('-code'));
    if (codeField && (!data[codeField] || data[codeField].toString().trim() === '')) {
      errors.push({
        field: codeField,
        message: 'Code is required',
        rule: 'required',
      });
    }

    // Validate code format (alphanumeric and underscores only)
    if (codeField && data[codeField] && !/^[a-zA-Z0-9_]+$/.test(data[codeField].toString())) {
      errors.push({
        field: codeField,
        message: 'Code can only contain letters, numbers, and underscores',
        rule: 'format',
      });
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }

  /**
   * Register Project-specific fields
   */
  async registerFields(): Promise<void> {
    // Register basic field types if not already registered
    if (!fieldRegistry.has('text')) {
      fieldRegistry.register('text', TextFieldAdapter);
    }
    if (!fieldRegistry.has('textarea')) {
      fieldRegistry.register('textarea', TextAreaFieldAdapter);
    }
    if (!fieldRegistry.has('date')) {
      fieldRegistry.register('date', DateFieldAdapter);
    }
  }

  /**
   * Enhance schema with Project-specific customizations
   */
  async enhanceSchema(baseSchema: FormSchema, context: FormContext): Promise<FormSchema> {
    // Add Project-specific customizations here
    return baseSchema;
  }
}
