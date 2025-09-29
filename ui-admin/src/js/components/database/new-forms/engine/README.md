# Simple Component Factory Pattern

This approach completely eliminates the registry pattern and uses a simple factory to create components directly.

## How it works

1. **No Registration**: Components are created on-demand using a factory
2. **Direct Imports**: All components are imported directly in the factory
3. **Simple Switch Statements**: Easy to understand and maintain
4. **No Startup Overhead**: No components are loaded until needed

## Adding New Components

### 1. Add Controller
```typescript
// In ComponentFactory.ts
import { NewEntityFormController } from '@src/js/components/database/new-forms/entities/NewEntity/NewEntityFormController.ts';

// Add to createController method
case EntityKind.NEW_ENTITY:
  return new NewEntityFormController(openbisFacade);
```

### 2. Add Field Renderer
```typescript
// In ComponentFactory.ts
import { NewFieldRenderer } from '@src/js/components/database/new-forms/components/fields/NewFieldRenderer.tsx';

// Add to createFieldRenderer method
case FormFieldDataType.NEW_TYPE:
  return NewFieldRenderer;
```

### 3. Add Action Handler
```typescript
// In ComponentFactory.ts
import { NewEntityFormModel } from '@src/js/components/database/new-forms/entities/NewEntity/NewEntityFormModel.ts';

// Add to getActionHandler method
case 'new-entity:save':
  return NewEntityFormModel.saveNewEntityAction;
```

### 4. Add Action Renderer
```typescript
// In ComponentFactory.ts
import { NewActionRenderer } from '@src/js/components/database/new-forms/components/fields/NewActionRenderer.tsx';

// Add to createActionRenderer method
case 'new-component':
  return NewActionRenderer;
```

## Benefits

- ✅ **No Registration**: No need to register components at startup
- ✅ **Simple**: Easy to understand and maintain
- ✅ **Fast**: No startup overhead
- ✅ **Type Safe**: Full TypeScript support
- ✅ **Easy to Extend**: Just add new cases to switch statements

## Usage

```typescript
// Create controller
const controller = ComponentFactory.createController(EntityKind.PROJECT, openbisFacade, user);

// Create field renderer
const fieldRenderer = ComponentFactory.createFieldRenderer(FormFieldDataType.VARCHAR);

// Create action handler
const actionHandler = ComponentFactory.getActionHandler('project:save');

// Create action renderer
const actionRenderer = ComponentFactory.createActionRenderer('button');
```

That's it! No complex registries, no dynamic imports, no over-engineering. Just simple, direct component creation.
