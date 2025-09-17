# New Forms V2 - Design Document

## Overview

New Forms V2 is a modern, React-based form engine that provides a configuration-driven approach for openBIS entity forms while maintaining the flexibility of code-first development. The system uses a **three-layer architecture** with Zustand for state management, lazy field registration, and a plugin architecture for widgets.

## Core Architecture

### **Three-Layer Architecture Flow:**
```
FormDispatcher → EntityFormRenderer → Controller.init() + Zustand.init() → useFormEngine
```

This architecture provides perfect separation of concerns with entity-specific customization while maintaining centralized state management.

### 1. **FormDispatcher (Router Layer)**
Routes to appropriate entity-specific renderers based on entity type:

```typescript
interface FormDispatcherProps {
  entityType: string;
  entityId: string;
  mode: FormMode;
  user: any;
  openbisFacade: any;
  onSave?: (result: any) => void;
  onCancel?: () => void;
  onDelete?: (entityId: string) => void;
}

export const FormDispatcher: React.FC<FormDispatcherProps> = (props) => {
  switch (props.entityType.toUpperCase()) {
    case 'SPACE':
      return <SpaceFormRenderer {...props} />;
    case 'PROJECT':
    case 'NEWPROJECT':
      return <ProjectFormRenderer {...props} />;
    case 'COLLECTION':
      return <CollectionFormRenderer {...props} />;
    case 'DATASET':
      return <DatasetFormRenderer {...props} />;
    default:
      return <UnsupportedEntityRenderer entityType={props.entityType} />;
  }
};
```

### 2. **Entity-Specific Form Renderers**
Each entity type gets its own dedicated renderer component that handles entity-specific initialization and UI:

```typescript
export const SpaceFormRenderer: React.FC<SpaceFormRendererProps> = (props) => {
  const [formId] = useState(() => `space_${props.entityId}_${Date.now()}`);
  const [isInitialized, setIsInitialized] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const initializeSpaceForm = async () => {
      try {
        // 1. Create Space controller
        const controller = new SpaceFormController(props.openbisFacade, props.user);
        
        // 2. Initialize controller with Space-specific logic
        await controller.init(props.entityId, props.mode);
        
        // 3. Load Space data
        const data = await controller.load(props.entityId, props.mode);
        
        // 4. Get Space form model and schema
        const formModel = new SpaceFormModel();
        const schema = await formModel.getSchema({...});
        
        // 5. Initialize Zustand store with Space-specific data
        createEntityForm({ formId, entityType: 'SPACE', ... });
        
        setIsInitialized(true);
      } catch (err) {
        setError(err.message);
      }
    };
    initializeSpaceForm();
  }, [formId, props.entityId, props.mode]);

  if (error) return <SpaceErrorDisplay error={error} />;
  if (!isInitialized) return <SpaceLoadingSpinner />;
  
  return <FormEngine formId={formId} {...props} />;
};
```

### 3. **Enhanced Controller with init() Method**
Controllers handle entity-specific initialization and business logic:

```typescript
export class SpaceFormController extends BaseFormController {
  private spaceMetadata: any = null;
  private permissions: Permissions | null = null;
  private isInitialized = false;

  async init(entityId: string, mode: FormMode): Promise<void> {
    if (this.isInitialized) return;

    // 1. Load Space metadata
    this.spaceMetadata = await this.loadMetadata(entityId);
    
    // 2. Check permissions
    this.permissions = await this.checkPermissions(entityId);
    
    // 3. Space-specific initialization
    await this.initializeSpaceSpecificData(entityId, mode);
    
    this.isInitialized = true;
  }

  private async initializeSpaceSpecificData(entityId: string, mode: FormMode): Promise<void> {
    const projects = await this.getChildren(entityId);
    const history = await this.getHistory(entityId);
    
    this.spaceContext = {
      projects,
      history,
      canCreateProjects: this.permissions?.canCreate || false,
    };
  }
}
```

### 4. **Enhanced Zustand Store Structure**
Multi-instance store with entity-specific state and actions:

```typescript
interface FormStore {
  forms: {
    [formId: string]: {
      entityType: string;
      entityId: string;
      mode: FormMode;
      data: FormData;
      schema: FormSchema;
      validation: ValidationState;
      isDirty: boolean;
      isLoading: boolean;
    }
  };
  activeFormId: string | null;
  
  // Entity-specific state
  entityControllers: { [formId: string]: any };
  entityMetadata: { [formId: string]: any };
  entityPermissions: { [formId: string]: Permissions };
  
  // Actions
  createEntityForm: (config: EntityFormConfig) => void;
  setEntityController: (formId: string, controller: any) => void;
  setEntityMetadata: (formId: string, metadata: any) => void;
  setEntityPermissions: (formId: string, permissions: Permissions) => void;
  updateFormData: (formId: string, data: Partial<FormData>) => void;
  validateForm: (formId: string) => ValidationResult;
  saveForm: (formId: string) => Promise<void>;
}
```

### 5. **Simplified useFormEngine Hook**
Works with pre-initialized data from entity renderers:

```typescript
export const useFormEngine = (formId: string, callbacks: FormCallbacks) => {
  const form = useFormStore(state => state.forms[formId]);
  const controller = useFormStore(state => state.entityControllers[formId]);
  const metadata = useFormStore(state => state.entityMetadata[formId]);
  const permissions = useFormStore(state => state.entityPermissions[formId]);
  
  if (!form || !controller) {
    throw new Error(`Form ${formId} not found or not initialized`);
  }

  return {
    form,
    controller,
    metadata,
    permissions,
    // ... form operations using pre-initialized data
  };
};
```

## Key Benefits of Three-Layer Architecture

### **1. Perfect Separation of Concerns**
- **FormDispatcher**: Pure routing logic
- **EntityFormRenderer**: Entity-specific initialization and UI
- **Controller**: Data operations and business logic
- **useFormEngine**: Pure form state management

### **2. Entity-Specific Customization**
- Each entity can have different initialization flows
- Custom error handling per entity type
- Entity-specific UI components and layouts
- Different validation rules per entity

### **3. Better Performance**
- **Pre-initialized data** in controllers
- **Cached metadata** in Zustand store
- **Lazy loading** of entity-specific components
- **Selective re-renders** based on entity type

### **4. Enhanced Maintainability**
- **Clear file structure** per entity type
- **Easier testing** of individual components
- **Simpler debugging** with clear responsibility boundaries
- **Easier feature additions** per entity

### **5. Better User Experience**
- **Entity-specific loading states** and error messages
- **Custom UI** for each entity type
- **Better error handling** with entity context
- **Consistent behavior** within entity types

## Entity System Architecture

### **FormModel Classes**
Each entity type has a FormModel class that:
- Defines base schema structure
- Registers fields lazily when needed
- Handles entity-specific validation rules
- Manages field dependencies and visibility

```typescript
abstract class BaseFormModel {
  abstract entityType: string;
  abstract baseSchema: FormSchema;
  
  // Lazy field registration
  abstract registerFields(): Promise<FieldRegistry>;
  
  // Schema enhancement
  abstract enhanceSchema(baseSchema: FormSchema, context: FormContext): FormSchema;
  
  // Entity-specific validation
  abstract validate(data: FormData): ValidationResult;
}

class SpaceFormModel extends BaseFormModel {
  entityType = 'SPACE';
  baseSchema = { /* base space schema */ };
  
  async registerFields() {
    // Lazy load and register space-specific fields
  }
}
```

### **Enhanced Controller Classes**
Controllers handle entity-specific initialization and business logic:

```typescript
abstract class BaseFormController {
  protected isInitialized = false;
  
  // Entity-specific initialization
  abstract init(entityId: string, mode: FormMode): Promise<void>;
  
  // Core operations
  abstract load(entityId: string): Promise<FormData>;
  abstract save(data: FormData): Promise<void>;
  abstract checkPermissions(entityId: string): Promise<Permissions>;
  abstract delete(entityId: string): Promise<void>;
  
  // Entity-specific metadata
  abstract loadMetadata(entityId: string): Promise<any>;
  abstract getChildren(entityId: string): Promise<any[]>;
  abstract getHistory(entityId: string): Promise<any[]>;
}
```

### 4. **Field Registration System**

#### Lazy Field Registration
Fields are registered per entity to avoid loading unused components:

```typescript
class SpaceFieldRegistry {
  private static registered = false;
  
  static async registerFields() {
    if (this.registered) return;
    
    // Register space-specific fields
    FieldRegistry.register('space-code', SpaceCodeField);
    FieldRegistry.register('space-description', SpaceDescriptionField);
    // ... other space fields
    
    this.registered = true;
  }
}
```

#### Field Component Reuse
- Reuse existing field components from `new-forms/components/fields/`
- Create new components only when needed
- Wrapper components to adapt existing components to new architecture

### 5. **Widget Plugin Architecture**

#### Widget System
Specialized widgets loaded on demand:

```typescript
interface WidgetPlugin {
  id: string;
  component: React.ComponentType<WidgetProps>;
  dependencies?: string[];
  loadCondition?: (formContext: FormContext) => boolean;
}

class WidgetRegistry {
  private static widgets = new Map<string, WidgetPlugin>();
  
  static register(plugin: WidgetPlugin) {
    this.widgets.set(plugin.id, plugin);
  }
  
  static async loadWidget(id: string, context: FormContext) {
    const plugin = this.widgets.get(id);
    if (plugin?.loadCondition?.(context)) {
      return plugin.component;
    }
    return null;
  }
}
```

#### Widget Integration
Widgets are integrated into forms through configuration:

```typescript
const spaceFormConfig = {
  entityType: 'SPACE',
  widgets: [
    { id: 'comments', permissions: ['READ'] },
    { id: 'storage', permissions: ['WRITE'] }
  ]
};
```

### 6. **Configuration System**

#### Hybrid Configuration Approach
- **Base schemas**: JSON files defining core structure
- **FormModel enhancement**: Code-based customization
- **Runtime configuration**: Dynamic field visibility and validation

```typescript
// Base schema (JSON)
{
  "entityType": "SPACE",
  "sections": [
    {
      "id": "identification",
      "title": "Identification",
      "fields": ["code", "description"]
    }
  ]
}

// FormModel enhancement
class SpaceFormModel {
  enhanceSchema(baseSchema: FormSchema, context: FormContext) {
    return {
      ...baseSchema,
      fields: {
        ...baseSchema.fields,
        // Add dynamic fields based on context
        ...this.getDynamicFields(context)
      }
    };
  }
}
```

## Implementation Plan

### Phase 1: Core Foundation
1. **FormDispatcher Setup**
   - Router component for entity type routing
   - Entity-specific renderer routing logic
   - Error handling for unsupported entities

2. **Enhanced Zustand Store**
   - Multi-form store with entity-specific state
   - Entity controllers, metadata, and permissions storage
   - Enhanced CRUD operations with entity context

3. **Base Classes Enhancement**
   - `BaseFormController` with `init()` method
   - `BaseFormModel` with entity-specific schema handling
   - `BaseFieldRegistry` utility class

### Phase 2: Entity Implementation
1. **Space Entity (Pilot)**
   - `SpaceFormRenderer` with entity-specific initialization
   - `SpaceFormController` with enhanced `init()` method
   - `SpaceFormModel` with lazy field registration
   - Space-specific field components and error handling

2. **Field System**
   - Reuse existing field components from `new-forms`
   - Create wrapper components for new architecture
   - Lazy loading implementation per entity

### Phase 3: Additional Entities
1. **Project Entity**
   - `ProjectFormRenderer` with project-specific initialization
   - `ProjectFormController` with project business logic
   - Project-specific UI components and validation

2. **Collection & Dataset Entities**
   - Similar pattern for collection and dataset entities
   - Entity-specific error handling and loading states
   - Custom UI components per entity type

### Phase 4: Widget System
1. **Widget Registry**
   - Plugin architecture for entity-specific widgets
   - Lazy loading system with entity context
   - Widget communication and state management

2. **Core Widgets**
   - Comments widget with entity context
   - Storage widget with entity-specific logic
   - Entity-specific widget components

### Phase 5: Advanced Features
1. **Configuration System**
   - JSON schema loading per entity
   - Runtime configuration with entity context
   - A/B testing support per entity type

2. **Performance Optimizations**
   - Entity-specific memoization
   - Virtual scrolling per entity
   - Code splitting with entity boundaries

## File Structure

```
new-forms-v2/
├── core/
│   ├── FormDispatcher.tsx        # Router layer
│   ├── useFormEngine.ts          # Simplified hook
│   ├── stores/
│   │   └── formStore.ts          # Enhanced Zustand store
│   ├── types/
│   │   └── index.ts              # Core types
│   └── utils/
│       └── fieldRegistry.ts      # Field registration utilities
├── entities/
│   ├── base/
│   │   ├── BaseFormModel.ts
│   │   └── BaseFormController.ts
│   ├── space/
│   │   ├── SpaceFormRenderer.tsx # Space-specific renderer
│   │   ├── SpaceFormModel.ts
│   │   ├── SpaceFormController.ts # Enhanced with init()
│   │   └── fields/
│   │       └── SpaceFieldRegistry.ts
│   ├── project/
│   │   ├── ProjectFormRenderer.tsx
│   │   ├── ProjectFormModel.ts
│   │   ├── ProjectFormController.ts
│   │   └── fields/
│   │       └── ProjectFieldRegistry.ts
│   ├── collection/
│   │   ├── CollectionFormRenderer.tsx
│   │   ├── CollectionFormModel.ts
│   │   ├── CollectionFormController.ts
│   │   └── fields/
│   │       └── CollectionFieldRegistry.ts
│   └── dataset/
│       ├── DatasetFormRenderer.tsx
│       ├── DatasetFormModel.ts
│       ├── DatasetFormController.ts
│       └── fields/
│           └── DatasetFieldRegistry.ts
├── widgets/
│   ├── registry/
│   │   └── WidgetRegistry.ts
│   ├── comments/
│   │   └── CommentsWidget.tsx
│   └── storage/
│       └── StorageWidget.tsx
├── components/
│   ├── FormEngine.tsx            # Simplified main component
│   ├── fields/
│   │   └── [reused from new-forms]
│   ├── widgets/
│   │   └── [widget components]
│   └── common/
│       ├── UnsupportedEntityRenderer.tsx
│       ├── EntityErrorDisplay.tsx
│       └── EntityLoadingSpinner.tsx
├── configs/
│   ├── schemas/
│   │   ├── space.json
│   │   ├── project.json
│   │   ├── collection.json
│   │   └── dataset.json
│   └── widgets/
│       └── [widget configurations]
└── hooks/
    ├── useFormValidation.ts
    └── useFormPermissions.ts
```

## Key Benefits

1. **Performance**: Lazy loading of fields and widgets
2. **Maintainability**: Clear separation of concerns
3. **Flexibility**: Hybrid configuration approach
4. **Scalability**: Plugin architecture for easy extension
5. **Reusability**: Field component reuse and sharing
6. **State Management**: Robust multi-form state handling

## Migration Strategy

- **Parallel Implementation**: New system runs alongside existing
- **Gradual Migration**: Entity by entity migration
- **Shared Components**: Reuse existing field components
- **API Compatibility**: Maintain similar interfaces where possible

This design provides a solid foundation for a modern, performant, and maintainable form engine while preserving the flexibility to adapt to specific openBIS requirements.
