# React Hooks

This folder contains custom React hooks that encapsulate common form-related functionality. These hooks provide reusable logic for form state management, loading, conflict resolution, dialog management, and more.

## Table of Contents

1. [Overview](#overview)
2. [Hooks Reference](#hooks-reference)
3. [Usage Patterns](#usage-patterns)
4. [Hook Combinations](#hook-combinations)
5. [Best Practices](#best-practices)

## Overview

The hooks in this folder follow React's hook patterns and provide:

- **State Management**: Form state, loading states, dialog states
- **Business Logic**: Conflict resolution, auto-save, form operations
- **Reusability**: Encapsulated logic that can be used across components
- **Type Safety**: Full TypeScript support

## Hooks Reference

### useFormState

Manages form state including fields, mode, dirty tracking, and validation.

**File**: `useFormState.ts`

**Purpose**: Core hook for managing form data and state

**Returns**:
```typescript
{
  form: Form | null;
  mode: FormMode;
  isDirty: boolean;
  isValid: boolean;
  updateField: (fieldId: string, value: any) => void;
  updateFieldMetadata: (fieldId: string, meta: any) => void;
  setMode: (mode: FormMode) => void;
  setForm: (form: Form | null) => void;
  resetForm: () => void;
}
```

**Features**:
- Tracks original form state for dirty detection
- Computes `isDirty` by comparing current vs original fields
- Computes `isValid` by checking required fields
- Provides field update functions
- Handles form reset to original state

**Usage**:
```typescript
const { form, mode, isDirty, isValid, updateField, setMode } = useFormState({
  initialForm: null,
  initialMode: FormMode.VIEW
});

// Update a field
updateField('project:code', 'NEW-CODE');

// Change mode
setMode(FormMode.EDIT);

// Check if form has changes
if (isDirty) {
  // Show unsaved changes warning
}
```

**Key Implementation Details**:
- Uses `useMemo` for `isDirty` and `isValid` to avoid unnecessary recalculations
- Stores original form separately for comparison
- Handles function updates in `setForm` for React state updates

### useOperationState

Manages generic async operation states (loading, saving, error).

**File**: `useOperationState.ts`

**Purpose**: Reusable hook for any async operation with loading/saving/error states

**Returns**:
```typescript
{
  operationState: { loading: boolean; saving: boolean; error: any | null };
  setLoading: (loading: boolean) => void;
  setSaving: (saving: boolean) => void;
  setError: (error: any | null) => void;
  clearError: () => void;
  resetOperationState: () => void;
}
```

**Features**:
- Separate loading and saving states
- Error state management
- Simple state setters for manual error handling

**Usage**:
```typescript
const { operationState, setLoading, setSaving, setError, clearError } = useOperationState();

// Example: Loading form data
const loadForm = useCallback(async () => {
  setLoading(true);
  clearError();
  
  try {
    const form = await controller.load(permId, entityKind, params);
    setForm(form);
  } catch (error: any) {
    const errorMessage = error?.message || error?.toString() || 'Failed to load form';
    setError(errorMessage);
    console.error('Load failed:', error);
  } finally {
    setLoading(false);
  }
}, [permId, entityKind, params, controller, setLoading, clearError, setError, setForm]);

// Example: Saving form
const handleSave = async () => {
  setSaving(true);
  clearError();
  
  try {
    await controller.save(form, mode);
    actionToastContext.raiseSuccess('Saved successfully');
  } catch (error: any) {
    const errorMessage = error?.message || error?.toString() || 'Failed to save';
    setError(errorMessage);
    actionToastContext.raiseError('Save failed');
  } finally {
    setSaving(false);
  }
};
```

**Key Implementation Details**:
- Use try/catch/finally blocks directly for error handling
- Always clear errors at the start of operations
- Always reset loading/saving state in finally blocks
- Format errors consistently before setting them

### useAutoSave

Manages automatic saving of form data to localStorage.

**File**: `useAutoSave.tsx`

**Purpose**: Auto-save form data to prevent data loss

**Returns**:
```typescript
{
  loadFromStorage: () => Form | null;
  clearStorage: () => void;
}
```

**Features**:
- Saves form data to localStorage at intervals (configurable, default: 5000ms)
- Saves on page unload (`beforeunload` event)
- Only saves dirty fields (changed fields compared to `originalForm`)
- Loads saved data via `loadFromStorage()` function
- Configurable save interval and max age for stale data
- Handles storage quota errors gracefully

**Usage**:
```typescript
const { loadFromStorage, clearStorage } = useAutoSave({
  form,
  originalForm,
  storageKey: `form-data:${entityKind}:${permId}:${user}`,
  isEnabled: isAutoSaveEnabled,
  interval: 5000, // 5 seconds
  maxAge: 24 * 60 * 60 * 1000 // 24 hours
});

// Restoration (EDIT and CREATE) is handled via useEntityAutoSaveFlow's pending-draft dialog
// Clear storage after successful save
const handleSave = async () => {
  await saveForm(form);
  clearStorage();
};
```

**Key Implementation Details**:
- Uses refs to avoid stale closures in intervals
- Saves timestamp with data for expiration checking
- Handles localStorage errors gracefully (quota exceeded, private mode)
- Cleans up intervals and event listeners on unmount
- Only saves when there are dirty fields (compares form vs originalForm)

### Conflict Resolution Utilities

Pure helper functions for detecting conflicts between local and server forms.

**File**: `utils/conflictResolutionUtil.ts` (exports plain functions, not a hook)

**Exports**:
```typescript
findConflicts(localForm: Form, serverForm: Form): [FormField, FormField][];
checkModificationDateConflict(localForm: Form, serverForm: Form): boolean;
```

**Features**:
- Compares only non-read-only fields
- Uses JSON.stringify for deep equality
- Returns `[localField, serverField]` pairs for UI consumption
- Separate helper checks modification dates for optimistic locking

**Usage**:
```typescript
import { findConflicts, checkModificationDateConflict } from '../utils/conflictResolutionUtil';

const serverForm = await controller.load(form.entityPermId);

if (checkModificationDateConflict(form, serverForm)) {
  const conflicts = findConflicts(form, serverForm);
  if (conflicts.length > 0) {
    openConflictDialog(conflicts);
    return;
  }
}

await save(form);
```

**Key Implementation Details**:
- Pure functions (no React state)
- Can be reused in any module without hook rules
- Keeps UI-specific flow inside `useConflictFlow`

### useEntityAutoSaveFlow

Feature-level hook that orchestrates the complete auto-save flow for a single form entity.

**File**: `useEntityAutoSaveFlow.tsx`

**Purpose**: Encapsulates preference management, auto-save, restoration, and UI integration

**Returns**:
```typescript
{
  isAutoSaveEnabled: boolean;
  setAutoSaveEnabled: (enabled: boolean) => void;
  actionOverrides: Record<string, Partial<FormAction>>;
  clearStorage: () => void;
}
```

**Features**:
- Per-entity auto-save preference (stored in localStorage, defaults to false)
- Integrates `useAutoSave` for draft saving
- Offers restoration (EDIT and CREATE alike) via a pending-draft restore/discard dialog, rather than auto-applying silently
- Provides `actionOverrides` for UI integration (auto-save toggle switch)
- Clears draft storage when disabled or after successful save

**Usage**:
```typescript
const {
  isAutoSaveEnabled,
  setAutoSaveEnabled,
  actionOverrides,
  clearStorage
} = useEntityAutoSaveFlow({
  form,
  originalForm,
  mode,
  user,
  entityKind,
  permId,
  onRestore: (savedData) => {
    setForm(savedData);
    actionToastContext.raiseInfo('Restored unsaved changes');
  }
});

// Use actionOverrides in EntityForm
<EntityForm
  form={form}
  actionOverrides={actionOverrides}
  // ... other props
/>

// Clear storage after successful save
const handleSave = async () => {
  await controller.save(form);
  clearStorage();
};
```

**Key Implementation Details**:
- Preference key format: `new-forms:auto-save-enabled:${user}:${entityKind}:${permId || 'unknown'}`
- Draft key format: `form-data:${entityKind}:${permId || 'new'}:${user}`
- Preference is removed (not set to false) when disabled to keep localStorage clean
- Draft is cleared when preference is disabled
- See [AUTOSAVE_FEATURE.md](../AUTOSAVE_FEATURE.md) for complete documentation

### useDialogState

Centralized state management for all form dialogs.

**File**: `useDialogState.ts`

**Purpose**: Manage state for conflict, delete, and move dialogs

**Returns**:
```typescript
{
  dialogs: DialogState;
  // Conflict dialog
  openConflictDialog: (fields: any[]) => void;
  closeConflictDialog: () => void;
  setConflictResolving: (isResolving: boolean) => void;
  // Delete dialog
  openDeleteDialog: (config: any) => void;
  closeDeleteDialog: () => void;
  // Move dialog
  openMoveDialog: (info: any) => void;
  closeMoveDialog: () => void;
}
```

**Features**:
- Centralized dialog state management
- Separate state for each dialog type
- Helper functions for opening/closing dialogs
- Tracks dialog-specific data (config, fields, info)

**Usage**:
```typescript
const {
  dialogs,
  openConflictDialog,
  closeConflictDialog,
  openDeleteDialog,
  closeDeleteDialog
} = useDialogState();

// Open conflict dialog
const handleConflict = (conflicts) => {
  openConflictDialog(conflicts);
};

// Open delete dialog with config
const handleDelete = () => {
  openDeleteDialog({
    includeReason: true,
    entityKind: 'Project',
    numberOfEntities: 1
  });
};

// Check if dialog is open
if (dialogs.conflict.isOpen) {
  return <ConflictDialog ... />;
}
```

**Key Implementation Details**:
- Groups related dialog state together
- Provides type-safe dialog state structure
- Each dialog has its own open/close functions

## Usage Patterns

### Basic Form with Loading

```typescript
function EntityForm({ permId, entityKind }) {
  const controller = useMemo(
    () => ControllerDispatcher.createController(entityKind, openbisFacade),
    [entityKind]
  );
  
  const { operationState, setLoading, setError, clearError } = useOperationState();
  const { form, mode, updateField, setForm } = useFormState({
    initialForm: null,
    initialMode: FormMode.VIEW
  });
  
  const loadForm = useCallback(async () => {
    setLoading(true);
    clearError();
    
    try {
      const loadedForm = await controller.load(permId, entityKind, params);
      setForm(loadedForm);
    } catch (error: any) {
      const errorMessage = error?.message || error?.toString() || 'Failed to load form';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [permId, entityKind, params, controller, setLoading, clearError, setError, setForm]);
  
  useEffect(() => {
    loadForm();
  }, [permId]);
  
  if (operationState.loading) return <Spinner />;
  if (operationState.error) return <ErrorMessage error={operationState.error} />;
  if (!form) return null;
  
  return <FormRenderer form={form} onFieldChange={updateField} />;
}
```
<｜tool▁calls▁begin｜><｜tool▁call▁begin｜>
read_file

### Form with Conflict Resolution

```typescript
function EntityFormWithConflicts({ permId, entityKind }) {
  const { form, updateField, setForm } = useFormState({ ... });
  const { openConflictDialog, dialogs } = useDialogState();
  const { setSaving } = useOperationState();
  
  const handleSave = async () => {
    setSaving(true);
    
    try {
      // Load latest from server
      const serverForm = await controller.load(permId);
      
      // Check for conflicts
      if (checkModificationDateConflict(form, serverForm)) {
        const conflicts = findConflicts(form, serverForm);
        if (conflicts.length > 0) {
          openConflictDialog(conflicts);
          return;
        }
      }
      
      // No conflicts, save
      await controller.save(form);
    } finally {
      setSaving(false);
    }
  };
  
  return (
    <>
      <FormRenderer form={form} onFieldChange={updateField} />
      {dialogs.conflict.isOpen && (
        <ConflictDialog
          conflicts={dialogs.conflict.fields}
          onResolve={handleResolveConflicts}
        />
      )}
    </>
  );
}
```

### Form with Auto-Save

```typescript
function EntityFormWithAutoSave({ permId, entityKind, user }) {
  const { form, originalForm, updateField, setForm } = useFormState({ ... });
  const actionToastContext = useActionToastCtx();
  
  // Use the orchestrating hook for complete auto-save flow
  const {
    isAutoSaveEnabled,
    setAutoSaveEnabled,
    actionOverrides,
    clearStorage
  } = useEntityAutoSaveFlow({
    form,
    originalForm,
    mode: FormMode.EDIT,
    user,
    entityKind,
    permId,
    onRestore: (savedData) => {
      setForm(savedData);
      actionToastContext.raiseInfo('Restored unsaved changes');
    }
  });
  
  const handleSave = async () => {
    await controller.save(form);
    clearStorage(); // Clear auto-saved data after successful save
  };
  
  return (
    <>
      <EntityForm
        form={form}
        onFieldChange={updateField}
        actionOverrides={actionOverrides} // Auto-save toggle uses this
        // ... other props
      />
    </>
  );
}
```

## Hook Combinations

### Complete Form Component

```typescript
function CompleteEntityForm({ permId, entityKind }) {
  // 1. Create controller
  const controller = useMemo(
    () => ControllerDispatcher.createController(entityKind, openbisFacade),
    [entityKind]
  );
  
  // 2. Form state
  const { form, mode, isDirty, isValid, updateField, setForm, setMode } = useFormState({
    initialForm: null,
    initialMode: FormMode.VIEW
  });
  
  // 3. Operation state (for loading, saving, errors)
  const { operationState, setLoading, setSaving, setError, clearError } = useOperationState();
  
  // 4. Conflict resolution
  import { checkModificationDateConflict, findConflicts } from '../utils/conflictResolutionUtil';
  
  // 5. Dialog state
  const { dialogs, openConflictDialog, openDeleteDialog, ... } = useDialogState();
  
  // 6. Auto-save (optional)
  const {
    isAutoSaveEnabled,
    setAutoSaveEnabled,
    actionOverrides,
    clearStorage
  } = useEntityAutoSaveFlow({
    form,
    originalForm,
    mode,
    user: currentUser,
    entityKind,
    permId,
    onRestore: (savedData) => setForm(savedData)
  });
  
  // Load form handler
  const loadForm = useCallback(async () => {
    setLoading(true);
    clearError();
    
    try {
      const loadedForm = await controller.load(permId, entityKind, params);
      setForm(loadedForm);
    } catch (error: any) {
      const errorMessage = error?.message || error?.toString() || 'Failed to load form';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [permId, entityKind, params, controller, setLoading, clearError, setError, setForm]);
  
  // Load form on mount
  useEffect(() => {
    loadForm();
  }, [permId]);
  
  // Save handler
  const handleSave = async () => {
    setSaving(true);
    clearError();
    
    try {
      // Check conflicts
      const serverForm = await controller.load(permId);
      if (checkModificationDateConflict(form, serverForm)) {
        const conflicts = findConflicts(form, serverForm);
        if (conflicts.length > 0) {
          openConflictDialog(conflicts);
          return;
        }
      }
      
      // Save
      await controller.save(form, mode);
      clearStorage();
    } catch (error: any) {
      const errorMessage = error?.message || error?.toString() || 'Failed to save';
      setError(errorMessage);
    } finally {
      setSaving(false);
    }
  };
  
  // Render...
}
```

## Best Practices

### 1. Hook Order

Use hooks in a consistent order:
1. Controller creation (useMemo)
2. Form state (useFormState)
3. Loading state (useFormLoading)
4. Operation state (useOperationState)
5. Specialized helpers (conflictResolution utils from `utils/conflictResolutionUtil.ts`, useDialogState)
6. Auto-save (useEntityAutoSaveFlow) - if needed

### 2. Error Handling

Always use try/catch/finally blocks for error handling:

```typescript
const { setLoading, setError, clearError } = useOperationState();

const loadForm = useCallback(async () => {
  setLoading(true);
  clearError();
  
  try {
    const form = await controller.load(permId, entityKind, params);
    setForm(form);
  } catch (error: any) {
    const errorMessage = error?.message || error?.toString() || 'Failed to load form';
    setError(errorMessage);
    console.error('Failed to load form:', error);
  } finally {
    setLoading(false);
  }
}, [permId, entityKind, params, controller, setLoading, clearError, setError, setForm]);
```

### 3. Cleanup

Clear auto-save storage after successful operations:

```typescript
const handleSave = async () => {
  await controller.save(form);
  clearStorage(); // Important!
};
```

### 4. Conflict Detection

Always check for conflicts before saving:

```typescript
const handleSave = async () => {
  const serverForm = await controller.load(permId);
  
  if (checkModificationDateConflict(form, serverForm)) {
    const conflicts = findConflicts(form, serverForm);
    if (conflicts.length > 0) {
      // Show conflict dialog
      return;
    }
  }
  
  // Proceed with save
};
```

### 5. State Updates

Use functional updates when updating form state:

```typescript
// Good
updateField('fieldId', newValue);

// Also good (for complex updates)
setForm(prevForm => ({
  ...prevForm,
  fields: prevForm.fields.map(field => 
    field.id === 'fieldId' ? { ...field, value: newValue } : field
  )
}));
```

### 6. Performance

- `useFormState` uses `useMemo` for `isDirty` and `isValid` - don't recalculate manually
- `useAutoSave` uses refs to avoid recreating callbacks
- Use `useCallback` for handlers passed to hooks when needed

### 7. Type Safety

Always use TypeScript types:

```typescript
const { form, updateField } = useFormState({ ... });

// TypeScript knows form is Form | null
if (form) {
  // TypeScript knows form is Form here
  form.fields.forEach(field => {
    // ...
  });
}
```

## Common Issues

### Stale Closures

**Problem**: Callbacks in hooks capture old values

**Solution**: Hooks like `useAutoSave` use refs to always get latest values

### Infinite Loops

**Problem**: useEffect dependencies cause infinite loops

**Solution**: Be careful with dependencies, especially with `form` object

```typescript
// Bad - form object changes on every render
useEffect(() => {
  saveToStorage();
}, [form]);

// Good - use form data directly or use refs
useEffect(() => {
  saveToStorage();
}, [form?.entityPermId, form?.fields]);
```

### Memory Leaks

**Problem**: Intervals or event listeners not cleaned up

**Solution**: Hooks handle cleanup automatically, but verify:

```typescript
useEffect(() => {
  const interval = setInterval(() => {
    // ...
  }, 1000);
  
  return () => clearInterval(interval); // Cleanup
}, []);
```

## Related Files

- `EntityFormContextProvider.tsx` - Uses all hooks together
- `types/formITypes.ts` - Form and field type definitions
- `utils/conflictResolutionUtil.ts` - Conflict resolution utilities (not a hook)
- `AUTOSAVE_FEATURE.md` - Complete auto-save feature documentation
- Form controllers - Used by form loading operations

