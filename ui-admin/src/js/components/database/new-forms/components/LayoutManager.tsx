import React from 'react';
import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormLayout, LayoutSection, LayoutItem } from '@src/js/components/database/new-forms/types/layout.types.ts';
import { FormFieldRenderer } from './FormFieldRenderer';
import { Grid, Accordion, AccordionSummary, AccordionDetails, Typography } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

interface LayoutManagerProps {
  form: Form;
  layout: FormLayout;
  mode: FormMode;
  onFieldUpdate: (fieldId: string, value: any) => void;
}

export const LayoutManager: React.FC<LayoutManagerProps> = ({ form, layout, mode, onFieldUpdate }) => {

  const findField = (fieldId: string) => {
    return form.fields.find(f => f.id === fieldId);
  };

  const renderItem = (item: LayoutItem) => {
    if (item.type === 'field') {
      const field = findField(item.id);
      if (!field) {
        return <div>Field '{item.id}' not found</div>;
      }
      return (
        <FormFieldRenderer
          field={field}
          onUpdate={onFieldUpdate}
          isEditing={mode === FormMode.EDIT}
          mode={mode}
        />
      );
    } else if (item.type === 'custom' && item.component) {
      return item.component;
    }
    return null;
  };

  const renderSection = (section: LayoutSection) => {
    const content = (
      <Grid container spacing={2}>
        {section.items.map((row, rowIndex) => (
          <React.Fragment key={rowIndex}>
            {row.map(item => (
              <Grid item xs={12} sm={12 / row.length} key={item.id}>
                {renderItem(item)}
              </Grid>
            ))}
          </React.Fragment>
        ))}
      </Grid>
    );

    if (section.isCollapsable) {
      return (
        <Accordion defaultExpanded>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography>{section.title}</Typography>
          </AccordionSummary>
          <AccordionDetails>{content}</AccordionDetails>
        </Accordion>
      );
    }

    return (
      <div style={{ marginBottom: '20px' }}>
        <h2>{section.title}</h2>
        {content}
      </div>
    );
  };

  return (
    <div>
      {layout.sections.map(section => (
        <div key={section.id}>
          {renderSection(section)}
        </div>
      ))}
    </div>
  );
};