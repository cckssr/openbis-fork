import { FormFieldDataType } from '@src/js/components/database/new-forms/types/form.enums.ts';

import { TextFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextFieldRenderer.tsx';
import { DateFieldRenderer } from '@src/js/components/database/new-forms/components/fields/DateFieldRenderer.tsx';
import { TextAreaFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextAreaFieldRenderer.tsx';
import { SelectFieldRenderer } from '@src/js/components/database/new-forms/components/fields/SelectFieldRender.tsx';
import { SwitchFieldRenderer } from '@src/js/components/database/new-forms/components/fields/SwitchFieldRender.tsx';

import { ButtonActionRenderer } from '@src/js/components/database/new-forms/components/actions/ButtonActionRenderer.tsx';
import { SwitchActionRenderer } from '@src/js/components/database/new-forms/components/actions/SwitchActionRenderer.tsx';
import { CKEditorFieldRenderer } from '@src/js/components/database/new-forms/components/fields/CKEditorFieldRenderer.tsx';

class ComponentRegistry {
  static getFieldRenderer(dataType: string) {
    switch (dataType) {
      case FormFieldDataType.VARCHAR:
        return TextFieldRenderer;
      case FormFieldDataType.TIMESTAMP:
        return DateFieldRenderer;
      case FormFieldDataType.MULTILINE_VARCHAR:
        return TextAreaFieldRenderer;
      case FormFieldDataType.CONTROLLED_VOCABULARY:
        return SelectFieldRenderer;
      case FormFieldDataType.BOOLEAN:
        return SwitchFieldRenderer;
      case FormFieldDataType.WORD_PROCESSOR:
        return CKEditorFieldRenderer;
      default:
        return TextFieldRenderer; 
    }
  }

  static getActionRenderer(componentType: string) {
    switch (componentType) {
      case 'button':
        return ButtonActionRenderer;
      case 'switch':
        return SwitchActionRenderer;
      default:
        return ButtonActionRenderer;
    }
  }
}

export default ComponentRegistry;
