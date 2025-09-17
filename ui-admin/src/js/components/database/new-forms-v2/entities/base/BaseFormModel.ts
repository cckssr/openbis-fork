// BaseFormModel - Abstract base class for entity-specific form models

import { BaseFormModel as IBaseFormModel, FormSchema, FormContext, ValidationResult, FormData } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

export abstract class BaseFormModel implements IBaseFormModel {
  abstract entityType: string;
  abstract baseSchema: FormSchema;

  /**
   * Register entity-specific fields
   * This method should be called during form initialization
   */
  abstract registerFields(): Promise<void>;

  /**
   * Enhance the base schema with entity-specific fields and logic
   */
  abstract enhanceSchema(baseSchema: FormSchema, context: FormContext): FormSchema;

  /**
   * Validate form data with entity-specific rules
   */
  abstract validate(data: FormData): ValidationResult;

  /**
   * Get the complete schema for the given context
   */
  public async getSchema(context: FormContext): Promise<FormSchema> {
    // Register fields if not already done
    await this.registerFields();
    
    // Enhance the base schema with context-specific modifications
    return this.enhanceSchema(this.baseSchema, context);
  }

  /**
   * Get field dependencies for the given field
   */
  protected getFieldDependencies(fieldId: string, context: FormContext): any[] {
    // Default implementation - can be overridden by subclasses
    return [];
  }

  /**
   * Check if a field should be visible based on context
   */
  protected isFieldVisible(fieldId: string, context: FormContext): boolean {
    // Default implementation - can be overridden by subclasses
    return true;
  }

  /**
   * Check if a field should be enabled based on context
   */
  protected isFieldEnabled(fieldId: string, context: FormContext): boolean {
    // Default implementation - can be overridden by subclasses
    return true;
  }

  /**
   * Check if a field is required based on context
   */
  protected isFieldRequired(fieldId: string, context: FormContext): boolean {
    // Default implementation - can be overridden by subclasses
    return false;
  }

  /**
   * Get field validation rules based on context
   */
  protected getFieldValidation(fieldId: string, context: FormContext): any {
    // Default implementation - can be overridden by subclasses
    return {};
  }

  /**
   * Get section visibility based on context
   */
  protected isSectionVisible(sectionId: string, context: FormContext): boolean {
    // Default implementation - can be overridden by subclasses
    return true;
  }

  /**
   * Get section order based on context
   */
  protected getSectionOrder(sectionId: string, context: FormContext): number {
    // Default implementation - can be overridden by subclasses
    return 0;
  }
}