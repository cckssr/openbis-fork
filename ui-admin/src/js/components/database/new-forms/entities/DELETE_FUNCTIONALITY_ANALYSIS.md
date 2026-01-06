# DELETE Functionality Analysis

## Executive Summary

### Current Status
- ✅ **Space**: Partially implemented - deletes but missing emptiness validation
- ⚠️ **Project**: Partially implemented - deletes but wrong behavior for non-empty projects
- ❌ **Collection**: Not implemented - stub method only
- ❌ **Object**: Not implemented - stub method only  
- ❌ **Dataset**: Not implemented - stub method only

### Critical Issues Found
1. **Dependency Structure Mismatch**: `useDeleteFlow` expects `{experiments, samples}` but entities return different structures
2. **Space Validation Missing**: No check if space is empty before deletion
3. **Project Logic Wrong**: Non-empty projects should move entities to trashcan, not delete them
4. **Incomplete Implementations**: Collection/Object/Dataset delete methods are stubs
5. **Missing UI Options**: No checkboxes for descendants (Object/Dataset), no choice for Project behavior

### Key Finding
✅ **Trashcan API Clarified**: Deletion with reason automatically moves to trashcan. Current API calls are correct, but logic around when to allow deletion needs fixing.

---

## Current Implementation Flow

### High-Level Delete Flow

```
User clicks Delete button
  ↓
EntityForm.onAction('delete')
  ↓
EntityFormContextProvider.handleAction('delete')
  ↓
handleDeleteWithDependencyCheck() [from useDeleteFlow hook]
  ↓
1. controller.delete(form, { checkOnly: true }) - Pre-check
2. controller.getDependentEntities(form) - Get dependencies
3. openDeleteDialog(config) - Show confirmation dialog
  ↓
User enters reason and confirms
  ↓
handleDeleteConfirm(reason) [from useDeleteFlow hook]
  ↓
controller.delete(form, context) - Actual deletion
  ↓
externalAppController.closeForm() - Close form
```

### Detailed Flow Breakdown

#### 1. **Action Trigger** (`EntityFormContextProvider.tsx:194-224`)
- Delete action is intercepted in `handleAction()` method
- Routes to `handleDeleteWithDependencyCheck()` instead of using ActionHandlerDispatcher
- **Note**: The `CoreFormModel.deleteAction` exists but is NOT used for delete flow

#### 2. **Dependency Check** (`useDeleteFlow.ts:43-81`)
- Calls `controller.delete(form, { checkOnly: true })` for pre-validation
- Calls `controller.getDependentEntities(form)` to get dependent entities
- Calculates `bypassesTrashcan` based on `totalDependentEntities === 0`
- Opens delete dialog with configuration

#### 3. **Delete Dialog** (`DeleteConfirmationDialog.tsx`)
- Shows warning if dependent entities exist
- Requires delete reason (if `includeReason: true`)
- Displays info about trashcan vs immediate deletion
- Shows count of dependent entities

#### 4. **Actual Deletion** (`useDeleteFlow.ts:83-114`)
- Calls `controller.delete(form, context)` with extended context
- Context includes: `deleteReason`, `dependentEntities`, `form`, `controller`, etc.
- Closes form after successful deletion

---

## Entity-Specific Implementation Status

### ✅ **Space** (`SpaceFormController.ts`)

**Current Implementation:**
- ✅ `delete()` method implemented (lines 73-89)
- ✅ Uses `SpaceDeletionOptions` with reason
- ✅ Calls `openbisFacade.deleteSpaces()`
- ✅ `getDependentEntities()` checks for projects and samples (lines 91-106)
- ⚠️ `checkOnly` mode returns early without validation (lines 74-79)
- ⚠️ **CRITICAL**: No validation that space is empty before deletion
- ⚠️ **CRITICAL**: No warning shown if space has dependent entities
- ⚠️ **CRITICAL**: Uses hardcoded reason `'delete via ng-ui'` instead of context reason (line 85)

**What's Missing:**
- ❌ Validation that space is empty before allowing deletion
- ❌ Warning dialog when space is not empty (should prevent deletion)
- ❌ Proper error handling for non-empty spaces

**Specification Requirements:**
- ✅ Delete space immediately (uses `deleteSpaces` API)
- ❌ **MISSING**: Check if space is empty
- ❌ **MISSING**: Show warning if not empty and prevent deletion

---

### ⚠️ **Project** (`ProjectFormController.ts`)

**Current Implementation:**
- ✅ `delete()` method implemented (lines 71-121)
- ✅ Checks for existing deletions in trashcan (lines 79-83)
- ✅ `getDependentEntities()` checks for experiments and samples (lines 123-133)
- ✅ Deletes dependent entities first (lines 102-104, 182-213)
- ✅ Uses `ProjectDeletionOptions` with reason
- ✅ Calls `openbisFacade.deleteProjects()`
- ⚠️ **CRITICAL**: Always deletes dependent entities, no option to move to trashcan
- ⚠️ **CRITICAL**: No distinction between empty vs non-empty project behavior

**What's Missing:**
- ❌ Logic to determine if project is empty
- ❌ Different behavior for empty vs non-empty projects:
  - Empty: Delete immediately (current behavior)
  - Non-empty: Move all entities to trashcan (NOT implemented)
- ❌ Warning dialog when project is not empty
- ❌ Option/UI to choose between immediate delete vs move to trashcan

**Specification Requirements:**
- ⚠️ **PARTIAL**: Empty project deletion works (but no explicit check)
- ❌ **MISSING**: Non-empty project should move entities to trashcan (currently deletes them)
- ❌ **MISSING**: Warning if project is not empty

---

### ❌ **Collection** (`CollectionFormController.ts`)

**Current Implementation:**
- ❌ `delete()` method is a stub (line 91-93) - only logs, doesn't delete
- ✅ `getDependentEntities()` checks for samples and datasets (lines 95-109)
- ❌ **CRITICAL**: No actual deletion implementation

**What's Missing:**
- ❌ Complete `delete()` implementation
- ❌ Move collection and all objects/datasets to trashcan
- ❌ Proper handling of dependent entities

**Specification Requirements:**
- ❌ **MISSING**: Move collection to trashcan
- ❌ **MISSING**: Move all objects to trashcan
- ❌ **MISSING**: Move all datasets to trashcan

---

### ❌ **Object** (`ObjectFormController.ts`)

**Current Implementation:**
- ❌ `delete()` method is a stub (line 152-154) - only logs, doesn't delete
- ✅ `getDependentEntities()` checks for datasets and children (lines 156-170)
- ❌ **CRITICAL**: No actual deletion implementation
- ❌ **CRITICAL**: No checkbox option for descendants

**What's Missing:**
- ❌ Complete `delete()` implementation
- ❌ Move object to trashcan
- ❌ Move all datasets to trashcan
- ❌ Checkbox option to trash descendant objects and datasets
- ❌ UI in delete dialog for the descendants checkbox

**Specification Requirements:**
- ❌ **MISSING**: Move object to trashcan
- ❌ **MISSING**: Move all datasets to trashcan
- ❌ **MISSING**: Optional checkbox to trash descendant objects and datasets

---

### ❌ **Dataset** (`DatasetFormController.ts`)

**Current Implementation:**
- ❌ `delete()` method is a stub (line 94-128) - only has commented legacy code
- ✅ `getDependentEntities()` returns empty (lines 130-136) - datasets have no dependents
- ❌ **CRITICAL**: No actual deletion implementation
- ❌ **CRITICAL**: No checkbox option for descendant datasets

**What's Missing:**
- ❌ Complete `delete()` implementation
- ❌ Move dataset to trashcan
- ❌ Checkbox option to trash descendant datasets
- ❌ UI in delete dialog for the descendants checkbox

**Specification Requirements:**
- ❌ **MISSING**: Move dataset to trashcan
- ❌ **MISSING**: Optional checkbox to trash descendant datasets

---

## Critical Implementation Issues

### 1. **Inconsistent Delete Behavior**
- **Space**: Deletes immediately but doesn't validate emptiness
- **Project**: Deletes immediately and also deletes dependents (should move to trashcan for non-empty)
- **Collection/Object/Dataset**: Not implemented at all

### 2. **Trashcan Logic Understanding** ✅ **CLARIFIED**

**Important Finding**: Based on codebase analysis (`TrashcanFormFacade.js`, `ProjectFormController.ts`):

- **Deletion with reason = Move to Trashcan**: When you call `deleteSpaces()`, `deleteProjects()`, `deleteExperiments()`, `deleteSamples()`, `deleteDataSets()` with a `DeletionOptions` that includes a reason, the entities are **automatically moved to trashcan** (not immediately deleted).

- **Permanent Deletion**: To permanently delete from trashcan, you use:
  ```javascript
  const confirmOperation = new openbis.ConfirmDeletionsOperation(deletionIds);
  confirmOperation.setForceDeletionOfDependentDeletions(includeDependent);
  await openbis.executeOperations([confirmOperation], ...);
  ```

- **Revert Deletion**: To restore from trashcan:
  ```javascript
  await openbis.revertDeletions(deletionIds);
  ```

**Current Implementation Issue**:
- The `bypassesTrashcan` flag in `useDeleteFlow.ts:66` is **incorrectly calculated**
- It's set to `true` when `totalDependentEntities === 0`, implying immediate deletion
- **Reality**: All deletions with a reason go to trashcan, regardless of dependencies
- The flag should probably indicate whether the entity can be deleted immediately (empty) vs needs trashcan (has dependents), but the current logic is flawed

**Conclusion**: The current API calls (`deleteSpaces()`, `deleteProjects()`, etc.) are correct for moving to trashcan. The issue is the logic around when to allow deletion vs when to require moving dependents to trashcan first.

### 3. **Delete Dialog Limitations**
- Current dialog (`DeleteConfirmationDialog.tsx`) only shows:
  - Warning about dependent entities
  - Reason input field
  - Info about trashcan vs immediate deletion
- **Missing**: 
  - Checkbox for "trash descendants" (Object, Dataset)
  - Option to choose "move to trashcan" vs "delete immediately" (Project)
  - Better messaging for different entity types

### 4. **Dependency Check Logic Issues**

#### Space (`SpaceFormController.ts:91-106`)
- Returns `{ projects: [], samples: [] }` structure
- But `useDeleteFlow.ts:60-61` only counts `experiments.length + samples.length`
- **Issue**: Space dependencies are `projects` and `samples`, but the flow expects `experiments` and `samples`
- **Result**: Space dependency count is always 0, so `bypassesTrashcan` is always `true`

#### Project (`ProjectFormController.ts:123-133`)
- Returns `{ experiments: [], samples: [] }` structure
- Matches expected structure in `useDeleteFlow.ts`
- ✅ Correct

#### Collection (`CollectionFormController.ts:95-109`)
- Returns `{ samples: [], datasets: [] }` structure
- **Issue**: Flow expects `experiments` and `samples`, but gets `samples` and `datasets`
- **Result**: Dependency count calculation is wrong

#### Object (`ObjectFormController.ts:156-170`)
- Returns `{ datasets: [], children: [] }` structure
- **Issue**: Flow expects `experiments` and `samples`, but gets `datasets` and `children`
- **Result**: Dependency count calculation is wrong

#### Dataset (`DatasetFormController.ts:130-136`)
- Returns `{ datasets: [], samples: [] }` structure
- **Issue**: Flow expects `experiments` and `samples`
- **Result**: Dependency count calculation is wrong

**Critical**: The `useDeleteFlow.ts` assumes a uniform structure `{ experiments: [], samples: [] }` but each entity type returns different structures.

### 5. **Missing Validation Logic**

#### Space
- No check if space is empty before deletion
- Should prevent deletion if `projects.length > 0 || samples.length > 0`

#### Project
- No explicit check if project is empty
- Should distinguish between:
  - Empty project → delete immediately
  - Non-empty project → move to trashcan (not delete dependents)

### 6. **Incomplete Error Handling**
- `SpaceFormController.delete()` doesn't validate emptiness
- `ProjectFormController.delete()` doesn't distinguish empty vs non-empty
- No user-friendly error messages for validation failures

---

## What's Done ✅

1. ✅ Basic delete flow infrastructure (`useDeleteFlow` hook)
2. ✅ Delete confirmation dialog with reason input
3. ✅ Dependency checking framework (`getDependentEntities()`)
4. ✅ Space deletion API integration (but missing validation)
5. ✅ Project deletion API integration (but wrong behavior for non-empty)
6. ✅ Project dependent entity deletion logic
7. ✅ Dialog state management (`useDialogState`)

---

## What's Missing ❌

### Space
- ❌ Empty space validation before deletion
- ❌ Warning dialog when space is not empty
- ❌ Prevention of deletion for non-empty spaces

### Project
- ❌ Empty vs non-empty project detection
- ❌ Move entities to trashcan for non-empty projects (currently deletes them)
- ❌ Warning dialog for non-empty projects
- ❌ UI option to choose behavior

### Collection
- ❌ Complete `delete()` implementation
- ❌ Move collection to trashcan
- ❌ Move all objects to trashcan
- ❌ Move all datasets to trashcan

### Object
- ❌ Complete `delete()` implementation
- ❌ Move object to trashcan
- ❌ Move all datasets to trashcan
- ❌ Checkbox for descendant objects/datasets
- ❌ UI for descendants checkbox in delete dialog

### Dataset
- ❌ Complete `delete()` implementation
- ❌ Move dataset to trashcan
- ❌ Checkbox for descendant datasets
- ❌ UI for descendants checkbox in delete dialog

### General
- ❌ Unified dependency structure across entity types
- ❌ Proper trashcan API integration (need to understand API)
- ❌ Enhanced delete dialog with entity-specific options
- ❌ Better error messages and validation

---

## Questions for Clarification

### 1. **Trashcan API** ✅ **ANSWERED**
- ✅ **ANSWERED**: Deletion with a reason automatically moves to trashcan
- ✅ **ANSWERED**: `deleteSpaces()`, `deleteProjects()`, etc. with `DeletionOptions` (including reason) = move to trashcan
- ✅ **ANSWERED**: Permanent deletion requires `ConfirmDeletionsOperation` from trashcan
- ✅ **ANSWERED**: Revert uses `revertDeletions()` API

### 2. **Project Non-Empty Behavior** ✅ **ANSWERED**
- ✅ **ANSWERED**: All entities should be moved to trashcan automatically
- ✅ **ANSWERED**: Single operation, no user selection needed
- ✅ **ANSWERED**: Automatic behavior (no additional confirmation beyond the delete dialog)

### 3. **Descendants Checkbox (Object/Dataset)** ✅ **ANSWERED**
- ✅ **ANSWERED**: Descendants are all `dependentEntities` that should be displayed
- ✅ **ANSWERED**: Checkbox should NOT be checked by default
- ✅ **ANSWERED**: Should display the dependent entities (count and types)

### 4. **Empty Space/Project Definition**
- Is a Space empty if it has no projects AND no samples?
- Is a Project empty if it has no experiments AND no samples?
- Should we also check for datasets?

### 5. **Dependency Structure**
- Should we standardize `getDependentEntities()` to return a consistent structure?
- Or should `useDeleteFlow` handle different structures per entity type?
- What's the best approach for maintainability?

### 6. **Error Messages** ✅ **ANSWERED**
- ✅ **ANSWERED**: For non-empty Space: Prevent deletion and throw error
- ✅ **ANSWERED**: Should show error message indicating space is not empty

### 7. **Collection/Object/Dataset Deletion**
- For Collection: Should it move collection + all objects + all datasets in one operation?
- For Object: Should descendants checkbox affect both child objects AND their datasets?
- For Dataset: What constitutes "descendant datasets"? Is there a dataset hierarchy?

---

## Recommendations

### 1. **Standardize Dependency Structure**
Create a unified interface for `getDependentEntities()`:
```typescript
interface DependentEntities {
  experiments?: any[];
  samples?: any[];
  datasets?: any[];
  collections?: any[];
  objects?: any[];
}
```

### 2. **Fix useDeleteFlow Logic**
Update `useDeleteFlow.ts` to handle different entity types and their specific dependency structures.

### 3. **Implement Missing Delete Methods**
Complete the `delete()` implementations for Collection, Object, and Dataset.

### 4. **Add Validation Logic**
- Space: Check emptiness before allowing deletion
- Project: Distinguish empty vs non-empty behavior

### 5. **Enhance Delete Dialog**
- Add entity-specific options (checkboxes, choices)
- Better messaging per entity type
- Show counts of entities that will be affected

### 6. **Clarify Trashcan API**
- Research/document how trashcan works in openBIS
- Implement proper trashcan integration
- Test with actual API calls

### 7. **Add Comprehensive Tests**
- Test empty vs non-empty scenarios
- Test dependency checking
- Test trashcan vs immediate deletion
- Test descendant deletion options

---

## Next Steps

1. **Clarify API**: Understand trashcan vs immediate deletion APIs
2. **Fix Critical Bugs**: 
   - Space emptiness validation
   - Project empty vs non-empty logic
   - Dependency structure inconsistencies
3. **Implement Missing Features**:
   - Collection/Object/Dataset deletion
   - Descendants checkbox functionality
   - Enhanced delete dialog
4. **Refactor**: Standardize dependency structures and flow logic
5. **Test**: Comprehensive testing of all scenarios

