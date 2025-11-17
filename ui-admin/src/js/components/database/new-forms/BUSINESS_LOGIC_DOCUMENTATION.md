# New Forms System - Business Logic Documentation

## Table of Contents
1. [Overview](#overview)
2. [Core Business Rules](#core-business-rules)
3. [Form Operations](#form-operations)
4. [Validation Rules](#validation-rules)
5. [Permission System](#permission-system)
6. [Conflict Resolution](#conflict-resolution)
7. [Auto-save Logic](#auto-save-logic)
8. [Entity-Specific Business Rules](#entity-specific-business-rules)
9. [Error Handling](#error-handling)
10. [State Management](#state-management)

## Overview

The New Forms System is a declarative, plugin-based form engine designed to handle various openBIS entity types (Spaces, Projects, Collections, Datasets, Objects) with consistent behavior and user experience. The system implements a registry pattern for dynamic component loading and provides comprehensive business logic for form operations.

## Core Business Rules

### 1. Form Lifecycle
- **Form Modes**: VIEW, CREATE, EDIT
- **State Transitions**: 
  - VIEW → EDIT (via edit action)
  - EDIT → VIEW (via save or cancel action)
  - CREATE → VIEW (via save action)
  - Any mode → VIEW (via cancel action)

### 2. Entity Identification
- Each form is identified by `entityPermId` (permanent ID)
- Entity types are mapped through `objectTypeToEntityKindMap`
- Forms maintain version information for conflict detection (not yet implemented, actually `lastModificationDate` is used for conflicts resolution)

### 3. Data Integrity
- All form data must be validated before save operations
- Read-only fields cannot be modified in any mode
- Required fields must have values before save
- Form state is immutable - changes create new state objects

## Form Operations

### Create Operation
**Business Rules:**
- Create requires CREATE permission (needs to be better defined)
- New entities are created with empty form state or partially prefilled values
- Mode is set to CREATE
- All fields are editable unless marked as read-only
- Save action creates new entity in openBIS
- After successful creation, form transitions to VIEW mode

**Implementation:**
```typescript
// Controller responsibility
async load(permId: string): Promise<Form> {
  // Load empty form structure for new entity
  return FormModel.createEmptyForm(permId);
}

// Save action
async save(form: Form, mode: FormMode): Promise<number> {
  if (mode === FormMode.CREATE) {
    return await this.createEntity(form);
  }
  // ... other modes
}
```

### Edit Operation
**Business Rules:**
- Edit requires EDIT permission (needs to be better defined)
- Only editable fields can be modified
- Form transitions from VIEW to EDIT mode
- Changes are tracked for conflict detection
- Auto-save can be enabled during edit mode
- Cancel action discards all changes

**Implementation:**
```typescript
// Edit action handler
static editAction = (context: ActionContext) => {
  context.setMode(FormMode.EDIT);
};

// Field change handling
const handleFieldChange = (fieldId: string, value: any) => {
  setForm(prevForm => ({
    ...prevForm,
    fields: prevForm.fields.map(field => 
      field.id === fieldId ? { ...field, value } : field
    )
  }));
};
```

### Save Operation
**Business Rules:**
- All validation rules must pass before save (not yet implemented)
- Conflict detection checks for concurrent modifications based on `lastModificationDate`
- Save operation is atomic - either all changes or none
- Version number is incremented after successful save (should be done backend side)
- Form transitions to VIEW mode after save

**Implementation:**
```typescript
// Save with conflict detection
if (mode === FormMode.EDIT) {
  const latestForm = await controller.load(form.entityPermId);
  if (checkModificationDateConflict(form, latestForm)) {
    // Show conflict resolution dialog
    const conflicts = findConflicts(form, latestForm);
    setConflictFields(conflicts);
    setShowConflictDialog(true);
    return;
  }
  // Proceed with save
  const newVersion = await controller.save(form);
}
```

### Delete Operation
**Business Rules:**
- Delete requires DELETE permission (needs to be better defined)
- Confirmation dialog should be shown
- Delete operation is irreversible
- Form closes after successful deletion

## Validation Rules

### Field-Level Validation (not yet implemented)
**Built-in Rules:**
- `required`: Field must have a non-empty value
- `minLength`: String must meet minimum length requirement
- `matchesField`: Field value must match another field's value

**Implementation:**
```typescript
// Validation rule registration
validationRuleRegistry.register('required', (value) => 
  (value !== null && value !== undefined && value !== '') || 'This field is required.'
);

validationRuleRegistry.register('minLength', (value, options) => 
  (value && value.length >= options.length) || `Must be at least ${options.length} characters.`
);
```

### Form-Level Validation
- All required fields must be valid
- Cross-field validation rules are applied
- Validation errors prevent save operations
- Error messages are displayed per field


## Permission System

### Permission Types (needs to have an expanded definition on single enities)
- `canEdit`: User can modify entity data
- `canDelete`: User can delete entity
- `canMove`: User can move entity to different parent

### Permission Checking
**Implementation:**
```typescript
async checkPermissions(form: Form): Promise<Record<string, boolean>> {
  const { SpacePermId, ProjectIdentifier, SampleIdentifier } = this.openbisFacade;
  const spaceCode = form.entityPermId;
  const spacePermId = new SpacePermId(spaceCode);
  
  // Create dummy IDs for permission checking
  const dummyProjectId = new ProjectIdentifier(createDummySampleIdentifier(spaceCode));
  const dummySampleId = new SampleIdentifier(createDummySampleIdentifier(spaceCode));
  
  const ids = [spacePermId, dummyProjectId, dummySampleId];
  const { editable, deletable } = await fetchRights(this.openbisFacade, spaceCode, ids);
  
  return { canEdit: editable, canDelete: deletable, canMove: true };
}
```

### Role-Based Access (not implemented yet)
- Admin users have full permissions
- Regular users have permissions based on role assignments
- Space-level and project-level permissions are checked
- Authorization groups are considered in permission evaluation

### Action Visibility
Actions are shown/hidden based on:
- Current form mode (VIEW, CREATE, EDIT)
- User permissions
- Entity state

```typescript
// Action visibility rules
const visibleActions = form.actions?.filter(action => {
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

## Conflict Resolution

### Conflict Detection
**Business Rules:**
- Conflicts are detected by comparing modification dates
- Only editable fields are checked for conflicts
- Server version must be newer than local version to trigger conflict

**Implementation:**
```typescript
const checkModificationDateConflict = (localForm: Form, serverForm: Form): boolean => {
  const localDate = findFormFieldById(localForm.fields, localForm.entityPermId + '-modificationDate')?.value;
  const serverDate = findFormFieldById(serverForm.fields, serverForm.entityPermId + '-modificationDate')?.value;
  return new Date(serverDate) > new Date(localDate);
};
```

### Conflict Resolution Process
1. **Detection**: System detects concurrent modifications
2. **Identification**: Conflicting fields are identified
3. **Presentation**: User sees local vs server values
4. **Resolution**: User chooses which version to keep
5. **Merge**: Selected values are applied to form
6. **Save**: Form is saved with resolved values

### Conflict Resolution UI
- Side-by-side comparison of local and server values
- Merge option for each conflicting field
- Clear indication of which values are different
- Option to keep local, server, or custom merged values

## Auto-save Logic

### Auto-save Configuration
- **Enabled/Disabled**: User can toggle auto-save
- **Interval**: Configurable save interval (default: 5 seconds)
- **Storage**: Uses browser localStorage
- **Scope**: Only active forms are auto-saved

### Auto-save Rules
- Only saves when form is in EDIT mode
- Saves only when form data has changed
- Does not save if validation errors exist
- Clears auto-save data after successful save

**Implementation:**
```typescript
const useAutoSave = ({
  formData,
  storageKey,
  isEnabled,
  interval = 5000
}) => {
  // Save data to localStorage
  const saveToStorage = useCallback(() => {
    const dataToSave = {
      data: formDataRef.current,
      timestamp: Date.now()
    };
    localStorage.setItem(storageKey, JSON.stringify(dataToSave));
  }, [storageKey]);

  // Auto-save interval
  useEffect(() => {
    if (!isEnabled) return;
    const handle = setInterval(() => {
      saveToStorage();
    }, interval);
    return () => clearInterval(handle);
  }, [isEnabled, interval, saveToStorage]);
};
```

## Entity-Specific Business Rules

### Space Entity
**Business Rules:**
- Code must be unique across all spaces
- Code cannot be changed after creation
- Description is optional but recommended
- Spaces can contain projects

**Field Configuration:**
- `code`: Required, read-only after creation
- `description`: Optional, editable
- `registrator`: Read-only, system-generated
- `registrationDate`: Read-only, system-generated

### Project Entity
**Business Rules:**
- Must belong to a valid Space
- Code must be unique within the Space
- Cannot be moved to different Space
- Can contain Collections and Samples

**Field Configuration:**
- `code`: Required, editable only during creation
- `space`: Required, read-only
- `description`: Optional, editable
- `identifier`: Read-only, system-generated

### Collection Entity
**Business Rules:**
- Must belong to a valid Project
- Code must be unique within the Project
- Can contain Datasets
- Type determines available properties

**Field Configuration:**
- `code`: Required, editable only during creation
- `project`: Required, read-only
- `type`: Required, read-only after creation
- `description`: Optional, editable

### Dataset Entity
**Business Rules:**
- Must belong to a valid Collection
- Must have valid file references
- File size and type restrictions apply
- Cannot be moved between Collections

**Field Configuration:**
- `code`: Required, editable only during creation
- `collection`: Required, read-only
- `type`: Required, read-only after creation
- `files`: Required, editable

## Error Handling

### Error Types
1. **Validation Errors**: Field-level validation failures
2. **Permission Errors**: Insufficient permissions for operation
3. **Conflict Errors**: Concurrent modification conflicts
4. **Network Errors**: API communication failures
5. **Business Logic Errors**: Rule violations

### Error Handling Strategy
- **Validation Errors**: Display inline with fields
- **Permission Errors**: Show error dialog, disable actions
- **Conflict Errors**: Show conflict resolution dialog
- **Network Errors**: Show retry option
- **Business Logic Errors**: Show descriptive error message

### Error Recovery
- **Validation Errors**: User corrects and retries
- **Permission Errors**: User requests permission or uses different account
- **Conflict Errors**: User resolves conflicts through UI
- **Network Errors**: Automatic retry with exponential backoff
- **Business Logic Errors**: User corrects data and retries

## State Management

### Form State Structure
```typescript
interface Form {
  entityPermId: string;
  entityKind: string;
  entityType: string;
  title: string;
  sections: SectionGroup[];
  fields: FormField[];
  version: number;
  mode?: FormMode;
  isDirty: boolean;
  isValid: boolean;
  meta: { [key: string]: any };
  actions?: FormAction[];
}
```

### State Updates
- **Immutable Updates**: All state changes create new objects
- **Field Changes**: Update specific field values
- **Mode Changes**: Transition between VIEW/EDIT/CREATE
- **Validation**: Re-validate on field changes
- **Dirty Tracking**: Mark form as dirty when fields change

### State Persistence
- **Auto-save**: Periodic saves to localStorage
- **Session Persistence**: Form state survives page refresh
- **Conflict Resolution**: State is updated with resolved values
- **Cleanup**: Auto-save data is cleared after successful save

## Conclusion

The New Forms System provides a comprehensive, rule-based approach to form management that ensures data integrity, handles concurrent access, and provides a consistent user experience across all openBIS entity types. The system's plugin architecture allows for easy extension while maintaining core business logic consistency.

Key strengths:
- **Declarative Configuration**: Forms are defined by data, not code
- **Plugin Architecture**: Easy to extend with new entity types
- **Conflict Resolution**: Handles concurrent modifications gracefully
- **Permission System**: Fine-grained access control
- **Validation Framework**: Extensible validation rules
- **Auto-save**: Prevents data loss
- **Error Handling**: Comprehensive error management

This business logic documentation serves as a reference for developers, testers, and stakeholders to understand the system's behavior and ensure consistent implementation across all form operations.
