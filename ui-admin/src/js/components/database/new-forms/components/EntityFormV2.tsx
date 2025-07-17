// ============================================================================
// 3. PRESENTATION: EntityFormV2.tsx (REFACTORED - NOW "DUMB")
// Description: This component is now stateless and purely presentational.
// It receives all data and handlers from its parent context and uses the
// registry to render the correct components.
// ============================================================================
import React from 'react';
import { FormMode, Form, FormAction as FormActionDef, FormField, VisibilityRule } from '@src/js/components/database/new-forms/types/form.types.ts';
import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';

interface EntityFormV2Props {
  form: Form;
  mode: FormMode;
  permissions: any;
  onFieldChange: (fieldId: string, value: any) => void;
  onAction: (actionName: string) => void;
  isSaving: boolean;
  error: string | null;
}

const EntityFormV2 = ({ form, mode, permissions, onFieldChange, onAction, isSaving, error }: EntityFormV2Props) => {

	const renderToolbar = () => {
		// UPDATED: Interpret declarative visibility rules
		const visibleActions = form.actions?.filter(action => {
			if (!action.visibility || action.visibility.length === 0) return true; // Default to visible
	
			// Every rule in the visibility array must be met
			return action.visibility.every((rule: VisibilityRule) => {
				let isVisible = true;
				if (rule.mode) {
					const modes = Array.isArray(rule.mode) ? rule.mode : [rule.mode];
					isVisible = isVisible && modes.includes(mode);
				}
				if (rule.permission) {
					isVisible = isVisible && permissions[rule.permission] === true;
				}
				return isVisible;
			});
		});
	
		return (
		  <div className="toolbar">
			{visibleActions?.map((action: FormActionDef) => (
			  <button key={action.name} onClick={() => onAction(action.name)} disabled={isSaving}>
				{action.label}
			  </button>
			))}
		  </div>
		);
	  };

  const renderSections = () => {
    // Create a map for quick field lookup
    const fieldsById = new Map(form.fields.map(f => [f.id, f]));

    return form.sections.map(section => (
      <div key={section.section} className="form-section">
        <h3>{section.section}</h3>
        {section.fields.map((fieldId: string) => {
          const field = fieldsById.get(fieldId);
          return field ? renderField(field) : null;
        })}
      </div>
    ));
  };

  const renderField = (field: FormField) => {
    // Get the correct renderer component from the registry
    const FieldRenderer = FormEngineRegistry.getFieldRenderer(field.dataType);
	console.log('field', field);
    if (!FieldRenderer) {
      return <div>Unsupported field type: {field.dataType}</div>;
    }
    return (
        <FieldRenderer
            key={field.id}
            field={field}
            onFieldChange={onFieldChange}
            mode={mode}
        />
    );
  };

  return (
    <div>
      {renderToolbar()}
      {error && <div style={{ color: 'red' }}>Error: {error}</div>}
      {isSaving && <div>Saving...</div>}
      {renderSections()}
    </div>
  );
};

export default EntityFormV2;