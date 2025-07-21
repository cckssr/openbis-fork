// ============================================================================
// 3. PRESENTATION: EntityForm.tsx (REFACTORED - NOW "DUMB")
// Description: This component is now stateless and purely presentational.
// It receives all data and handlers from its parent context and uses the
// registry to render the correct components.
// ============================================================================
import React from 'react';
import { FormMode, Form, FormAction as FormActionDef, FormField, VisibilityRule, SectionGroup } from '@src/js/components/database/new-forms/types/form.types.ts';
import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import LoadingDialog from '@src/js/components/common/loading/LoadingDialog.jsx';
import { Stack } from '@mui/material'
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';
import ConflictResolutionDialog from '@src/js/components/database/new-forms/components/ConflictResolutionDialog.tsx';

interface EntityFormProps {
  form: Form;
  mode: FormMode;
  permissions: any;
  onFieldChange: (fieldId: string, value: any) => void;
  onAction: (actionName: string) => void;
  isSaving: boolean;
  error: string | null;
  showConflictDialog: boolean;
  conflictFields: any;
  handleResolveConflicts: (resolved: Record<string, any>) => void;
  setShowConflictDialog: (show: boolean) => void;
}

const EntityForm = ({ form, mode, permissions, onFieldChange, onAction, isSaving, error, showConflictDialog, conflictFields, handleResolveConflicts, setShowConflictDialog }: EntityFormProps) => {

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
      <Stack key={form.entityPermId + 'actions'} direction='row' spacing={{ xs: 1, sm: 2 }} sx={{
        justifyContent: 'flex-start',
        alignItems: 'center',
        padding: '16px 16px',
        backgroundColor: 'rgb(248,248,248)'
      }}>
        {visibleActions?.map((action: FormActionDef) => {
          const ActionRenderer = FormEngineRegistry.getActionRenderer(action.component);
          if (ActionRenderer) {
            return <ActionRenderer key={action.name} action={action} onAction={onAction} mode={mode} />
          }
        })}
      </Stack>
    );
  };

  const renderSections = () => {
    // Create a map for quick field lookup
    const fieldsById = new Map(form.fields.map(f => [f.id, f]));

    return form.sections.map(({ section, fields }: SectionGroup) => {
      // Group fields by column within each section
      const leftFields = fields.map((fieldId: string)=> fieldsById.get(fieldId)).filter((field : FormField | undefined) => field?.column === 'left');
      const rightFields = fields.map((fieldId: string)=> fieldsById.get(fieldId)).filter((field : FormField | undefined) => field?.column === 'right');
      const centerFields = fields.map((fieldId: string)=> fieldsById.get(fieldId)).filter((field : FormField | undefined) => field?.column === 'center');

      return (
        <CollapsableSection isCollapsed={false} title={section} renderWarnings={null} key={section}>
          <div style={{ padding: '8px 16px' }}>
            {/* Render left and right columns side by side */}
            {(leftFields.length > 0 || rightFields.length > 0) && (
              <div style={{ display: 'flex', gap: '16px' }}>
                {/* Left column */}
                <div style={{ flex: 1 }}>
                  {leftFields.map((field: FormField | undefined) => {
                    return renderField(field);
                  })}
                </div>
                {/* Right column */}
                <div style={{ flex: 1 }}>
                  {rightFields.map((field: FormField | undefined) => {
                    return renderField(field);
                  })}
                </div>
              </div>
            )}
            {/* Render center fields on their own row */}
            {centerFields.length > 0 && (
              <div>
                {centerFields.map((field: FormField | undefined) => {
                    return renderField(field);
                  })}
              </div>
            )}
          </div>
        </CollapsableSection>
      );
    });
    /* return form.sections.map(section => (
      <div key={section.section} className="form-section">
        <h3>{section.section}</h3>
        {section.fields.map((fieldId: string) => {
          const field = fieldsById.get(fieldId);
          return field ? renderField(field) : null;
        })}
      </div>
    )); */
  };

  const renderField = (field: FormField | undefined) => {
    if (!field) return null;
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
      {isSaving && <LoadingDialog loading={isSaving} />}
      {renderSections()}
      {showConflictDialog && (
          <ConflictResolutionDialog
            open={showConflictDialog}
            conflicts={conflictFields}
            onResolve={handleResolveConflicts}
            onCancel={() => setShowConflictDialog(false)}
          />
        )}
    </div>
  );
};

export default EntityForm;