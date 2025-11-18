import React from 'react'
import '@testing-library/jest-dom'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import EntityForm from '@src/js/components/database/new-forms/components/EntityForm.tsx'
import { FormMode, FormSection, FormFieldDataType } from '@src/js/components/database/new-forms/types/form.enums.ts'
import { Form } from '@src/js/components/database/new-forms/types/form.types.ts'
import ComponentRegistry from '@src/js/components/database/new-forms/engine/ComponentRegistry.ts'

jest.mock('@mui/material', () => ({
  __esModule: true,
  Stack: ({ children }: { children: React.ReactNode }) => <div data-testid='stack'>{children}</div>
}))

jest.mock('@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx', () => ({
  __esModule: true,
  default: ({ title, children }: { title: string; children: React.ReactNode }) => (
    <section data-testid={`section-${title}`}>
      <h3>{title}</h3>
      {children}
    </section>
  )
}))

jest.mock('@src/js/components/database/new-forms/engine/ComponentRegistry.ts', () => ({
  __esModule: true,
  default: {
    getActionRenderer: jest.fn(),
    getFieldRenderer: jest.fn()
  }
}))

const mockedRegistry = ComponentRegistry as jest.Mocked<typeof ComponentRegistry>

const MockActionRenderer = ({ action, onAction }: any) => (
  <button data-testid={`action-${action.name}`} onClick={() => onAction(action.name)}>
    {action.label}
  </button>
)

const MockFieldRenderer = ({ field }: any) => (
  <div data-testid={`field-${field.id}`}>
    {field.label}: {field.value}
  </div>
)

const buildBaseForm = (): Form => ({
  entityPermId: 'SPACE-101',
  entityKind: 'space',
  entityType: 'space',
  title: 'Space 101',
  fields: [
    {
      id: 'SPACE-101-code',
      label: 'Code',
      value: 'SPACE-101',
      dataType: FormFieldDataType.VARCHAR,
      required: true,
      readOnly: true,
      isMultiValue: false,
      section: FormSection.IDENTIFICATION_INFO,
      column: 'left',
      meta: {}
    }
  ],
  version: 1,
  isDirty: false,
  isValid: true,
  meta: {},
  actions: []
})

describe('EntityForm', () => {
  beforeEach(() => {
    mockedRegistry.getFieldRenderer.mockReturnValue(MockFieldRenderer)
    mockedRegistry.getActionRenderer.mockReturnValue(MockActionRenderer)
  })

  afterEach(() => {
    jest.clearAllMocks()
  })

  it('renders only the actions that pass visibility rules', async () => {
    const onAction = jest.fn()
    const form: Form = {
      ...buildBaseForm(),
      actions: [
        { name: 'edit', label: 'Edit', component: 'button', isAllowed: true, visibility: [] },
        {
          name: 'view-only',
          label: 'View Only',
          component: 'button',
          isAllowed: true,
          visibility: [{ mode: FormMode.VIEW }]
        },
        {
          name: 'admin-only',
          label: 'Admin Only',
          component: 'button',
          isAllowed: true,
          visibility: [{ permission: 'CAN_ADMIN' }]
        }
      ]
    }

    render(
      <EntityForm
        form={form}
        mode={FormMode.VIEW}
        permissions={{}}
        onFieldChange={jest.fn()}
        onFieldMetadataChange={jest.fn()}
        onAction={onAction}
        params={{}}
      />
    )

    expect(screen.getByTestId('action-edit')).toBeInTheDocument()
    expect(screen.getByTestId('action-view-only')).toBeInTheDocument()
    expect(screen.queryByTestId('action-admin-only')).toBeNull()

    await userEvent.click(screen.getByTestId('action-edit'))

    expect(onAction).toHaveBeenCalledWith('edit')
  })

  it('derives sections from fields when none are provided', () => {
    const form: Form = {
      ...buildBaseForm(),
      fields: [
        {
          id: 'SPACE-101-code',
          label: 'Code',
          value: 'SPACE-101',
          dataType: FormFieldDataType.VARCHAR,
          required: true,
          readOnly: true,
          isMultiValue: false,
          section: FormSection.IDENTIFICATION_INFO,
          column: 'left',
          meta: {}
        },
        {
          id: 'SPACE-101-registrator',
          label: 'Registrator',
          value: 'user-1',
          dataType: FormFieldDataType.VARCHAR,
          required: false,
          readOnly: true,
          isMultiValue: false,
          section: FormSection.IDENTIFICATION_INFO,
          column: 'right',
          meta: {}
        },
        {
          id: 'SPACE-101-description',
          label: 'Description',
          value: 'Space description',
          dataType: FormFieldDataType.WORD_PROCESSOR,
          required: false,
          readOnly: false,
          isMultiValue: false,
          section: FormSection.GENERAL,
          column: 'center',
          meta: {}
        }
      ]
    }

    render(
      <EntityForm
        form={form}
        mode={FormMode.VIEW}
        permissions={{}}
        onFieldChange={jest.fn()}
        onFieldMetadataChange={jest.fn()}
        onAction={jest.fn()}
        params={{}}
      />
    )

    const identificationSection = screen.getByTestId(`section-${FormSection.IDENTIFICATION_INFO}`)
    const generalSection = screen.getByTestId(`section-${FormSection.GENERAL}`)

    expect(within(identificationSection).getByTestId('field-SPACE-101-code')).toBeInTheDocument()
    expect(within(identificationSection).getByTestId('field-SPACE-101-registrator')).toBeInTheDocument()
    expect(within(generalSection).getByTestId('field-SPACE-101-description')).toBeInTheDocument()
  })
})

