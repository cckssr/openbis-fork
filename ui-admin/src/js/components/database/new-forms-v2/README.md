# New Forms V2 - Three-Layer Architecture

A modern, React-based form engine that provides a configuration-driven approach for openBIS entity forms while maintaining the flexibility of code-first development.

## Architecture Overview

The system uses a **three-layer architecture** that provides perfect separation of concerns:

```
FormDispatcher → EntityFormRenderer → Controller.init() + Zustand.init() → useFormEngine
```

### 1. **FormDispatcher (Router Layer)**
Routes to appropriate entity-specific renderers based on entity type.

### 2. **EntityFormRenderer (Entity Layer)**
Each entity type gets its own dedicated renderer component that handles entity-specific initialization and UI.

### 3. **Controller + Zustand (Data Layer)**
Controllers handle entity-specific business logic, while Zustand manages form state.

### 4. **useFormEngine (Form Layer)**
Simplified hook that works with pre-initialized data from entity renderers.

## Key Benefits

- **Perfect Separation of Concerns**: Each layer has a single responsibility
- **Entity-Specific Customization**: Each entity can have different initialization flows and UI
- **Better Performance**: Pre-initialized data and cached metadata
- **Enhanced Maintainability**: Clear file structure and easier testing
- **Better User Experience**: Entity-specific loading states and error handling

## Quick Start

```tsx
import { FormDispatcher } from './core/FormDispatcher';
import { FormMode } from './core/types';

// Use the FormDispatcher to render any entity form
<FormDispatcher
  entityType="SPACE"
  entityId="MY_SPACE"
  mode={FormMode.EDIT}
  user={user}
  openbisFacade={openbisFacade}
  onSave={handleSave}
  onCancel={handleCancel}
  onDelete={handleDelete}
/>
```

## File Structure

```
new-forms-v2/
├── core/
│   ├── FormDispatcher.tsx        # Router layer
│   ├── useFormEngine.ts          # Simplified hook
│   ├── stores/
│   │   └── formStore.ts          # Enhanced Zustand store
│   └── types/
│       └── index.ts              # Core types
├── entities/
│   ├── base/
│   │   ├── BaseFormModel.ts
│   │   └── BaseFormController.ts
│   ├── space/
│   │   ├── SpaceFormRenderer.tsx # Space-specific renderer
│   │   ├── SpaceFormModel.ts
│   │   ├── SpaceFormController.ts
│   │   └── components/
│   │       ├── SpaceErrorDisplay.tsx
│   │       ├── SpaceLoadingSpinner.tsx
│   │       ├── SpaceFormHeader.tsx
│   │       └── SpaceFormFooter.tsx
│   └── [other entities...]
├── components/
│   ├── FormEngine.tsx            # Simplified main component
│   └── common/
│       └── UnsupportedEntityRenderer.tsx
└── index.ts                      # Main entry point
```

## Implementation Status

### ✅ Completed
- [x] Core architecture and types
- [x] FormDispatcher router component
- [x] Enhanced Zustand store with entity-specific state
- [x] Base classes for controllers and models
- [x] Space entity implementation (pilot)
- [x] Simplified useFormEngine hook
- [x] FormEngine component
- [x] Entity-specific UI components

### 🚧 In Progress
- [ ] Project entity implementation
- [ ] Collection entity implementation
- [ ] Dataset entity implementation
- [ ] Widget system
- [ ] Field registration system
- [ ] Configuration system

### 📋 Planned
- [ ] Performance optimizations
- [ ] Advanced validation rules
- [ ] A/B testing support
- [ ] Migration tools

## Usage Examples

See `example-usage.tsx` for comprehensive usage examples.

## Migration from Legacy Forms

The new system is designed to run alongside the existing form system. Migration can be done entity by entity:

1. Implement the new entity renderer
2. Test with existing data
3. Gradually replace legacy forms
4. Remove legacy code

## Contributing

When adding new entities:

1. Create entity-specific renderer in `entities/[entity]/`
2. Implement controller with `init()` method
3. Create form model with schema
4. Add entity-specific UI components
5. Update FormDispatcher routing
6. Add tests and documentation

## License

This project is part of the openBIS system.
