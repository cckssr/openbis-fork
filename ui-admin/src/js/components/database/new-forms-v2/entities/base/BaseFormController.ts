// BaseFormController - Abstract base class for entity-specific controllers

import { BaseFormController as IBaseFormController, FormMode, FormData, Permissions, FormContext } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

export abstract class BaseFormController implements IBaseFormController {
  protected isInitialized = false;
  protected openbisFacade: any;
  protected user: any;

  constructor(openbisFacade: any, user: any) {
    this.openbisFacade = openbisFacade;
    this.user = user;
  }

  /**
   * Initialize the controller with entity-specific setup
   * This method should be called before any other operations
   */
  abstract init(entityId: string, mode: FormMode): Promise<void>;

  /**
   * Load entity data for the form
   */
  abstract load(entityId: string): Promise<FormData>;

  /**
   * Save form data to the entity
   */
  abstract save(data: FormData): Promise<void>;

  /**
   * Check user permissions for the entity
   */
  abstract checkPermissions(entityId: string): Promise<Permissions>;

  /**
   * Delete the entity
   */
  abstract delete(entityId: string): Promise<void>;

  /**
   * Load entity metadata
   */
  abstract loadMetadata(entityId: string): Promise<any>;

  /**
   * Get child entities
   */
  abstract getChildren(entityId: string): Promise<any[]>;

  /**
   * Get entity history
   */
  abstract getHistory(entityId: string): Promise<any[]>;

  /**
   * Check if controller is initialized
   */
  public get initialized(): boolean {
    return this.isInitialized;
  }

  /**
   * Transform entity data to form data format
   */
  protected transformEntityToFormData(entity: any): FormData {
    // Default implementation - can be overridden by subclasses
    return {
      ...entity,
      // Add any common transformations here
    };
  }

  /**
   * Transform form data to entity format
   */
  protected transformFormDataToEntity(data: FormData): any {
    // Default implementation - can be overridden by subclasses
    return {
      ...data,
      // Add any common transformations here
    };
  }

  /**
   * Validate entity-specific business rules
   */
  protected validateBusinessRules(data: FormData): string[] {
    // Default implementation - can be overridden by subclasses
    return [];
  }

  /**
   * Get entity-specific context
   */
  protected getEntityContext(entityId: string, mode: FormMode): FormContext {
    return {
      entityType: this.getEntityType(),
      entityId,
      mode,
      user: this.user,
      openbisFacade: this.openbisFacade,
    };
  }

  /**
   * Get the entity type for this controller
   */
  protected abstract getEntityType(): string;
}