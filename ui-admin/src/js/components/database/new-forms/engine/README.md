# Form Engine Architecture

The form engine uses a dispatcher/registry pattern to manage form controllers, action handlers, and component renderers. This approach provides a clean separation of concerns and makes it easy to extend the system with new entities, actions, and field types.

## Architecture Overview

The engine consists of four main components:

1. **ControllerDispatcher** - Creates form controllers based on entity kind
2. **ActionHandlerDispatcher** - Retrieves action handlers by action name
3. **ComponentRegistry** - Provides field and action renderers based on data types
4. **CoreFormModel** - Contains shared action handlers used across all forms

## Components

### ControllerDispatcher

Creates the appropriate form controller for a given entity kind. Controllers handle entity-specific business logic and data operations.

**Supported Entity Kinds**:
- `EntityKind.SPACE` → `SpaceFormController`
- `EntityKind.PROJECT` / `EntityKind.NEW_PROJECT` → `ProjectFormController`
- `EntityKind.COLLECTION` → `CollectionFormController`
- `EntityKind.DATASET` → `DatasetFormController`
- `EntityKind.OBJECT` / `EntityKind.NEW_OBJECT` → `ObjectFormController`

### ActionHandlerDispatcher

Retrieves action handlers by action name. Actions can be core (shared across all forms) or entity-specific.

**Location**: `ActionHandlerDispatcher.ts`

**Usage**:
```typescript
import ActionHandlerDispatcher from '@src/js/components/database/new-forms/engine/ActionHandlerDispatcher.ts';

const actionHandler = ActionHandlerDispatcher.getActionHandler('project:save');
await actionHandler(context);
```

**Core Actions** (from `CoreFormModel`):
- `'edit'` - Switch form to edit mode
- `'cancel'` - Cancel edit and return to view mode
- `'new-form:cancel'` - Close new form dialog
- `'delete'` - Delete the entity
- `'move'` - Move entity to different location

**Entity-Specific Actions**:
- `'space:save'` - Save space
- `'space:new-project'` - Create new project in space
- `'space:new-object'` - Create new object in space
- `'project:save'` - Save project
- `'object:save'` - Save object
- `'collection:save'` - Save collection

### ComponentRegistry

Provides React components for rendering form fields and actions based on data types and component types.

**Location**: `ComponentRegistry.ts`

**Usage**:
```typescript
import ComponentRegistry from '@src/js/components/database/new-forms/engine/ComponentRegistry.ts';

// Get field renderer
const FieldRenderer = ComponentRegistry.getFieldRenderer(FormFieldDataType.VARCHAR);

// Get action renderer
const ActionRenderer = ComponentRegistry.getActionRenderer('button');
```

**Supported Field Data Types**:
- `FormFieldDataType.VARCHAR` → `TextFieldRenderer`
- `FormFieldDataType.TIMESTAMP` → `DateFieldRenderer`
- `FormFieldDataType.MULTILINE_VARCHAR` → `TextAreaFieldRenderer`
- `FormFieldDataType.CONTROLLED_VOCABULARY` → `SelectFieldRenderer`
- `FormFieldDataType.BOOLEAN` → `SwitchFieldRenderer`
- `FormFieldDataType.WORD_PROCESSOR` → `CKEditorFieldRenderer`

**Supported Action Component Types**:
- `'button'` → `ButtonActionRenderer`
- `'switch'` → `SwitchActionRenderer`

### CoreFormModel

Contains shared action handlers that are used across all entity forms. These actions provide common functionality like editing, canceling, deleting, and moving entities.

**Location**: `CoreFormModel.ts`

**Available Actions**:
- `editAction` - Sets form mode to EDIT
- `cancelEditAction` - Sets form mode to VIEW
- `cancelNewFormAction` - Closes the new form dialog
- `deleteAction` - Deletes the entity
- `moveAction` - Moves the entity
- `unknownAction` - Fallback handler for unknown actions

## Adding New Components

### 1. Adding a New Controller

To add support for a new entity type:

```typescript
// In ControllerDispatcher.ts
import { NewEntityFormController } from '@src/js/components/database/new-forms/entities/NewEntity/NewEntityFormController.ts';

// Add to createController method
case EntityKind.NEW_ENTITY:
  return new NewEntityFormController(openbisFacade);
```

**Steps**:
1. Create your controller class implementing `IFormController`
2. Import it in `ControllerDispatcher.ts`
3. Add a case in the `createController` switch statement

### 2. Adding a New Field Renderer

To add support for a new field data type:

```typescript
// In ComponentRegistry.ts
import { NewFieldRenderer } from '@src/js/components/database/new-forms/components/fields/NewFieldRenderer.tsx';

// Add to getFieldRenderer method
case FormFieldDataType.NEW_TYPE:
  return NewFieldRenderer;
```

**Steps**:
1. Create your field renderer component
2. Import it in `ComponentRegistry.ts`
3. Add a case in the `getFieldRenderer` switch statement
4. Add the new data type to `FormFieldDataType` enum if needed

### 3. Adding a New Action Handler

To add a new action handler:

```typescript
// In ActionHandlerDispatcher.ts
import { NewEntityFormModel } from '@src/js/components/database/new-forms/entities/NewEntity/NewEntityFormModel.ts';

// Add to getActionHandler method
case 'new-entity:save':
  return NewEntityFormModel.saveNewEntityAction;
```

**For Core Actions** (shared across all forms):
```typescript
// In CoreFormModel.ts
static newCoreAction = (context: IExtendedActionContext) => {
  // Your action logic here
};

// In ActionHandlerDispatcher.ts
case 'new-core-action':
  return CoreFormModel.newCoreAction;
```

**Steps**:
1. Create your action handler function in the appropriate FormModel
2. Import the FormModel in `ActionHandlerDispatcher.ts`
3. Add a case in the `getActionHandler` switch statement
4. Use the naming convention: `'entity:action'` for entity-specific actions

### 4. Adding a New Action Renderer

To add a new action component type:

```typescript
// In ComponentRegistry.ts
import { NewActionRenderer } from '@src/js/components/database/new-forms/components/actions/NewActionRenderer.tsx';

// Add to getActionRenderer method
case 'new-action-type':
  return NewActionRenderer;
```

**Steps**:
1. Create your action renderer component
2. Import it in `ComponentRegistry.ts`
3. Add a case in the `getActionRenderer` switch statement

## Benefits

- ✅ **Separation of Concerns**: Each dispatcher/registry handles a specific responsibility
- ✅ **No Registration Overhead**: Components are loaded on-demand when needed
- ✅ **Type Safe**: Full TypeScript support with proper typing
- ✅ **Easy to Extend**: Just add new cases to switch statements
- ✅ **Centralized Management**: All component creation logic in one place
- ✅ **Maintainable**: Clear, simple switch statements that are easy to understand

## Best Practices

1. **Action Naming**: Use the format `'entity:action'` for entity-specific actions (e.g., `'project:save'`)
2. **Core Actions**: Place shared actions in `CoreFormModel` rather than duplicating them
3. **Default Cases**: Always include default cases that provide sensible fallbacks
4. **Type Safety**: Use enums (`EntityKind`, `FormFieldDataType`) instead of string literals when possible
