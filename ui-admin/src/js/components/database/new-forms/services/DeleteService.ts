/**
 * DeleteService - Centralized service for common delete operations
 * 
 * This service handles operations that are common across all entity types:
 * - Moving entities to trashcan
 * - Checking for existing deletions in trashcan
 * - Formatting deletion error messages
 * - Extracting identifiers from entities
 * 
 * Entity-specific logic (like getDependentEntities) remains in controllers.
 */

export interface DeleteServiceConfig {
  openbisFacade: any;
}

export interface MoveToTrashcanResult {
  success: boolean;
  count: number;
  error?: string;
}

export class DeleteService {
  private openbisFacade: any;

  constructor(config: DeleteServiceConfig) {
    if (!config.openbisFacade) {
      throw new Error('openbisFacade is required for DeleteService');
    }
    this.openbisFacade = config.openbisFacade;
  }

  /**
   * Moves experiments (collections) to trashcan
   * @param experiments Array of experiment objects
   * @param reason Deletion reason
   * @returns Result with success status and count
   */
  async moveExperimentsToTrashcan(
    experiments: any[],
    reason: string
  ): Promise<MoveToTrashcanResult> {
    if (!experiments || experiments.length === 0) {
      return { success: true, count: 0 };
    }

    try {
      const { ExperimentIdentifier, ExperimentDeletionOptions } = this.openbisFacade;
      const experimentIds = experiments.map((exp: any) => {
        const identifier = this.extractIdentifier(exp, 'EXPERIMENT');
        return new ExperimentIdentifier(identifier);
      });

      const experimentDeletionOptions = new ExperimentDeletionOptions();
      experimentDeletionOptions.setReason(reason);
      await this.openbisFacade.deleteExperiments(experimentIds, experimentDeletionOptions);

      return { success: true, count: experimentIds.length };
    } catch (error: any) {
      // Sanitize error for logging - only log safe message, not full error object
      const errorMessage = error?.message || String(error);
      return { success: false, count: 0, error: errorMessage };
    }
  }

  /**
   * Moves samples (objects) to trashcan
   * @param samples Array of sample objects
   * @param reason Deletion reason
   * @returns Result with success status and count
   */
  async moveSamplesToTrashcan(
    samples: any[],
    reason: string
  ): Promise<MoveToTrashcanResult> {
    if (!samples || samples.length === 0) {
      return { success: true, count: 0 };
    }

    try {
      const { SampleIdentifier, SampleDeletionOptions } = this.openbisFacade;
      const sampleIds = samples.map((sample: any) => {
        const identifier = this.extractIdentifier(sample, 'SAMPLE');
        return new SampleIdentifier(identifier);
      });

      const sampleDeletionOptions = new SampleDeletionOptions();
      sampleDeletionOptions.setReason(reason);
      await this.openbisFacade.deleteSamples(sampleIds, sampleDeletionOptions);

      return { success: true, count: sampleIds.length };
    } catch (error: any) {
      // Sanitize error for logging - only log safe message, not full error object
      const errorMessage = error?.message || String(error);
      return { success: false, count: 0, error: errorMessage };
    }
  }

  /**
   * Moves datasets to trashcan
   * @param datasets Array of dataset objects
   * @param reason Deletion reason
   * @returns Result with success status and count
   */
  async moveDataSetsToTrashcan(
    datasets: any[],
    reason: string
  ): Promise<MoveToTrashcanResult> {
    if (!datasets || datasets.length === 0) {
      return { success: true, count: 0 };
    }

    try {
      const { DataSetPermId, DataSetDeletionOptions } = this.openbisFacade;
      const datasetIds = datasets.map((dataset: any) => {
        const permId = this.extractDatasetPermId(dataset);
        return new DataSetPermId(permId);
      });

      const datasetDeletionOptions = new DataSetDeletionOptions();
      datasetDeletionOptions.setReason(reason);
      await this.openbisFacade.deleteDataSets(datasetIds, datasetDeletionOptions);

      return { success: true, count: datasetIds.length };
    } catch (error: any) {
      // Sanitize error for logging - only log safe message, not full error object
      const errorMessage = error?.message || String(error);
      return { success: false, count: 0, error: errorMessage };
    }
  }

  /**
   * Moves projects to trashcan
   * @param projects Array of project identifiers or objects
   * @param reason Deletion reason
   * @returns Result with success status and count
   */
  async moveProjectsToTrashcan(
    projects: any[],
    reason: string
  ): Promise<MoveToTrashcanResult> {
    if (!projects || projects.length === 0) {
      return { success: true, count: 0 };
    }

    try {
      const { ProjectIdentifier, ProjectDeletionOptions } = this.openbisFacade;
      const projectIds = projects.map((project: any) => {
        // Project can be passed as identifier string or object
        if (typeof project === 'string') {
          return new ProjectIdentifier(project);
        }
        const identifier = this.extractIdentifier(project, 'PROJECT');
        return new ProjectIdentifier(identifier);
      });

      const projectDeletionOptions = new ProjectDeletionOptions();
      projectDeletionOptions.setReason(reason);
      await this.openbisFacade.deleteProjects(projectIds, projectDeletionOptions);

      return { success: true, count: projectIds.length };
    } catch (error: any) {
      // Sanitize error for logging - only log safe message, not full error object
      const errorMessage = error?.message || String(error);
      return { success: false, count: 0, error: errorMessage };
    }
  }

  /**
   * Moves spaces to trashcan
   * @param spaces Array of space identifiers or objects
   * @param reason Deletion reason
   * @returns Result with success status and count
   */
  async moveSpacesToTrashcan(
    spaces: any[],
    reason: string
  ): Promise<MoveToTrashcanResult> {
    if (!spaces || spaces.length === 0) {
      return { success: true, count: 0 };
    }

    try {
      const { SpacePermId, SpaceDeletionOptions } = this.openbisFacade;
      const spaceIds = spaces.map((space: any) => {
        // Space can be passed as permId string or object
        if (typeof space === 'string') {
          return new SpacePermId(space);
        }
        const permId = space.getPermId ? space.getPermId() : space.permId || space;
        return new SpacePermId(permId);
      });

      const spaceDeletionOptions = new SpaceDeletionOptions();
      spaceDeletionOptions.setReason(reason);
      await this.openbisFacade.deleteSpaces(spaceIds, spaceDeletionOptions);

      return { success: true, count: spaceIds.length };
    } catch (error: any) {
      // Sanitize error for logging - only log safe message, not full error object
      const errorMessage = error?.message || String(error);
      return { success: false, count: 0, error: errorMessage };
    }
  }

  /**
   * Checks for existing deletions in trashcan that prevent deletion
   * @param identifier Entity identifier (e.g., project identifier, space code)
   * @param entityKind The entity kind being deleted ('SPACE', 'PROJECT', etc.)
   * @param checkEntityKinds Array of entity kinds to check for in deletions (e.g., ['SAMPLE'] for Space, ['EXPERIMENT', 'SAMPLE'] for Project)
   * @returns Array of deletion objects that must be permanently deleted first
   */
  async checkExistingDeletions(
    identifier: string,
    entityKind: string,
    checkEntityKinds: string[]
  ): Promise<any[]> {
    try {
      const { DeletionSearchCriteria, DeletionFetchOptions } = this.openbisFacade;
      const criteria = new DeletionSearchCriteria();
      const fetchOptions = new DeletionFetchOptions();
      fetchOptions.withDeletedObjects();

      const deletions = await this.openbisFacade.searchDeletions(criteria, fetchOptions);

      const dependentDeletions: any[] = [];
      const deletionList = deletions.getObjects() || deletions;

      if (Array.isArray(deletionList)) {
        deletionList.forEach((deletion: any) => {
          const deletedObjects = deletion.getDeletedObjects();
          if (!deletedObjects || !Array.isArray(deletedObjects)) {
            return;
          }

          for (const deletedObject of deletedObjects) {
            const kind = deletedObject.entityKind;
            if (checkEntityKinds.includes(kind)) {
              if (this.matchesIdentifier(deletedObject, identifier, entityKind)) {
                dependentDeletions.push(deletion);
                break; // Only add the deletion once even if it has multiple matching objects
              }
            }
          }
        });
      }

      return dependentDeletions;
    } catch (error: any) {
      // Sanitize error for logging - only log safe message, not full error object
      const errorMessage = error?.message || String(error);
      // Sanitize user-facing error message - don't expose internal details
      throw new Error(`Failed to check existing deletions: ${errorMessage}`);
    }
  }

  /**
   * Checks if a deleted object matches the given identifier based on entity kind
   * @param deletedObject The deleted object from trashcan
   * @param identifier The identifier to match against
   * @param entityKind The entity kind being deleted
   * @returns True if the deleted object belongs to the entity
   */
  private matchesIdentifier(
    deletedObject: any,
    identifier: string,
    entityKind: string
  ): boolean {
    const deletedIdentifier = deletedObject.identifier;
    if (!deletedIdentifier) {
      return false;
    }

    switch (entityKind) {
      case 'SPACE':
        // Space identifier is the first part: /SPACE_CODE
        // Check if the sample identifier starts with the space identifier
        return deletedIdentifier.startsWith(`/${identifier}/`);

      case 'PROJECT':
        // Project identifier format: /SPACE_CODE/PROJECT_CODE
        // Check if deleted object belongs to this project
        const parts = deletedIdentifier.split('/');
        if (parts.length > 3) {
          const projectIdentifier = `/${parts[1]}/${parts[2]}`;
          return projectIdentifier === identifier;
        }
        return false;

      default:
        return false;
    }
  }

  /**
   * Formats deletion error message for existing deletions in trashcan
   * @param dependentDeletions Array of deletion objects
   * @param entityType Human-readable entity type name (e.g., 'space', 'project')
   * @returns Formatted error message
   */
  formatDeletionError(dependentDeletions: any[], entityType: string): string {
    let text = `This ${entityType} can only be deleted if the following deletion sets in Trashcan are deleted permanently:\n`;
    dependentDeletions.forEach((deletion: any) => {
      const deletionDate = new Date(deletion.deletionDate);
      const formattedDate = deletionDate.toLocaleDateString() + ' ' + deletionDate.toLocaleTimeString();
      text += `${formattedDate} (reason: ${deletion.reason}) \n`;
    });
    return text;
  }

  /**
   * Extracts identifier from an entity object based on entity kind
   * @param entity The entity object
   * @param entityKind The entity kind ('EXPERIMENT', 'SAMPLE', 'PROJECT', etc.)
   * @returns The identifier string
   */
  extractIdentifier(entity: any, entityKind: string): string {
    switch (entityKind) {
      case 'EXPERIMENT':
      case 'SAMPLE':
        // Experiments and samples use getIdentifier().getIdentifier()
        if (entity.getIdentifier) {
          const identifier = entity.getIdentifier();
          return identifier?.getIdentifier ? identifier.getIdentifier() : identifier;
        }
        return entity.identifier || entity;

      case 'PROJECT':
        // Projects can have identifier or code
        if (entity.getIdentifier) {
          const identifier = entity.getIdentifier();
          return identifier?.getIdentifier ? identifier.getIdentifier() : identifier;
        }
        return entity.identifier || entity.code || entity;

      case 'SPACE':
        // Spaces use permId or code
        return entity.getPermId ? entity.getPermId() : (entity.permId || entity.code || entity);

      default:
        // Fallback: try common patterns
        if (entity.getIdentifier) {
          const identifier = entity.getIdentifier();
          return identifier?.getIdentifier ? identifier.getIdentifier() : identifier;
        }
        return entity.identifier || entity.code || entity.permId || entity;
    }
  }

  /**
   * Extracts permId from a dataset object
   * @param dataset The dataset object
   * @returns The permId string
   */
  extractDatasetPermId(dataset: any): string {
    // Datasets can use getPermId() or getCode()
    if (dataset.getPermId) {
      const permId = dataset.getPermId();
      return permId?.permId ? permId.permId : permId;
    }
    if (dataset.getCode) {
      return dataset.getCode();
    }
    return dataset.permId || dataset.code || dataset;
  }

  /**
   * Moves multiple entity types to trashcan in a single operation
   * Useful for operations that need to move different entity types together
   * @param entitiesByKind Map of entity kind to array of entities
   * @param reason Deletion reason
   * @returns Map of results by entity kind
   */
  async moveMultipleEntityTypesToTrashcan(
    entitiesByKind: Map<string, any[]>,
    reason: string
  ): Promise<Map<string, MoveToTrashcanResult>> {
    const results = new Map<string, MoveToTrashcanResult>();

    for (const [entityKind, entities] of entitiesByKind.entries()) {
      let result: MoveToTrashcanResult;

      switch (entityKind.toUpperCase()) {
        case 'EXPERIMENT':
          result = await this.moveExperimentsToTrashcan(entities, reason);
          break;
        case 'SAMPLE':
          result = await this.moveSamplesToTrashcan(entities, reason);
          break;
        case 'DATASET':
        case 'DATA_SET':
          result = await this.moveDataSetsToTrashcan(entities, reason);
          break;
        case 'PROJECT':
          result = await this.moveProjectsToTrashcan(entities, reason);
          break;
        case 'SPACE':
          result = await this.moveSpacesToTrashcan(entities, reason);
          break;
        default:
          result = { success: false, count: 0, error: `Unknown entity kind: ${entityKind}` };
      }

      results.set(entityKind, result);
    }

    return results;
  }
}

