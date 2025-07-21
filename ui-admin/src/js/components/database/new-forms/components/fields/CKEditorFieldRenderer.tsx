import React from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import ClassicEditor from '@ckeditor/ckeditor5-build-classic';
import { FormField } from '@src/js/components/database/new-forms/types/form.types.ts';

type CKEditorFieldProps = {
  field: FormField;
  value: string;
  disabled?: boolean;
  onChange: (value: string) => void;
  onFocus?: () => void;
  onBlur?: () => void;
};

const CKEditorField: React.FC<CKEditorFieldProps> = ({
  field,
  value,
  disabled,
  onChange,
  onFocus,
  onBlur
}) => {
  return (
    <div>
      <label>{field.label}</label>
      <CKEditor editor={ClassicEditor}
        data={value || ''}
        disabled={disabled}
        onChange={(_, editor) => {
          const data = editor.getData();
          onChange(data);
        }}
        onFocus={onFocus}
        onBlur={onBlur}
      />
    </div>
  );
};

export default CKEditorField;