import React from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import SourceCodeField from '@src/js/components/common/form/SourceCodeField.jsx';
import FormFieldView from '@src/js/components/common/form/FormFieldView.jsx';
import MultiValueFieldEditor from './MultiValueFieldEditor.tsx';
import { highlight, languages } from 'prismjs/components/prism-core.js';
import 'prismjs/components/prism-clike.js';
import 'prismjs/components/prism-python.js';
import 'prismjs/components/prism-sql.js';
import 'prismjs/components/prism-log.js';
import 'prismjs/components/prism-json.js';
import 'prismjs/components/prism-markup.js';
import 'prismjs/themes/prism.css';
import { useTheme } from '@mui/material/styles';

const languageMap: Record<string, any> = {
    python: languages.python,
    sql: languages.sql,
    log: languages.log,
    xml: languages.xml,
    json: languages.json,
}

export const SourceCodeFieldRenderer: React.FC<FieldRendererProps> = ({ field, onFieldChange, mode }) => {
    const theme = useTheme();
    const isEditing = mode === 'edit' || mode === 'create';
    const language = field.dataType.toString().toLowerCase();

    if (field.isMultiValue && !isEditing) {
        const values: any[] = Array.isArray(field.value) ? field.value : [];
        const languageDef = languageMap[language];
        const lines = values.map((v, i) => (
            <div
                key={i}
                dangerouslySetInnerHTML={{ __html: highlight(String(v ?? ''), languageDef) }}
                style={{
                    fontFamily: (theme.typography as any).sourceCode?.fontFamily ?? 'monospace',
                    fontSize: theme.typography.body2.fontSize,
                    whiteSpace: 'pre',
                    tabSize: 4,
                    overflowX: 'auto',
                    marginBottom: i < values.length - 1 ? '8px' : 0,
                }}
            />
        ));
        return (
            <FormFieldView
                label={field.label}
                value={lines.length > 0 ? <>{lines}</> : undefined}
                disableUnderline={true}
                description={field.meta?.helpText}
            />
        );
    } else if (field.isMultiValue && isEditing && !field.readOnly) {
        return (
            <MultiValueFieldEditor
                label={field.label}
                required={field.required}
                values={Array.isArray(field.value) ? field.value : []}
                onChange={(vals) => onFieldChange(field.id, vals)}
                renderInput={(val, onChange) => (
                    <SourceCodeField
                        name={field.label}
                        mandatory={field.required}
                        label={null}
                        language={language}
                        disabled={false}
                        value={val ?? ''}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => onChange(e.target.value)}
                        description={field.meta?.helpText}
                        styles={{}}
                        minRows={2}
                        maxRows={10}
                    />
                )}
            />
        );
    } else {
        return (
            <SourceCodeField
                id={field.id}
                name={field.label}
                mandatory={field.required}
                label={field.label}
                language={language}
                mode={isEditing && !field.readOnly ? 'edit' : 'view'}
                disabled={isEditing && field.readOnly}
                value={field.value}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => onFieldChange(field.id, e.target.value)}
                description={field.meta?.helpText}
                styles={{}}
            />
        );
    }
}
