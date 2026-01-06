# Delete Feature - Specification & Developer Documentation

## Table of Contents

1. [Overview](#overview)
2. [Requirements](#requirements)
3. [Architecture](#architecture)
4. [Implementation Details](#implementation-details)
5. [API Reference](#api-reference)
6. [Flow Diagrams](#flow-diagrams)
7. [Code Examples](#code-examples)
8. [Testing Guidelines](#testing-guidelines)

---

## Overview

The delete feature provides a unified way to delete entities (Space, Project, Collection, Object, Dataset) in the openBIS system. Deletion operations move entities to the trashcan (soft delete) rather than permanently deleting them, allowing for recovery if needed.

### Key Concepts

- **Trashcan**: A temporary storage area where deleted entities are moved before permanent deletion
- **Dependent Entities**: Entities that belong to or are associated with the entity being deleted
- **Descendants**: Child entities in a hierarchical relationship (e.g., child objects, child datasets)
- **Immediate Deletion**: Only allowed for empty entities (no dependents)
- **Trashcan Deletion**: All deletions with a reason automatically move to trashcan

---

## Requirements

### Space Deletion

**Behavior:**
- ✅ Space must be empty (no projects, no samples) before deletion
- ✅ If space is not empty: **Prevent deletion** and show error message
- ✅ If space is empty: Move space to trashcan immediately
- ✅ Check for existing SAMPLE deletions in trashcan before allowing deletion

**Error Messages:**
- Non-empty space: `"Cannot delete space: Space is not empty. It contains X project(s) and Y sample(s). Please delete or move these entities first."`
- Existing deletions in trashcan: Lists deletion sets that must be permanently deleted first

**Dependencies Checked:**
- Projects (`projects`)
- Samples (`samples`)
- Existing deletions in trashcan (SAMPLE entities)

---

### Project Deletion

**Behavior:**
- ✅ **Empty Project**: Move project to trashcan immediately
- ✅ **Non-Empty Project**: Move all dependent entities (experiments, samples) to trashcan automatically, then move project to trashcan
- ✅ Check for existing deletions in trashcan before allowing deletion
- ✅ Show success toast after moving entities to trashcan (for non-empty projects)

**Flow:**
1. Check for existing deletions in trashcan
2. Get dependent entities (experiments, samples)
3. If empty: Move project to trashcan
4. If non-empty:
   - Move all experiments to trashcan
   - Move all samples to trashcan
   - Move project to trashcan
   - Show success toast: "Successfully moved X entities to trashcan"
   - **Do NOT close the form** (project still exists)

**Dependencies Checked:**
- Experiments (`experiments`)
- Samples (`samples`)
- Existing deletions in trashcan (EXPERIMENT and SAMPLE entities)

---

### Collection Deletion

**Behavior:**
- ✅ Move collection to trashcan
- ✅ Move all objects (samples) to trashcan
- ✅ Move all datasets to trashcan
- ✅ Show detailed list in dialog: objects and datasets that will be deleted

**Dialog Message Format:**
```
The collection has X object(s), which will also be deleted:
[Object Code 1]
[Object Code 2]
...

The collection has Y data set(s) which will also be deleted:
[Dataset Code 1]
[Dataset Code 2]
...

By providing a reason for deletion and clicking 'Accept', this entity will be moved to the Trashcan.
```

**Dependencies Checked:**
- Samples (`samples`) - displayed as "objects"
- Datasets (`datasets`)

---

### Object Deletion

**Behavior:**
- ✅ Move object to trashcan
- ✅ **Always** move all attached datasets to trashcan (automatic, no checkbox needed)
- ✅ Optional: Checkbox to trash descendant objects and their datasets
- ✅ Show list of attached datasets in dialog

**Dialog Message Format:**
```
The object has X data set(s) attached, which will also be moved to trashcan:
[Dataset Code 1]
[Dataset Code 2]
...

[Optional Checkbox]: Also trash descendant objects and their datasets (Y descendants)
```

**Dependencies Checked:**
- Datasets (`datasets`) - always moved
- Children (`children`) - optional via checkbox

**Descendants Checkbox:**
- Unchecked by default
- When checked: Recursively moves all descendant objects and their datasets to trashcan

---

### Dataset Deletion

**Behavior:**
- ✅ Move dataset to trashcan after confirmation
- ✅ Optional: Checkbox to trash descendant datasets

**Dialog Message Format:**
```
[Optional Checkbox]: Also trash descendant datasets (X descendants)
```

**Dependencies Checked:**
- Descendant datasets (`datasets`) - optional via checkbox

**Descendants Checkbox:**
- Unchecked by default
- When checked: Recursively moves all descendant datasets to trashcan

---

## Architecture

### Component Structure

```
EntityFormContextProvider
  ├── useDeleteFlow (hook)
  │   ├── handleDeleteWithDependencyCheck()
  │   ├── handleDeleteConfirm()
  │   └── handleDeleteCancel()
  ├── DeleteConfirmationDialog
  │   ├── renderAdditionalText() - Entity-specific messages
  │   └── renderDescendantsCheckbox() - For Object/Dataset
  └── Controller (entity-specific)
      ├── delete()
      ├── getDependentEntities()
      └── checkExistingDeletions() - For Space/Project
```

### Key Files

| File | Purpose |
|------|---------|
| `useDeleteFlow.ts` | Main delete flow logic, dependency checking, dialog management |
| `DeleteConfirmationDialog.tsx` | UI component for delete confirmation |
| `EntityFormContextProvider.tsx` | Orchestrates delete action |
| `{Entity}FormController.ts` | Entity-specific delete implementation |
| `IFormController.ts` | Interface defining delete contract |

---

## Implementation Details

### Delete Flow Sequence

```
1. User clicks Delete button
   ↓
2. EntityFormContextProvider.handleAction('delete')
   ↓
3. useDeleteFlow.handleDeleteWithDependencyCheck()
   ├── controller.delete(form, { checkOnly: true }) - Validation
   ├── controller.getDependentEntities(form) - Get dependencies
   └── normalizeDependentEntities() - Normalize structure
   ↓
4. openDeleteDialog(config) - Show confirmation dialog
   ├── renderAdditionalText() - Show entity-specific info
   └── renderDescendantsCheckbox() - Show checkbox if applicable
   ↓
5. User enters reason and confirms
   ↓
6. useDeleteFlow.handleDeleteConfirm(reason, includeDescendants)
   ├── controller.delete(form, context) - Actual deletion
   ├── Show success toast
   └── externalAppController.closeForm() - Close form (if entity deleted)
```

### Dependency Structure Normalization

Different entity types return different dependency structures. The `normalizeDependentEntities()` function in `useDeleteFlow.ts` normalizes them to a consistent format:

| Entity Type | Raw Structure | Normalized Structure |
|-------------|---------------|---------------------|
| Space | `{projects, samples}` | `{experiments: projects, samples, datasets: []}` |
| Project | `{experiments, samples}` | `{experiments, samples, datasets: []}` |
| Collection | `{samples, datasets}` | `{experiments: [], samples, datasets}` |
| Object | `{datasets, children}` | `{experiments: [], samples: children, datasets}` |
| Dataset | `{datasets, samples}` | `{experiments: [], samples, datasets}` |

**Why Normalization?**
- Allows consistent handling in `useDeleteFlow`
- Enables unified dialog rendering
- Maintains original structure in `rawDependentEntities` for controllers

### Trashcan API

**Important**: Deletion with a reason automatically moves entities to trashcan. There is no separate "move to trashcan" API.

**Deletion APIs:**
- `deleteSpaces([spaceId], deletionOptions)` - Moves space to trashcan
- `deleteProjects([projectId], deletionOptions)` - Moves project to trashcan
- `deleteExperiments([experimentId], deletionOptions)` - Moves experiment to trashcan
- `deleteSamples([sampleId], deletionOptions)` - Moves sample to trashcan
- `deleteDataSets([datasetId], deletionOptions)` - Moves dataset to trashcan

**DeletionOptions:**
```typescript
const deletionOptions = new {Entity}DeletionOptions();
deletionOptions.setReason(reason); // Required - moves to trashcan
```

**Permanent Deletion** (from trashcan):
```typescript
const confirmOperation = new ConfirmDeletionsOperation(deletionIds);
confirmOperation.setForceDeletionOfDependentDeletions(includeDependent);
await openbis.executeOperations([confirmOperation], ...);
```

**Revert Deletion** (restore from trashcan):
```typescript
await openbis.revertDeletions(deletionIds);
```

---

## API Reference

### IFormController.delete()

```typescript
delete(
  form: Form, 
  context?: {
    checkOnly?: boolean;              // If true, only validate, don't delete
    deleteReason?: string;            // Reason for deletion
    rawDependentEntities?: any;       // Original dependency structure
    dependentEntities?: any;          // Normalized dependency structure
    includeDescendants?: boolean;    // For Object/Dataset: include descendants
  }
): Promise<void | { skipped: boolean; message?: string }>
```

**Return Values:**
- `Promise<void>` - Normal deletion completed
- `Promise<{ skipped: true, message: string }>` - Deletion skipped (e.g., non-empty project - only moved entities)

**Example:**
```typescript
// Space deletion
async delete(form: Form, context?: any): Promise<void> {
  if (context?.checkOnly) {
    // Validation only
    const deps = await this.getDependentEntities(form);
    if (deps.projects.length > 0 || deps.samples.length > 0) {
      throw new Error('Space is not empty');
    }
    return;
  }
  
  // Actual deletion
  const deletionOptions = new SpaceDeletionOptions();
  deletionOptions.setReason(context?.deleteReason || 'default reason');
  await this.openbisFacade.deleteSpaces([spaceId], deletionOptions);
}
```

### getDependentEntities()

```typescript
getDependentEntities(form: Form): Promise<{
  experiments?: any[];
  samples?: any[];
  datasets?: any[];
  projects?: any[];      // For Space
  children?: any[];       // For Object
}>
```

**Returns:** Entity-specific structure of dependent entities.

**Example:**
```typescript
// Collection (Experiment)
async getDependentEntities(form: Form): Promise<any> {
  const experiment = await this.openbisFacade.getExperiments([id], fetchOptions);
  return {
    samples: experiment.getSamples() || [],
    datasets: experiment.getDataSets() || []
  };
}
```

### checkExistingDeletions()

```typescript
checkExistingDeletions(identifier: string): Promise<any[]>
```

**Purpose:** Check for existing deletions in trashcan that prevent deletion.

**Implemented for:**
- Space: Checks for SAMPLE entities in trashcan
- Project: Checks for EXPERIMENT and SAMPLE entities in trashcan

**Returns:** Array of deletion objects that must be permanently deleted first.

---

## Flow Diagrams

### Standard Delete Flow

```
┌─────────────────┐
│ User clicks     │
│ Delete button   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│ handleDeleteWithDependency  │
│ Check()                     │
└────────┬────────────────────┘
         │
         ├──► controller.delete(checkOnly: true)
         │    └──► Validate (throw error if invalid)
         │
         ├──► controller.getDependentEntities()
         │    └──► Get dependencies
         │
         └──► normalizeDependentEntities()
              └──► Normalize structure
         │
         ▼
┌─────────────────────────────┐
│ openDeleteDialog(config)    │
│ - Show entity info           │
│ - Show dependent entities    │
│ - Show descendants checkbox  │
│   (if applicable)            │
└────────┬────────────────────┘
         │
         │ User confirms
         ▼
┌─────────────────────────────┐
│ handleDeleteConfirm()       │
│ - controller.delete()       │
│ - Show success toast        │
│ - Close form (if deleted)   │
└─────────────────────────────┘
```

### Project Delete Flow (Non-Empty)

```
┌─────────────────────────────┐
│ Project Delete (Non-Empty)  │
└────────┬────────────────────┘
         │
         ├──► checkExistingDeletions()
         │    └──► Check trashcan
         │
         ├──► getDependentEntities()
         │    └──► Get experiments & samples
         │
         ├──► moveEntitiesToTrashcan(experiments)
         │    └──► deleteExperiments() with reason
         │
         ├──► moveEntitiesToTrashcan(samples)
         │    └──► deleteSamples() with reason
         │
         └──► Return { skipped: true, message: "..." }
              └──► Show success toast
              └──► Keep form open (project not deleted)
```

### Object Delete Flow (With Datasets)

```
┌─────────────────────────────┐
│ Object Delete               │
└────────┬────────────────────┘
         │
         ├──► getDependentEntities()
         │    └──► Get datasets & children
         │
         ├──► Move datasets to trashcan
         │    └──► deleteDataSets() with reason
         │
         ├──► [If descendants checked]
         │    └──► deleteDescendantObjects()
         │         ├──► Recursively get children
         │         ├──► Move child datasets
         │         └──► Move child objects
         │
         └──► Move object to trashcan
              └──► deleteSamples() with reason
```

---

## Code Examples

### Adding Delete Support to a New Entity Type

**Step 1: Implement IFormController interface**

```typescript
export class MyEntityFormController implements IFormController {
  async delete(form: Form, context?: any): Promise<void> {
    if (context?.checkOnly) {
      // Validation only
      return;
    }
    
    const deletionOptions = new MyEntityDeletionOptions();
    deletionOptions.setReason(context?.deleteReason || 'default');
    await this.openbisFacade.deleteMyEntities([entityId], deletionOptions);
  }
  
  async getDependentEntities(form: Form): Promise<any> {
    // Return structure: { experiments?, samples?, datasets? }
    return {
      samples: [],
      datasets: []
    };
  }
}
```

**Step 2: Update normalizeDependentEntities()**

```typescript
// In useDeleteFlow.ts
case EntityKind.MY_ENTITY:
  normalized.samples = dependentEntities.samples || [];
  normalized.datasets = dependentEntities.datasets || [];
  break;
```

**Step 3: Add dialog messaging (if needed)**

```typescript
// In DeleteConfirmationDialog.tsx
if (entityKind === EntityKind.MY_ENTITY && rawDependentEntities) {
  // Custom messaging
}
```

### Custom Delete Behavior Example

**Example: Custom validation before deletion**

```typescript
async delete(form: Form, context?: any): Promise<void> {
  if (context?.checkOnly) {
    // Custom validation
    const customCheck = await this.performCustomValidation(form);
    if (!customCheck.isValid) {
      throw new Error(customCheck.errorMessage);
    }
    return;
  }
  
  // Perform deletion
  // ...
}
```

### Error Handling Example

```typescript
try {
  await controller.delete(form, deleteContext);
  actionToastContext.raiseSuccess('Entity moved to trashcan successfully');
  externalAppController?.closeForm({...});
} catch (error: any) {
  // Error is automatically shown via setError()
  setError(error.message ?? error);
  actionToastContext?.raiseError('Error deleting entity');
}
```

---

## Testing Guidelines

### Test Cases

#### Space Deletion
- ✅ Delete empty space → Should succeed, move to trashcan
- ✅ Delete non-empty space → Should fail with error message
- ✅ Delete space with samples in trashcan → Should fail, list deletions

#### Project Deletion
- ✅ Delete empty project → Should succeed, move to trashcan, close form
- ✅ Delete non-empty project → Should move entities to trashcan, show toast, keep form open
- ✅ Delete project with deletions in trashcan → Should fail, list deletions

#### Collection Deletion
- ✅ Delete collection with objects → Should move collection, objects, and datasets
- ✅ Delete empty collection → Should move only collection
- ✅ Dialog should show list of objects and datasets

#### Object Deletion
- ✅ Delete object with datasets → Should move object and all datasets
- ✅ Delete object with descendants (checkbox checked) → Should move descendants recursively
- ✅ Delete object with descendants (checkbox unchecked) → Should move only object and direct datasets
- ✅ Dialog should show list of attached datasets

#### Dataset Deletion
- ✅ Delete dataset → Should move to trashcan
- ✅ Delete dataset with descendants (checkbox checked) → Should move descendants recursively
- ✅ Delete dataset with descendants (checkbox unchecked) → Should move only dataset

### Integration Tests

```typescript
describe('Delete Flow', () => {
  it('should prevent deletion of non-empty space', async () => {
    const space = await loadSpace('SPACE_WITH_PROJECTS');
    await expect(deleteSpace(space)).rejects.toThrow('not empty');
  });
  
  it('should move entities to trashcan for non-empty project', async () => {
    const project = await loadProject('PROJECT_WITH_EXPERIMENTS');
    const result = await deleteProject(project);
    expect(result.skipped).toBe(true);
    expect(result.message).toContain('moved');
  });
});
```

---

## Common Patterns

### Pattern 1: Always Move Dependencies

**Used by:** Collection, Object (for datasets)

```typescript
// Move dependencies first
if (dependentEntities.datasets?.length > 0) {
  await this.moveDatasetsToTrashcan(dependentEntities.datasets, reason);
}

// Then move main entity
await this.moveEntityToTrashcan(entityId, reason);
```

### Pattern 2: Conditional Behavior Based on Emptiness

**Used by:** Project

```typescript
const isEmpty = !hasDependencies(dependentEntities);

if (!isEmpty) {
  // Move dependencies, skip main entity
  await this.moveDependenciesToTrashcan(dependentEntities, reason);
  return { skipped: true, message: '...' };
} else {
  // Move main entity
  await this.moveEntityToTrashcan(entityId, reason);
}
```

### Pattern 3: Optional Descendants

**Used by:** Object, Dataset

```typescript
if (context?.includeDescendants) {
  await this.moveDescendantsToTrashcan(dependentEntities, reason);
}

await this.moveEntityToTrashcan(entityId, reason);
```

---

## Troubleshooting

### Issue: Delete dialog shows wrong dependency count

**Cause:** Dependency structure mismatch between entity type and normalization.

**Solution:** Check `normalizeDependentEntities()` handles your entity type correctly.

### Issue: Entities not moving to trashcan

**Cause:** Missing `setReason()` on DeletionOptions.

**Solution:** Always set reason: `deletionOptions.setReason(reason)`

### Issue: Form closes when it shouldn't

**Cause:** Not returning `{ skipped: true }` for non-empty project deletion.

**Solution:** Return `{ skipped: true, message: '...' }` when only moving dependencies.

### Issue: Descendants checkbox not showing

**Cause:** Entity type not handled in `renderDescendantsCheckbox()`.

**Solution:** Add entity type check: `entityKind === EntityKind.YOUR_ENTITY`

---

## Future Enhancements

Potential improvements:

1. **Bulk Delete**: Support deleting multiple entities at once
2. **Delete Preview**: Show full tree of what will be deleted before confirmation
3. **Delete Scheduling**: Schedule deletions for later execution
4. **Delete Templates**: Pre-defined delete reasons
5. **Delete History**: Track deletion history per entity

---

## Related Documentation

- [Form Engine Architecture](../engine/README.md)
- [Entity Form Controllers](../README.md)
- [Trashcan Management](../../../tools/form/trashcan/README.md)

---

## Changelog

### Version 1.0 (Current)
- ✅ Space deletion with emptiness validation
- ✅ Project deletion with automatic entity movement
- ✅ Collection deletion with detailed messaging
- ✅ Object deletion with datasets and optional descendants
- ✅ Dataset deletion with optional descendants
- ✅ Success toast notifications
- ✅ Dependency structure normalization
- ✅ Trashcan integration

---

## Contact & Support

For questions or issues with the delete feature:
- Check existing issues in the codebase
- Review this documentation
- Consult the openBIS API documentation for deletion APIs

