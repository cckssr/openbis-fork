# React Form Engine Design

## Overview

This design outlines a modern, React-based form engine that can dynamically render all openBIS entity forms through a configuration-driven approach. The engine will replace the existing JavaScript MVC forms with a unified, maintainable, and extensible solution.

## Architecture Philosophy

### Configuration-Driven Approach
Instead of hardcoded form components, the engine uses JSON configurations to define:
- Form layouts and sections
- Field types and validation rules
- Toolbar configurations
- Widget integrations
- Permission mappings

### Component Composition
The engine follows React's composition patterns:
- Small, focused components that do one thing well
- Higher-order components for cross-cutting concerns
- Render props and hooks for flexible data sharing
- Context providers for global state management

## Core Architecture

### 1. Form Engine Core

```typescript
interface FormEngineProps {
  entityType: 'SPACE' | 'PROJECT' | 'EXPERIMENT' | 'SAMPLE';
  mode: 'CREATE' | 'EDIT' | 'VIEW';
  entityId?: string;
  configuration: FormConfiguration;
  onSave?: (data: EntityData) => Promise<void>;
  onCancel?: () => void;
}

const FormEngine: React.FC<FormEngineProps> = ({
  entityType,
  mode,
  entityId,
  configuration,
  onSave,
  onCancel
}) => {
  // Core form engine logic
};
```

### 2. Configuration Schema

```typescript
interface FormConfiguration {
  entityType: EntityType;
  sections: FormSection[];
  toolbar: ToolbarConfiguration;
  widgets: WidgetConfiguration[];
  validation: ValidationRules;
  permissions: PermissionConfiguration;
}

interface FormSection {
  id: string;
  title: string;
  collapsible: boolean;
  defaultExpanded: boolean;
  fields: FormField[];
  conditionalDisplay?: ConditionalLogic;
}

interface FormField {
  id: string;
  type: FieldType;
  label: string;
  required: boolean;
  validation?: FieldValidation;
  props?: Record<string, any>;
}
```

### 3. State Management Architecture

```typescript
// Global form state using Zustand or Redux Toolkit
interface FormState {
  // Entity data
  entityData: EntityData;
  originalData: EntityData;
  isDirty: boolean;
  
  // UI state
  mode: FormMode;
  loading: boolean;
  errors: ValidationErrors;
  
  // Permissions
  permissions: PermissionSet;
  
  // Relationships
  relationships: EntityRelationships;
}

// Actions
interface FormActions {
  updateField: (fieldId: string, value: any) => void;
  validateForm: () => Promise<ValidationResult>;
  saveEntity: () => Promise<void>;
  loadEntity: (id: string) => Promise<void>;
  resetForm: () => void;
}
```

## Component Architecture

### 1. Core Components

#### FormEngine (Main Container)
```typescript
const FormEngine = () => {
  return (
    <FormProvider>
      <PermissionProvider>
        <FormLayout>
          <FormHeader />
          <FormToolbar />
          <FormContent />
          <FormFooter />
        </FormLayout>
      </PermissionProvider>
    </FormProvider>
  );
};
```

#### FormRenderer (Dynamic Section Renderer)
```typescript
const FormRenderer = ({ sections }: { sections: FormSection[] }) => {
  return (
    <>
      {sections.map(section => (
        <SectionRenderer 
          key={section.id} 
          section={section} 
        />
      ))}
    </>
  );
};
```

#### SectionRenderer (Individual Section)
```typescript
const SectionRenderer = ({ section }: { section: FormSection }) => {
  const [expanded, setExpanded] = useState(section.defaultExpanded);
  
  return (
    <CollapsibleSection 
      title={section.title}
      expanded={expanded}
      onToggle={setExpanded}
    >
      <FieldRenderer fields={section.fields} />
    </CollapsibleSection>
  );
};
```

### 2. Field Components

#### Universal Field Renderer
```typescript
const FieldRenderer = ({ fields }: { fields: FormField[] }) => {
  return (
    <>
      {fields.map(field => (
        <FieldWrapper key={field.id} field={field}>
          <DynamicField field={field} />
        </FieldWrapper>
      ))}
    </>
  );
};

const DynamicField = ({ field }: { field: FormField }) => {
  const Component = getFieldComponent(field.type);
  return <Component {...field.props} />;
};
```

#### Field Type Registry
```typescript
const fieldRegistry = {
  'text': TextInput,
  'textarea': TextArea,
  'select': SelectInput,
  'multiselect': MultiSelectInput,
  'date': DatePicker,
  'richtext': RichTextEditor,
  'entity-selector': EntitySelector,
  'file-upload': FileUpload,
  'relationship': RelationshipField,
  // Custom field types
  'sample-field': SampleField,
  'storage-selector': StorageSelector,
};
```

### 3. Widget System

#### Widget Container
```typescript
const WidgetContainer = ({ widgets }: { widgets: WidgetConfiguration[] }) => {
  return (
    <div className="widget-container">
      {widgets.map(widget => (
        <WidgetRenderer 
          key={widget.id} 
          widget={widget} 
        />
      ))}
    </div>
  );
};
```

#### Specialized Widgets
```typescript
// Comments Widget
const CommentsWidget = () => {
  const { entityId, permissions } = useFormContext();
  return <CommentsSystem entityId={entityId} permissions={permissions} />;
};

// Storage Widget
const StorageWidget = () => {
  const { entityData, updateField } = useFormContext();
  return <StorageManager data={entityData} onChange={updateField} />;
};

// Dilution Widget
const DilutionWidget = () => {
  const { entityData } = useFormContext();
  return <DilutionTable data={entityData} />;
};
```

### 4. Toolbar System

#### Dynamic Toolbar
```typescript
const FormToolbar = () => {
  const { mode, permissions, entityType } = useFormContext();
  const config = getToolbarConfig(entityType, mode, permissions);
  
  return (
    <Toolbar>
      {config.buttons.map(button => (
        <ToolbarButton 
          key={button.id}
          {...button}
        />
      ))}
      <ToolbarDropdown options={config.dropdownOptions} />
    </Toolbar>
  );
};
```

## Data Flow Architecture

### 1. API Integration Layer

```typescript
// API abstraction layer
interface EntityAPI {
  get(id: string, fetchOptions?: FetchOptions): Promise<Entity>;
  create(data: EntityData): Promise<Entity>;
  update(id: string, data: Partial<EntityData>): Promise<Entity>;
  delete(id: string, options?: DeleteOptions): Promise<void>;
  search(criteria: SearchCriteria): Promise<SearchResult>;
}

// Implementation for different entity types
class SpaceAPI implements EntityAPI { /* ... */ }
class ProjectAPI implements EntityAPI { /* ... */ }
class ExperimentAPI implements EntityAPI { /* ... */ }
class SampleAPI implements EntityAPI { /* ... */ }
```

### 2. Permission System

```typescript
// Permission hooks
const usePermissions = (entityType: EntityType, entityId?: string) => {
  const [permissions, setPermissions] = useState<PermissionSet>();
  
  useEffect(() => {
    loadPermissions(entityType, entityId).then(setPermissions);
  }, [entityType, entityId]);
  
  return permissions;
};

// Permission-aware components
const PermissionGate = ({ 
  permission, 
  children 
}: { 
  permission: string; 
  children: React.ReactNode; 
}) => {
  const permissions = usePermissions();
  return permissions?.has(permission) ? <>{children}</> : null;
};
```

### 3. Validation System

```typescript
// Validation engine
interface ValidationEngine {
  validateField(field: FormField, value: any): ValidationResult;
  validateForm(data: EntityData, rules: ValidationRules): ValidationResult;
  validateRelationships(relationships: EntityRelationships): ValidationResult;
}

// Validation hooks
const useValidation = (validationRules: ValidationRules) => {
  const validate = useCallback((data: EntityData) => {
    return validationEngine.validateForm(data, validationRules);
  }, [validationRules]);
  
  return { validate };
};
```

## Configuration Examples

### Space Form Configuration
```json
{
  "entityType": "SPACE",
  "sections": [
    {
      "id": "identification",
      "title": "Identification Info",
      "collapsible": true,
      "defaultExpanded": true,
      "fields": [
        {
          "id": "code",
          "type": "text",
          "label": "Space Code",
          "required": true,
          "validation": {
            "pattern": "^[A-Z_]+$",
            "maxLength": 50
          }
        },
        {
          "id": "description",
          "type": "richtext",
          "label": "Description",
          "required": false
        }
      ]
    }
  ],
  "toolbar": {
    "buttons": [
      {
        "id": "save",
        "label": "Save",
        "action": "save",
        "variant": "primary",
        "permissions": ["UPDATE"]
      }
    ]
  }
}
```

### Sample Form Configuration
```json
{
  "entityType": "SAMPLE",
  "sections": [
    {
      "id": "identification",
      "title": "Identification Info",
      "fields": [
        {
          "id": "code",
          "type": "text",
          "label": "Sample Code",
          "required": true
        },
        {
          "id": "sampleType",
          "type": "select",
          "label": "Sample Type",
          "required": true,
          "props": {
            "options": "dynamic:sampleTypes"
          }
        }
      ]
    },
    {
      "id": "relationships",
      "title": "Relationships",
      "fields": [
        {
          "id": "parents",
          "type": "relationship",
          "label": "Parent Samples",
          "props": {
            "relationshipType": "parent",
            "entityType": "SAMPLE"
          }
        }
      ]
    }
  ],
  "widgets": [
    {
      "id": "comments",
      "type": "comments",
      "title": "Comments",
      "permissions": ["READ"]
    },
    {
      "id": "storage",
      "type": "storage",
      "title": "Storage Location",
      "permissions": ["UPDATE"]
    }
  ]
}
```

## Performance Optimizations

### 1. Code Splitting
```typescript
// Lazy load widgets
const CommentsWidget = lazy(() => import('./widgets/CommentsWidget'));
const StorageWidget = lazy(() => import('./widgets/StorageWidget'));

// Lazy load field components
const RichTextEditor = lazy(() => import('./fields/RichTextEditor'));
```

### 2. Memoization
```typescript
// Memoize expensive computations
const MemoizedFieldRenderer = memo(FieldRenderer);
const MemoizedSectionRenderer = memo(SectionRenderer);

// Memoize selectors
const selectEntityData = createSelector(
  (state: FormState) => state.entityData,
  (entityData) => entityData
);
```

### 3. Virtual Scrolling
```typescript
// For large lists (samples, relationships)
const VirtualizedList = ({ items, renderItem }) => {
  return (
    <FixedSizeList
      height={400}
      itemCount={items.length}
      itemSize={50}
    >
      {renderItem}
    </FixedSizeList>
  );
};
```

## Testing Strategy

### 1. Unit Tests
- Individual component testing
- Hook testing with React Testing Library
- Validation logic testing
- API integration testing

### 2. Integration Tests
- Form rendering with different configurations
- Widget integration testing
- Permission system testing
- Data flow testing

### 3. E2E Tests
- Complete form workflows
- Cross-browser compatibility
- Performance testing
- Accessibility testing

This design provides a solid foundation for a modern, maintainable React form engine that can handle all the complexity of the openBIS system while being flexible and extensible.