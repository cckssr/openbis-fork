# Type Definitions

This folder contains all TypeScript type definitions, interfaces, and enums used throughout the form system. These types provide type safety, documentation, and ensure consistency across the codebase.

## Table of Contents

1. [Overview](#overview)
2. [File Structure](#file-structure)
3. [Core Types](#core-types)
4. [Enums](#enums)
5. [Interfaces](#interfaces)
6. [Type Relationships](#type-relationships)
7. [Usage Examples](#usage-examples)

## Overview

The type system is organized into four main files:

- **`form.enums.ts`** - Enumeration types for form modes, entity kinds, data types, etc.
- **`form.types.ts`** - Core form data structures and component props
- **`IFormController.ts`** - Interface for entity-specific form controllers
- **`FormState.ts`** - State management types and context definitions

## File Structure

### form.enums.ts

Contains all enumeration types used throughout the form system.

**Enums Defined**:
- `FormMode` - Form display/edit modes
- `EntityKind` - Types of entities in the system
- `FormFieldDataType` - Data types for form fields
- `Widget` - Special widget types
- `FormSection` - Form section names

### form.types.ts

Contains core data structures and component prop types.

**Main Types**:
- `Form` - Complete form definition
- `FormField` - Individual field definition
- `FormAction` - Action button/control definition
- `FieldRendererProps` - Props for field renderer components
- `ActionRendererProps` - Props for action renderer components
- Action context interfaces

### IFormController.ts

Defines the contract for entity-specific form controllers.

**Interface**: `IFormController` - Methods that all form controllers must implement

### FormState.ts

Contains types for form state management and React context.

**Main Types**:
- `FormState` - Form state structure
- `FormActions` - Available form actions
- `FormPermissions` - Permission flags
- `FormContextValue` - React context value type
- `Conflict` - Conflict resolution data structure

## Core Types

### Form

The central data structure representing a complete form.

```typescript
interface Form {
  entityPermId: string;      // Unique entity identifier
  entityKind: string;         // Entity type (e.g., 'Space', 'Project')
  entityType: string;         // Entity type code
  title: string;              // Form title
  sections: SectionGroup[];   // Form sections
  fields: FormField[];        // All form fields
  version: number;            // Version for optimistic locking
  mode?: FormMode;            // Current form mode
  isDirty: boolean;           // Whether form has unsaved changes
  isValid: boolean;           // Whether form passes validation
  meta: { [key: string]: any }; // Entity-specific metadata
  actions?: FormAction[];     // Available actions
}
```

**Key Properties**:
- `entityPermId`: Used to identify and load the entity
- `version`: Critical for conflict detection (optimistic locking)
- `isDirty`: Tracks if form has been modified
- `isValid`: Computed based on field validation rules
- `fields`: Array of all fields in the form
- `sections`: Groups fields into logical sections

### FormField

Represents a single field in the form.

```typescript
interface FormField<T = any> {
  id: string;                          // Unique field identifier
  name?: string;                       // Field name
  label: string;                       // Display label
  value: T;                            // Current field value
  initialValue?: any;                  // Original value (for reset)
  dataType: FormFieldDataType;         // Field data type
  required: boolean;                   // Whether field is mandatory
  readOnly: boolean;                   // Whether field is read-only
  isMultiValue: boolean;               // Whether field supports multiple values
  section: FormSection;                // Section this field belongs to
  column?: 'left' | 'right' | 'center'; // Column placement
  meta: FormFieldMeta;                 // Field metadata
  options?: { label: string; value: string }[]; // Options for select fields
  validation?: ValidationRuleDef[];    // Validation rules
}
```

**Key Properties**:
- `id`: Must be unique within the form
- `dataType`: Determines which field renderer to use
- `meta`: Extensible metadata object for field-specific data
- `value`: Can be any type (string, number, boolean, object, etc.)
- `readOnly`: Field cannot be edited even in edit mode
- `required`: Field must have a value for form to be valid

### FormFieldMeta

Extensible metadata object for field-specific configuration.

```typescript
interface FormFieldMeta {
  isFrozen?: boolean;
  vocabularyOptions?: { code: string; label: string }[];
  helpText?: string;
  emptyOption?: string;
  mode?: string;
  isMarkdown?: boolean;
  [key: string]: any; // For future extensions
}
```

**Common Metadata Properties**:
- `helpText`: Tooltip or help text for the field
- `emptyOption`: Default option text for select fields
- `vocabularyOptions`: Options for controlled vocabulary fields
- `mode`: Editor mode (for CKEditor fields)
- `isMarkdown`: Whether content is in markdown format

### FormAction

Defines an action button or control in the form toolbar.

```typescript
interface FormAction {
  name: string;                        // Unique action identifier
  label: string;                       // Display label
  component: 'button' | 'switch' | 'dropdown' | string; // Renderer type
  handler?: (...args: any[]) => void;  // Optional custom handler
  isAllowed: boolean;                  // Whether action is permitted
  visibility: VisibilityRule[];        // Visibility rules
  value?: any;                         // Current value (for switches)
}
```

**Action Naming Convention**: Use format `'entity:action'` (e.g., `'project:save'`, `'space:new-project'`)

**Component Types**:
- `'button'`: Standard button (uses `ButtonActionRenderer`)
- `'switch'`: Toggle switch (uses `SwitchActionRenderer`)
- `'dropdown'`: Dropdown menu (future implementation)

## Enums

### FormMode

Defines the current mode of the form.

```typescript
enum FormMode {
  VIEW = 'view',      // Read-only display mode
  CREATE = 'create',  // Creating new entity
  EDIT = 'edit'       // Editing existing entity
}
```

**Usage**:
- Controls field editability
- Determines which actions are available
- Affects UI rendering (toolbars, buttons, etc.)

### EntityKind

Types of entities in the openBIS system.

```typescript
enum EntityKind {
  SPACE = 'space',
  PROJECT = 'project',
  EXPERIMENT = 'experiment',
  OBJECT = 'object',
  SAMPLE = 'sample',
  COLLECTION = 'collection',
  DATASET = 'dataSet',
  NEW_PROJECT = 'newProject',
  NEW_OBJECT = 'newObject',
  NEW_COLLECTION = 'newCollection',
  NEW_DATASET = 'newDataSet',
}
```

**Usage**:
- Determines which controller to use
- Used in `ControllerDispatcher` to create controllers
- Identifies entity type in forms

### FormFieldDataType

Data types for form fields, determines which renderer to use.

```typescript
enum FormFieldDataType {
  VARCHAR = 'VARCHAR',                           // Text field
  MULTILINE_VARCHAR = 'MULTILINE_VARCHAR',       // Textarea
  INTEGER = 'INTEGER',                           // Number field
  REAL = 'REAL',                                 // Decimal number
  TIMESTAMP = 'TIMESTAMP',                       // Date picker
  BOOLEAN = 'BOLEAN',                            // Switch/checkbox
  CONTROLLED_VOCABULARY = 'CONTROLLED_VOCABULARY', // Dropdown
  HYPERLINK = 'HYPERLINK',                       // URL field
  SAMPLE = 'SAMPLE',                             // Sample reference
  WORD_PROCESSOR = 'WORD_PROCESSOR',              // Rich text editor
  WORD_PROCESSOR_PAGE = 'WORD_PROCESSOR_PAGE',    // Document editor
  SPREADSHEET = 'SPREADSHEET',                    // Spreadsheet widget
}
```

**Renderer Mapping** (in `ComponentRegistry`):
- `VARCHAR` → `TextFieldRenderer`
- `MULTILINE_VARCHAR` → `TextAreaFieldRenderer`
- `TIMESTAMP` → `DateFieldRenderer`
- `BOOLEAN` → `SwitchFieldRenderer`
- `CONTROLLED_VOCABULARY` → `SelectFieldRenderer`
- `WORD_PROCESSOR` → `CKEditorFieldRenderer`

### FormSection

Predefined form section names.

```typescript
enum FormSection {
  SELECT_TYPE = 'Select Type',
  IDENTIFICATION_INFO = 'Identification Info',
  GENERAL = 'General',
  OVERVIEW = 'Overview'
}
```

**Usage**: Groups fields into logical sections in the form UI

## Interfaces

### IFormController

Contract that all entity-specific form controllers must implement.

```typescript
interface IFormController {
  load(entityPermId: string, entityKind?: string, params?: any): Promise<Form>;
  save(form: Form, mode?: FormMode): Promise<any>;
  checkPermissions(form: Form): Promise<Record<'canEdit' | 'canDelete' | 'canMove', boolean>>;
  delete(form: Form, context?: any): Promise<void>;
  getDependentEntities(form: Form): Promise<any>;
  move(form: Form, context?: any, params?: any): Promise<void>;
}
```

**Key Methods**:
- `load()`: Fetches entity data and converts to Form structure
- `save()`: Saves form data back to openBIS
- `checkPermissions()`: Determines what actions user can perform
- `delete()`: Deletes the entity
- `getDependentEntities()`: Finds entities that depend on this one
- `move()`: Moves entity to different parent

**Implementation**: Each entity type (Space, Project, etc.) has its own controller implementing this interface.

### FieldRendererProps

Props passed to field renderer components.

```typescript
interface FieldRendererProps {
  field: FormField;
  onFieldChange: (fieldId: string, value: any) => void;
  onFieldMetadataChange?: (fieldId: string, meta: any) => void;
  mode: FormMode;
  params?: any;
}
```

**Usage**: All field renderers must accept these props.

### ActionRendererProps

Props passed to action renderer components.

```typescript
interface ActionRendererProps {
  action: FormAction;
  onAction: (actionName: string) => void;
  mode: FormMode;
}
```

**Usage**: All action renderers must accept these props.

### Action Context Interfaces

Hierarchy of context interfaces for action handlers:

```typescript
// Base context
interface IBaseActionContext {
  controller: IFormController;
  form: Form;
  setForm: React.Dispatch<React.SetStateAction<Form | null>>;
}

// Mode-aware context
interface IModeActionContext extends IBaseActionContext {
  mode: FormMode;
  setMode: React.Dispatch<React.SetStateAction<FormMode>>;
}

// Extended context with external dependencies
interface IExtendedActionContext extends IModeActionContext {
  externalAppController: any;
  onAfterSave: (params?: any) => void;
  deleteReason?: string;
  dependentEntities?: any;
}

// Auto-save context
interface IAutoSaveActionContext extends IBaseActionContext {
  isAutoSaveEnabled: boolean;
  setAutoSaveEnabled: (isAutoSaveEnabled: boolean) => void;
}
```

**Usage**: Action handlers receive appropriate context based on their needs.

## Type Relationships

### Form Structure

```
Form
├── entityPermId: string
├── entityKind: EntityKind
├── fields: FormField[]
│   ├── id: string
│   ├── dataType: FormFieldDataType
│   ├── value: any
│   └── meta: FormFieldMeta
├── sections: SectionGroup[]
│   └── fields: string[] (field IDs)
└── actions?: FormAction[]
    ├── name: string
    └── component: string
```

### Controller Flow

```
IFormController
  ↓
load() → Form
  ↓
Form → fields[] → FormField
  ↓
FormField.dataType → ComponentRegistry → FieldRenderer
  ↓
FieldRenderer receives FieldRendererProps
```

### Action Flow

```
Form.actions[] → FormAction
  ↓
FormAction.component → ComponentRegistry → ActionRenderer
  ↓
ActionRenderer receives ActionRendererProps
  ↓
onAction() → ActionHandlerDispatcher → Action Handler
  ↓
Action Handler receives IExtendedActionContext
```

## Usage Examples

### Creating a Form

```typescript
const form: Form = {
  entityPermId: 'PROJECT-123',
  entityKind: EntityKind.PROJECT,
  entityType: 'MY_PROJECT_TYPE',
  title: 'My Project',
  version: 1,
  isDirty: false,
  isValid: true,
  sections: [
    { section: FormSection.GENERAL, fields: ['name', 'description'] }
  ],
  fields: [
    {
      id: 'name',
      label: 'Project Name',
      value: 'My Project',
      dataType: FormFieldDataType.VARCHAR,
      required: true,
      readOnly: false,
      isMultiValue: false,
      section: FormSection.GENERAL,
      meta: {}
    }
  ],
  meta: {},
  actions: [
    {
      name: 'project:save',
      label: 'Save',
      component: 'button',
      isAllowed: true,
      visibility: [{ mode: [FormMode.EDIT, FormMode.CREATE] }]
    }
  ]
};
```

### Using Field Renderer Props

```typescript
const TextFieldRenderer: React.FC<FieldRendererProps> = ({ 
  field, 
  onFieldChange, 
  mode 
}) => {
  const isEditing = mode === FormMode.EDIT || mode === FormMode.CREATE;
  
  return (
    <TextField
      value={field.value}
      onChange={(e) => onFieldChange(field.id, e.target.value)}
      disabled={isEditing && field.readOnly}
    />
  );
};
```

### Implementing IFormController

```typescript
class ProjectFormController implements IFormController {
  async load(permId: string): Promise<Form> {
    // Fetch project from openBIS
    const project = await openbisFacade.getProject(permId);
    
    // Convert to Form structure
    return {
      entityPermId: permId,
      entityKind: EntityKind.PROJECT,
      fields: [
        {
          id: 'code',
          label: 'Code',
          value: project.code,
          dataType: FormFieldDataType.VARCHAR,
          required: true,
          readOnly: false,
          isMultiValue: false,
          section: FormSection.GENERAL,
          meta: {}
        }
      ],
      // ... rest of form
    };
  }
  
  async save(form: Form): Promise<any> {
    // Convert Form back to openBIS format and save
    // ...
  }
  
  // ... implement other methods
}
```

### Action Handler with Context

```typescript
const saveAction = async (context: IExtendedActionContext) => {
  const { form, controller, setForm, onAfterSave } = context;
  
  try {
    const result = await controller.save(form);
    setForm({ ...form, version: result.version, isDirty: false });
    onAfterSave?.(result);
  } catch (error) {
    // Handle error
  }
};
```

## Best Practices

1. **Type Safety**: Always use the provided types instead of `any`
2. **Field IDs**: Use consistent naming conventions (e.g., `'project:code'`)
3. **Action Names**: Use format `'entity:action'` (e.g., `'project:save'`)
4. **Metadata**: Use `FormFieldMeta` for field-specific configuration
5. **Version Tracking**: Always update form version after save
6. **Validation**: Define validation rules in `FormField.validation`
7. **Read-Only Fields**: Set `readOnly: true` for fields that shouldn't be edited
8. **Required Fields**: Mark mandatory fields with `required: true`

## Extending Types

### Adding a New Field Data Type

1. Add to `FormFieldDataType` enum:
```typescript
export enum FormFieldDataType {
  // ... existing types
  NEW_TYPE = 'NEW_TYPE',
}
```

2. Create field renderer component
3. Register in `ComponentRegistry.getFieldRenderer()`

### Adding a New Entity Kind

1. Add to `EntityKind` enum:
```typescript
export enum EntityKind {
  // ... existing kinds
  NEW_ENTITY = 'newEntity',
}
```

2. Create form controller implementing `IFormController`
3. Register in `ControllerDispatcher.createController()`

### Extending FormFieldMeta

The `FormFieldMeta` interface uses an index signature, so you can add properties without modifying the type:

```typescript
const field: FormField = {
  // ... other properties
  meta: {
    customProperty: 'value',
    anotherProperty: 123
  }
};
```

## Related Files

- `ComponentRegistry.ts` - Uses `FormFieldDataType` to select renderers
- `ControllerDispatcher.ts` - Uses `EntityKind` to select controllers
- `ActionHandlerDispatcher.ts` - Uses action names from `FormAction`
- Field renderers - Use `FieldRendererProps`
- Action renderers - Use `ActionRendererProps`
- Form controllers - Implement `IFormController`

