import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/form.types.ts';
import DateField from '@src/js/components/common/form/DateField.jsx';

export const DateFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
    const isEditing = mode === 'edit' || mode === 'create';
	
    return (
		//@ts-ignore
        <DateField label={field.label}
            mandatory={field.required}
            mode={isEditing && !field.readOnly ? 'edit' : 'view'}
            disabled={isEditing && field.readOnly}
            value={{ dateObject: new Date(field.value) }}
            onChange={(date: Date) => onFieldChange(field.id, date.toISOString())}
            disableUnderline={true}
        />
    );
};