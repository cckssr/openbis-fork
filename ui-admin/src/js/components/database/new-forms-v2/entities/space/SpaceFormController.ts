// SpaceFormController - Space-specific controller with enhanced init() method

import { BaseFormController } from '@src/js/components/database/new-forms-v2/entities/base/BaseFormController.ts';
import { FormMode, FormData, Permissions } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

export class SpaceFormController extends BaseFormController {
  private spaceMetadata: any = null;
  private permissions: Permissions | null = null;
  private spaceContext: any = null;

  /**
   * Initialize Space controller with Space-specific setup
   */
  async init(entityId: string, mode: FormMode): Promise<void> {
    if (this.isInitialized) return;

    try {
      // 1. Load Space metadata
      this.spaceMetadata = await this.loadMetadata(entityId);
      
      // 2. Check permissions
      this.permissions = await this.checkPermissions(entityId);
      
      // 3. Validate Space exists and is accessible
      if (!this.spaceMetadata && mode !== FormMode.CREATE) {
        throw new Error(`Space ${entityId} not found`);
      }
      
      // 4. Space-specific initialization
      await this.initializeSpaceSpecificData(entityId, mode);
      
      this.isInitialized = true;
    } catch (error) {
      console.error('Space controller initialization failed:', error);
      throw error;
    }
  }

  /**
   * Space-specific initialization logic
   */
  private async initializeSpaceSpecificData(entityId: string, mode: FormMode): Promise<void> {
    if (mode === FormMode.CREATE) {
      // For new spaces, initialize with default values
      this.spaceContext = {
        projects: [],
        history: [],
        canCreateProjects: this.permissions?.canCreate || false,
        parentSpace: null,
        isNew: true,
      };
    } else {
      // For existing spaces, load related data
      const [projects, history] = await Promise.all([
        this.getChildren(entityId),
        this.getHistory(entityId),
      ]);
      
      this.spaceContext = {
        projects,
        history,
        canCreateProjects: this.permissions?.canCreate || false,
        parentSpace: this.spaceMetadata?.parentSpace,
        isNew: false,
      };
    }
  }

  /**
   * Load Space data for the form
   */
  async load(entityId: string): Promise<FormData> {
    if (!this.isInitialized) {
      throw new Error('Controller not initialized. Call init() first.');
    }

    // Use pre-loaded metadata for better performance
    if (this.spaceMetadata) {
      return this.transformSpaceDataToFormData(this.spaceMetadata);
    }

    // Fallback to API call
    const space = await this.openbisFacade.getSpace(entityId);
    return this.transformSpaceDataToFormData(space);
  }

  /**
   * Save Space form data
   */
  async save(data: FormData): Promise<void> {
    if (!this.isInitialized) {
      throw new Error('Controller not initialized. Call init() first.');
    }

    // Validate business rules
    const businessErrors = this.validateSpaceBusinessRules(data);
    if (businessErrors.length > 0) {
      throw new Error(`Business validation failed: ${businessErrors.join(', ')}`);
    }

    // Transform form data to Space format
    const spaceData = this.transformFormDataToSpace(data);
    
    // Save via openBIS API
    if (this.spaceContext?.isNew) {
      await this.openbisFacade.createSpace(spaceData);
    } else {
      await this.openbisFacade.updateSpace(spaceData);
    }
  }

  /**
   * Check Space permissions
   */
  async checkPermissions(entityId: string): Promise<Permissions> {
    try {
      const permissions = await this.openbisFacade.getSpacePermissions(entityId);
      return {
        canRead: permissions.canRead || false,
        canWrite: permissions.canWrite || false,
        canCreate: permissions.canCreate || false,
        canDelete: permissions.canDelete || false,
        canAdmin: permissions.canAdmin || false,
      };
    } catch (error) {
      console.error('Failed to check Space permissions:', error);
      // Return default permissions for new spaces
      return {
        canRead: true,
        canWrite: true,
        canCreate: true,
        canDelete: false,
        canAdmin: false,
      };
    }
  }

  /**
   * Delete Space
   */
  async delete(entityId: string): Promise<void> {
    if (!this.isInitialized) {
      throw new Error('Controller not initialized. Call init() first.');
    }

    // Check if space has children
    const children = await this.getChildren(entityId);
    if (children.length > 0) {
      throw new Error('Cannot delete space with child projects or spaces');
    }

    await this.openbisFacade.deleteSpace(entityId);
  }

  /**
   * Load Space metadata
   */
  async loadMetadata(entityId: string): Promise<any> {
    try {
      return await this.openbisFacade.getSpaceMetadata(entityId);
    } catch (error) {
      console.error('Failed to load Space metadata:', error);
      return null;
    }
  }

  /**
   * Get child projects and spaces
   */
  async getChildren(entityId: string): Promise<any[]> {
    try {
      const [projects, spaces] = await Promise.all([
        this.openbisFacade.getSpaceProjects(entityId),
        this.openbisFacade.getSpaceSpaces(entityId),
      ]);
      return [...projects, ...spaces];
    } catch (error) {
      console.error('Failed to load Space children:', error);
      return [];
    }
  }

  /**
   * Get Space history
   */
  async getHistory(entityId: string): Promise<any[]> {
    try {
      return await this.openbisFacade.getSpaceHistory(entityId);
    } catch (error) {
      console.error('Failed to load Space history:', error);
      return [];
    }
  }

  /**
   * Transform Space data to form data format
   */
  private transformSpaceDataToFormData(space: any): FormData {
    return {
      code: space.code || '',
      description: space.description || '',
      identifier: space.identifier || '',
      permId: space.permId || '',
      registrationDate: space.registrationDate || null,
      modificationDate: space.modificationDate || null,
      // Add any other Space-specific fields
    };
  }

  /**
   * Transform form data to Space format
   */
  private transformFormDataToSpace(data: FormData): any {
    return {
      code: data.code,
      description: data.description,
      identifier: data.identifier,
      // Add any other Space-specific transformations
    };
  }

  /**
   * Validate Space-specific business rules
   */
  private validateSpaceBusinessRules(data: FormData): string[] {
    const errors: string[] = [];

    // Space code validation
    if (!data.code || data.code.trim() === '') {
      errors.push('Space code is required');
    } else if (!/^[A-Z0-9_-]+$/.test(data.code)) {
      errors.push('Space code must contain only uppercase letters, numbers, underscores, and hyphens');
    }

    // Description validation
    if (data.description && data.description.length > 1000) {
      errors.push('Description must be less than 1000 characters');
    }

    return errors;
  }

  /**
   * Get Space context
   */
  public getSpaceContext(): any {
    return this.spaceContext;
  }

  /**
   * Get Space metadata
   */
  public getSpaceMetadata(): any {
    return this.spaceMetadata;
  }

  /**
   * Get Space permissions
   */
  public getSpacePermissions(): Permissions | null {
    return this.permissions;
  }

  /**
   * Get the entity type for this controller
   */
  protected getEntityType(): string {
    return 'SPACE';
  }
}