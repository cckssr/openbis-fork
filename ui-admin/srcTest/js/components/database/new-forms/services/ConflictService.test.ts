import { ConflictService } from '@src/js/components/database/new-forms/services/ConflictService.ts'
import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts'

describe('ConflictService', () => {
  let conflictService: ConflictService

  beforeEach(() => {
    conflictService = new ConflictService()
  })

  describe('checkModificationDateConflict', () => {
    it('should return false when no modification dates exist', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [],
      } as Form

      const result = conflictService.checkModificationDateConflict(localForm, serverForm)
      expect(result).toBe(false)
    })

    it('should return false when local date is missing', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-modificationDate',
            value: '2024-01-15 10:00:00',
          } as FormField,
        ],
      } as Form

      const result = conflictService.checkModificationDateConflict(localForm, serverForm)
      expect(result).toBe(false)
    })

    it('should return false when server date is missing', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-modificationDate',
            value: '2024-01-15 10:00:00',
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [],
      } as Form

      const result = conflictService.checkModificationDateConflict(localForm, serverForm)
      expect(result).toBe(false)
    })

    it('should return false when dates are not strings', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-modificationDate',
            value: 1234567890,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-modificationDate',
            value: 1234567891,
          } as FormField,
        ],
      } as Form

      const result = conflictService.checkModificationDateConflict(localForm, serverForm)
      expect(result).toBe(false)
    })

    it('should return true when server date is newer', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-modificationDate',
            value: '2024-01-15 10:00:00',
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-modificationDate',
            value: '2024-01-15 11:00:00',
          } as FormField,
        ],
      } as Form

      const result = conflictService.checkModificationDateConflict(localForm, serverForm)
      expect(result).toBe(true)
    })

    it('should return false when local date is newer or equal', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-modificationDate',
            value: '2024-01-15 11:00:00',
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-modificationDate',
            value: '2024-01-15 10:00:00',
          } as FormField,
        ],
      } as Form

      const result = conflictService.checkModificationDateConflict(localForm, serverForm)
      expect(result).toBe(false)
    })
  })

  describe('findConflicts', () => {
    it('should return empty array when no conflicts exist', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Test description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Test description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const result = conflictService.findConflicts(localForm, serverForm)
      expect(result).toEqual([])
    })

    it('should find conflicts when values differ', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-description',
            label: 'Description',
            value: 'Local description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-description',
            label: 'Description',
            value: 'Server description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const result = conflictService.findConflicts(localForm, serverForm)
      expect(result).toHaveLength(1)
      expect(result[0].fieldId).toBe('ENTITY1-description')
      expect(result[0].fieldName).toBe('Description')
      expect(result[0].localValue).toBe('Local description')
      expect(result[0].serverValue).toBe('Server description')
    })

    it('should ignore read-only fields', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-code',
            value: 'Local code',
            readOnly: true,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-code',
            value: 'Server code',
            readOnly: true,
          } as FormField,
        ],
      } as Form

      const result = conflictService.findConflicts(localForm, serverForm)
      expect(result).toEqual([])
    })

    it('should handle multiple conflicts', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-description',
            label: 'Description',
            value: 'Local description',
            readOnly: false,
          } as FormField,
          {
            id: 'ENTITY1-name',
            label: 'Name',
            value: 'Local name',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-description',
            label: 'Description',
            value: 'Server description',
            readOnly: false,
          } as FormField,
          {
            id: 'ENTITY1-name',
            label: 'Name',
            value: 'Server name',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const result = conflictService.findConflicts(localForm, serverForm)
      expect(result).toHaveLength(2)
    })

    it('should handle object values', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-metadata',
            label: 'Metadata',
            value: { key1: 'value1', key2: 'value2' },
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-metadata',
            label: 'Metadata',
            value: { key1: 'value1', key2: 'different' },
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const result = conflictService.findConflicts(localForm, serverForm)
      expect(result).toHaveLength(1)
    })

    it('should handle array values', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-tags',
            label: 'Tags',
            value: ['tag1', 'tag2'],
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        fields: [
          {
            id: 'ENTITY1-tags',
            label: 'Tags',
            value: ['tag1', 'tag3'],
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const result = conflictService.findConflicts(localForm, serverForm)
      expect(result).toHaveLength(1)
    })
  })

  describe('resolveConflicts', () => {
    it('should resolve conflicts using local values', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        version: 1,
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Local description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        version: 2,
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Server description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const resolutions = {
        'ENTITY1-description': 'local' as const,
      }

      const result = conflictService.resolveConflicts(localForm, serverForm, resolutions)

      expect(result.fields[0].value).toBe('Local description')
      expect(result.version).toBe(2) // Should use server version
    })

    it('should resolve conflicts using server values', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        version: 1,
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Local description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        version: 2,
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Server description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const resolutions = {
        'ENTITY1-description': 'server' as const,
      }

      const result = conflictService.resolveConflicts(localForm, serverForm, resolutions)

      expect(result.fields[0].value).toBe('Server description')
      expect(result.version).toBe(2)
    })

    it('should use local values when no resolution specified', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        version: 1,
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Local description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        version: 2,
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Server description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const result = conflictService.resolveConflicts(localForm, serverForm, {})

      expect(result.fields[0].value).toBe('Local description')
    })

    it('should handle custom resolution', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        version: 1,
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Local description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        version: 2,
        fields: [
          {
            id: 'ENTITY1-description',
            value: 'Server description',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const resolutions = {
        'ENTITY1-description': 'custom' as const,
      }

      const result = conflictService.resolveConflicts(localForm, serverForm, resolutions)

      // Custom resolution currently returns local field (simplified implementation)
      expect(result.fields[0].value).toBe('Local description')
    })

    it('should handle fields that exist only in local form', () => {
      const localForm: Form = {
        entityPermId: 'ENTITY1',
        version: 1,
        fields: [
          {
            id: 'ENTITY1-localField',
            value: 'Local only',
            readOnly: false,
          } as FormField,
        ],
      } as Form

      const serverForm: Form = {
        entityPermId: 'ENTITY1',
        version: 2,
        fields: [],
      } as Form

      const result = conflictService.resolveConflicts(localForm, serverForm, {})

      expect(result.fields).toHaveLength(1)
      expect(result.fields[0].value).toBe('Local only')
    })
  })
})

