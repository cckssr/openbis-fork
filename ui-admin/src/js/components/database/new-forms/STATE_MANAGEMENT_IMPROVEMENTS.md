# State Management Improvements for EntityFormContextProvider

## Current Problems

### 1. **Too Many useState Calls (10+ separate states)**
```typescript
// Current approach - scattered state
const [loading, setLoading] = useState(false);
const [saving, setSaving] = useState(false);
const [error, setError] = useState<any>(null);
const [showConflictDialog, setShowConflictDialog] = useState(false);
const [conflictFields, setConflictFields] = useState<any[]>([]);
const [conflictResolutionActive, setConflictResolutionActive] = useState(false);
const [showDeleteDialog, setShowDeleteDialog] = useState(false);
const [deleteDialogConfig, setDeleteDialogConfig] = useState<any>(null);
const [showMoveDialog, setShowMoveDialog] = useState(false);
const [moveInfo, setMoveInfo] = useState<any>(null);
```

**Issues:**
- Hard to see relationships between related states
- Easy to forget to update related states together
- Difficult to test in isolation
- No clear grouping of concerns

### 2. **Mixed Concerns**
- Async operation state (loading/saving/error) mixed with UI state (dialogs)
- Business logic state (permissions, conflict resolution) mixed with presentation state

### 3. **Interdependent State Updates**
- Multiple state updates needed for simple operations (e.g., opening a dialog)
- Risk of inconsistent state if updates are not atomic

## Solution Options

### Option 1: Grouped State with Custom Hooks (RECOMMENDED) ⭐

**Pros:**
- ✅ Better organization and clarity
- ✅ Reusable across components
- ✅ Easier to test
- ✅ Type-safe with TypeScript
- ✅ Maintains useState simplicity (no learning curve)
- ✅ Gradual migration possible

**Cons:**
- ⚠️ Still uses useState under the hood (but organized)
- ⚠️ Requires creating custom hooks

**Example:**
```typescript
// Grouped dialog state
const { dialogs, openConflictDialog, closeConflictDialog } = useDialogState();

// Grouped operation state
const { operationState, setLoading, executeOperation } = useOperationState();
```

### Option 2: useReducer Pattern

**Pros:**
- ✅ Single source of truth
- ✅ Predictable state updates
- ✅ Better for complex state machines
- ✅ Easier to debug (action log)

**Cons:**
- ⚠️ More boilerplate
- ⚠️ Learning curve for team
- ⚠️ Can be overkill for simpler cases

**Example:**
```typescript
type State = {
  loading: boolean;
  saving: boolean;
  error: any | null;
  dialogs: DialogState;
  // ...
};

type Action = 
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_SAVING'; payload: boolean }
  | { type: 'OPEN_CONFLICT_DIALOG'; payload: any[] }
  // ...

const [state, dispatch] = useReducer(formReducer, initialState);
```

### Option 3: State Machine (XState or similar)

**Pros:**
- ✅ Explicit state transitions
- ✅ Great for complex workflows
- ✅ Visualizable state diagrams
- ✅ Built-in guard conditions

**Cons:**
- ⚠️ Additional dependency
- ⚠️ Steeper learning curve
- ⚠️ Might be overkill for this use case

### Option 4: Zustand (already in dependencies)

**Pros:**
- ✅ Simple API
- ✅ Good TypeScript support
- ✅ Minimal boilerplate
- ✅ Already in dependencies

**Cons:**
- ⚠️ Adds external state management
- ⚠️ Might be overkill for component-level state

## Recommended Approach: Custom Hooks Pattern

### Benefits

1. **Separation of Concerns**
   - `useDialogState` - All dialog-related state
   - `useOperationState` - All async operation state
   - `useFormState` - Already exists for form state

2. **Better Maintainability**
   - Related state grouped together
   - Clear API for state updates
   - Easy to locate and modify

3. **Testability**
   - Each hook can be tested independently
   - Mock easier in tests
   - Clear boundaries

4. **Type Safety**
   - TypeScript interfaces for all state shapes
   - Compile-time error checking

### Migration Path

1. ✅ Create `useDialogState` hook (DONE)
2. ✅ Create `useOperationState` hook (DONE)
3. ⏳ Refactor `EntityFormContextProvider` to use new hooks
4. ⏳ Update tests
5. ⏳ Consider extracting other hooks if needed

### Before vs After Comparison

#### Before:
```typescript
// 10+ separate useState calls
const [loading, setLoading] = useState(false);
const [saving, setSaving] = useState(false);
const [error, setError] = useState<any>(null);
const [showConflictDialog, setShowConflictDialog] = useState(false);
const [conflictFields, setConflictFields] = useState<any[]>([]);
// ... more

// Complex update logic
const handleSave = async () => {
  setSaving(true);
  setError(null);
  try {
    // ...
  } catch (e) {
    setError(e);
  } finally {
    setSaving(false);
  }
};
```

#### After:
```typescript
// Organized hooks
const { operationState, executeOperation } = useOperationState();
const { dialogs, openConflictDialog, closeConflictDialog } = useDialogState();

// Cleaner update logic
const handleSave = async () => {
  await executeOperation(
    async () => {
      // save logic
    },
    { setSaving: true }
  );
};
```

## Questions to Consider

1. **Do we need to support multiple dialogs open at once?**
   - Current implementation seems to allow only one at a time
   - If yes, we might need a dialog queue

2. **Should we extract permissions state into its own hook?**
   - Currently just one useState
   - Might grow in complexity

3. **Do we want optimistic updates for saves?**
   - Could add to `useOperationState`

4. **Should we add undo/redo functionality?**
   - Would require more sophisticated state management

5. **Performance considerations?**
   - Are there performance issues with current approach?
   - If not, grouped state should be sufficient

## Next Steps

1. Review this document with the team
2. Decide on approach (recommend Option 1)
3. Refactor EntityFormContextProvider
4. Update tests
5. Document patterns for future components

