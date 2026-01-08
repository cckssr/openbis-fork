import { MoveService } from '@src/js/components/database/new-forms/services/MoveService.ts'
import openbis from '@srcTest/js/services/openbis.js'
import dto from '@srcTest/js/services/openbis/dto.js'

// Mock update classes
class MockProjectUpdate {
  setProjectId(id: any) {}
  setSpaceId(id: string) {}
}

class MockExperimentUpdate {
  setExperimentId(id: any) {}
  setProjectId(id: any) {}
  setProperties(props: any) {}
}

class MockSampleUpdate {
  setSampleId(id: any) {}
  setExperimentId(id: any) {}
  setProjectId(id: any) {}
  setSpaceId(id: any) {}
}

class MockDataSetUpdate {
  setDataSetId(id: any) {}
  setExperimentId(id: any) {}
  setSampleId(id: any) {}
  setProperties(props: any) {}
}

// Mock fetch options classes
class MockProjectFetchOptions {
  withSpace() { return this }
}

class MockExperimentFetchOptions {
  withProperties() { return this }
  withProject() { return this }
}

class MockSampleFetchOptions {
  withExperiment() { return this }
  withProject() { return this }
  withSpace() { return this }
  withChildrenUsing(options: any) { return this }
}

class MockDataSetFetchOptions {
  withProperties() { return this }
  withExperiment() { return this }
  withSample() { return this }
}

describe('MoveService', () => {
  let moveService: MoveService
  let mockOpenbisFacade: any

  beforeEach(() => {
    jest.clearAllMocks()
    
    // Create mock ID classes if not available in dto
    const MockSamplePermId = dto.SamplePermId || class {
      constructor(permId: string) {
        this.permId = permId
      }
    }
    const MockProjectPermId = dto.ProjectPermId || class {
      constructor(permId: string) {
        this.permId = permId
      }
    }
    const MockExperimentPermId = dto.ExperimentPermId || class {
      constructor(permId: string) {
        this.permId = permId
      }
    }

    // Create mock openbis facade
    mockOpenbisFacade = {
      ProjectPermId: MockProjectPermId,
      ProjectUpdate: MockProjectUpdate,
      ProjectIdentifier: dto.ProjectIdentifier,
      ProjectFetchOptions: MockProjectFetchOptions,
      ExperimentPermId: MockExperimentPermId,
      ExperimentUpdate: MockExperimentUpdate,
      ExperimentFetchOptions: MockExperimentFetchOptions,
      ExperimentIdentifier: dto.ExperimentIdentifier,
      SamplePermId: MockSamplePermId,
      SampleUpdate: MockSampleUpdate,
      SampleFetchOptions: MockSampleFetchOptions,
      SampleIdentifier: dto.SampleIdentifier,
      DataSetPermId: dto.DataSetPermId,
      DataSetUpdate: MockDataSetUpdate,
      DataSetFetchOptions: MockDataSetFetchOptions,
      updateProjects: jest.fn(),
      updateExperiments: jest.fn(),
      updateSamples: jest.fn(),
      updateDataSets: jest.fn(),
      getExperiments: jest.fn(),
      getSamples: jest.fn(),
      getDataSets: jest.fn(),
      getProjects: jest.fn(),
    }

    moveService = new MoveService({ openbisFacade: mockOpenbisFacade })
  })

  describe('constructor', () => {
    it('should throw error if openbisFacade is not provided', () => {
      expect(() => {
        new MoveService({ openbisFacade: null as any })
      }).toThrow('openbisFacade is required for MoveService')
    })

    it('should create instance with valid openbisFacade', () => {
      expect(moveService).toBeInstanceOf(MoveService)
    })
  })

  describe('moveProject', () => {
    it('should move project to different space successfully', async () => {
      mockOpenbisFacade.updateProjects.mockResolvedValue(undefined)

      const result = await moveService.moveProject('PROJECT1', 'SPACE2')

      expect(result.success).toBe(true)
      expect(result.message).toBe('Project moved successfully')
      expect(mockOpenbisFacade.updateProjects).toHaveBeenCalledTimes(1)
    })

    it('should handle errors gracefully', async () => {
      mockOpenbisFacade.updateProjects.mockRejectedValue(new Error('Move failed'))

      const result = await moveService.moveProject('PROJECT1', 'SPACE2')

      expect(result.success).toBe(false)
      expect(result.error).toBe('Move failed')
    })
  })

  describe('moveCollection', () => {
    it('should move collection to different project successfully', async () => {
      const mockExperiment = {
        getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT1/EXPERIMENT1' }),
        getProperties: () => ({}),
      }
      const mockProject = {
        getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT2' }),
      }

      mockOpenbisFacade.getExperiments.mockResolvedValue({
        'EXPERIMENT1': mockExperiment,
      })
      mockOpenbisFacade.updateExperiments.mockResolvedValue(undefined)

      const result = await moveService.moveCollection('EXPERIMENT1', mockProject)

      expect(result.success).toBe(true)
      expect(result.message).toBe('Collection moved successfully')
      expect(mockOpenbisFacade.getExperiments).toHaveBeenCalledTimes(1)
      expect(mockOpenbisFacade.updateExperiments).toHaveBeenCalledTimes(1)
    })

    it('should handle collection not found', async () => {
      mockOpenbisFacade.getExperiments.mockResolvedValue({})

      const result = await moveService.moveCollection('EXPERIMENT1', { getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT2' }) })

      expect(result.success).toBe(false)
      expect(result.error).toContain('not found')
    })

    it('should handle errors gracefully', async () => {
      mockOpenbisFacade.getExperiments.mockRejectedValue(new Error('Fetch failed'))

      const result = await moveService.moveCollection('EXPERIMENT1', { getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT2' }) })

      expect(result.success).toBe(false)
      expect(result.error).toBe('Fetch failed')
    })
  })

  describe('moveObject', () => {
    it('should move object without descendants successfully', async () => {
      mockOpenbisFacade.updateSamples.mockResolvedValue(undefined)

      const target = {
        '@type': 'as.dto.project.Project',
        getPermId: () => 'PROJECT1',
        getSpace: () => ({ getPermId: () => 'SPACE1' }),
      }

      const result = await moveService.moveObject('SAMPLE1', target, false)

      expect(result.success).toBe(true)
      expect(result.message).toBe('Object moved successfully')
      expect(mockOpenbisFacade.updateSamples).toHaveBeenCalledTimes(1)
    })

    it('should move object to space', async () => {
      mockOpenbisFacade.updateSamples.mockResolvedValue(undefined)

      const target = {
        '@type': 'as.dto.space.Space',
        getPermId: () => 'SPACE1',
      }

      const result = await moveService.moveObject('SAMPLE1', target, false)

      expect(result.success).toBe(true)
      expect(mockOpenbisFacade.updateSamples).toHaveBeenCalledTimes(1)
    })

    it('should move object to experiment', async () => {
      mockOpenbisFacade.updateSamples.mockResolvedValue(undefined)

      const target = {
        '@type': 'as.dto.experiment.Experiment',
        getPermId: () => 'EXPERIMENT1',
        getProject: () => ({
          getPermId: () => 'PROJECT1',
          getSpace: () => ({ getPermId: () => 'SPACE1' }),
        }),
      }

      const result = await moveService.moveObject('SAMPLE1', target, false)

      expect(result.success).toBe(true)
      expect(mockOpenbisFacade.updateSamples).toHaveBeenCalledTimes(1)
    })

    it('should handle errors gracefully', async () => {
      mockOpenbisFacade.updateSamples.mockRejectedValue(new Error('Move failed'))

      const target = {
        '@type': 'as.dto.project.Project',
        getPermId: () => 'PROJECT1',
        getSpace: () => ({ getPermId: () => 'SPACE1' }),
      }

      const result = await moveService.moveObject('SAMPLE1', target, false)

      expect(result.success).toBe(false)
      expect(result.error).toBe('Move failed')
    })
  })

  describe('moveObjectWithDescendants', () => {
    it('should move object with descendants successfully', async () => {
      const mockSample = {
        getPermId: () => ({ getPermId: () => 'SAMPLE1' }),
        getExperiment: () => ({
          getPermId: () => ({ getPermId: () => 'EXPERIMENT1' }),
        }),
        getChildren: () => [
          {
            getPermId: () => ({ getPermId: () => 'SAMPLE2' }),
            getExperiment: () => ({
              getPermId: () => ({ getPermId: () => 'EXPERIMENT1' }),
            }),
            getChildren: () => [],
          },
        ],
      }

      mockOpenbisFacade.getSamples.mockResolvedValue({
        'SAMPLE1': mockSample,
      })
      mockOpenbisFacade.updateSamples.mockResolvedValue(undefined)

      const target = {
        '@type': 'as.dto.project.Project',
        getPermId: () => 'PROJECT1',
        getSpace: () => ({ getPermId: () => 'SPACE1' }),
      }

      const result = await moveService.moveObject('SAMPLE1', target, true)

      expect(result.success).toBe(true)
      expect(mockOpenbisFacade.getSamples).toHaveBeenCalledTimes(1)
      expect(mockOpenbisFacade.updateSamples).toHaveBeenCalledTimes(1)
    })

    it('should handle object not found', async () => {
      mockOpenbisFacade.getSamples.mockResolvedValue({})

      const target = {
        '@type': 'as.dto.project.Project',
        getPermId: () => 'PROJECT1',
        getSpace: () => ({ getPermId: () => 'SPACE1' }),
      }

      const result = await moveService.moveObject('SAMPLE1', target, true)

      expect(result.success).toBe(false)
      expect(result.error).toContain('not found')
    })
  })

  describe('moveDataset', () => {
    it('should move dataset to experiment successfully', async () => {
      const mockDataset = {
        getProperties: () => ({}),
      }
      const mockTarget = {
        '@type': 'as.dto.experiment.Experiment',
        getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/EXPERIMENT1' }),
      }

      mockOpenbisFacade.getDataSets.mockResolvedValue({
        'DATASET1': mockDataset,
      })
      mockOpenbisFacade.updateDataSets.mockResolvedValue(undefined)

      const result = await moveService.moveDataset('DATASET1', mockTarget)

      expect(result.success).toBe(true)
      expect(result.message).toBe('Dataset moved successfully')
      expect(mockOpenbisFacade.getDataSets).toHaveBeenCalledTimes(1)
      expect(mockOpenbisFacade.updateDataSets).toHaveBeenCalledTimes(1)
    })

    it('should move dataset to sample successfully', async () => {
      const mockDataset = {
        getProperties: () => ({}),
      }
      const mockTarget = {
        '@type': 'as.dto.sample.Sample',
        getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/SAMPLE1' }),
        getExperiment: () => null,
      }

      mockOpenbisFacade.getDataSets.mockResolvedValue({
        'DATASET1': mockDataset,
      })
      mockOpenbisFacade.updateDataSets.mockResolvedValue(undefined)

      const result = await moveService.moveDataset('DATASET1', mockTarget)

      expect(result.success).toBe(true)
      expect(mockOpenbisFacade.updateDataSets).toHaveBeenCalledTimes(1)
    })

    it('should handle dataset not found', async () => {
      mockOpenbisFacade.getDataSets.mockResolvedValue({})

      const mockTarget = {
        '@type': 'as.dto.experiment.Experiment',
        getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/EXPERIMENT1' }),
      }

      const result = await moveService.moveDataset('DATASET1', mockTarget)

      expect(result.success).toBe(false)
      expect(result.error).toContain('not found')
    })

    it('should handle invalid target type', async () => {
      const mockDataset = {
        getProperties: () => ({}),
      }
      mockOpenbisFacade.getDataSets.mockResolvedValue({
        'DATASET1': mockDataset,
      })

      const mockTarget = {
        '@type': 'as.dto.space.Space',
        getPermId: () => 'SPACE1',
      }

      const result = await moveService.moveDataset('DATASET1', mockTarget)

      expect(result.success).toBe(false)
      expect(result.error).toContain('Invalid target type')
    })

    it('should handle errors gracefully', async () => {
      mockOpenbisFacade.getDataSets.mockRejectedValue(new Error('Fetch failed'))

      const mockTarget = {
        '@type': 'as.dto.experiment.Experiment',
        getIdentifier: () => ({ getIdentifier: () => '/SPACE/PROJECT/EXPERIMENT1' }),
      }

      const result = await moveService.moveDataset('DATASET1', mockTarget)

      expect(result.success).toBe(false)
      expect(result.error).toBe('Fetch failed')
    })
  })

  describe('prepareSampleUpdate', () => {
    it('should prepare sample update for project target', () => {
      const target = {
        '@type': 'as.dto.project.Project',
        getPermId: () => 'PROJECT1',
        getSpace: () => ({ getPermId: () => 'SPACE1' }),
      }

      const result = moveService.prepareSampleUpdate('SAMPLE1', target)

      expect(result).toBeDefined()
      expect(result.setSampleId).toBeDefined()
      expect(result.setProjectId).toBeDefined()
      expect(result.setSpaceId).toBeDefined()
      expect(result.setExperimentId).toBeDefined()
    })

    it('should prepare sample update for space target', () => {
      const target = {
        '@type': 'as.dto.space.Space',
        getPermId: () => 'SPACE1',
      }

      const result = moveService.prepareSampleUpdate('SAMPLE1', target)

      expect(result).toBeDefined()
    })

    it('should throw error for invalid target type', () => {
      const target = {
        '@type': 'as.dto.dataset.DataSet',
        getPermId: () => 'DATASET1',
      }

      expect(() => {
        moveService.prepareSampleUpdate('SAMPLE1', target)
      }).toThrow('Invalid target type')
    })
  })

  describe('getEntityTypeFromDtoType', () => {
    it('should map DTO types correctly', () => {
      expect(moveService.getEntityTypeFromDtoType('as.dto.space.Space')).toBe('SPACE')
      expect(moveService.getEntityTypeFromDtoType('as.dto.project.Project')).toBe('PROJECT')
      expect(moveService.getEntityTypeFromDtoType('as.dto.experiment.Experiment')).toBe('EXPERIMENT')
      expect(moveService.getEntityTypeFromDtoType('as.dto.sample.Sample')).toBe('SAMPLE')
      expect(moveService.getEntityTypeFromDtoType('as.dto.dataset.DataSet')).toBe('DATASET')
      expect(moveService.getEntityTypeFromDtoType('unknown.type')).toBe('UNKNOWN')
    })
  })

  describe('extractSpaceIdFromTarget', () => {
    it('should extract space ID from space target', () => {
      const target = {
        '@type': 'as.dto.space.Space',
        getPermId: () => 'SPACE1',
      }

      const result = moveService.extractSpaceIdFromTarget(target)
      expect(result).toBe('SPACE1')
    })

    it('should extract space ID from project target', () => {
      const target = {
        '@type': 'as.dto.project.Project',
        getPermId: () => 'PROJECT1',
        getSpace: () => ({ getPermId: () => 'SPACE1' }),
      }

      const result = moveService.extractSpaceIdFromTarget(target)
      expect(result).toBe('SPACE1')
    })

    it('should extract space ID from experiment target', () => {
      const target = {
        '@type': 'as.dto.experiment.Experiment',
        getPermId: () => 'EXPERIMENT1',
        getProject: () => ({
          getPermId: () => 'PROJECT1',
          getSpace: () => ({ getPermId: () => 'SPACE1' }),
        }),
      }

      const result = moveService.extractSpaceIdFromTarget(target)
      expect(result).toBe('SPACE1')
    })
  })
})

