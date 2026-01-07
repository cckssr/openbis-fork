/**
 * MoveService - Centralized service for common move operations
 * 
 * This service handles operations that are common across all entity types:
 * - Moving entities between parent containers
 * - Preparing update objects for different entity types
 * - Handling descendants when moving objects
 * - Extracting target information from move parameters
 * 
 * Entity-specific logic (like getDependentEntities) remains in controllers.
 */

export interface MoveServiceConfig {
  openbisFacade: any;
}

export interface MoveResult {
  success: boolean;
  message?: string;
  error?: string;
}

export class MoveService {
  private openbisFacade: any;

  constructor(config: MoveServiceConfig) {
    if (!config.openbisFacade) {
      throw new Error('openbisFacade is required for MoveService');
    }
    this.openbisFacade = config.openbisFacade;
  }

  /**
   * Moves a project to a different space
   * @param projectPermId The permId of the project to move
   * @param targetSpacePermId The permId of the target space
   * @returns Result with success status
   */
  async moveProject(projectPermId: string, targetSpacePermId: string): Promise<MoveResult> {
    try {
      const { ProjectPermId, ProjectUpdate } = this.openbisFacade;
      const projectId = new ProjectPermId(projectPermId);
      const projectUpdate = new ProjectUpdate();
      projectUpdate.setProjectId(projectId);
      projectUpdate.setSpaceId(targetSpacePermId);

      await this.openbisFacade.updateProjects([projectUpdate]);
      console.log(`[MoveService] Moved project ${projectPermId} to space ${targetSpacePermId}`);
      return { success: true, message: 'Project moved successfully' };
    } catch (error: any) {
      console.error('[MoveService] Error moving project:', error);
      return { success: false, error: error.message || String(error) };
    }
  }

  /**
   * Moves a collection (experiment) to a different project
   * @param collectionPermId The permId of the collection to move
   * @param targetProject The target project entity
   * @returns Result with success status
   */
  async moveCollection(collectionPermId: string, targetProject: any): Promise<MoveResult> {
    try {
      // First, we need to get the collection to preserve its properties
      const { ExperimentPermId, ExperimentFetchOptions, ExperimentUpdate, ProjectIdentifier } = this.openbisFacade;
      const experimentId = new ExperimentPermId(collectionPermId);
      const fetchOptions = new ExperimentFetchOptions();
      fetchOptions.withProperties();
      fetchOptions.withProject();
      
      const result = await this.openbisFacade.getExperiments([experimentId], fetchOptions);
      const experiment = result[collectionPermId];
      
      if (!experiment) {
        return { success: false, error: `Collection with permId ${collectionPermId} not found` };
      }

      // Extract project identifier from target
      // ProjectIdentifier can be created from identifier string (e.g., /SPACE/PROJECT) or from the project entity
      let projectIdentifier: any;
      if (targetProject.getIdentifier) {
        // If target has getIdentifier(), use it
        const identifier = targetProject.getIdentifier();
        projectIdentifier = identifier?.getIdentifier ? new ProjectIdentifier(identifier.getIdentifier()) : new ProjectIdentifier(identifier);
      } else if (typeof targetProject === 'string') {
        // If target is a string, assume it's an identifier
        projectIdentifier = new ProjectIdentifier(targetProject);
      } else {
        // Try to get identifier from permId
        const { ProjectPermId, ProjectFetchOptions } = this.openbisFacade;
        const projectId = new ProjectPermId(targetProject.getPermId ? targetProject.getPermId() : targetProject);
        const projectFetchOptions = new ProjectFetchOptions();
        const projectResult = await this.openbisFacade.getProjects([projectId], projectFetchOptions);
        const project = projectResult[targetProject.getPermId ? targetProject.getPermId() : targetProject];
        if (project && project.getIdentifier) {
          const identifier = project.getIdentifier();
          projectIdentifier = identifier?.getIdentifier ? new ProjectIdentifier(identifier.getIdentifier()) : new ProjectIdentifier(identifier);
        } else {
          return { success: false, error: 'Could not extract project identifier from target' };
        }
      }

      const experimentUpdate = new ExperimentUpdate();
      experimentUpdate.setExperimentId(experiment.getIdentifier());
      experimentUpdate.setProjectId(projectIdentifier);
      
      // Preserve properties
      if (experiment.getProperties) {
        experimentUpdate.setProperties(experiment.getProperties());
      }

      await this.openbisFacade.updateExperiments([experimentUpdate]);
      console.log(`[MoveService] Moved collection ${collectionPermId} to project`);
      return { success: true, message: 'Collection moved successfully' };
    } catch (error: any) {
      console.error('[MoveService] Error moving collection:', error);
      return { success: false, error: error.message || String(error) };
    }
  }

  /**
   * Moves an object (sample) to a different parent (Space, Project, or Collection)
   * @param objectPermId The permId of the object to move
   * @param target The target entity (Space, Project, or Collection)
   * @param includeDescendants Whether to move descendant objects as well
   * @returns Result with success status
   */
  async moveObject(
    objectPermId: string,
    target: any,
    includeDescendants?: boolean
  ): Promise<MoveResult> {
    try {
      if (includeDescendants) {
        return await this.moveObjectWithDescendants(objectPermId, target);
      }

      const sampleUpdate = this.prepareSampleUpdate(objectPermId, target);
      await this.openbisFacade.updateSamples([sampleUpdate]);
      console.log(`[MoveService] Moved object ${objectPermId}`);
      return { success: true, message: 'Object moved successfully' };
    } catch (error: any) {
      console.error('[MoveService] Error moving object:', error);
      return { success: false, error: error.message || String(error) };
    }
  }

  /**
   * Moves a dataset to a different parent (Collection or Object)
   * @param datasetPermId The permId of the dataset to move
   * @param target The target entity (Collection/Experiment or Object/Sample)
   * @returns Result with success status
   */
  async moveDataset(datasetPermId: string, target: any): Promise<MoveResult> {
    try {
      // First, we need to get the dataset to preserve its properties
      const { DataSetPermId, DataSetFetchOptions, DataSetUpdate } = this.openbisFacade;
      const datasetId = new DataSetPermId(datasetPermId);
      const fetchOptions = new DataSetFetchOptions();
      fetchOptions.withProperties();
      fetchOptions.withExperiment();
      fetchOptions.withSample();

      const result = await this.openbisFacade.getDataSets([datasetId], fetchOptions);
      const dataset = result[datasetPermId];

      if (!dataset) {
        return { success: false, error: `Dataset with permId ${datasetPermId} not found` };
      }

      const datasetUpdate = new DataSetUpdate();
      datasetUpdate.setDataSetId(datasetId);
      
      // Preserve properties
      if (dataset.getProperties) {
        datasetUpdate.setProperties(dataset.getProperties());
      }

      const targetType = target['@type'];
      switch (targetType) {
        case 'as.dto.experiment.Experiment':
          datasetUpdate.setExperimentId(target.getIdentifier());
          datasetUpdate.setSampleId(null);
          break;
        case 'as.dto.sample.Sample':
          datasetUpdate.setSampleId(target.getIdentifier());
          // If the sample has an experiment, set it too
          if (target.getExperiment) {
            const experiment = target.getExperiment();
            if (experiment) {
              datasetUpdate.setExperimentId(experiment.getIdentifier());
            }
          }
          break;
        default:
          return { success: false, error: `Invalid target type for dataset move: ${targetType}` };
      }

      await this.openbisFacade.updateDataSets([datasetUpdate]);
      console.log(`[MoveService] Moved dataset ${datasetPermId}`);
      return { success: true, message: 'Dataset moved successfully' };
    } catch (error: any) {
      console.error('[MoveService] Error moving dataset:', error);
      return { success: false, error: error.message || String(error) };
    }
  }

  /**
   * Prepares a SampleUpdate object for moving a sample/object
   * This is the common logic extracted from controllers
   * @param samplePermId The permId of the sample to move
   * @param target The target entity (Space, Project, or Collection)
   * @returns SampleUpdate object ready to be executed
   */
  prepareSampleUpdate(samplePermId: string, target: any): any {
    const { SampleUpdate, SamplePermId } = this.openbisFacade;
    const sampleUpdate = new SampleUpdate();
    sampleUpdate.setSampleId(new SamplePermId(samplePermId));

    const targetType = target['@type'];
    switch (targetType) {
      case 'as.dto.project.Project':
        sampleUpdate.setExperimentId(null);
        sampleUpdate.setProjectId(target.getPermId());
        sampleUpdate.setSpaceId(target.getSpace().getPermId());
        break;
      case 'as.dto.experiment.Experiment':
        sampleUpdate.setSpaceId(target.getProject().getSpace().getPermId());
        sampleUpdate.setProjectId(target.getProject().getPermId());
        sampleUpdate.setExperimentId(target.getPermId());
        break;
      case 'as.dto.space.Space':
        sampleUpdate.setExperimentId(null);
        sampleUpdate.setProjectId(null);
        sampleUpdate.setSpaceId(target.getPermId());
        break;
      default:
        throw new Error(`Invalid target type for sample move: ${targetType}`);
    }

    return sampleUpdate;
  }

  /**
   * Moves an object with all its descendant objects
   * @param objectPermId The permId of the object to move
   * @param target The target entity (Space, Project, or Collection)
   * @returns Result with success status
   */
  async moveObjectWithDescendants(objectPermId: string, target: any): Promise<MoveResult> {
    try {
      const { SamplePermId, SampleFetchOptions } = this.openbisFacade;
      const sampleId = new SamplePermId(objectPermId);
      const fetchOptions = new SampleFetchOptions();
      fetchOptions.withExperiment();
      fetchOptions.withProject();
      fetchOptions.withSpace();
      fetchOptions.withChildrenUsing(fetchOptions);

      const result = await this.openbisFacade.getSamples([sampleId], fetchOptions);
      const sample = result[objectPermId];

      if (!sample) {
        return { success: false, error: `Object with permId ${objectPermId} not found` };
      }

      // Gather all descendants
      const samplesToUpdate: any[] = [];
      this.gatherAllDescendants(samplesToUpdate, sample);

      // Determine current level (EXPERIMENT, PROJECT, or SPACE)
      let level: string;
      let currentEntityPermId: string;

      if (sample.getExperiment()) {
        level = 'EXPERIMENT';
        currentEntityPermId = sample.getExperiment().getPermId().getPermId();
      } else if (sample.getProject()) {
        level = 'PROJECT';
        currentEntityPermId = sample.getProject().getPermId().getPermId();
      } else {
        level = 'SPACE';
        currentEntityPermId = sample.getSpace().getPermId().getPermId();
      }

      // Filter samples based on current level - only move descendants that belong to the same parent
      const updates: any[] = [];
      samplesToUpdate.forEach((descendant: any) => {
        let shouldUpdate = false;

        switch (level) {
          case 'EXPERIMENT':
            shouldUpdate =
              descendant.getExperiment() != null &&
              currentEntityPermId === descendant.getExperiment().getPermId().getPermId();
            break;
          case 'PROJECT':
            shouldUpdate =
              descendant.getExperiment() == null &&
              currentEntityPermId === descendant.getProject().getPermId().getPermId();
            break;
          case 'SPACE':
            shouldUpdate =
              descendant.getExperiment() == null &&
              descendant.getProject() == null &&
              currentEntityPermId === descendant.getSpace().getPermId().getPermId();
            break;
        }

        if (shouldUpdate) {
          const sampleUpdate = this.prepareSampleUpdate(descendant.getPermId().getPermId(), target);
          updates.push(sampleUpdate);
        }
      });

      if (updates.length > 0) {
        await this.openbisFacade.updateSamples(updates);
        console.log(`[MoveService] Moved ${updates.length} object(s) with descendants`);
        return { success: true, message: `Moved ${updates.length} object(s) with descendants successfully` };
      }

      return { success: true, message: 'No descendants to move' };
    } catch (error: any) {
      console.error('[MoveService] Error moving object with descendants:', error);
      return { success: false, error: error.message || String(error) };
    }
  }

  /**
   * Recursively gathers all descendant samples
   * @param entities Array to collect descendants into
   * @param entity The entity to gather descendants from
   */
  private gatherAllDescendants(entities: any[], entity: any): void {
    entities.push(entity);
    const children = entity.getChildren ? entity.getChildren() : [];
    if (Array.isArray(children)) {
      children.forEach((child: any) => this.gatherAllDescendants(entities, child));
    }
  }

  /**
   * Gets entity type string from DTO type
   * @param dtoType The DTO type string (e.g., 'as.dto.space.Space')
   * @returns Entity type string (e.g., 'SPACE')
   */
  getEntityTypeFromDtoType(dtoType: string): string {
    const typeMap: Record<string, string> = {
      'as.dto.space.Space': 'SPACE',
      'as.dto.project.Project': 'PROJECT',
      'as.dto.experiment.Experiment': 'EXPERIMENT',
      'as.dto.sample.Sample': 'SAMPLE',
      'as.dto.dataset.DataSet': 'DATASET',
    };
    return typeMap[dtoType] || 'UNKNOWN';
  }

  /**
   * Extracts space permId from target entity
   * @param target The target entity
   * @returns Space permId
   */
  extractSpaceIdFromTarget(target: any): string {
    const targetType = target['@type'];
    switch (targetType) {
      case 'as.dto.space.Space':
        return target.getPermId();
      case 'as.dto.project.Project':
        return target.getSpace().getPermId();
      case 'as.dto.experiment.Experiment':
        return target.getProject().getSpace().getPermId();
      case 'as.dto.sample.Sample':
        if (target.getSpace) {
          return target.getSpace().getPermId();
        }
        // Fallback: try to get from experiment/project
        if (target.getExperiment && target.getExperiment().getProject) {
          return target.getExperiment().getProject().getSpace().getPermId();
        }
        if (target.getProject) {
          return target.getProject().getSpace().getPermId();
        }
        throw new Error('Cannot extract space ID from target');
      default:
        throw new Error(`Cannot extract space ID from target type: ${targetType}`);
    }
  }

  /**
   * Extracts project permId from target entity (if applicable)
   * @param target The target entity
   * @returns Project permId or null
   */
  extractProjectIdFromTarget(target: any): string | null {
    const targetType = target['@type'];
    switch (targetType) {
      case 'as.dto.project.Project':
        return target.getPermId();
      case 'as.dto.experiment.Experiment':
        return target.getProject().getPermId();
      case 'as.dto.sample.Sample':
        if (target.getProject) {
          return target.getProject().getPermId();
        }
        if (target.getExperiment && target.getExperiment().getProject) {
          return target.getExperiment().getProject().getPermId();
        }
        return null;
      default:
        return null;
    }
  }

  /**
   * Extracts experiment permId from target entity (if applicable)
   * @param target The target entity
   * @returns Experiment permId or null
   */
  extractExperimentIdFromTarget(target: any): string | null {
    const targetType = target['@type'];
    switch (targetType) {
      case 'as.dto.experiment.Experiment':
        return target.getPermId();
      case 'as.dto.sample.Sample':
        if (target.getExperiment) {
          return target.getExperiment().getPermId();
        }
        return null;
      default:
        return null;
    }
  }
}
