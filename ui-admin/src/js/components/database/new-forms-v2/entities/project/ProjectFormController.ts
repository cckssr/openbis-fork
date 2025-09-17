import { BaseFormController } from '@src/js/components/database/new-forms-v2/entities/base/BaseFormController.ts';
import { FormData, Permissions, FormMode } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

export class ProjectFormController extends BaseFormController {
  readonly entityType = 'PROJECT';
  private openbisFacade: any;
  private spacePermId: string = '';

  constructor(openbisFacade: any, user: any) {
    super(openbisFacade, user);
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
  }

  /**
   * Load Project data from openBIS
   */
  async load(entityId: string, mode: FormMode): Promise<FormData> {
    try {
      if (entityId === 'NEW_PROJECT') {
        // Return empty data for new project
        return {
          'new-project-permId': '',
          'new-project-identifier': '',
          'new-project-path': '',
          'new-project-space': this.spacePermId,
          'new-project-code': '',
          'new-project-registrator': '',
          'new-project-registrationDate': '',
          'new-project-modifier': '',
          'new-project-modificationDate': '',
          'new-project-description': '',
        };
      }

      const { ProjectPermId, ProjectFetchOptions } = this.openbisFacade;
      const id = new ProjectPermId(entityId);
      const fetchOptions = new ProjectFetchOptions();
      fetchOptions.withSpace && fetchOptions.withSpace();
      
      const result = await this.openbisFacade.getProjects([id], fetchOptions);
      const projectDto = result[entityId];

      if (!projectDto) {
        throw new Error(`Project with permId ${entityId} not found`);
      }

      // Convert DTO to FormData
      return {
        [`${entityId}-permId`]: projectDto.permId?.permId || '',
        [`${entityId}-identifier`]: projectDto.identifier?.identifier || '',
        [`${entityId}-path`]: projectDto.identifier?.identifier || '',
        [`${entityId}-space`]: projectDto.space?.code || '',
        [`${entityId}-code`]: projectDto.code || '',
        [`${entityId}-registrator`]: projectDto.registrator?.userId || '',
        [`${entityId}-registrationDate`]: projectDto.registrationDate ? new Date(projectDto.registrationDate).toISOString() : '',
        [`${entityId}-modifier`]: projectDto.modifier || '',
        [`${entityId}-modificationDate`]: projectDto.modificationDate ? new Date(projectDto.modificationDate).toISOString() : '',
        [`${entityId}-description`]: projectDto.description || '',
      };
    } catch (error) {
      console.error('Failed to load Project:', error);
      throw error;
    }
  }

  /**
   * Save Project data to openBIS
   */
  async save(data: FormData, mode: FormMode): Promise<string> {
    try {
      if (mode === 'CREATE') {
        // Create new project
        const { ProjectCreation, SpacePermId } = this.openbisFacade;
        const creation = new ProjectCreation();
        
        const codeField = Object.keys(data).find(key => key.endsWith('-code'));
        const descriptionField = Object.keys(data).find(key => key.endsWith('-description'));
        const spaceField = Object.keys(data).find(key => key.endsWith('-space'));
        
        creation.setCode(data[codeField!] as string);
        creation.setSpaceId(new SpacePermId(data[spaceField!] as string));
        creation.setDescription(data[descriptionField!] as string);
        
        const result = await this.openbisFacade.createProjects([creation]);
        return result[0].getPermId();
      } else {
        // Update existing project
        const { ProjectPermId, ProjectUpdate } = this.openbisFacade;
        const projectUpdate = new ProjectUpdate();
        
        const codeField = Object.keys(data).find(key => key.endsWith('-code'));
        const descriptionField = Object.keys(data).find(key => key.endsWith('-description'));
        
        projectUpdate.setProjectId(new ProjectPermId(data[codeField!] as string));
        projectUpdate.setDescription(data[descriptionField!] as string);
        
        const result = await this.openbisFacade.updateProjects([projectUpdate]);
        return data[codeField!] as string;
      }
    } catch (error) {
      console.error('Failed to save Project:', error);
      throw error;
    }
  }

  /**
   * Delete Project from openBIS
   */
  async delete(entityId: string): Promise<void> {
    try {
      const { ProjectIdentifier, ProjectDeletionOptions } = this.openbisFacade;
      const projectIdentifier = new ProjectIdentifier(entityId);
      const deletionOptions = new ProjectDeletionOptions();
      deletionOptions.setReason('Deleted via new-forms-v2');
      
      await this.openbisFacade.deleteProjects([projectIdentifier], deletionOptions);
    } catch (error) {
      console.error('Failed to delete Project:', error);
      throw error;
    }
  }

  /**
   * Move Project to different parent (space)
   */
  async move(entityId: string, newParentId: string): Promise<void> {
    try {
      const { ProjectPermId, ProjectUpdate } = this.openbisFacade;
      const projectUpdate = new ProjectUpdate();
      
      projectUpdate.setProjectId(new ProjectPermId(entityId));
      // Note: Moving projects between spaces is not directly supported in openBIS
      // This would require creating a new project in the target space and deleting the old one
      throw new Error('Moving projects between spaces is not supported');
    } catch (error) {
      console.error('Failed to move Project:', error);
      throw error;
    }
  }

  /**
   * Check user permissions for Project operations
   */
  async checkPermissions(entityId: string, user: any): Promise<Permissions> {
    try {
      // Mock implementation - in real implementation, this would check actual permissions
      // For now, return basic permissions
      return {
        canEdit: true,
        canDelete: true,
        canMove: false, // Projects can't be moved between spaces easily
        canView: true,
      };
    } catch (error) {
      console.error('Failed to check permissions:', error);
      return {
        canEdit: false,
        canDelete: false,
        canMove: false,
        canView: true,
      };
    }
  }

  /**
   * Set the parent space for new projects
   */
  setSpacePermId(spacePermId: string): void {
    this.spacePermId = spacePermId;
  }
}
