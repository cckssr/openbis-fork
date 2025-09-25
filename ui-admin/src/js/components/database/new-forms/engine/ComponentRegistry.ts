import { FormFieldDataType } from '@src/js/components/database/new-forms/types/form.enums.ts';

import { TextFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextFieldRenderer.tsx';
import { DateFieldRenderer } from '@src/js/components/database/new-forms/components/fields/DateFieldRenderer.tsx';
import { TextAreaFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextAreaFieldRenderer.tsx';

import { ButtonActionRenderer } from '@src/js/components/database/new-forms/components/fields/ButtonActionRenderer.tsx';
import { SwitchActionRenderer } from '@src/js/components/database/new-forms/components/fields/SwitchActionRenderer.tsx';

class ComponentRegistry {
  static getFieldRenderer(dataType: string) {
    switch (dataType) {
      case FormFieldDataType.VARCHAR:
        return TextFieldRenderer;
      case FormFieldDataType.TIMESTAMP:
        return DateFieldRenderer;
      case FormFieldDataType.MULTILINE_VARCHAR:
        return TextAreaFieldRenderer;
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
