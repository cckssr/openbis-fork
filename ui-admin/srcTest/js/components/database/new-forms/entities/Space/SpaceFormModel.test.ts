import { SpaceFormModel } from '@src/js/components/database/new-forms/entities/Space/SpaceFormModel.ts'
import { FormSection } from '@src/js/components/database/new-forms/types/form.enums.ts'
import spaceDtoFixture from '@srcTest/js/components/database/new-forms/entities/Space/spaceDto.fixture.json'

/**
 * SpaceFormModel tests use a real SpaceDto structure to ensure
 * the adaptation logic works with actual API responses.
 * 
 * The fixture represents a typical Space DTO from openBIS:
 * - Uses timestamp numbers for dates (not ISO strings)
 * - Contains nested registrator Person object
 * - May have null description
 * - Includes fetchOptions and metadata that are ignored by the model
 */
describe('SpaceFormModel', () => {
  it('adapts a real SpaceDto into a normalized form structure', () => {
    // Use the real DTO structure from the API
    const dto = spaceDtoFixture as any

    const form = SpaceFormModel.adaptSpaceDtoToForm(dto)

    // Verify core form metadata
    expect(form.entityPermId).toBe('DEFAULT')
    expect(form.title).toBe('Space: DEFAULT')
    expect(form.version).toBe(1) // Default when version is missing
    expect(form.entityKind).toBe('space')
    expect(form.entityType).toBe('space')
    expect(form.sections).toBeUndefined() // Sections are now derived from fields

    // Verify all expected fields are present in correct order
    const fieldIds = form.fields.map(field => field.id)
    expect(fieldIds).toEqual([
      'DEFAULT-code',
      'DEFAULT-description',
      'DEFAULT-registrator',
      'DEFAULT-registrationDate',
      'DEFAULT-modifier',
      'DEFAULT-modificationDate'
    ])

    // Verify field properties from real DTO
    const codeField = form.fields.find(field => field.id === 'DEFAULT-code')
    expect(codeField?.value).toBe('DEFAULT')
    expect(codeField?.section).toBe(FormSection.IDENTIFICATION_INFO)
    expect(codeField?.column).toBe('left')
    expect(codeField?.readOnly).toBe(true)
    expect(codeField?.required).toBe(true)

    // Description can be null in real DTOs
    const descriptionField = form.fields.find(field => field.id === 'DEFAULT-description')
    expect(descriptionField?.value).toBeNull()
    expect(descriptionField?.section).toBe(FormSection.GENERAL)
    expect(descriptionField?.column).toBe('center')
    expect(descriptionField?.readOnly).toBe(false)

    // Registrator comes from nested Person object
    const registratorField = form.fields.find(field => field.id === 'DEFAULT-registrator')
    expect(registratorField?.value).toBe('system')
    expect(registratorField?.section).toBe(FormSection.IDENTIFICATION_INFO)
    expect(registratorField?.column).toBe('right')

    // Dates are converted from timestamps to formatted strings
    const registrationDateField = form.fields.find(field => field.id === 'DEFAULT-registrationDate')
    expect(registrationDateField?.value).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    expect(registrationDateField?.dataType).toBe('TIMESTAMP')
  })

  it('handles DTO with description text', () => {
    const dto = { ...spaceDtoFixture, description: 'Test space description' } as any

    const form = SpaceFormModel.adaptSpaceDtoToForm(dto)

    const descriptionField = form.fields.find(field => field.id === 'DEFAULT-description')
    expect(descriptionField?.value).toBe('Test space description')
  })
})

