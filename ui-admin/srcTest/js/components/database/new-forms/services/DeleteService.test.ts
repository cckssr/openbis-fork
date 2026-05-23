import { DeleteService } from '@src/js/components/database/new-forms/services/DeleteService.ts'
import openbis from '@srcTest/js/services/openbis.js'
import dto from '@srcTest/js/services/openbis/dto.js'

// Mock deletion options classes
class MockDeletionOptions {
  private reason: string = ''
  setReason(reason: string) {
    this.reason = reason
  }
  getReason() {
    return this.reason
  }
}

// Mock deletion search classes
class MockDeletionSearchCriteria {
  constructor() {}
}

class MockDeletionFetchOptions {
  withDeletedObjects() {
    return this
  }
}

describe('DeleteService', () => {
  let deleteService: DeleteService
  let mockOpenbisFacade: any

  beforeEach(() => {
    jest.clearAllMocks()
    
    // Create mock openbis facade with proper constructors
    mockOpenbisFacade = {
      ExperimentIdentifier: dto.ExperimentIdentifier,
      ExperimentDeletionOptions: MockDeletionOptions,
      SampleIdentifier: dto.SampleIdentifier,
      SampleDeletionOptions: MockDeletionOptions,
      DataSetPermId: dto.DataSetPermId,
      DataSetDeletionOptions: MockDeletionOptions,
      ProjectIdentifier: dto.ProjectIdentifier,
      ProjectDeletionOptions: MockDeletionOptions,
      SpacePermId: dto.SpacePermId,
      SpaceDeletionOptions: MockDeletionOptions,
      DeletionSearchCriteria: MockDeletionSearchCriteria,
      DeletionFetchOptions: MockDeletionFetchOptions,
      deleteExperiments: jest.fn(),
      deleteSamples: jest.fn(),
      deleteDataSets: jest.fn(),
      deleteProjects: jest.fn(),
      deleteSpaces: jest.fn(),
      searchDeletions: jest.fn(),
    }

    deleteService = new DeleteService({ openbisFacade: mockOpenbisFacade })
  })

  describe('constructor', () => {
    it('should throw error if openbisFacade is not provided', () => {
      expect(() => {
        new DeleteService({ openbisFacade: null as any })
      }).toThrow('openbisFacade is required for DeleteService')
    })

    it('should create instance with valid openbisFacade', () => {
      expect(deleteService).toBeInstanceOf(DeleteService)
    })
  })

  describe('moveExperimentsToTrashcan', () => {
    it('should return success with count 0 for empty array', async () => {
      const result = await deleteService.moveExperimentsToTrashcan([], 'test reason')
      
      expect(result.success).toBe(true)
      expect(result.count).toBe(0)
      expect(mockOpenbisFacade.deleteExperiments).not.toHaveBeenCalled()
    })

    it('should move experiments to trashcan successfully', async () => {
      const experiments = [
        { getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/EXPERIMENT1' }) },
        { getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/EXPERIMENT2' }) },
      ]

      mockOpenbisFacade.deleteExperiments.mockResolvedValue(undefined)

      const result = await deleteService.moveExperimentsToTrashcan(experiments, 'test reason')

      expect(result.success).toBe(true)
      expect(result.count).toBe(2)
      expect(mockOpenbisFacade.deleteExperiments).toHaveBeenCalledTimes(1)
      expect(mockOpenbisFacade.deleteExperiments).toHaveBeenCalledWith(
        expect.arrayContaining([
          expect.any(dto.ExperimentIdentifier),
          expect.any(dto.ExperimentIdentifier),
        ]),
        expect.objectContaining({
          setReason: expect.any(Function),
        })
      )
    })

    it('should handle errors gracefully', async () => {
      const experiments = [
        { getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/EXPERIMENT1' }) },
      ]

      mockOpenbisFacade.deleteExperiments.mockRejectedValue(new Error('Delete failed'))

      const result = await deleteService.moveExperimentsToTrashcan(experiments, 'test reason')

      expect(result.success).toBe(false)
      expect(result.count).toBe(0)
      expect(result.error).toBe('Delete failed')
    })

    it('should handle experiments with identifier string', async () => {
      const experiments = [
        { identifier: '/SPACE/PROJECT/EXPERIMENT1' },
      ]

      mockOpenbisFacade.deleteExperiments.mockResolvedValue(undefined)

      const result = await deleteService.moveExperimentsToTrashcan(experiments, 'test reason')

      expect(result.success).toBe(true)
      expect(result.count).toBe(1)
    })
  })

  describe('moveSamplesToTrashcan', () => {
    it('should return success with count 0 for empty array', async () => {
      const result = await deleteService.moveSamplesToTrashcan([], 'test reason')
      
      expect(result.success).toBe(true)
      expect(result.count).toBe(0)
      expect(mockOpenbisFacade.deleteSamples).not.toHaveBeenCalled()
    })

    it('should move samples to trashcan successfully', async () => {
      const samples = [
        { getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/SAMPLE1' }) },
        { getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/SAMPLE2' }) },
      ]

      mockOpenbisFacade.deleteSamples.mockResolvedValue(undefined)

      const result = await deleteService.moveSamplesToTrashcan(samples, 'test reason')

      expect(result.success).toBe(true)
      expect(result.count).toBe(2)
      expect(mockOpenbisFacade.deleteSamples).toHaveBeenCalledTimes(1)
    })

    it('should handle errors gracefully', async () => {
      const samples = [
        { getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/SAMPLE1' }) },
      ]

      mockOpenbisFacade.deleteSamples.mockRejectedValue(new Error('Delete failed'))

      const result = await deleteService.moveSamplesToTrashcan(samples, 'test reason')

      expect(result.success).toBe(false)
      expect(result.count).toBe(0)
      expect(result.error).toBe('Delete failed')
    })
  })

  describe('moveDataSetsToTrashcan', () => {
    it('should return success with count 0 for empty array', async () => {
      const result = await deleteService.moveDataSetsToTrashcan([], 'test reason')
      
      expect(result.success).toBe(true)
      expect(result.count).toBe(0)
      expect(mockOpenbisFacade.deleteDataSets).not.toHaveBeenCalled()
    })

    it('should move datasets to trashcan successfully', async () => {
      const datasets = [
        { getPermId: () => 'DATASET1' },
        { getCode: () => 'DATASET2' },
      ]

      mockOpenbisFacade.deleteDataSets.mockResolvedValue(undefined)

      const result = await deleteService.moveDataSetsToTrashcan(datasets, 'test reason')

      expect(result.success).toBe(true)
      expect(result.count).toBe(2)
      expect(mockOpenbisFacade.deleteDataSets).toHaveBeenCalledTimes(1)
    })

    it('should handle errors gracefully', async () => {
      const datasets = [
        { getPermId: () => 'DATASET1' },
      ]

      mockOpenbisFacade.deleteDataSets.mockRejectedValue(new Error('Delete failed'))

      const result = await deleteService.moveDataSetsToTrashcan(datasets, 'test reason')

      expect(result.success).toBe(false)
      expect(result.count).toBe(0)
      expect(result.error).toBe('Delete failed')
    })
  })

  describe('moveProjectsToTrashcan', () => {
    it('should return success with count 0 for empty array', async () => {
      const result = await deleteService.moveProjectsToTrashcan([], 'test reason')
      
      expect(result.success).toBe(true)
      expect(result.count).toBe(0)
      expect(mockOpenbisFacade.deleteProjects).not.toHaveBeenCalled()
    })

    it('should move projects to trashcan successfully', async () => {
      const projects = [
        { identifier: '/SPACE/PROJECT1' },
        'PROJECT2', // String identifier
      ]

      mockOpenbisFacade.deleteProjects.mockResolvedValue(undefined)

      const result = await deleteService.moveProjectsToTrashcan(projects, 'test reason')

      expect(result.success).toBe(true)
      expect(result.count).toBe(2)
      expect(mockOpenbisFacade.deleteProjects).toHaveBeenCalledTimes(1)
    })

    it('should handle errors gracefully', async () => {
      const projects = [
        { identifier: '/SPACE/PROJECT1' },
      ]

      mockOpenbisFacade.deleteProjects.mockRejectedValue(new Error('Delete failed'))

      const result = await deleteService.moveProjectsToTrashcan(projects, 'test reason')

      expect(result.success).toBe(false)
      expect(result.count).toBe(0)
      expect(result.error).toBe('Delete failed')
    })
  })

  describe('moveSpacesToTrashcan', () => {
    it('should return success with count 0 for empty array', async () => {
      const result = await deleteService.moveSpacesToTrashcan([], 'test reason')
      
      expect(result.success).toBe(true)
      expect(result.count).toBe(0)
      expect(mockOpenbisFacade.deleteSpaces).not.toHaveBeenCalled()
    })

    it('should move spaces to trashcan successfully', async () => {
      const spaces = ['SPACE1', 'SPACE2']

      mockOpenbisFacade.deleteSpaces.mockResolvedValue(undefined)

      const result = await deleteService.moveSpacesToTrashcan(spaces, 'test reason')

      expect(result.success).toBe(true)
      expect(result.count).toBe(2)
      expect(mockOpenbisFacade.deleteSpaces).toHaveBeenCalledTimes(1)
    })

    it('should handle errors gracefully', async () => {
      const spaces = ['SPACE1']

      mockOpenbisFacade.deleteSpaces.mockRejectedValue(new Error('Delete failed'))

      const result = await deleteService.moveSpacesToTrashcan(spaces, 'test reason')

      expect(result.success).toBe(false)
      expect(result.count).toBe(0)
      expect(result.error).toBe('Delete failed')
    })
  })

  describe('checkExistingDeletions', () => {
    it('should return empty array when no deletions found', async () => {
      const mockDeletions = {
        getObjects: () => [],
      }
      mockOpenbisFacade.searchDeletions.mockResolvedValue(mockDeletions)

      const result = await deleteService.checkExistingDeletions('SPACE1', 'SPACE', ['SAMPLE'])

      expect(result).toEqual([])
      expect(mockOpenbisFacade.searchDeletions).toHaveBeenCalledTimes(1)
    })

    it('should find dependent deletions for space', async () => {
      const mockDeletedObject = {
        entityKind: 'SAMPLE',
        identifier: '/SPACE1/PROJECT1/SAMPLE1',
      }
      const mockDeletion = {
        deletionDate: new Date('2024-01-01'),
        reason: 'Test deletion',
        getDeletedObjects: () => [mockDeletedObject],
      }
      const mockDeletions = {
        getObjects: () => [mockDeletion],
      }
      mockOpenbisFacade.searchDeletions.mockResolvedValue(mockDeletions)

      const result = await deleteService.checkExistingDeletions('SPACE1', 'SPACE', ['SAMPLE'])

      expect(result).toHaveLength(1)
      expect(result[0]).toBe(mockDeletion)
    })

    it('should find dependent deletions for project', async () => {
      const mockDeletedObject1 = {
        entityKind: 'EXPERIMENT',
        identifier: '/SPACE1/PROJECT1/EXPERIMENT1',
      }
      const mockDeletedObject2 = {
        entityKind: 'SAMPLE',
        identifier: '/SPACE1/PROJECT1/SAMPLE1',
      }
      const mockDeletion = {
        deletionDate: new Date('2024-01-01'),
        reason: 'Test deletion',
        getDeletedObjects: () => [mockDeletedObject1, mockDeletedObject2],
      }
      const mockDeletions = {
        getObjects: () => [mockDeletion],
      }
      mockOpenbisFacade.searchDeletions.mockResolvedValue(mockDeletions)

      const result = await deleteService.checkExistingDeletions('/SPACE1/PROJECT1', 'PROJECT', ['EXPERIMENT', 'SAMPLE'])

      expect(result).toHaveLength(1)
    })

    it('should handle errors gracefully', async () => {
      mockOpenbisFacade.searchDeletions.mockRejectedValue(new Error('Search failed'))

      await expect(
        deleteService.checkExistingDeletions('SPACE1', 'SPACE', ['SAMPLE'])
      ).rejects.toThrow('Failed to check existing deletions: Search failed')
    })
  })

  describe('formatDeletionError', () => {
    it('should format deletion error message correctly', () => {
      const deletions = [
        {
          deletionDate: new Date('2024-01-15T10:30:00'),
          reason: 'Test deletion 1',
        },
        {
          deletionDate: new Date('2024-01-20T14:45:00'),
          reason: 'Test deletion 2',
        },
      ]

      const result = deleteService.formatDeletionError(deletions, 'space')

      expect(result).toContain('This space can only be deleted')
      expect(result).toContain('Test deletion 1')
      expect(result).toContain('Test deletion 2')
    })

    it('should handle empty array', () => {
      const result = deleteService.formatDeletionError([], 'project')
      expect(result).toContain('This project can only be deleted')
    })
  })

  describe('extractIdentifier', () => {
    it('should extract identifier from experiment with getIdentifier', () => {
      const experiment = {
        getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/EXPERIMENT' }),
      }
      // @ts-ignore - accessing public method for testing
      const result = deleteService.extractIdentifier(experiment, 'EXPERIMENT')
      expect(result).toBe('/SPACE/PROJECT/EXPERIMENT')
    })

    it('should extract identifier from sample with getIdentifier', () => {
      const sample = {
        getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/SAMPLE' }),
      }
      // @ts-ignore - accessing public method for testing
      const result = deleteService.extractIdentifier(sample, 'SAMPLE')
      expect(result).toBe('/SPACE/PROJECT/SAMPLE')
    })

    it('should extract identifier from object with identifier property', () => {
      const entity = { identifier: '/SPACE/PROJECT/ENTITY' }
      // @ts-ignore - accessing public method for testing
      const result = deleteService.extractIdentifier(entity, 'EXPERIMENT')
      expect(result).toBe('/SPACE/PROJECT/ENTITY')
    })

    it('should extract permId from space', () => {
      const space = { getPermId: () => 'SPACE1' }
      // @ts-ignore - accessing public method for testing
      const result = deleteService.extractIdentifier(space, 'SPACE')
      expect(result).toBe('SPACE1')
    })
  })

  describe('extractDatasetPermId', () => {
    it('should extract permId from dataset with getPermId', () => {
      const dataset = {
        getPermId: () => 'DATASET1',
      }
      // @ts-ignore - accessing public method for testing
      const result = deleteService.extractDatasetPermId(dataset)
      expect(result).toBe('DATASET1')
    })

    it('should extract code from dataset with getCode', () => {
      const dataset = {
        getCode: () => 'DATASET_CODE',
      }
      // @ts-ignore - accessing public method for testing
      const result = deleteService.extractDatasetPermId(dataset)
      expect(result).toBe('DATASET_CODE')
    })

    it('should extract permId from object with permId property', () => {
      const dataset = { permId: 'DATASET1' }
      // @ts-ignore - accessing public method for testing
      const result = deleteService.extractDatasetPermId(dataset)
      expect(result).toBe('DATASET1')
    })
  })
})

