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

### useFormLoading

Manages form loading state and error handling.

**File**: `useFormLoading.ts`

**Purpose**: Handles async form loading with loading and error states

**Returns**:
```typescript
{
  loading: boolean;
  error: string | null;
  loadForm: () => Promise<Form | null>;
  clearError: () => void;
}
```

**Features**:
- Manages loading state during async operations
- Handles and stores errors
- Provides error clearing function
- Returns loaded form or null on error

**Usage**:
```typescript
const { loading, error, loadForm, clearError } = useFormLoading({
  controller: projectController,
  permId: 'PROJECT-123',
  entityKind: EntityKind.PROJECT,
  params: {}
});

// Load form
useEffect(() => {
  loadForm().then(form => {
    if (form) {
      setForm(form);
    }
  });
}, []);

// Display loading state
if (loading) return <Spinner />;

// Display error
if (error) return <ErrorMessage message={error} />;
```

**Key Implementation Details**:
- Automatically sets loading to false in finally block
- Catches and formats errors consistently
- Returns null on error to allow graceful handling

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
  executeOperation: <T>(operation: () => Promise<T>, options?: { setLoading?: boolean; setSaving?: boolean }) => Promise<T | null>;
}
```

**Features**:
- Separate loading and saving states
- Error state management
- Helper function to wrap async operations
- Automatic state management

**Usage**:
```typescript
const { operationState, executeOperation, setSaving } = useOperationState();

// Manual state management
const handleSave = async () => {
  setSaving(true);
  try {
    await saveForm(form);
  } catch (error) {
    setError(error);
  } finally {
    setSaving(false);
  }
};

// Using executeOperation helper
const handleSave = async () => {
  const result = await executeOperation(
    () => saveForm(form),
    { setSaving: true }
  );
  if (result) {
    // Success
  }
};
```

**Key Implementation Details**:
- `executeOperation` automatically handles try/catch/finally
- Clears error when starting new operation
- Returns null on error, result on success

### useAutoSave

Manages automatic saving of form data to localStorage.

**File**: `useAutoSave.tsx`

**Purpose**: Auto-save form data to prevent data loss

**Returns**:
```typescript
{
  saveToStorage: () => void;
  loadFromStorage: () => Form | null;
  clearStorage: () => void;
}
```

**Features**:
- Saves form data to localStorage at intervals
- Saves on page unload
- Loads saved data on mount if enabled
- Configurable save interval
- Optional restore callback

**Usage**:
```typescript
const { saveToStorage, loadFromStorage, clearStorage } = useAutoSave({
  formData: form,
  storageKey: `form-${permId}`,
  isEnabled: isAutoSaveEnabled,
  interval: 5000, // 5 seconds
  onDataRestore: (savedData) => {
    setForm(savedData);
    showRestoreDialog();
  }
});

// Clear storage after successful save
const handleSave = async () => {
  await saveForm(form);
  clearStorage();
};
```

**Key Implementation Details**:
- Uses refs to avoid stale closures in intervals
- Saves timestamp with data for potential expiration
- Handles localStorage errors gracefully
- Cleans up intervals on unmount

**Note**: Currently commented out in `EntityFormContextProvider` - may need activation

### Conflict Resolution Utilities

Pure helper functions for detecting conflicts between local and server forms.

**File**: `useConflictResolution.tsx` (exports plain functions)

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
import { findConflicts, checkModificationDateConflict } from './useConflictResolution';

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
  
  const { loading, error, loadForm } = useFormLoading({
    controller,
    permId,
    entityKind
  });
  
  const { form, mode, updateField, setForm } = useFormState({
    initialForm: null,
    initialMode: FormMode.VIEW
  });
  
  useEffect(() => {
    loadForm().then(loadedForm => {
      if (loadedForm) {
        setForm(loadedForm);
      }
    });
  }, [permId]);
  
  if (loading) return <Spinner />;
  if (error) return <ErrorMessage error={error} />;
  if (!form) return null;
  
  return <FormRenderer form={form} onFieldChange={updateField} />;
}
```

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
function EntityFormWithAutoSave({ permId, entityKind }) {
  const { form, updateField } = useFormState({ ... });
  const [isAutoSaveEnabled, setAutoSaveEnabled] = useState(false);
  
  const { loadFromStorage, clearStorage } = useAutoSave({
    formData: form,
    storageKey: `form-${permId}`,
    isEnabled: isAutoSaveEnabled,
    interval: 5000,
    onDataRestore: (savedData) => {
      if (confirm('Restore unsaved changes?')) {
        setForm(savedData);
      }
    }
  });
  
  const handleSave = async () => {
    await controller.save(form);
    clearStorage(); // Clear auto-saved data after successful save
  };
  
  return (
    <>
      <FormRenderer form={form} onFieldChange={updateField} />
      <Switch
        checked={isAutoSaveEnabled}
        onChange={setAutoSaveEnabled}
        label="Enable Auto-save"
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
  
  // 3. Loading state
  const { loading, error, loadForm, clearError } = useFormLoading({
    controller,
    permId,
    entityKind
  });
  
  // 4. Operation state (for save/delete)
  const { operationState, executeOperation, setSaving } = useOperationState();
  
  // 5. Conflict resolution
  
  // 6. Dialog state
  const { dialogs, openConflictDialog, openDeleteDialog, ... } = useDialogState();
  
  // 7. Auto-save (optional)
  const { clearStorage } = useAutoSave({
    formData: form,
    storageKey: `form-${permId}`,
    isEnabled: false, // Enable as needed
    interval: 5000
  });
  
  // Load form on mount
  useEffect(() => {
    loadForm().then(loadedForm => {
      if (loadedForm) setForm(loadedForm);
    });
  }, [permId]);
  
  // Save handler
  const handleSave = async () => {
    await executeOperation(
      async () => {
        // Check conflicts
        const serverForm = await controller.load(permId);
        if (checkModificationDateConflict(form, serverForm)) {
          const conflicts = findConflicts(form, serverForm);
          if (conflicts.length > 0) {
            openConflictDialog(conflicts);
            throw new Error('Conflicts detected');
          }
        }
        
        // Save
        const result = await controller.save(form);
        clearStorage();
        return result;
      },
      { setSaving: true }
    );
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
5. Specialized helpers (conflictResolution utils, useDialogState)
6. Auto-save (useAutoSave) - if needed

### 2. Error Handling

Always handle errors from hooks:

```typescript
const { error, loadForm } = useFormLoading({ ... });

useEffect(() => {
  loadForm().then(form => {
    if (!form) {
      // Handle error (error state is already set)
      console.error('Failed to load form');
    }
  });
}, []);
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
- `types/FormState.ts` - Type definitions for hook return values
- `types/formITypes.ts.ts` - Form and field type definitions
- Form controllers - Used by `useFormLoading`

