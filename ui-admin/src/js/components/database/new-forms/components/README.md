# Form Components Architecture

This document explains the main form components (`EntityForm.tsx`, `EntityFormContextProvider2.tsx`) and their integration with the application entry point (`DatabaseComponent.jsx`). These components form the core of the new form system.

## Table of Contents

1. [Overview](#overview)
2. [Component Hierarchy](#component-hierarchy)
3. [EntityFormContextProvider2](#entityformcontextprovider2)
4. [EntityForm](#entityform)
5. [DatabaseComponent Integration](#databasecomponent-integration)
6. [Data Flow](#data-flow)
7. [Action Flow](#action-flow)
8. [State Management](#state-management)

## Overview

The form system follows a **container/presenter pattern**:

- **`EntityFormContextProvider2.tsx`** (Container) - Manages all state, business logic, and side effects
- **`EntityForm.tsx`** (Presenter) - Pure presentation component that renders the form UI
- **`DatabaseComponent.jsx`** (Entry Point) - Integrates the form system into the application

This separation provides:
- **Testability**: Presentation logic separated from business logic
- **Reusability**: Form component can be used in different contexts
- **Maintainability**: Clear separation of concerns

## Component Hierarchy

```
DatabaseComponent.jsx (Entry Point)
  ↓
EntityFormContextProvider2.tsx (State Management)
  ↓
EntityForm.tsx (UI Rendering)
  ↓
Field Renderers / Action Renderers (via ComponentRegistry)
```

## EntityFormContextProvider2

**File**: `EntityFormContextProvider2.tsx`

**Role**: Container component that manages all form state and business logic.

### Responsibilities

1. **State Management**: Uses custom hooks to manage form state
2. **Form Loading**: Loads form data from controllers
3. **Action Handling**: Processes user actions (save, delete, move, etc.)
4. **Dialog Management**: Manages conflict, delete, and move dialogs
5. **Error Handling**: Handles errors and displays error dialogs
6. **External Integration**: Communicates with AppController via `externalAppController`

### Key Hooks Used

The component uses several custom hooks (see [hooks documentation](../hooks/README.md)):

```typescript
// Form state management
const { form, mode, setForm, setMode, updateField, updateFieldMetadata } = useFormState({ 
  initialForm: null, 
  initialMode 
});

// Operation state (loading, saving, error)
const { 
  operationState, 
  setLoading, setSaving, 
  setError, clearError, 
  executeOperation 
} = useOperationState();

// Dialog state management
const {
  dialogs,
  openConflictDialog, closeConflictDialog,
  openDeleteDialog, closeDeleteDialog,
  openMoveDialog, closeMoveDialog,
} = useDialogState();

// Conflict resolution
const { checkModificationDateConflict, findConflicts } = useConflictResolution();
```

### Controller Creation

Creates the appropriate controller using the dispatcher pattern:

```typescript
const controller: IFormController = useMemo(
  () => ControllerDispatcher.createController(entityKind, openbisFacade, user),
  [entityKind, openbisFacade, user]
);
```

See [engine documentation](../engine/README.md) for details on `ControllerDispatcher`.

### Props

```typescript
interface EntityFormContextProviderProps {
  openbisFacade: any;           // openBIS API facade
  params: any;                   // Additional parameters (e.g., parentId)
  entityKind: string;            // Entity type (EntityKind enum value)
  user: string;                  // Current user
  sessionID: string;             // Session token
  permId: string;                // Entity permanent ID
  initialMode: FormMode;         // Initial form mode (view/edit/create)
  externalAppController: any;   // AppController integration object
}
```

### Key Methods

#### `loadForm()`

Loads form data from the controller:

```typescript
const loadForm = useCallback(async () => {
  await executeOperation(
    async () => {
      if (entityKind === EntityKind.NEW_OBJECT) {
        const loadedForm = await controller.load(permId, entityKind, params, 'ENTRY');
        setForm(loadedForm);
      } else {
        const loadedForm = await controller.load(permId, entityKind, params);
        setForm(loadedForm);
      }
    },
    { setLoading: true }
  );
}, [permId, entityKind, params, controller]);
```

**Behavior**:
- Handles both new entity creation and existing entity loading
- Uses `executeOperation` for automatic loading state management
- Updates form state on success

#### `handleAction()`

Processes user actions from the form:

```typescript
const handleAction = useCallback(async (actionName: string) => {
  const actionHandler = ActionHandlerDispatcher.getActionHandler(actionName);
  
  if (actionName.toLowerCase().includes('save')) {
    await handleSaveActions(actionHandler);
  } else if (actionName === 'delete') {
    await handleDeleteWithDependencyCheck();
  } else if (actionName === 'move') {
    await handleMove();
  } else {
    // Handle other actions
    const context = getExtendedActionContext();
    await actionHandler(context);
  }
}, [form, mode, ...]);
```

**Action Routing**:
- **Save actions**: Routes to `handleSaveActions()` with conflict detection
- **Delete action**: Routes to `handleDeleteWithDependencyCheck()`
- **Move action**: Routes to `handleMove()`
- **Other actions**: Executes directly with action context

See [engine documentation](../engine/README.md) for details on `ActionHandlerDispatcher`.

#### `handleSaveActions()`

Handles save operations with conflict detection:

```typescript
const handleSaveActions = async (actionHandler: any) => {
  if (mode === FormMode.EDIT) {
    // Load latest version from server
    const latestForm = await controller.load(form.entityPermId);
    
    // Check for conflicts
    if (checkModificationDateConflict(form, latestForm)) {
      const conflicts = findConflicts(form, latestForm);
      openConflictDialog(conflicts);
      return;
    }
    
    // No conflicts, proceed with save
    await actionHandler(context);
  } else if (mode === FormMode.CREATE) {
    // Create new entity
    await actionHandler(context);
  }
};
```

**Conflict Detection**:
1. Loads latest form from server
2. Compares modification dates
3. Finds conflicting fields
4. Shows conflict resolution dialog if conflicts exist
5. Proceeds with save if no conflicts

See [hooks documentation](../hooks/README.md#useconflictresolution) for conflict resolution details.

#### `getExtendedActionContext()`

Creates action context for action handlers:

```typescript
const getExtendedActionContext = useCallback((reason?: string): IExtendedActionContext => {
  return {
    controller,
    form,
    setForm,
    mode,
    setMode,
    onAfterSave: (params?: any) => {
      setMode(FormMode.VIEW);
      if (params) {
        externalAppController.objectCreate(params);
      }
      loadForm();
    },
    externalAppController,
    deleteReason: reason,
    dependentEntities: dialogs.delete.config?.dependentEntities,
  };
}, [form, mode, ...]);
```

**Context Contents**:
- Controller instance
- Current form state
- Form state setters
- Callback for post-save actions
- External app controller integration
- Delete reason and dependent entities

### Dialog Management

The component manages three types of dialogs:

1. **Conflict Resolution Dialog**: Shown when save conflicts are detected
2. **Delete Confirmation Dialog**: Shown before deleting entities
3. **Move Dialog**: Shown when moving entities

All dialogs are managed through `useDialogState` hook. See [hooks documentation](../hooks/README.md#usedialogstate) for details.

### External App Controller Integration

The `externalAppController` object provides integration with the main application:

```typescript
externalAppController = {
  createNewObject: (params) => void,  // Create new entity
  objectChange: (params) => void,     // Notify object change
  objectCreate: (params) => void,      // Notify object creation
  closeForm: (params) => void         // Close form
}
```

These methods are called to:
- Navigate to new entities after creation
- Notify the app of changes
- Close forms when needed

## EntityForm

**File**: `EntityForm.tsx`

**Role**: Pure presentation component that renders the form UI.

### Responsibilities

1. **Render Toolbar**: Displays action buttons based on visibility rules
2. **Render Sections**: Organizes fields into collapsible sections
3. **Render Fields**: Uses field renderers to display form fields
4. **Layout Management**: Handles column layout (left, right, center)

### Props

```typescript
interface EntityFormProps {
  form: Form;                                    // Form data structure
  mode: FormMode;                                // Current form mode
  permissions: any;                              // User permissions
  onFieldChange: (fieldId: string, value: any) => void;
  onFieldMetadataChange: (fieldId: string, meta: any) => void;
  onAction: (actionName: string) => void;        // Action handler callback
  params: any;                                   // Additional parameters
}
```

### Component Structure

```typescript
<>
  {renderToolbar()}    {/* Action buttons */}
  {renderSections()}   {/* Form fields organized in sections */}
</>
```

### Action Visibility Rules

The toolbar filters actions based on visibility rules:

```typescript
const visibleActions = form.actions?.filter(action => {
  if (!action.visibility || action.visibility.length === 0) return true;
  
  // Every rule in the visibility array must be met
  return action.visibility.every((rule: VisibilityRule) => {
    let isVisible = true;
    if (rule.mode) {
      const modes = Array.isArray(rule.mode) ? rule.mode : [rule.mode];
      isVisible = isVisible && modes.includes(mode);
    }
    if (rule.permission) {
      isVisible = isVisible && permissions[rule.permission] === true;
    }
    return isVisible;
  });
});
```

**Visibility Rule Logic**:
- If no visibility rules: action is visible
- All rules must pass for action to be visible
- Rules can check:
  - `mode`: Form mode (view/edit/create)
  - `permission`: User permission flag

See [types documentation](../types/README.md#formaction) for `FormAction` structure.

### Section Rendering

Sections are rendered with field grouping by column:

```typescript
const buildSectionGroups = () => {
  const fieldsById = new Map(form.fields.map(f => [f.id, f]));

  if (form.sections?.length) {
    return form.sections.map(({ section, fields }) => ({
      section,
      fields: fields
        .map(id => fieldsById.get(id))
        .filter((field): field is FormField => Boolean(field)),
    }));
  }

  const order: string[] = [];
  const grouped = new Map<string, FormField[]>();

  form.fields.forEach(field => {
    if (!grouped.has(field.section)) {
      grouped.set(field.section, []);
      order.push(field.section);
    }
    grouped.get(field.section)!.push(field);
  });

  return order.map(section => ({
    section,
    fields: grouped.get(section) ?? [],
  }));
};

const renderSections = () => buildSectionGroups().map(({ section, fields }) => {
  const leftFields = fields.filter(field => field.column === 'left');
  const rightFields = fields.filter(field => field.column === 'right');
  const centerFields = fields.filter(field => field.column === 'center');

  return (
    <CollapsableSection title={section}>
      {/* Left and right columns side by side */}
      <div style={{ display: 'flex' }}>
        <div style={{ flex: 1 }}>{leftFields.map(renderField)}</div>
        <div style={{ flex: 1 }}>{rightFields.map(renderField)}</div>
      </div>
      {/* Center fields full width */}
      <div>{centerFields.map(renderField)}</div>
    </CollapsableSection>
  );
});
```

**Layout**:
- **Left/Right columns**: Rendered side-by-side in a flex container
- **Center fields**: Rendered full-width on their own row
- Fields are grouped by their `column` property

### Field Rendering

Fields are rendered using the component registry:

```typescript
const renderField = (field: FormField | undefined) => {
  if (!field) return null;
  
  const FieldRenderer = ComponentRegistry.getFieldRenderer(field.dataType);
  if (!FieldRenderer) {
    return <div>Unsupported field type: {field.dataType}</div>;
  }
  
  return (
    <FieldRenderer
      field={field}
      onFieldChange={onFieldChange}
      onFieldMetadataChange={onFieldMetadataChange}
      mode={mode}
      params={params}
    />
  );
};
```

**Field Renderer Selection**:
- Uses `ComponentRegistry.getFieldRenderer()` to get the appropriate renderer
- Renderer is selected based on `field.dataType`
- Falls back to error message if renderer not found

See [engine documentation](../engine/README.md#componentregistry) and [fields documentation](./fields/README.md) for field renderer details.

## DatabaseComponent Integration

**File**: `DatabaseComponent.jsx`

**Role**: Entry point that integrates the new form system into the application.

### Integration Point

The component renders `EntityFormContextProvider` in its `renderJson()` method:

```typescript
renderJson() {
  const { object } = this.props;
  return (
    <EntityFormContextProvider
      openbisFacade={openbis}
      params={object.params}
      entityKind={object.type}
      permId={object.id}
      user={AppController.getInstance().getUser()}
      sessionID={AppController.getInstance().getSessionToken()}
      initialMode={String(object.type).includes('new') ? 'create' : 'view'}
      externalAppController={this.externalAppController}
    />
  );
}
```

### External App Controller

The component provides an `externalAppController` object that bridges the form system with the main application:

```typescript
externalAppController = {
  createNewObject: (params) => {
    AppController.getInstance().objectNew(
      pages.DATABASE,
      params.newObjectType,
      { parentId: params.fromId, parentType: params.fromObjectType }
    );
  },
  objectChange: (params) => {
    AppController.getInstance().objectChange(
      pages.DATABASE,
      params.objectTypeChanging,
      params.id,
      params.changed
    );
  },
  objectCreate: (params) => {
    AppController.getInstance().objectCreate(
      pages.DATABASE,
      params.oldType,
      params.oldId,
      params.newType,
      params.newId
    );
  },
  closeForm: (params) => {
    AppController.getInstance().objectClose(
      pages.DATABASE,
      params.type,
      params.id
    );
  }
}
```

**Methods**:
- `createNewObject()`: Opens a new entity creation form
- `objectChange()`: Notifies the app of entity changes
- `objectCreate()`: Handles entity creation (navigates to new entity)
- `closeForm()`: Closes the current form

### Initial Mode Detection

The component determines the initial form mode:

```typescript
initialMode={String(object.type).includes('new') ? 'create' : 'view'}
```

- If entity type contains 'new': `FormMode.CREATE`
- Otherwise: `FormMode.VIEW`

## Data Flow

### Form Loading Flow

```
DatabaseComponent
  ↓ (renders with props)
EntityFormContextProvider2
  ↓ (useEffect on mount)
loadForm()
  ↓
ControllerDispatcher.createController()
  ↓
{Entity}FormController.load()
  ↓
{Entity}FormModel.adapt{Entity}DtoToForm()
  ↓
setForm(loadedForm)
  ↓
EntityForm (receives form prop)
  ↓
Renders fields and actions
```

### Field Update Flow

```
User edits field
  ↓
FieldRenderer.onChange()
  ↓
onFieldChange(fieldId, value)
  ↓
EntityFormContextProvider2.updateField()
  ↓
useFormState.updateField()
  ↓
Form state updated
  ↓
EntityForm re-renders with new value
```

### Save Flow

```
User clicks Save
  ↓
EntityForm.onAction('entity:save')
  ↓
EntityFormContextProvider2.handleAction()
  ↓
handleSaveActions()
  ↓
Check for conflicts (if EDIT mode)
  ↓
ActionHandlerDispatcher.getActionHandler('entity:save')
  ↓
{Entity}FormModel.save{Entity}Action()
  ↓
Controller.save()
  ↓
onAfterSave() callback
  ↓
externalAppController.objectCreate() (if CREATE mode)
  ↓
loadForm() (reload form)
```

### Delete Flow

```
User clicks Delete
  ↓
EntityForm.onAction('delete')
  ↓
EntityFormContextProvider2.handleAction()
  ↓
handleDeleteWithDependencyCheck()
  ↓
Controller.getDependentEntities()
  ↓
openDeleteDialog() with config
  ↓
User confirms with reason
  ↓
handleDeleteConfirm()
  ↓
Controller.delete()
  ↓
externalAppController.closeForm()
```

## Action Flow

### Action Execution Pipeline

1. **User Interaction**: User clicks action button in toolbar
2. **Action Renderer**: `ButtonActionRenderer` calls `onAction(actionName)`
3. **EntityForm**: Passes action to `EntityFormContextProvider2.handleAction()`
4. **Action Routing**: Routes to appropriate handler based on action name
5. **Action Handler**: Retrieved from `ActionHandlerDispatcher`
6. **Context Creation**: `getExtendedActionContext()` creates action context
7. **Handler Execution**: Action handler executes with context
8. **State Update**: Form state updated based on action result
9. **External Notification**: `externalAppController` methods called if needed

### Action Types

**Core Actions** (handled by `CoreFormModel`):
- `'edit'` - Switch to edit mode
- `'cancel'` - Cancel edit, return to view
- `'new-form:cancel'` - Close new form dialog
- `'delete'` - Delete entity (with dependency check)
- `'move'` - Move entity

**Entity-Specific Actions** (handled by entity models):
- `'{entity}:save'` - Save entity
- `'{entity}:new-child'` - Create child entity (e.g., `'space:new-project'`)

See [engine documentation](../engine/README.md#actionhandlerdispatcher) for action handler details.

## State Management

### State Layers

The component uses multiple state management layers:

1. **Form State** (`useFormState`):
   - Form data
   - Form mode
   - Dirty/valid flags
   - Field update functions

2. **Operation State** (`useOperationState`):
   - Loading state
   - Saving state
   - Error state

3. **Dialog State** (`useDialogState`):
   - Conflict dialog state
   - Delete dialog state
   - Move dialog state

4. **Local State**:
   - Permissions
   - Auto-save enabled flag

### State Updates

**Form Updates**:
- Field changes: `updateField()` → `useFormState` → form state updated
- Metadata changes: `updateFieldMetadata()` → `useFormState` → form state updated
- Mode changes: `setMode()` → `useFormState` → mode updated

**Operation Updates**:
- Loading: `setLoading(true)` → `useOperationState` → loading state updated
- Saving: `setSaving(true)` → `useOperationState` → saving state updated
- Errors: `setError(error)` → `useOperationState` → error state updated

**Dialog Updates**:
- Open/close: Dialog state hook methods → dialog state updated
- Config: Dialog config passed when opening

See [hooks documentation](../hooks/README.md) for detailed hook documentation.

## Related Documentation

- **[Engine Documentation](../engine/README.md)**: Controllers, action handlers, component registry
- **[Hooks Documentation](../hooks/README.md)**: Custom hooks used by EntityFormContextProvider
- **[Types Documentation](../types/README.md)**: Form, FormField, FormAction types
- **[Fields Documentation](./fields/README.md)**: Field renderers
- **[Actions Documentation](./actions/README.md)**: Action renderers
- **[Entities Documentation](../entities/README.md)**: Entity controllers and models

## Best Practices

### EntityFormContextProvider2

1. **Use Hooks**: Always use custom hooks for state management
2. **Error Handling**: Always handle errors in async operations
3. **Loading States**: Show loading indicators during operations
4. **Conflict Detection**: Always check for conflicts before saving in EDIT mode
5. **Dependency Checks**: Check for dependent entities before delete
6. **External Integration**: Use `externalAppController` for app integration

### EntityForm

1. **Pure Component**: Keep it as a pure presentation component
2. **No Business Logic**: All logic should be in EntityFormContextProvider2
3. **Visibility Rules**: Respect action visibility rules
4. **Field Rendering**: Use ComponentRegistry for field renderers
5. **Layout**: Follow column layout conventions

### DatabaseComponent Integration

1. **Props Mapping**: Map application props to form system props correctly
2. **External Controller**: Provide all required externalAppController methods
3. **Initial Mode**: Set correct initial mode based on entity type
4. **Session Management**: Pass valid session token

