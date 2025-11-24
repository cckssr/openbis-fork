# Entities Folder Structure

This folder contains entity-specific implementations following a consistent pattern. Each entity type (Space, Project, Collection, Object, Dataset) has its own subfolder with controller and model files that handle entity-specific business logic and data transformation.

## Table of Contents

1. [Overview](#overview)
2. [Folder Structure](#folder-structure)
3. [Common Pattern](#common-pattern)
4. [File Responsibilities](#file-responsibilities)
5. [Implementation Guide](#implementation-guide)
6. [Field Utilities](#field-utilities)
7. [Best Practices](#best-practices)

## Overview

The entities folder follows a **separation of concerns** pattern:

- **Controllers** (`{Entity}FormController.ts`) - Handle business logic and openBIS API interactions
- **Models** (`{Entity}FormModel.ts`) - Handle data transformation (DTO ↔ Form) and action handlers
- **Utilities** (`formField.formFieldUtil.ts`) - Shared field creation functions

This pattern ensures:
- **Consistency**: All entities follow the same structure
- **Maintainability**: Clear separation of concerns
- **Reusability**: Shared utilities reduce code duplication
- **Type Safety**: TypeScript interfaces ensure contract compliance

## Folder Structure

```
entities/
├── README.md                    # This file
├── formField.formFieldUtil.ts          # Shared field creation utilities
├── Project/
│   ├── ProjectFormController.ts
│   └── ProjectFormModel.ts
├── Space/
│   ├── SpaceFormController.ts
│   └── SpaceFormModel.ts
├── Collection/
│   ├── CollectionFormController.ts
│   └── CollectionFormModel.ts
├── Object/
│   ├── ObjectFormController.ts
│   └── ObjectFormModel.ts
└── Dataset/
    ├── DatasetFormController.ts
    └── DatasetAdapter.ts        # Some entities may have additional files
```

## Common Pattern

### Standard Entity Structure

Every entity folder should contain at minimum:

1. **`{Entity}FormController.ts`** - Implements `IFormController`
2. **`{Entity}FormModel.ts`** - Contains adaptation methods and action handlers

### Controller Pattern

All controllers follow this structure:

```typescript
export class {Entity}FormController implements IFormController {
  private openbisFacade: any;

  constructor(openbisFacade: any) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
  }

  async load(permId: string, entityKind?: string, params?: any): Promise<Form> {
    // 1. Create appropriate ID object (e.g., ProjectPermId)
    // 2. Set up fetch options
    // 3. Fetch entity from openBIS
    // 4. Call Model.adapt{Dto}ToForm() to convert
    // 5. Return Form object
  }

  async save(form: Form, mode: FormMode): Promise<any> {
    // Route to create or update based on mode
    if (mode === FormMode.CREATE) {
      return this._create{Entity}(form);
    } else if (mode === FormMode.EDIT) {
      return this._update{Entity}(form);
    }
  }

  async checkPermissions(form: Form): Promise<Record<'canEdit' | 'canDelete' | 'canMove', boolean>> {
    // Check user permissions using fetchRights utility
  }

  async delete(form: Form, context?: any): Promise<void> {
    // Delete entity and dependent entities if needed
  }

  async getDependentEntities(form: Form): Promise<any> {
    // Return entities that depend on this one
  }

  async move(form: Form, context?: any, params?: any): Promise<void> {
    // Move entity to different parent location
  }

  // Private helper methods
  private async _create{Entity}(form: Form): Promise<any> { }
  private async _update{Entity}(form: Form): Promise<any> { }
}
```

### Model Pattern

All models follow this structure:

```typescript
export class {Entity}FormModel {
  // Adaptation method: DTO → Form
  static adapt{Entity}DtoToForm(dto: any): Form {
    const permId = dto.permId.permId;
    return {
      entityPermId: permId,
      entityType: dto.type?.code || EntityKind.{ENTITY},
      title: `{Entity}: ${dto.code}`,
      version: dto.version || 1,
      entityKind: EntityKind.{ENTITY},
      meta: {},
      sections: [
        {
          section: FormSection.IDENTIFICATION_INFO,
          fields: [/* field IDs */]
        },
        {
          section: FormSection.GENERAL,
          fields: [/* field IDs */]
        }
      ],
      fields: [
        // Use utility functions from formField.formFieldUtil.ts
        getCodeField(dto),
        getDescriptionField(dto),
        // ... other fields
      ],
      isDirty: false,
      isValid: true,
      actions: [
        {
          name: '{entity}:save',
          label: 'Save',
          component: 'button',
          isAllowed: true,
          visibility: [{ mode: FormMode.EDIT }]
        },
        // ... other actions
      ]
    };
  }

  // Adaptation method for new entities (CREATE mode)
  static adaptNew{Entity}DtoToForm(tmpPermId: string, params: any): Form {
    // Similar structure but for new entities
    // Fields may have empty values and readOnly: false
  }

  // Action handler
  static save{Entity}Action = async (context: IExtendedActionContext) => {
    const { form, controller, onAfterSave, mode } = context;
    const result = await controller.save(form, mode);
    onAfterSave();
  };
}
```

## File Responsibilities

### Controller File (`{Entity}FormController.ts`)

**Responsibilities**:
- ✅ Implement `IFormController` interface
- ✅ Handle all openBIS API interactions
- ✅ Manage entity lifecycle (create, read, update, delete)
- ✅ Check permissions
- ✅ Handle dependent entities
- ✅ Implement move operations
- ✅ Handle entity-specific business logic

**What it should NOT do**:
- ❌ Transform DTOs to Forms (delegate to Model)
- ❌ Define action handlers (delegate to Model)
- ❌ Create field definitions (use utilities)

### Model File (`{Entity}FormModel.ts`)

**Responsibilities**:
- ✅ Transform openBIS DTOs to Form objects
- ✅ Transform Forms back to openBIS format (if needed)
- ✅ Define action handlers (save, delete, etc.)
- ✅ Define form structure (sections, fields, actions)
- ✅ Handle entity-specific form adaptations

**What it should NOT do**:
- ❌ Make API calls (delegate to Controller)
- ❌ Handle business logic (delegate to Controller)
- ❌ Create field objects directly (use utilities)

### Field Utilities (`formField.formFieldUtil.ts`)

**Responsibilities**:
- ✅ Provide reusable field creation functions
- ✅ Standardize field definitions across entities
- ✅ Handle common field patterns (code, description, dates, etc.)

**Available Utilities**:
- `getCodeField()` - Code/identifier field
- `getDescriptionField()` - Description field (rich text)
- `getPermIdField()` - Permanent ID field
- `getIdentifierField()` - Full identifier field
- `getPathField()` - Path field
- `getSpaceField()` - Space reference field
- `getProjectField()` - Project reference field
- `getRegistratorField()` - Registrator user field
- `getRegistrationDateField()` - Registration date field
- `getModifierField()` - Modifier user field
- `getModificationDateField()` - Modification date field
- `getTypeField()` - Entity type field

## Implementation Guide

### Step 1: Create Entity Folder

```bash
mkdir entities/NewEntity
```

### Step 2: Create Controller

```typescript
// entities/NewEntity/NewEntityFormController.ts
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { FormMode, EntityKind } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { NewEntityFormModel } from './NewEntityFormModel.ts';

export class NewEntityFormController implements IFormController {
  private openbisFacade: any;

  constructor(openbisFacade: any) {
    if (!openbisFacade) throw new Error('openbisFacade is required');
    this.openbisFacade = openbisFacade;
  }

  async load(permId: string, entityKind?: string, params?: any): Promise<Form> {
    // Handle new entity creation
    if (entityKind === EntityKind.NEW_ENTITY) {
      return NewEntityFormModel.adaptNewEntityDtoToForm(permId, params);
    }

    // Load existing entity
    const { NewEntityPermId, NewEntityFetchOptions } = this.openbisFacade;
    const id = new NewEntityPermId(permId);
    const fetchOptions = new NewEntityFetchOptions();
    // Configure fetch options as needed
    
    const result = await this.openbisFacade.getNewEntities([id], fetchOptions);
    const dto = result[permId];
    
    if (!dto) throw new Error(`NewEntity with permId ${permId} not found`);
    return NewEntityFormModel.adaptNewEntityDtoToForm(dto);
  }

  async save(form: Form, mode: FormMode): Promise<any> {
    if (mode === FormMode.CREATE) {
      return this._createNewEntity(form);
    } else if (mode === FormMode.EDIT) {
      return this._updateNewEntity(form);
    } else {
      throw new Error(`Invalid form mode: ${mode}`);
    }
  }

  async checkPermissions(form: Form) {
    // Implement permission checking
    return { canEdit: true, canDelete: true, canMove: false };
  }

  async delete(form: Form, context?: any): Promise<void> {
    // Implement delete logic
  }

  async getDependentEntities(form: Form): Promise<any> {
    // Return dependent entities
    return { entities: [] };
  }

  async move(form: Form, context?: any, params?: any): Promise<void> {
    // Implement move logic if applicable
  }

  private async _createNewEntity(form: Form): Promise<any> {
    // Create entity in openBIS
  }

  private async _updateNewEntity(form: Form): Promise<any> {
    // Update entity in openBIS
  }
}
```

### Step 3: Create Model

```typescript
// entities/NewEntity/NewEntityFormModel.ts
import { Form, IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts.ts';
import { EntityKind, FormSection, FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import {
  getCodeField,
  getDescriptionField,
  getPermIdField,
  // ... other utilities
} from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';

export class NewEntityFormModel {
  static adaptNewEntityDtoToForm(dto: any): Form {
    const permId = dto.permId.permId;
    return {
      entityPermId: permId,
      entityType: dto.type?.code || EntityKind.NEW_ENTITY,
      title: `NewEntity: ${dto.code}`,
      version: dto.version || 1,
      entityKind: EntityKind.NEW_ENTITY,
      meta: {},
      sections: [
        {
          section: FormSection.IDENTIFICATION_INFO,
          fields: [
            permId + '-permId',
            permId + '-code',
          ]
        },
        {
          section: FormSection.GENERAL,
          fields: [
            permId + '-description',
          ]
        }
      ],
      fields: [
        getPermIdField(dto),
        getCodeField(dto),
        getDescriptionField(dto, { column: 'center' }),
      ],
      isDirty: false,
      isValid: true,
      actions: [
        {
          name: 'new-entity:save',
          label: 'Save',
          component: 'button',
          isAllowed: true,
          visibility: [{ mode: FormMode.EDIT }]
        },
        {
          name: 'edit',
          label: 'Edit',
          component: 'button',
          isAllowed: true,
          visibility: [{ mode: FormMode.VIEW }]
        },
        {
          name: 'cancel',
          label: 'Cancel',
          component: 'button',
          isAllowed: true,
          visibility: [{ mode: FormMode.EDIT }]
        }
      ]
    };
  }

  static adaptNewNewEntityDtoToForm(tmpPermId: string, params: any): Form {
    const permId = tmpPermId + '-' + EntityKind.NEW_ENTITY;
    return {
      entityPermId: tmpPermId,
      entityType: EntityKind.NEW_ENTITY,
      title: `New NewEntity`,
      version: 1,
      entityKind: EntityKind.NEW_ENTITY,
      meta: { parentId: params.parentId },
      sections: [
        {
          section: FormSection.IDENTIFICATION_INFO,
          fields: [permId + '-code']
        },
        {
          section: FormSection.GENERAL,
          fields: [permId + '-description']
        }
      ],
      fields: [
        getCodeField(
          { permId: { permId } },
          { readOnly: false, value: '', id: permId + '-code' }
        ),
        getDescriptionField(
          { permId: { permId } },
          { column: 'center', value: '', id: permId + '-description' }
        ),
      ],
      isDirty: false,
      isValid: true,
      actions: [
        {
          name: 'new-entity:save',
          label: 'Save',
          component: 'button',
          isAllowed: true,
          visibility: [{ mode: FormMode.CREATE }]
        },
        {
          name: 'new-form:cancel',
          label: 'Cancel',
          component: 'button',
          isAllowed: true,
          visibility: [{ mode: FormMode.CREATE }]
        }
      ]
    };
  }

  static saveNewEntityAction = async (context: IExtendedActionContext) => {
    const { form, controller, onAfterSave, mode } = context;
    const result = await controller.save(form, mode);
    console.log("NewEntity saved successfully!", result);
    if (mode === FormMode.CREATE) {
      onAfterSave({
        oldType: EntityKind.NEW_ENTITY,
        oldId: form.entityPermId,
        newType: EntityKind.NEW_ENTITY,
        newId: result
      });
    } else {
      onAfterSave();
    }
  };
}
```

### Step 4: Register in Dispatchers

**Register Controller** in `ControllerDispatcher.ts`:

```typescript
import { NewEntityFormController } from '@src/js/components/database/new-forms/entities/NewEntity/NewEntityFormController.ts';

static createController(entityKind: string, openbisFacade: any, user?: string) {
  switch (entityKind) {
    // ... existing cases
    case EntityKind.NEW_ENTITY:
      return new NewEntityFormController(openbisFacade);
    default:
      throw new Error(`Unknown entity kind: ${entityKind}`);
  }
}
```

**Register Action Handler** in `ActionHandlerDispatcher.ts`:

```typescript
import { NewEntityFormModel } from '@src/js/components/database/new-forms/entities/NewEntity/NewEntityFormModel.ts';

static getActionHandler(actionName: string) {
  switch (actionName) {
    // ... existing cases
    case 'new-entity:save':
      return NewEntityFormModel.saveNewEntityAction;
    default:
      console.warn(`Unknown action: ${actionName}`);
      return CoreFormModel.unknownAction;
  }
}
```

**Add Entity Kind** in `form.enums.ts`:

```typescript
export enum EntityKind {
  // ... existing kinds
  NEW_ENTITY = 'newEntity',
}
```

## Field Utilities

### Using Field Utilities

Field utilities provide a consistent way to create form fields:

```typescript
import {
  getCodeField,
  getDescriptionField,
  getPermIdField
} from '@src/js/components/database/new-forms/entities/formFieldGetters.ts';

// Basic usage
const codeField = getCodeField(dto);

// With overrides
const codeField = getCodeField(dto, {
  value: 'custom-value',
  readOnly: false,
  column: 'right'
});
```

### Field Override Pattern

All field utilities accept an optional `overrides` parameter:

```typescript
type FieldOverrides<T = any> = Partial<Omit<FormField<T>, 'value'>> & { value?: T };
```

This allows you to override any field property while maintaining type safety.

### Creating Custom Field Utilities

If you need a field that doesn't exist in the utilities:

```typescript
// In formField.formFieldUtil.ts
export function getCustomField(dto: any, overrides: FieldOverrides = {}): FormField<string> {
  const permId = dto.permId.permId;
  return {
    id: permId + '-custom',
    label: 'Custom Field',
    value: overrides.value ?? dto.customProperty,
    dataType: FormFieldDataType.VARCHAR,
    required: false,
    readOnly: false,
    isMultiValue: false,
    section: FormSection.GENERAL,
    column: 'left',
    meta: {},
    ...overrides
  };
}
```

## Best Practices

### 1. Controller Best Practices

- **Always validate openbisFacade**: Check in constructor
- **Handle both CREATE and EDIT modes**: Route in `save()` method
- **Use private methods**: `_create{Entity}()`, `_update{Entity}()` for clarity
- **Error handling**: Throw descriptive errors
- **Logging**: Use console.log for debugging (remove in production)
- **Fetch options**: Configure appropriately for each entity type
- **Dependent entities**: Always check before delete

### 2. Model Best Practices

- **Static methods**: All adaptation methods should be static
- **Use utilities**: Always use field utilities, don't create fields manually
- **Consistent structure**: Follow the same section/field organization
- **Action naming**: Use format `'{entity}:{action}'`
- **Action visibility**: Set appropriate visibility rules
- **Version tracking**: Always include version in Form

### 3. Field ID Naming

Use consistent field ID patterns:

```typescript
// Pattern: {permId}-{fieldName}
permId + '-code'
permId + '-description'
permId + '-permId'
permId + '-identifier'
```

### 4. Action Naming Convention

```typescript
// Entity-specific actions
'{entity}:save'      // e.g., 'project:save'
'{entity}:new-child' // e.g., 'space:new-project'

// Core actions (handled by CoreFormModel)
'edit'
'cancel'
'delete'
'move'
'new-form:cancel'
```

### 5. Section Organization

Organize fields into logical sections:

- **`FormSection.IDENTIFICATION_INFO`**: IDs, codes, identifiers, dates
- **`FormSection.GENERAL`**: Description, properties, content
- **`FormSection.SELECT_TYPE`**: Type selection (for new entities)
- **`FormSection.OVERVIEW`**: Summary information

### 6. Column Layout

Use column property for field layout:

```typescript
column: 'left'   // Left column
column: 'right'  // Right column
column: 'center' // Full width (for descriptions, documents)
```

### 7. Error Handling

```typescript
// In Controller
if (!dto) throw new Error(`Entity with permId ${permId} not found`);

// In Model
if (!dto.permId) throw new Error('DTO missing permId');
```

### 8. Type Safety

- Use TypeScript types from `form.types.ts`
- Use enums from `form.enums.ts`
- Avoid `any` types when possible
- Use proper typing for DTOs

## Common Patterns

### Pattern 1: New Entity Creation

```typescript
// In Controller.load()
if (entityKind === EntityKind.NEW_ENTITY) {
  return Model.adaptNewEntityDtoToForm(permId, params);
}

// In Model
static adaptNewEntityDtoToForm(tmpPermId: string, params: any): Form {
  // Create form with empty/default values
  // Fields have readOnly: false
  // Actions include 'new-form:cancel'
}
```

### Pattern 2: Dependent Entity Deletion

```typescript
async delete(form: Form, context?: any): Promise<void> {
  // Get dependent entities
  const dependentEntities = await this.getDependentEntities(form);
  
  // Delete dependents first
  if (dependentEntities.length > 0) {
    await this.deleteDependentEntities(context.deleteReason, dependentEntities);
  }
  
  // Delete main entity
  await this.deleteEntity(form);
}
```

### Pattern 3: Permission Checking

```typescript
async checkPermissions(form: Form) {
  const { EntityPermId, DummyIdentifier } = this.openbisFacade;
  const entityId = new EntityPermId(form.entityPermId);
  const dummyId = new DummyIdentifier(createDummyIdentifier(...));
  const ids = [entityId, dummyId];
  
  const { editable, deletable } = await fetchRights(this.openbisFacade, form.entityPermId, ids);
  return { canEdit: editable, canDelete: deletable, canMove: true };
}
```

### Pattern 4: Move Operation

```typescript
async move(form: Form, context?: any, params?: any): Promise<void> {
  const { EntityUpdate, TargetPermId } = this.openbisFacade;
  const update = new EntityUpdate();
  update.setEntityId(new EntityPermId(form.entityPermId));
  update.setTargetId(params.target.getPermId());
  await this.openbisFacade.updateEntities([update]);
}
```

## Troubleshooting

### Common Issues

1. **Field not appearing**: Check field ID matches in sections array
2. **Action not working**: Verify registration in ActionHandlerDispatcher
3. **Controller not found**: Check registration in ControllerDispatcher
4. **Type errors**: Ensure EntityKind enum includes new entity
5. **Save not working**: Check mode (CREATE vs EDIT) routing

### Debug Checklist

- [ ] Controller implements IFormController
- [ ] Model has static adaptation methods
- [ ] Field IDs match between sections and fields arrays
- [ ] Actions registered in ActionHandlerDispatcher
- [ ] Controller registered in ControllerDispatcher
- [ ] EntityKind added to enum
- [ ] Field utilities used correctly
- [ ] Error handling in place

## Related Files

- `types/IFormController.ts` - Controller interface
- `types/formITypes.ts.ts` - Form and field type definitions
- `types/form.enums.ts` - EntityKind and other enums
- `engine/ControllerDispatcher.ts` - Controller registration
- `engine/ActionHandlerDispatcher.ts` - Action handler registration
- `formField.formFieldUtil.ts` - Field creation utilities

