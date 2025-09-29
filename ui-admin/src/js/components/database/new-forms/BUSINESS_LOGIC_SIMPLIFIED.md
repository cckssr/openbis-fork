# Form System - Simplified Business Logic

## Overview
A clean, maintainable form system for openBIS entities with focused separation of concerns.

## Core Principles

### 1. **Single Responsibility**
- Each hook handles one specific concern
- Services contain pure business logic
- Components are purely presentational

### 2. **Composition over Inheritance**
- Custom hooks compose functionality
- Services are injected, not inherited
- Easy to test and mock

### 3. **Explicit Dependencies**
- All dependencies are injected
- No hidden global state
- Clear data flow

## Architecture

```
EntityFormContextProvider (Orchestrator)
├── useFormState (Form data & mode)
├── useFormLoading (Data loading)
├── useFormActions (Action execution)
├── useFormPermissions (Permission checking)
├── FormService (Business logic)
└── ConflictService (Conflict resolution)
```

## Business Rules

### Form Operations
- **Create**: New entity with empty form
- **Edit**: Modify existing entity (requires permissions)
- **Save**: Persist changes with validation
- **Cancel**: Discard changes, return to view mode
- **Delete**: Remove entity (requires permissions)

### Validation
- Required fields must have values
- Field-level validation rules apply
- Form cannot be saved with validation errors

### Permissions
- `canEdit`: Modify entity data
- `canDelete`: Remove entity
- `canMove`: Change entity location

### Conflict Resolution
- Detect concurrent modifications by timestamp
- Show user differences for conflicting fields
- Allow user to choose: local, server, or custom values

## Key Benefits

1. **Maintainable**: Small, focused components
2. **Testable**: Isolated business logic
3. **Reusable**: Custom hooks can be shared
4. **Performant**: Minimal re-renders
5. **Debuggable**: Clear data flow

## Usage Example

```typescript
const MyForm = () => {
  const { state, actions, permissions } = useFormContext();
  
  return (
    <div>
      {state.loading && <Loading />}
      {state.error && <Error message={state.error} />}
      <FormFields 
        fields={state.form?.fields}
        onChange={actions.updateField}
        mode={state.mode}
      />
      <ActionButtons 
        actions={state.form?.actions}
        onAction={actions.executeAction}
        permissions={permissions}
      />
    </div>
  );
};
```
