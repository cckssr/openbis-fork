import React, { useState, useEffect, useCallback } from 'react';

// ============================================================================
// 1. CORE ENGINE: TYPES
// Location: src/engine/types.ts
// Description: Defines the core TypeScript interfaces for the entire engine.
// These types are the contract for schemas, components, and actions.
// ============================================================================

/** A generic key-value object for form data. */
export type FormState = {
  [key: string]: any;
};

/** Defines a validation rule in the declarative schema. */
export type ValidationRuleDef = {
  rule: string; // Name of the registered validation rule (e.g., 'required', 'minLength')
  message: string;
  [key: string]: any; // Additional options for the rule (e.g., minLength: 8)
};

/** Defines a conditional visibility rule in the declarative schema. */
export type VisibilityConditionDef = {
  field: string;      // The field to check
  operator: string;   // The registered operator (e.g., 'is', 'isNotEmpty')
  value?: any;        // The value to compare against
};

/** The configuration for a single field within the form schema. */
export type FieldConfig = {
  type: string; // Corresponds to a registered FieldRenderer
  label: string;
  defaultValue?: any;
  validation?: ValidationRuleDef[];
  visibility?: VisibilityConditionDef[];
  // Other field-specific options can be added here
  [key: string]: any;
};

/** The configuration for a single action button in the form schema. */
export type ActionConfig = {
  type: string; // Corresponds to a registered Action
  label: string;
  // Other action-specific options
  [key: string]: any;
};

/** The configuration for a section in the form schema. */
export type SectionConfig = {
    type: string; // Corresponds to a registered SectionRenderer
    title: string;
    fields: string[]; // Array of field names in this section
};


/** The complete, declarative, JSON-serializable form schema. */
export type FormSchema = {
  fields: { [fieldName: string]: FieldConfig };
  sections: { [sectionName: string]: SectionConfig };
  actions: { [actionName:string]: ActionConfig };
};

/** The context object passed to every registered action handler. */
export type ActionContext = {
  formState: FormState;
  setFormState: React.Dispatch<React.SetStateAction<FormState>>;
  schema: FormSchema;
  // Could also include other app-wide context, like API clients or navigation functions
  showModal: (title: string, message: string) => void;
};

/** The props passed to every registered FieldRenderer component. */
export type FieldRendererProps = {
  fieldName: string;
  value: any;
  onChange: (fieldName: string, value: any) => void;
  config: FieldConfig;
  error?: string;
};

/** The props passed to every registered SectionRenderer component. */
export type SectionRendererProps = {
    config: SectionConfig;
    children: React.ReactNode; // The rendered fields for this section
};


// ============================================================================
// 2. CORE ENGINE: REGISTRY
// Location: src/engine/registry.ts
// Description: A generic registry class to manage dynamic registration.
// ============================================================================

/**
 * A generic, observable registry for managing dynamic components, actions, or rules.
 */
export class Registry<T> {
  private items = new Map<string, T>();
  private listeners = new Set<() => void>();

  register(name: string, item: T): void {
    console.log(`[Registry] Registering '${name}'`);
    this.items.set(name, item);
    this.notifyListeners();
  }

  unregister(name: string): void {
    console.log(`[Registry] Unregistering '${name}'`);
    this.items.delete(name);
    this.notifyListeners();
  }

  get(name: string): T | undefined {
    return this.items.get(name);
  }

  getAll(): Map<string, T> {
    return this.items;
  }
  
  // Basic hook for components to re-render when registry changes
  useRegistry(): Map<string, T> {
      const [, forceUpdate] = useState({});
      useEffect(() => {
          const listener = () => forceUpdate({});
          this.subscribe(listener);
          return () => this.unsubscribe(listener);
      }, []);
      return this.items;
  }

  private subscribe(listener: () => void) {
      this.listeners.add(listener);
  }

  private unsubscribe(listener: () => void) {
      this.listeners.delete(listener);
  }

  private notifyListeners() {
    this.listeners.forEach(listener => listener());
  }
}

// ============================================================================
// 3. REGISTRIES: SINGLETON INSTANCES
// Location: src/registries/index.ts
// Description: Centralized, singleton instances of our registries.
// ============================================================================

export const fieldRendererRegistry = new Registry<React.ComponentType<FieldRendererProps>>();
export const sectionRendererRegistry = new Registry<React.ComponentType<SectionRendererProps>>();
export const actionRegistry = new Registry<(context: ActionContext) => void>();
export const validationRuleRegistry = new Registry<(value: any, options: any, allValues: FormState) => true | string>();
export const conditionOperatorRegistry = new Registry<(fieldValue: any, checkValue: any) => boolean>();


// ============================================================================
// 4. CORE ENGINE: THE `useFormEngine` HOOK
// Location: src/engine/useFormEngine.ts
// Description: The heart of the engine. It takes a schema and state,
// and returns render-ready data structures.
// ============================================================================

type UseFormEngineArgs = {
  schema: FormSchema;
  formState: FormState;
  setFormState: React.Dispatch<React.SetStateAction<FormState>>;
  // Pass in any other context actions need
  showModal: (title: string, message: string) => void;
};

export const useFormEngine = ({ schema, formState, setFormState, showModal }: UseFormEngineArgs) => {
  // By using the hook, this component will re-render if registries change
  const fieldRenderers = fieldRendererRegistry.useRegistry();
  const sectionRenderers = sectionRendererRegistry.useRegistry();
  const actions = actionRegistry.useRegistry();
  const validationRules = validationRuleRegistry.useRegistry();
  const conditionOperators = conditionOperatorRegistry.useRegistry();

  const handleFieldChange = useCallback((fieldName: string, value: any) => {
    setFormState(prevState => ({
      ...prevState,
      [fieldName]: value,
    }));
  }, [setFormState]);

  // Memoize the processing to avoid re-calculating on every render
  const processedForm = React.useMemo(() => {
    console.log("Processing form schema...");
    const errors: { [key: string]: string } = {};
    const visibleFields = new Set<string>();

    // 1. Determine field visibility
    Object.entries(schema.fields).forEach(([fieldName, config]) => {
      if (!config.visibility || config.visibility.length === 0) {
        visibleFields.add(fieldName);
        return;
      }
      
      const isVisible = config.visibility.every(condition => {
        const operator = conditionOperators.get(condition.operator);
        const fieldValue = formState[condition.field];
        if (!operator) {
            console.warn(`Visibility operator '${condition.operator}' not found.`);
            return true; // Default to visible if operator is missing
        }
        return operator(fieldValue, condition.value);
      });

      if (isVisible) {
        visibleFields.add(fieldName);
      }
    });

    // 2. Validate visible fields
    visibleFields.forEach(fieldName => {
        const config = schema.fields[fieldName];
        const value = formState[fieldName];
        if (!config.validation) return;

        for (const ruleDef of config.validation) {
            const rule = validationRules.get(ruleDef.rule);
            if (rule) {
                const result = rule(value, ruleDef, formState);
                if (typeof result === 'string') {
                    errors[fieldName] = result;
                    break; // Stop at first error for a field
                }
            } else {
                console.warn(`Validation rule '${ruleDef.rule}' not found.`);
            }
        }
    });
    
    // 3. Prepare rendered sections and fields
    const renderedSections = Object.entries(schema.sections).map(([sectionName, sectionConfig]) => {
        const SectionComponent = sectionRenderers.get(sectionConfig.type);
        if (!SectionComponent) {
            return <div key={sectionName} className="p-4 bg-red-100 border border-red-400 rounded-lg">Error: Section renderer '{sectionConfig.type}' not found.</div>;
        }

        const sectionFields = sectionConfig.fields
            .filter(fieldName => visibleFields.has(fieldName))
            .map(fieldName => {
                const fieldConfig = schema.fields[fieldName];
                const FieldComponent = fieldRenderers.get(fieldConfig.type);
                if (!FieldComponent) {
                    return <div key={fieldName} className="p-4 bg-red-100 border border-red-400 rounded-lg">Error: Field renderer '{fieldConfig.type}' not found.</div>;
                }
                return (
                    <FieldComponent
                        key={fieldName}
                        fieldName={fieldName}
                        config={fieldConfig}
                        value={formState[fieldName]}
                        onChange={handleFieldChange}
                        error={errors[fieldName]}
                    />
                );
            });
        
        return (
            <SectionComponent key={sectionName} config={sectionConfig}>
                {sectionFields}
            </SectionComponent>
        );
    });

    // 4. Prepare rendered actions
    const actionContext: ActionContext = { formState, setFormState, schema, showModal };
    const renderedActions = Object.entries(schema.actions).map(([actionName, actionConfig]) => {
        const actionHandler = actions.get(actionConfig.type);
        if (!actionHandler) {
            return <button key={actionName} disabled className="px-4 py-2 text-white bg-gray-400 rounded-md cursor-not-allowed">{actionConfig.label} (disabled)</button>;
        }
        return (
            <button
                key={actionName}
                onClick={() => actionHandler(actionContext)}
                className="px-4 py-2 font-semibold text-white bg-indigo-600 rounded-md shadow-sm hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
            >
                {actionConfig.label}
            </button>
        );
    });

    return { renderedSections, renderedActions, errors, isValid: Object.keys(errors).length === 0 };

  }, [schema, formState, fieldRenderers, sectionRenderers, actions, validationRules, conditionOperators, handleFieldChange, showModal, setFormState]);

  return processedForm;
};


// ============================================================================
// 5. CORE COMPONENTS & LOGIC
// Location: src/components/core/
// Description: Basic, built-in renderers and logic for the engine.
// ============================================================================

// --- Default Field Renderer ---
const TextFieldRenderer: React.FC<FieldRendererProps> = ({ fieldName, config, value, onChange, error }) => (
  <div className="mb-4">
    <label htmlFor={fieldName} className="block text-sm font-medium leading-6 text-gray-900">{config.label}</label>
    <div className="mt-1">
      <input
        type={config.inputType || 'text'}
        id={fieldName}
        name={fieldName}
        value={value || ''}
        onChange={(e) => onChange(fieldName, e.target.value)}
        className={`block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ${error ? 'ring-red-500' : 'ring-gray-300'} placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6`}
      />
    </div>
    {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
  </div>
);

// --- Default Section Renderer ---
const DefaultSectionRenderer: React.FC<SectionRendererProps> = ({ config, children }) => (
    <div className="p-6 mb-6 bg-white border border-gray-200 rounded-lg shadow-sm">
        <h2 className="pb-4 mb-4 text-lg font-semibold text-gray-900 border-b border-gray-200">{config.title}</h2>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            {children}
        </div>
    </div>
);


// --- Initialize Core Registries ---
export const initializeCore = () => {
  // Register renderers
  fieldRendererRegistry.register('text', TextFieldRenderer);
  fieldRendererRegistry.register('number', (props) => <TextFieldRenderer {...props} config={{...props.config, inputType: 'number'}} />);
  fieldRendererRegistry.register('password', (props) => <TextFieldRenderer {...props} config={{...props.config, inputType: 'password'}} />);
  sectionRendererRegistry.register('default', DefaultSectionRenderer);

  // Register validation rules
  validationRuleRegistry.register('required', (value) => (value !== null && value !== undefined && value !== '') || 'This field is required.');
  validationRuleRegistry.register('minLength', (value, options) => (value && value.length >= options.length) || `Must be at least ${options.length} characters.`);
  validationRuleRegistry.register('matchesField', (value, options, allValues) => (value === allValues[options.field]) || options.message);
  
  // Register condition operators
  conditionOperatorRegistry.register('isNotEmpty', (fieldValue) => fieldValue !== null && fieldValue !== undefined && fieldValue !== '');
  conditionOperatorRegistry.register('is', (fieldValue, checkValue) => fieldValue === checkValue);
};


// ============================================================================
// 6. PLUGIN EXAMPLE: A "THIRD-PARTY" RICH TEXT EDITOR
// Location: src/plugins/RichTextEditor.tsx
// ============================================================================

const RichTextEditorComponent: React.FC<FieldRendererProps> = ({ fieldName, config, value, onChange, error }) => (
  <div className="mb-4 md:col-span-2"> {/* Example of plugin-specific layout */}
    <label htmlFor={fieldName} className="block text-sm font-medium leading-6 text-gray-900">{config.label}</label>
    <div className="mt-1">
      <textarea
        id={fieldName}
        name={fieldName}
        rows={5}
        value={value || ''}
        onChange={(e) => onChange(fieldName, e.target.value)}
        className={`block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ${error ? 'ring-red-500' : 'ring-gray-300'} placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6`}
        placeholder="This is a mock rich text editor..."
      />
    </div>
    {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
  </div>
);

export const initializeRichTextPlugin = () => {
    fieldRendererRegistry.register('rich-text', RichTextEditorComponent);
};

// ============================================================================
// 7. PLUGIN EXAMPLE: CUSTOM ACTIONS
// Location: src/plugins/CustomActions.ts
// ============================================================================

const submitAction = (context: ActionContext) => {
    // In a real app, you'd perform validation before showing the modal
    const message = `Form Submitted Successfully! Data: ${JSON.stringify(context.formState, null, 2)}`;
    context.showModal('Submission', message);
};

const openExternalDialogAction = (context: ActionContext) => {
    const message = `This action could trigger anything outside the form, like opening a different modal, navigating, or calling an API. Current name is: '${context.formState.fullName || 'N/A'}'`;
    context.showModal('External Action Triggered', message);
};

export const initializeCustomActionsPlugin = () => {
    actionRegistry.register('submit', submitAction);
    actionRegistry.register('openDialog', openExternalDialogAction);
};


// ============================================================================
// 8. MAIN APPLICATION: Putting it all together
// Location: src/App.tsx
// ============================================================================

// --- Initialize all modules ONCE at the application root ---
initializeCore();
initializeRichTextPlugin();
initializeCustomActionsPlugin();


// --- Define the form using the declarative schema ---
const userProfileSchema: FormSchema = {
    fields: {
        fullName: {
            type: 'text',
            label: 'Full Name',
            defaultValue: 'John Doe',
            validation: [{ rule: 'required', message: 'Full name cannot be empty.' }],
        },
        email: {
            type: 'text',
            label: 'Email Address',
            validation: [{ rule: 'required', message: 'Email is required.' }],
        },
        password: {
            type: 'password',
            label: 'Password',
            validation: [{ rule: 'minLength', length: 8, message: 'Password must be at least 8 characters.' }],
        },
        confirmPassword: {
            type: 'password',
            label: 'Confirm Password',
            validation: [{ rule: 'matchesField', field: 'password', message: "Passwords don't match." }],
            visibility: [{ field: 'password', operator: 'isNotEmpty' }],
        },
        bio: {
            type: 'rich-text', // Using our plugin
            label: 'Biography',
            defaultValue: 'I am a software engineer...'
        },
    },
    sections: {
        accountInfo: {
            type: 'default',
            title: 'Account Information',
            fields: ['fullName', 'email'],
        },
        security: {
            type: 'default',
            title: 'Security',
            fields: ['password', 'confirmPassword'],
        },
        profile: {
            type: 'default',
            title: 'Public Profile',
            fields: ['bio'],
        },
    },
    actions: {
        submit: {
            type: 'submit', // From our plugin
            label: 'Save Changes',
        }, 
        custom: {
            type: 'openDialog', // From our plugin
            label: 'Show Custom Dialog',
        }
    },
};

// --- A helper to extract default values from the schema ---
const getDefaultState = (schema: FormSchema): FormState => {
    return Object.entries(schema.fields).reduce((acc, [key, config]) => {
        acc[key] = config.defaultValue ?? '';
        return acc;
    }, {} as FormState);
};


// --- The Main App Component ---
export default function App() {
  const [formState, setFormState] = useState<FormState>(() => getDefaultState(userProfileSchema));
  const [modal, setModal] = useState<{ title: string; message: string } | null>(null);

  const showModal = (title: string, message: string) => {
    setModal({ title, message });
  };

  const { renderedSections, renderedActions } = useFormEngine({
    schema: userProfileSchema,
    formState,
    setFormState,
    showModal,
  });

  return (
    <div className="bg-gray-50 min-h-screen font-sans">
      <div className="container mx-auto p-4 sm:p-6 lg:p-8">
        <header className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Agnostic Form Engine</h1>
          <p className="mt-2 text-gray-600">This form is rendered dynamically based on a declarative schema and extensible registries.</p>
        </header>

        <main>
          {renderedSections}
          <footer className="flex items-center justify-end gap-x-4 mt-6 p-6 bg-white border border-gray-200 rounded-lg shadow-sm">
            {renderedActions}
          </footer>
        </main>

        {/* Modal for demonstrating actions */}
        {modal && (
          <div className="fixed inset-0 z-10 bg-gray-500 bg-opacity-75 flex items-center justify-center">
            <div className="relative bg-white rounded-lg shadow-xl p-6 w-full max-w-lg">
              <h3 className="text-lg font-semibold text-gray-900">{modal.title}</h3>
              <div className="mt-2">
                <p className="text-sm text-gray-600 whitespace-pre-wrap">{modal.message}</p>
              </div>
              <div className="mt-4">
                <button
                  type="button"
                  className="inline-flex justify-center rounded-md border border-transparent bg-indigo-100 px-4 py-2 text-sm font-medium text-indigo-900 hover:bg-indigo-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2"
                  onClick={() => setModal(null)}
                >
                  Got it, thanks!
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
