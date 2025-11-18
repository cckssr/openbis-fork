import React from 'react';
import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { Form, FormAction as FormActionDef, FormField, VisibilityRule, SectionGroup } from '@src/js/components/database/new-forms/types/form.types.ts';
import ComponentRegistry from '@src/js/components/database/new-forms/engine/ComponentRegistry.ts';
import { Stack } from '@mui/material'
import CollapsableSection from '@src/js/components/common/imaging/components/viewer/CollapsableSection.jsx';


interface EntityFormProps {
  form: Form;
  mode: FormMode;
  permissions: any;
  onFieldChange: (fieldId: string, value: any) => void;
  onFieldMetadataChange: (fieldId: string, meta: any) => void;
  onAction: (actionName: string) => void;
  params: any;
}

const EntityForm = ({ form, mode, permissions, onFieldChange, onFieldMetadataChange, onAction, params }: EntityFormProps) => {

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
          const ActionRenderer = ComponentRegistry.getActionRenderer(action.component);
          if (ActionRenderer) {
            return <ActionRenderer key={action.name} action={action} onAction={onAction} mode={mode} />
          }
        })}
      </Stack>
    );
  };

  const buildSectionGroups = () => {
    const fieldsById = new Map(form.fields.map(f => [f.id, f]));

    if (form.sections && form.sections.length > 0) {
      return form.sections.map(({ section, fields }: SectionGroup) => ({
        section,
        fields: fields
          .map((fieldId: string) => fieldsById.get(fieldId))
          .filter((field: FormField | undefined): field is FormField => Boolean(field)),
      }));
    }

    const order: string[] = [];
    const grouped = new Map<string, FormField[]>();

    form.fields.forEach(field => {
      if (!grouped.has(field.section)) {
        grouped.set(field.section, []);
        order.push(field.section);
      }
      grouped.get(field.section)?.push(field);
    });

    return order.map(section => ({
      section,
      fields: grouped.get(section) ?? [],
    }));
  };

  const renderSections = () => {
    return buildSectionGroups().map(({ section, fields }) => {
      const leftFields = fields.filter(field => field.column === 'left');
      const rightFields = fields.filter(field => field.column === 'right');
      const centerFields = fields.filter(field => field.column === 'center');

      return (
        <CollapsableSection isCollapsed={false} title={section} renderWarnings={null} key={section}>
          <div style={{ padding: '8px 16px' }}>
            {(leftFields.length > 0 || rightFields.length > 0) && (
              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1 }}>
                  {leftFields.map(field => renderField(field))}
                </div>
                <div style={{ flex: 1 }}>
                  {rightFields.map(field => renderField(field))}
                </div>
              </div>
            )}
            {centerFields.length > 0 && (
              <div>
                {centerFields.map(field => renderField(field))}
              </div>
            )}
          </div>
        </CollapsableSection>
      );
    });
  };

  const renderField = (field: FormField | undefined) => {
    if (!field) return null;
    // Get the correct renderer component from the registry
    const FieldRenderer = ComponentRegistry.getFieldRenderer(field.dataType);
    if (!FieldRenderer) {
      return <div>Unsupported field type: {field.dataType}</div>;
    }
    return (
      <FieldRenderer
        key={field.id}
        field={field}
        onFieldChange={onFieldChange}
        onFieldMetadataChange={onFieldMetadataChange}
        mode={mode}
        params={params}
      />
    );
  };

  return (
    <>
      {renderToolbar()}
      {renderSections()}
    </>
  );
};

export default EntityForm;