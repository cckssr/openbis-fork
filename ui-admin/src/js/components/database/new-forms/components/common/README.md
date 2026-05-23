# Common Components

This folder contains reusable dialog components and utility components used across the form system. These components provide common functionality like entity deletion, conflict resolution, entity movement, and entity search.

## Components

### DeleteConfirmationDialog

A dialog component that confirms entity deletion with optional reason input and dependency warnings.

**File**: `DeleteConfirmationDialog.tsx`

**Props**:
```typescript
interface DeleteConfirmationDialogProps {
  open: boolean;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
  config?: DeleteDialogConfig;
  classes?: any;  // Material-UI styles
}
```

**DeleteDialogConfig**:
```typescript
interface DeleteDialogConfig {
  includeReason: boolean;           // Whether to require a deletion reason
  entityKind: string;                // Type of entity being deleted
  dependentEntities: {                // Entities that depend on this one
    experiments?: any[];
    samples?: any[];
  } | null;
  numberOfEntities: number;          // Total number of entities to delete
  bypassesTrashcan: boolean;         // Whether deletion bypasses trashcan
  inputValue?: string;               // Pre-filled reason value
}
```

**Features**:
- Optional reason input (configurable via `includeReason`)
- Warning messages for dependent entities
- Displays count of dependent entities that will be deleted
- Shows whether entities go to trashcan or are permanently deleted
- Validates that reason is provided (if required)
- Uses "risky" button type for confirmation

**Usage Example**:
```typescript
<DeleteConfirmationDialog
  open={showDeleteDialog}
  onConfirm={(reason) => handleDelete(reason)}
  onCancel={() => setShowDeleteDialog(false)}
  config={{
    includeReason: true,
    entityKind: 'Project',
    dependentEntities: { experiments: [exp1, exp2] },
    numberOfEntities: 3,
    bypassesTrashcan: false
  }}
/>
```

### ConflictResolutionDialog

A dialog for resolving field conflicts when concurrent edits are detected. Allows users to choose between local, server, or merged values.

**File**: `ConflictResolutionDialog.tsx`

**Props**:
```typescript
interface ConflictResolutionDialogProps {
  open: boolean;
  conflicts: [FormField, FormField][];  // Array of [localField, serverField] pairs
  onResolve: (resolved: Record<string, any>) => void;
  onCancel: () => void;
}
```

**Features**:
- Displays conflicting fields side-by-side
- Three resolution options per field:
  - **Local**: Keep the local (user's) value
  - **Server**: Use the server's current value
  - **Merge**: Combine both values (editable text field)
- Radio button selection for each conflict
- Merge option allows custom editing of combined value
- Validates that all conflicts are resolved before allowing confirmation
- Shows warning message about conflicting changes

**Usage Example**:
```typescript
<ConflictResolutionDialog
  open={showConflictDialog}
  conflicts={[
    [localField1, serverField1],
    [localField2, serverField2]
  ]}
  onResolve={(resolved) => {
    // resolved is { fieldId1: value1, fieldId2: value2 }
    handleResolveConflicts(resolved);
  }}
  onCancel={() => setShowConflictDialog(false)}
/>
```

### MoveDialog

A dialog for moving entities to different parent locations (e.g., moving a project to a different space).

**File**: `MoveDialog.tsx`

**Props**:
```typescript
interface MoveDialogProps {
  open: boolean;
  onConfirm: (moveResult: any) => void;
  onCancel: () => void;
  form: Form;
  moveInfo: any;
  openbisFacade?: any;
  entityFormController?: IFormController;
}
```

**Features**:
- Displays current entity information (type, current parent)
- Entity search dropdown for selecting target location
- Option to move descendants (for samples/objects)
- Validates that a target is selected before allowing confirmation
- Shows entity type and current parent information
- Uses `AdvancedEntitySearchDropdown` for target selection
- Handles move operation through entity form controller

**Usage Example**:
```typescript
<MoveDialog
  open={showMoveDialog}
  onConfirm={(result) => handleMoveComplete(result)}
  onCancel={() => setShowMoveDialog(false)}
  form={form}
  moveInfo={moveInfo}
  openbisFacade={openbisFacade}
  entityFormController={controller}
/>
```

**Entity-Specific Behavior**:
- **Projects**: Can be moved to different spaces
- **Experiments/Collections**: Can be moved to different spaces or projects
- **Samples/Objects**: Can be moved to spaces, projects, or experiments/collections
- **Samples**: Includes option to move descendants (children, grandchildren, etc.)

### AdvancedEntitySearchDropdown

An autocomplete dropdown component for searching and selecting entities (spaces, projects, experiments, samples, datasets).

**File**: `AdvancedEntitySearchDropdown.tsx`

**Props**:
```typescript
interface AdvancedEntitySearchDropdownProps {
  openbisFacade: any;
  entityType: string;                    // Type of entity to search for
  onSelectionChange: (selected: any) => void;
  selectedEntity?: any;
  placeholder?: string;
  includeProjects?: boolean;
  includeExperiments?: boolean;
  includeSamples?: boolean;
  includeSpaces?: boolean;
  includeDatasets?: boolean;
  required?: boolean;
}
```

**Features**:
- Debounced search (300ms delay)
- Minimum 2 characters required to search
- Groups results by entity type
- Displays entity identifier and permId
- Loading indicator during search
- Supports multiple entity types in single search
- Handles nested identifier structures
- Custom option rendering with entity details

**Supported Entity Types**:
- `EntityKind.PROJECT` - Searches spaces
- `EntityKind.EXPERIMENT` - Searches spaces and projects
- `EntityKind.SAMPLE` - Searches spaces, projects, and experiments

**Usage Example**:
```typescript
<AdvancedEntitySearchDropdown
  openbisFacade={openbisFacade}
  entityType={EntityKind.PROJECT}
  onSelectionChange={(selected) => setTargetEntity(selected)}
  selectedEntity={targetEntity}
  placeholder="Search target space to move to"
  required={true}
/>
```

**Search Implementation**:
- Uses openBIS facade search methods
- `searchSpaces()` - Searches spaces by code
- `searchProject()` - Searches projects by code with space relationships
- Results are grouped by entity type (`@type` property)

## Common Patterns

### Dialog State Management

All dialogs follow a similar pattern for state management:

```typescript
const [showDialog, setShowDialog] = useState(false);
const [dialogConfig, setDialogConfig] = useState(null);

const handleOpen = (config) => {
  setDialogConfig(config);
  setShowDialog(true);
};

const handleConfirm = (result) => {
  // Process result
  setShowDialog(false);
  setDialogConfig(null);
};

const handleCancel = () => {
  setShowDialog(false);
  setDialogConfig(null);
};
```

### Error Handling

Dialogs should handle errors gracefully:

```typescript
const [error, setError] = useState<string | null>(null);

// In dialog content
{error && (
  <Message type="error">{error}</Message>
)}
```

### Loading States

For async operations, show loading states:

```typescript
const [loading, setLoading] = useState(false);

// Disable buttons during loading
<Button 
  onClick={handleConfirm}
  disabled={loading || !isValid}
/>
```

## Integration with Form System

These dialogs are typically used in `EntityFormContextProvider`:

1. **DeleteConfirmationDialog**: Triggered when delete action is called
2. **ConflictResolutionDialog**: Triggered when save conflicts are detected
3. **MoveDialog**: Triggered when move action is called
4. **AdvancedEntitySearchDropdown**: Used within MoveDialog and other entity selection scenarios

## Best Practices

1. **Consistent API**: All dialogs follow similar prop patterns (`open`, `onConfirm`, `onCancel`)
2. **Validation**: Validate inputs before allowing confirmation
3. **User Feedback**: Show loading states, error messages, and success indicators
4. **Accessibility**: Ensure keyboard navigation and screen reader support
5. **Error Messages**: Provide clear, actionable error messages
6. **Type Safety**: Use TypeScript interfaces for all props
7. **Reusability**: Keep dialogs generic and configurable

## Related Files

- `EntityFormContextProvider.tsx` - Main integration point for dialogs
- `form.types.ts` - Type definitions (`Form`, `FormField`)
- `MoveService.ts` - Service for handling move operations (referenced but not fully implemented)

