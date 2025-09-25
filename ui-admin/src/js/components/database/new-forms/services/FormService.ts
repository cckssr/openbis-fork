import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { Form } from '@src/js/components/database/new-forms/types/form.types.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';

export interface FormServiceConfig {
  controller: IFormController;
  permId: string;
  entityKind: string;
  params?: any;
}

export class FormService {
  private controller: IFormController;
  private permId: string;
  private entityKind: string;
  private params?: any;

  constructor(config: FormServiceConfig) {
    this.controller = config.controller;
    this.permId = config.permId;
    this.entityKind = config.entityKind;
    this.params = config.params;
  }

  async loadForm(): Promise<Form> {
    return await this.controller.load(this.permId, this.entityKind, this.params);
  }

  async saveForm(form: Form): Promise<Form> {
    if (!form) {
      throw new Error('Cannot save: no form provided');
    }

    // Validate form before saving
    this.validateForm(form);

    return await this.controller.save(form);
  }

  async deleteForm(permId: string): Promise<void> {
    return await this.controller.delete(permId);
  }

  async checkPermissions(form: Form, user: string): Promise<Record<string, boolean>> {
    if (!this.controller.checkPermissions) {
      // Return default permissions if not implemented
      return { canEdit: true, canDelete: true, canMove: true };
    }
    return await this.controller.checkPermissions(form, user);
  }

  private validateForm(form: Form): void {
    if (!form) {
      throw new Error('Form is required');
    }

    // Check required fields
    const missingRequiredFields = form.fields
      .filter(field => field.required)
      .filter(field => !this.hasValue(field.value));

    if (missingRequiredFields.length > 0) {
      const fieldNames = missingRequiredFields.map(f => f.label || f.id).join(', ');
      throw new Error(`Required fields are missing: ${fieldNames}`);
    }

    // Check field-level validation rules
    for (const field of form.fields) {
      if (field.validation) {
        this.validateField(field);
      }
    }
  }

  private hasValue(value: any): boolean {
    if (value === null || value === undefined) return false;
    if (typeof value === 'string') return value.trim() !== '';
    if (Array.isArray(value)) return value.length > 0;
    return true;
  }

  private validateField(field: any): void {
    if (!field.validation) return;

    for (const rule of field.validation) {
      const isValid = this.applyValidationRule(field.value, rule);
      if (!isValid) {
        throw new Error(rule.message);
      }
    }
  }

  private applyValidationRule(value: any, rule: any): boolean {
    switch (rule.rule) {
      case 'required':
        return this.hasValue(value);
      case 'minLength':
        return !value || value.length >= rule.length;
      case 'matchesField':
        // This would need access to other field values
        return true; // Simplified for now
      default:
        return true;
    }
  }
}
