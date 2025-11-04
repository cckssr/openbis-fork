import { Form } from '@src/js/components/database/new-forms/types/form.types.ts';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';

// Types for move operations
export interface MoveEntityConfig {
  entityType: EntityKind;
  entityPermIds: string | string[];
  optionalPostAction?: () => void;
  openbisFacade: any;
  entityFormController?: IFormController; // For UI operations like navigation and side menu updates
}

export interface MoveEntityModel {
  entity: any;
  entities: any[];
  selected: any;
  isNewExperiment?: boolean;
  experimentIdentifier?: string;
  experimentType?: string;
}

export interface SearchCriteria {
  entityKind: string;
  logicalOperator: 'AND' | 'OR';
  rules: Record<string, {
    type: string;
    name: string;
    value: string;
  }>;
}

export interface MoveResult {
  success: boolean;
  message?: string;
  error?: string;
}

export class MoveService {
  private openbisFacade: any;
  private entityFormController?: IFormController;
  private entityType: EntityKind;
  private entityPermIds: string | string[];
  private optionalPostAction?: () => void;
  private moveEntityModel: MoveEntityModel;

  constructor(config: MoveEntityConfig) {
    this.openbisFacade = config.openbisFacade;
    this.entityFormController = config.entityFormController;
    this.entityType = config.entityType;
    this.entityPermIds = config.entityPermIds;
    this.optionalPostAction = config.optionalPostAction;
    this.moveEntityModel = {
      entity: null,
      entities: [],
      selected: null
    };
  }

  /**
   * Initialize the move service by loading the entities to be moved
   */
  async init(): Promise<void> {
    const result = await this.searchAndCallback();
    this.moveEntityModel.entity = result.objects[0];
    this.moveEntityModel.entities = result.objects;
  }

  /**
   * Search for entities based on the provided permIds
   */
  private async searchAndCallback(): Promise<any> {
    const criteria = this.buildSearchCriteria();

    switch (this.entityType) {
      case EntityKind.EXPERIMENT:
        return await this.openbisFacade.searchForExperimentsAdvanced(criteria, null);
      case EntityKind.SAMPLE:
        return await this.openbisFacade.searchForSamplesAdvanced(
          criteria,
          {
            only: true,
            withType: true,
            withExperiment: true,
            withProject: true,
            withSpace: true
          }
        );
      case EntityKind.DATASET:
        return await this.openbisFacade.searchForDataSetsAdvanced(criteria, null);
      case EntityKind.PROJECT:
        console.log('MoveService.searchAndCallback', criteria);
        const { ProjectPermId, ProjectFetchOptions, ExperimentIdentifier, RightsFetchOptions } = this.openbisFacade;
        const id = new ProjectPermId(permId);
        const fetchOptions = new ProjectFetchOptions();
        fetchOptions.withSpace();

        const result = await this.openbisFacade.searchProjects([id], fetchOptions);

        const projectDto = result[permId];
        console.log({ projectDto });
        return 
      default:
        throw new Error(`Unsupported entity type: ${this.entityType}`);
    }
  }

  /**
   * Build search criteria based on entity permIds
   */
  private buildSearchCriteria(): SearchCriteria {
    if (typeof this.entityPermIds === 'string') {
      const trimmedPermId = this.entityPermIds.trim();
      return {
        entityKind: this.entityType,
        logicalOperator: 'AND',
        rules: {
          'UUIDv4': {
            type: 'Attribute',
            name: 'PERM_ID',
            value: trimmedPermId
          }
        }
      };
    } else {
      const rules: Record<string, any> = {};
      for (let pIdx = 0; pIdx < this.entityPermIds.length; pIdx++) {
        rules[`UUIDv4_${pIdx}`] = {
          type: 'Attribute',
          name: 'PERM_ID',
          value: this.entityPermIds[pIdx]
        };
      }
      return {
        entityKind: this.entityType,
        logicalOperator: 'OR',
        rules
      };
    }
  }

  /**
   * Wait for index update after move operation
   */
  private async waitForIndexUpdate(): Promise<void> {
    const result = await this.searchAndCallback();
    const entity = result.objects[0];
    const found = this.checkEntityMoved(entity);

    if (!found) {
      // Wait 300ms and try again
      await new Promise(resolve => setTimeout(resolve, 300));
      return this.waitForIndexUpdate();
    } else {
      await this.handleMoveSuccess(entity);
    }
  }

  /**
   * Check if entity has been successfully moved
   */
  private checkEntityMoved(entity: any): boolean {
    const selectedEntity = this.moveEntityModel.selected;
    if (!selectedEntity) return false;

    switch (this.entityType) {
      case EntityKind.EXPERIMENT:
        return entity.getProject().getIdentifier().identifier === selectedEntity.getIdentifier().identifier;

      case EntityKind.SAMPLE:
        const selectedEntityType = selectedEntity['@type'];
        switch (selectedEntityType) {
          case 'as.dto.project.Project':
            return entity.getExperiment() == null &&
              entity.getProject().getIdentifier().identifier === selectedEntity.getIdentifier().identifier;
          case 'as.dto.experiment.Experiment':
            return entity.getExperiment().getIdentifier().identifier === selectedEntity.getIdentifier().identifier;
          case 'as.dto.space.Space':
            return entity.getExperiment() == null &&
              entity.getProject() == null &&
              entity.getSpace().getPermId().permId === selectedEntity.getPermId().permId;
        }
        break;

      case EntityKind.DATASET:
        return (entity.getSample() && entity.getSample().getIdentifier().identifier === selectedEntity.getIdentifier().identifier) ||
          (entity.getExperiment() && entity.getExperiment().getIdentifier().identifier === selectedEntity.getIdentifier().identifier);

      case EntityKind.PROJECT:
        return entity.getSpace().getPermId().identifier === selectedEntity.getPermId().identifier;
    }

    return false;
  }

  /**
   * Handle successful move operation
   */
  private async handleMoveSuccess(entity: any): Promise<void> {
    // Show success message
    this.showSuccess('Moved successfully');

    // Unblock UI
    this.unblockUI();

    // Refresh old node parent
    await this.mainController?.sideMenu.refreshNodeParentByPermId(this.entityType, entity.getPermId().permId);

    // Determine selected entity type
    const selectedType = this.moveEntityModel.selected['@type'];
    const selectedEntityType = this.getEntityTypeFromDtoType(selectedType);

    // Refresh new node parent
    await this.mainController?.sideMenu.refreshNodeByPermId(selectedEntityType, this.moveEntityModel.selected.getPermId().permId);

    // Navigate to the moved entity
    this.navigateToMovedEntity(entity);

    // Execute optional post action
    if (this.optionalPostAction) {
      this.optionalPostAction();
    }
  }

  /**
   * Get entity type string from DTO type
   */
  private getEntityTypeFromDtoType(dtoType: string): string {
    const typeMap: Record<string, string> = {
      'as.dto.space.Space': 'SPACE',
      'as.dto.project.Project': 'PROJECT',
      'as.dto.experiment.Experiment': 'EXPERIMENT',
      'as.dto.sample.Sample': 'SAMPLE',
      'as.dto.dataset.DataSet': 'DATASET'
    };
    return typeMap[dtoType] || 'UNKNOWN';
  }

  /**
   * Navigate to the moved entity
   */
  private navigateToMovedEntity(entity: any): void {
    if (!this.mainController) return;

    switch (this.entityType) {
      case EntityKind.EXPERIMENT:
        this.mainController.changeView(
          'showExperimentPageFromIdentifier',
          encodeURIComponent(`["${entity.getIdentifier().identifier}",false]`)
        );
        this.mainController.sideMenu.moveToNodeId(JSON.stringify({
          type: 'EXPERIMENT',
          id: entity.getPermId().permId
        }));
        break;

      case EntityKind.SAMPLE:
        this.mainController.changeView('showViewSamplePageFromPermId', entity.getPermId().permId);
        this.mainController.sideMenu.moveToNodeId(JSON.stringify({
          type: 'SAMPLE',
          id: entity.getPermId().permId
        }));
        break;

      case EntityKind.DATASET:
        this.mainController.changeView('showViewDataSetPageFromPermId', entity.getPermId().permId);
        this.mainController.sideMenu.moveToNodeId(JSON.stringify({
          type: 'DATASET',
          id: entity.getPermId().permId
        }));
        break;

      case EntityKind.PROJECT:
        this.mainController.changeView('showProjectPageFromIdentifier', entity.getPermId().permId);
        this.mainController.sideMenu.moveToNodeId(JSON.stringify({
          type: 'PROJECT',
          id: entity.getPermId().permId
        }));
        break;
    }
  }

  /**
   * Main move operation
   */
  async move(descendants: boolean = false): Promise<MoveResult> {
    try {
      this.blockUI();

      // Validate move operation
      if (this.moveEntityModel.isNewExperiment && !this.moveEntityModel.experimentIdentifier) {
        this.showUserError('Please choose the project and experiment name.');
        return { success: false, error: 'Missing experiment identifier' };
      }

      if (this.moveEntityModel.isNewExperiment && !this.moveEntityModel.experimentType) {
        this.showUserError('Please choose the experiment type.');
        return { success: false, error: 'Missing experiment type' };
      }

      // Execute move operation
      if (this.moveEntityModel.isNewExperiment) {
        await this.handleNewExperimentMove();
      } else {
        await this.executeMoveOperation(descendants);
      }

      // Wait for index update
      await this.waitForIndexUpdate();

      return { success: true, message: 'Move operation completed successfully' };
    } catch (error) {
      this.handleMoveError(error);
      const errorMessage = error instanceof Error ? error.message : 'Move operation failed';
      return { success: false, error: errorMessage };
    }
  }

  /**
   * Handle move operation for new experiment creation
   */
  private async handleNewExperimentMove(): Promise<void> {
    const experimentType = this.moveEntityModel.experimentType;
    const experimentIdentifier = this.moveEntityModel.experimentIdentifier;

    // Create new experiment
    const result = await this.mainController.serverFacade.createExperiment(
      experimentType!,
      this.getProjectIdentifierFromExperimentIdentifier(experimentIdentifier!),
      this.getCodeFromIdentifier(experimentIdentifier!)
    );

    const experimentPermId = result[0].permId;
    const criteria = {
      entityKind: 'EXPERIMENT',
      logicalOperator: 'AND',
      rules: {
        'UUIDv4': {
          type: 'Attribute',
          name: 'PERM_ID',
          value: experimentPermId
        }
      }
    };

    const experimentSearchResult = await this.mainController.serverFacade.searchForExperimentsAdvanced(
      criteria,
      { only: true, withProject: true, withProjectSpace: true }
    );

    const newExperiment = experimentSearchResult.objects[0];
    await this.mainController.sideMenu.refreshNodeByPermId('PROJECT', newExperiment.getProject().getPermId().getPermId());
    this.moveEntityModel.selected = newExperiment;

    await this.executeMoveOperation(false);
  }

  /**
   * Execute the actual move operation based on entity type
   */
  private async executeMoveOperation(descendants: boolean): Promise<void> {
    switch (this.entityType) {
      case EntityKind.EXPERIMENT:
        await this.moveExperiment();
        break;
      case EntityKind.SAMPLE:
        await this.moveSample(descendants);
        break;
      case EntityKind.DATASET:
        await this.moveDataset();
        break;
      case EntityKind.PROJECT:
        await this.moveProject();
        break;
      default:
        throw new Error(`Unsupported entity type for move: ${this.entityType}`);
    }
  }

  /**
   * Move experiment entity
   */
  private async moveExperiment(): Promise<void> {
    const { ExperimentUpdate } = this.openbisFacade;
    const experimentUpdate = new ExperimentUpdate();
    experimentUpdate.setExperimentId(this.moveEntityModel.entity.getIdentifier());
    experimentUpdate.setProjectId(this.moveEntityModel.selected.getIdentifier());
    experimentUpdate.setProperties(this.moveEntityModel.entity.getProperties());

    await this.openbisFacade.updateExperiments([experimentUpdate]);
  }

  /**
   * Move sample entity
   */
  private async moveSample(descendants: boolean): Promise<void> {
    const { SampleFetchOptions, SampleUpdate, SpacePermId } = this.openbisFacade;

    const prepareSampleUpdate = (samplePermId: any) => {
      const sampleUpdate = new SampleUpdate();
      sampleUpdate.setSampleId(samplePermId);

      const selectedEntityType = this.moveEntityModel.selected['@type'];
      switch (selectedEntityType) {
        case 'as.dto.project.Project':
          sampleUpdate.setExperimentId(null);
          sampleUpdate.setProjectId(this.moveEntityModel.selected.getPermId());
          sampleUpdate.setSpaceId(this.moveEntityModel.selected.getSpace().getPermId());
          break;
        case 'as.dto.experiment.Experiment':
          sampleUpdate.setSpaceId(this.moveEntityModel.selected.getProject().getSpace().getPermId());
          sampleUpdate.setProjectId(this.moveEntityModel.selected.getProject().getPermId());
          sampleUpdate.setExperimentId(this.moveEntityModel.selected.getPermId());
          break;
        case 'as.dto.space.Space':
          sampleUpdate.setExperimentId(null);
          sampleUpdate.setProjectId(null);
          sampleUpdate.setSpaceId(this.moveEntityModel.selected.getPermId());
          break;
      }
      return sampleUpdate;
    };

    const permIds = this.moveEntityModel.entities.map(x => x.getPermId());
    const selectedEntityType = this.moveEntityModel.selected['@type'];

    if (descendants) {
      await this.moveSampleWithDescendants(permIds, selectedEntityType, prepareSampleUpdate);
    } else {
      const sampleUpdates = permIds.map(x => prepareSampleUpdate(x));
      await this.openbisFacade.updateSamples(sampleUpdates);
    }
  }

  /**
   * Move sample with descendants
   */
  private async moveSampleWithDescendants(
    permIds: any[],
    selectedEntityType: string,
    prepareSampleUpdate: (permId: any) => any
  ): Promise<void> {
    const { SampleFetchOptions } = this.openbisFacade;
    const fetchOptions = new SampleFetchOptions();
    fetchOptions.withExperiment();
    fetchOptions.withProject();
    fetchOptions.withSpace();
    fetchOptions.withChildrenUsing(fetchOptions);

    const map = await this.openbisFacade.getSamples(permIds, fetchOptions);
    const samplesToUpdate: any[] = [];
    const updates: any[] = [];

    for (let i = 0; i < this.moveEntityModel.entities.length; i++) {
      const entity = this.moveEntityModel.entities[i];
      const permId = entity.getPermId();
      this.gatherAllDescendants(samplesToUpdate, map[permId]);

      let level: string;
      let currentEntity: string;

      if (entity.getExperiment()) {
        level = 'EXPERIMENT';
        currentEntity = entity.getExperiment().getPermId().getPermId();
      } else if (entity.getProject()) {
        level = 'PROJECT';
        currentEntity = entity.getProject().getPermId().getPermId();
      } else {
        level = 'SPACE';
        currentEntity = entity.getSpace().getPermId().getPermId();
      }

      // Filter samples based on current level
      samplesToUpdate.forEach((sample: any) => {
        let shouldUpdate = false;

        switch (level) {
          case 'EXPERIMENT':
            shouldUpdate = sample.getExperiment() != null &&
              currentEntity === sample.getExperiment().getPermId().getPermId();
            break;
          case 'PROJECT':
            shouldUpdate = sample.getExperiment() == null &&
              currentEntity === sample.getProject().getPermId().getPermId();
            break;
          case 'SPACE':
            shouldUpdate = sample.getExperiment() == null &&
              sample.getProject() == null &&
              currentEntity === sample.getSpace().getPermId().getPermId();
            break;
        }

        if (shouldUpdate) {
          const sampleUpdate = prepareSampleUpdate(sample.getPermId());
          updates.push(sampleUpdate);
        }
      });
    }

    await this.openbisFacade.updateSamples(updates);
  }

  /**
   * Move dataset entity
   */
  private async moveDataset(): Promise<void> {
    const { DataSetUpdate } = this.openbisFacade;
    const datasetUpdate = new DataSetUpdate();
    datasetUpdate.setDataSetId(this.moveEntityModel.entity.getPermId());
    datasetUpdate.setProperties(this.moveEntityModel.entity.getProperties());

    const selectedType = this.moveEntityModel.selected['@type'];
    switch (selectedType) {
      case 'as.dto.experiment.Experiment':
        datasetUpdate.setExperimentId(this.moveEntityModel.selected.getIdentifier());
        break;
      case 'as.dto.sample.Sample':
        if (this.moveEntityModel.selected.getExperiment()) {
          datasetUpdate.setExperimentId(this.moveEntityModel.selected.getExperiment().getIdentifier());
        }
        datasetUpdate.setSampleId(this.moveEntityModel.selected.getIdentifier());
        break;
    }

    await this.openbisFacade.updateDataSets([datasetUpdate]);
  }

  /**
   * Move project entity
   */
  private async moveProject(): Promise<void> {
    const { ProjectUpdate } = this.openbisFacade;
    const projectUpdate = new ProjectUpdate();
    projectUpdate.setProjectId(this.moveEntityModel.entity.getIdentifier());
    projectUpdate.setSpaceId(this.moveEntityModel.selected.getPermId());

    await this.openbisFacade.updateProjects([projectUpdate]);
  }

  /**
   * Gather all descendants of a sample
   */
  private gatherAllDescendants(entities: any[], entity: any): void {
    entities.push(entity);
    entity.getChildren().forEach((child: any) => this.gatherAllDescendants(entities, child));
  }

  /**
   * Handle multiple sample movement completion
   */
  private async handleMultipleSampleMoveCompletion(descendants: boolean): Promise<void> {
    const selectedEntity = this.moveEntityModel.selected;
    const selectedEntityType = selectedEntity['@type'];

    const callback = async (resultObject: any) => {
      const samples = resultObject.objects;
      const samplePermIds = samples.map((x: any) => x.permId.permId);
      const entitiesDone: string[] = [];

      let selectedType: string;
      let selectedEntityPermId: string;

      switch (selectedEntityType) {
        case 'as.dto.project.Project':
          selectedType = 'PROJECT';
          selectedEntityPermId = selectedEntity.getPermId().getPermId();
          break;
        case 'as.dto.experiment.Experiment':
          selectedType = 'EXPERIMENT';
          selectedEntityPermId = selectedEntity.getPermId().getPermId();
          break;
        case 'as.dto.space.Space':
          selectedType = 'SPACE';
          selectedEntityPermId = selectedEntity.getPermId().getPermId();
          break;
        default:
          throw new Error(`Unknown selected entity type: ${selectedEntityType}`);
      }

      // Refresh old entities
      for (let i = 0; i < this.moveEntityModel.entities.length; i++) {
        const entity = this.moveEntityModel.entities[i];
        const permId = entity.permId.permId;

        if (entitiesDone.includes(permId)) {
          continue;
        }

        // Refresh old entities
        if (entity.getExperiment()) {
          await this.mainController?.sideMenu.refreshNodeByPermId('EXPERIMENT', entity.getExperiment().getPermId().permId);
        } else if (entity.getProject()) {
          await this.mainController?.sideMenu.refreshNodeByPermId('PROJECT', entity.getProject().getPermId().permId);
        } else {
          await this.mainController?.sideMenu.refreshNodeByPermId('SPACE', entity.getSpace().getPermId().permId);
        }

        if (samplePermIds.includes(permId)) {
          entitiesDone.push(permId);
        }
      }

      await this.mainController?.sideMenu.refreshNodeByPermId(selectedType, selectedEntityPermId);

      const message = descendants
        ? `Moving of ${entitiesDone.length} objects and their descendants has been finished`
        : `Moving of ${entitiesDone.length} objects has been finished`;

      this.showSuccess(message);
      this.unblockUI();

      if (this.optionalPostAction) {
        this.optionalPostAction();
      }
    };

    // Search for samples based on selected entity type
    const criteria = this.buildSampleSearchCriteria(selectedEntityType, selectedEntity);
    await this.openbisFacade.searchForSamplesAdvanced(criteria, { only: true });
  }

  /**
   * Build search criteria for samples based on selected entity
   */
  private buildSampleSearchCriteria(selectedEntityType: string, selectedEntity: any): SearchCriteria {
    switch (selectedEntityType) {
      case 'as.dto.project.Project':
        return {
          entityKind: this.entityType,
          logicalOperator: 'AND',
          rules: {
            'UUIDv4_0': { type: 'Attribute', name: 'PROJECT_PERM_ID', value: selectedEntity.getPermId().permId },
            'UUIDv4_1': { type: 'Experiment', name: 'NULL.NULL', value: 'NULL' }
          }
        };
      case 'as.dto.experiment.Experiment':
        return {
          entityKind: this.entityType,
          logicalOperator: 'AND',
          rules: {
            'UUIDv4': { type: 'Experiment', name: 'ATTR.PERM_ID', value: selectedEntity.getPermId().permId }
          }
        };
      case 'as.dto.space.Space':
        return {
          entityKind: this.entityType,
          logicalOperator: 'AND',
          rules: {
            'UUIDv4_0': { type: 'Attribute', name: 'SPACE', value: selectedEntity.getPermId().permId },
            'UUIDv4_1': { type: 'Project', name: 'NULL.NULL', value: 'NULL' },
            'UUIDv4_2': { type: 'Experiment', name: 'NULL.NULL', value: 'NULL' }
          }
        };
      default:
        throw new Error(`Unknown selected entity type: ${selectedEntityType}`);
    }
  }

  /**
   * Handle move operation errors
   */
  private handleMoveError(error: any): void {
    let message = JSON.stringify(error);
    if (error && error.data && error.data.message) {
      message = error.data.message;
    }
    this.showError(`Move failed: ${message}`);
    this.unblockUI();
  }

  // Utility methods for UI operations
  private blockUI(): void {
    // Implementation depends on your UI framework
    console.log('Blocking UI...');
  }

  private unblockUI(): void {
    // Implementation depends on your UI framework
    console.log('Unblocking UI...');
  }

  private showSuccess(message: string): void {
    // Implementation depends on your UI framework
    console.log(`Success: ${message}`);
  }

  private showError(message: string): void {
    // Implementation depends on your UI framework
    console.error(`Error: ${message}`);
  }

  private showUserError(message: string): void {
    // Implementation depends on your UI framework
    console.error(`User Error: ${message}`);
  }

  // Utility methods for identifier parsing
  private getProjectIdentifierFromExperimentIdentifier(experimentIdentifier: string): string {
    // Implementation depends on your identifier utility
    return experimentIdentifier.split('/').slice(0, 3).join('/');
  }

  private getCodeFromIdentifier(identifier: string): string {
    // Implementation depends on your identifier utility
    return identifier.split('/').pop() || '';
  }

  // Getters for accessing model data
  get entity(): any {
    return this.moveEntityModel.entity;
  }

  get entities(): any[] {
    return this.moveEntityModel.entities;
  }

  get selected(): any {
    return this.moveEntityModel.selected;
  }

  set selected(value: any) {
    this.moveEntityModel.selected = value;
  }

  set isNewExperiment(value: boolean) {
    this.moveEntityModel.isNewExperiment = value;
  }

  set experimentIdentifier(value: string) {
    this.moveEntityModel.experimentIdentifier = value;
  }

  set experimentType(value: string) {
    this.moveEntityModel.experimentType = value;
  }
}
