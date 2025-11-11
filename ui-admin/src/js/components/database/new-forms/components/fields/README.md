# Field Renderers

Field renderers are React components responsible for rendering form fields based on their data type. They translate field definitions into appropriate UI input components.

## Overview

Field renderers receive field definitions and render them as appropriate input components (text fields, date pickers, dropdowns, etc.). They are registered in the `ComponentRegistry` and retrieved based on the field's `dataType`.

## Components

### TextFieldRenderer

Renders single-line text input fields. Used for `VARCHAR` data types.

**File**: `TextFieldRenderer.tsx`

**Data Type**: `FormFieldDataType.VARCHAR`

**Features**:
- Single-line text input
- Respects `readOnly` flag
- Shows/hides based on form mode (edit/create vs view)
- Supports required/mandatory fields
- Disables underline styling

**Usage**:
```typescript
<TextFieldRenderer
  field={{
    id: 'name',
    label: 'Name',
    value: 'Project Name',
    dataType: FormFieldDataType.VARCHAR,
    required: true,
    readOnly: false
  }}
  onFieldChange={(id, value) => updateField(id, value)}
  mode={FormMode.EDIT}
/>
```

### TextAreaFieldRenderer

Renders multi-line text input fields. Used for `MULTILINE_VARCHAR` data types.

**File**: `TextAreaFieldRenderer.tsx`

**Data Type**: `FormFieldDataType.MULTILINE_VARCHAR`

**Features**:
- Multi-line text input
- Supports help text from `field.meta?.helpText`
- Respects `readOnly` flag
- Shows/hides based on form mode

**Usage**:
```typescript
<TextAreaFieldRenderer
  field={{
    id: 'description',
    label: 'Description',
    value: 'Long description text...',
    dataType: FormFieldDataType.MULTILINE_VARCHAR,
    meta: { helpText: 'Enter a detailed description' }
  }}
  onFieldChange={(id, value) => updateField(id, value)}
  mode={FormMode.EDIT}
/>
```

### DateFieldRenderer

Renders date picker fields. Used for `TIMESTAMP` data types.

**File**: `DateFieldRenderer.tsx`

**Data Type**: `FormFieldDataType.TIMESTAMP`

**Features**:
- Date picker component
- Converts between ISO string (field value) and Date object (component)
- Respects `readOnly` flag
- Shows/hides based on form mode

**Usage**:
```typescript
<DateFieldRenderer
  field={{
    id: 'created',
    label: 'Created Date',
    value: '2024-01-15T10:30:00Z',
    dataType: FormFieldDataType.TIMESTAMP
  }}
  onFieldChange={(id, value) => updateField(id, value)}
  mode={FormMode.EDIT}
/>
```

### SelectFieldRenderer

Renders dropdown/select fields. Used for `CONTROLLED_VOCABULARY` data types.

**File**: `SelectFieldRender.tsx`

**Data Type**: `FormFieldDataType.CONTROLLED_VOCABULARY`

**Features**:
- Dropdown select component
- Supports empty option from `field.meta?.emptyOption`
- Supports help text from `field.meta?.helpText`
- Respects `readOnly` flag
- Passes entire field as `reference` prop

**Usage**:
```typescript
<SelectFieldRenderer
  field={{
    id: 'status',
    label: 'Status',
    value: 'active',
    dataType: FormFieldDataType.CONTROLLED_VOCABULARY,
    meta: {
      emptyOption: 'Select a status',
      helpText: 'Choose the project status'
    }
  }}
  onFieldChange={(id, value) => updateField(id, value)}
  mode={FormMode.EDIT}
/>
```

### SwitchFieldRenderer

Renders boolean fields as dropdown selects (Yes/No). Used for `BOOLEAN` data types.

**File**: `SwitchFieldRender.tsx`

**Data Type**: `FormFieldDataType.BOOLEAN`

**Features**:
- Renders as select dropdown (not a switch component)
- Options: empty, "Yes" (true), "No" (false)
- Supports help text and empty option
- Respects `readOnly` flag

**Note**: Despite the name, this renderer uses a SelectField, not a Switch component.

**Usage**:
```typescript
<SwitchFieldRenderer
  field={{
    id: 'isActive',
    label: 'Is Active',
    value: true,
    dataType: FormFieldDataType.BOOLEAN
  }}
  onFieldChange={(id, value) => updateField(id, value)}
  mode={FormMode.EDIT}
/>
```

### CKEditorFieldRenderer

Renders rich text editor fields with markdown support. Used for `WORD_PROCESSOR` data types.

**File**: `CKEditorFieldRenderer.tsx`

**Data Type**: `FormFieldDataType.WORD_PROCESSOR`

**Features**:
- Full-featured rich text editor (CKEditor)
- Markdown mode toggle
- Title extraction from editor content
- Metadata tracking (markdown state, title)
- Disabled toolbar in view mode
- Supports both classic and document editor modes
- Session ID for file uploads

**Props**:
- `field`: Field definition with optional `meta.mode` ('classic' or 'document')
- `onFieldChange`: Callback for content changes
- `onFieldMetadataChange`: Callback for metadata changes (title, markdown state)
- `mode`: Form mode
- `params`: Additional parameters including `sessionID`

**Usage**:
```typescript
<CKEditorFieldRenderer
  field={{
    id: 'content',
    label: 'Content',
    value: '<p>HTML content</p>',
    dataType: FormFieldDataType.WORD_PROCESSOR,
    meta: {
      mode: 'classic',
      isMarkdown: false
    }
  }}
  onFieldChange={(id, value) => updateField(id, value)}
  onFieldMetadataChange={(id, meta) => updateFieldMetadata(id, meta)}
  mode={FormMode.EDIT}
  params={{ sessionID: 'session-123' }}
/>
```

**Subdirectory**: `CKEditor/` contains:
- `CKEditorClassic.jsx` - Main CKEditor component
- `CKEditorConfig.js` - Editor configuration
- `CKEditorDocument.jsx` - Document editor variant
- `MarkdownToggler.jsx` - Markdown toggle component
- `CKEditorClassic.css` - Editor styles

## Field Renderer Interface

All field renderers must implement the `FieldRendererProps` interface:

```typescript
interface FieldRendererProps {
  field: FormField;                              // Field definition
  onFieldChange: (fieldId: string, value: any) => void;  // Value change callback
  onFieldMetadataChange?: (fieldId: string, meta: any) => void;  // Metadata change callback
  mode: FormMode;                               // Current form mode
  params?: any;                                 // Additional parameters
}
```

## Registration

Field renderers are registered in `ComponentRegistry.getFieldRenderer()`:

```typescript
// In ComponentRegistry.ts
static getFieldRenderer(dataType: string) {
  switch (dataType) {
    case FormFieldDataType.VARCHAR:
      return TextFieldRenderer;
    case FormFieldDataType.TIMESTAMP:
      return DateFieldRenderer;
    case FormFieldDataType.MULTILINE_VARCHAR:
      return TextAreaFieldRenderer;
    case FormFieldDataType.CONTROLLED_VOCABULARY:
      return SelectFieldRenderer;
    case FormFieldDataType.BOOLEAN:
      return SwitchFieldRenderer;
    case FormFieldDataType.WORD_PROCESSOR:
      return CKEditorFieldRenderer;
    default:
      return TextFieldRenderer;  // Default fallback
  }
}
```

## Creating a New Field Renderer

To add a new field renderer:

### 1. Create the Component

```typescript
// NewFieldRenderer.tsx
import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';

export const NewFieldRenderer: React.FC<FieldRendererProps> = ({ 
  field, 
  onFieldChange, 
  mode 
}) => {
  const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;
  
  return (
    <YourInputComponent
      label={field.label}
      value={field.value}
      required={field.required}
      disabled={isEditing && field.readOnly}
      onChange={(value) => onFieldChange(field.id, value)}
    />
  );
};
```

### 2. Register in ComponentRegistry

```typescript
// In ComponentRegistry.ts
import { NewFieldRenderer } from '@src/js/components/database/new-forms/components/fields/NewFieldRenderer.tsx';

static getFieldRenderer(dataType: string) {
  switch (dataType) {
    // ... existing cases
    case FormFieldDataType.NEW_TYPE:
      return NewFieldRenderer;
    default:
      return TextFieldRenderer;
  }
}
```

### 3. Add Data Type Enum

```typescript
// In form.enums.ts
export enum FormFieldDataType {
  // ... existing types
  NEW_TYPE = 'NEW_TYPE'
}
```

## Common Patterns

### Mode-Based Rendering

All field renderers should respect the form mode:

```typescript
const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;

return (
  <InputComponent
    mode={isEditing && !field.readOnly ? FormMode.EDIT : FormMode.VIEW}
    disabled={isEditing && field.readOnly}
  />
);
```

### Value Conversion

Some renderers need to convert between field value format and component format:

```typescript
// DateFieldRenderer: ISO string ↔ Date object
value={{ dateObject: new Date(field.value) }}
onChange={(date: Date) => onFieldChange(field.id, date.toISOString())}

// SwitchFieldRenderer: boolean ↔ string
value={field.value ? 'true' : 'false'}
onChange={(e) => onFieldChange(field.id, e.target.value === 'true')}
```

### Metadata Support

Fields can include metadata in `field.meta`:

```typescript
// Help text
description={field.meta?.helpText}

// Empty option for selects
emptyOption={field.meta?.emptyOption}

// Custom mode for CKEditor
mode={field.meta?.mode}
```

### Read-Only Handling

Fields can be read-only even in edit mode:

```typescript
const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;
const isDisabled = isEditing && field.readOnly;

<InputComponent
  disabled={isDisabled}
  mode={isEditing && !field.readOnly ? FormMode.EDIT : FormMode.VIEW}
/>
```

## Best Practices

1. **Mode Awareness**: Always check form mode and read-only status
2. **Value Handling**: Properly convert between field value format and component format
3. **Metadata**: Support common metadata fields (`helpText`, `emptyOption`, etc.)
4. **Accessibility**: Ensure keyboard navigation and screen reader support
5. **Type Safety**: Use TypeScript interfaces and proper typing
6. **Error Handling**: Handle cases where field data might be incomplete
7. **Consistency**: Follow existing patterns for mode handling and styling
8. **Performance**: Avoid unnecessary re-renders (use React.memo if needed)

## Field Definition Structure

Fields follow this structure:

```typescript
interface FormField {
  id: string;                    // Unique field identifier
  label: string;                  // Display label
  value: any;                     // Current field value
  dataType: FormFieldDataType;    // Data type (determines renderer)
  required: boolean;              // Whether field is mandatory
  readOnly: boolean;              // Whether field is read-only
  meta?: {                        // Optional metadata
    helpText?: string;
    emptyOption?: string;
    mode?: string;
    isMarkdown?: boolean;
    [key: string]: any;
  };
}
```

## Related Files

- `ComponentRegistry.ts` - Registration and retrieval of field renderers
- `form.types.ts` - Type definitions (`FieldRendererProps`, `FormField`, `FormFieldDataType`)
- `form.enums.ts` - Enum definitions (`FormFieldDataType`, `FormMode`)
- `EntityForm.tsx` - Usage of field renderers in forms

