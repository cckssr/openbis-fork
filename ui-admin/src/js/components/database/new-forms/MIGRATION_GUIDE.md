# Migration Guide: Refactored Form System

## What Changed

### Before (Overengineered)
- **239 lines** in `EntityFormContextProvider.tsx`
- **12+ state variables** in one component
- **200+ line** action handler with nested if-else
- Mixed concerns (UI + business logic + API calls)
- Hard to test and maintain

### After (Clean Architecture)
- **Focused hooks** for specific concerns
- **Service classes** for business logic
- **Minimal context provider** (orchestrator only)
- **Clear separation** of concerns
- **Easy to test** and maintain

## Migration Steps

### 1. Replace the Context Provider

**Old:**
```typescript
// EntityFormContextProvider.tsx (239 lines)
const EntityFormContextProvider = ({ ... }) => {
  const [form, setForm] = useState<Form | null>(null);
  const [initialForm, setInitialForm] = useState<Form | null>(null);
  const [mode, setMode] = useState<FormMode>(initialMode);
  const [loading, setLoading] = useState(false);
  // ... 8+ more state variables
  
  // 200+ line handleAction function
  const handleAction = useCallback(async (actionName: string) => {
    // Complex nested logic
  }, [/* many dependencies */]);
  
  // ... rest of component
};
```

**New:**
```typescript
// EntityFormContextProvider.refactored.tsx (Clean orchestration)
const EntityFormContextProvider = ({ ... }) => {
  // Focused hooks
  const formState = useFormState({ initialForm: null, initialMode });
  const { loading, loadForm } = useFormLoading({ controller, permId, entityKind, params });
  const { executeAction } = useFormActions({ form, mode, controller, ... });
  const { permissions } = useFormPermissions({ form, controller, user });
  
  // Services
  const formService = useMemo(() => new FormService({ controller, permId, entityKind, params }), []);
  const conflictService = useMemo(() => new ConflictService(), []);
  
  // Clean orchestration
  return (
    <FormContext.Provider value={contextValue}>
      <EntityForm {...props} />
    </FormContext.Provider>
  );
};
```

### 2. Use New Custom Hooks

**Form State Management:**
```typescript
// Old: Multiple useState calls
const [form, setForm] = useState<Form | null>(null);
const [mode, setMode] = useState<FormMode>(initialMode);
const [isDirty, setIsDirty] = useState(false);

// New: Single focused hook
const { form, mode, isDirty, updateField, setMode } = useFormState({
  initialForm: null,
  initialMode
});
```

**Action Handling:**
```typescript
// Old: Complex handleAction with nested logic
const handleAction = useCallback(async (actionName: string) => {
  // 200+ lines of complex logic
}, [/* many dependencies */]);

// New: Clean action execution
const { executeAction } = useFormActions({
  form, mode, controller, permissions, ...
});
```

### 3. Extract Business Logic to Services

**Form Operations:**
```typescript
// FormService.ts
export class FormService {
  async loadForm(): Promise<Form> { }
  async saveForm(form: Form): Promise<Form> { }
  async deleteForm(permId: string): Promise<void> { }
  async checkPermissions(form: Form, user: string): Promise<Record<string, boolean>> { }
}
```

**Conflict Resolution:**
```typescript
// ConflictService.ts
export class ConflictService {
  checkModificationDateConflict(localForm: Form, serverForm: Form): boolean { }
  findConflicts(localForm: Form, serverForm: Form): Conflict[] { }
  resolveConflicts(localForm: Form, serverForm: Form, resolutions: Record<string, string>): Form { }
}
```

## Benefits of Migration

### 1. **Maintainability**
- **Before**: 239-line God component
- **After**: Focused 50-line orchestrator + focused hooks

### 2. **Testability**
- **Before**: Hard to test due to mixed concerns
- **After**: Each hook and service can be tested independently

### 3. **Reusability**
- **Before**: Logic tied to specific component
- **After**: Hooks and services can be reused across components

### 4. **Performance**
- **Before**: Unnecessary re-renders due to complex state
- **After**: Optimized re-renders with focused state

### 5. **Developer Experience**
- **Before**: Hard to understand and modify
- **After**: Clear, self-documenting code

## Testing the Migration

### 1. **Unit Tests for Hooks**
```typescript
// useFormState.test.ts
describe('useFormState', () => {
  it('should update field values', () => {
    const { result } = renderHook(() => useFormState({ initialForm: mockForm, initialMode: FormMode.VIEW }));
    act(() => {
      result.current.updateField('fieldId', 'newValue');
    });
    expect(result.current.form?.fields[0].value).toBe('newValue');
  });
});
```

### 2. **Unit Tests for Services**
```typescript
// FormService.test.ts
describe('FormService', () => {
  it('should validate required fields', () => {
    const service = new FormService(mockConfig);
    const invalidForm = { ...mockForm, fields: [{ ...mockField, required: true, value: '' }] };
    expect(() => service.saveForm(invalidForm)).toThrow('Required fields are missing');
  });
});
```

### 3. **Integration Tests**
```typescript
// EntityFormContextProvider.test.tsx
describe('EntityFormContextProvider', () => {
  it('should load form and handle actions', async () => {
    render(<EntityFormContextProvider {...props} />);
    await waitFor(() => expect(screen.getByText('Form Title')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit'));
    expect(screen.getByText('Save')).toBeInTheDocument();
  });
});
```

## Rollback Plan

If issues arise, you can easily rollback by:
1. Reverting to the original `EntityFormContextProvider.tsx`
2. The new hooks and services can remain for future use
3. No breaking changes to the public API

## Next Steps

1. **Test the refactored components** in a development environment
2. **Migrate gradually** by replacing one hook at a time
3. **Add comprehensive tests** for the new architecture
4. **Update documentation** to reflect the new patterns
5. **Train the team** on the new architecture patterns
