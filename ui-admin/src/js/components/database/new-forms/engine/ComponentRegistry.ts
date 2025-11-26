import { FormFieldDataType } from '@src/js/components/database/new-forms/types/formEnums.ts';

import { TextFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextFieldRenderer.tsx';
import { DateFieldRenderer } from '@src/js/components/database/new-forms/components/fields/DateFieldRenderer.tsx';
import { TextAreaFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextAreaFieldRenderer.tsx';
import { SelectFieldRenderer } from '@src/js/components/database/new-forms/components/fields/SelectFieldRender.tsx';
import { SwitchFieldRenderer } from '@src/js/components/database/new-forms/components/fields/SwitchFieldRender.tsx';
import { CKEditorFieldRenderer } from '@src/js/components/database/new-forms/components/fields/CKEditorFieldRenderer.tsx';

class ComponentRegistry {
  static getFieldRenderer(dataType: string) {
    switch (dataType) {
      case FormFieldDataType.VARCHAR:
        return TextFieldRenderer;
      case FormFieldDataType.TIMESTAMP:
        return DateFieldRenderer;
      case FormFieldDataType.MULTILINE_VARCHAR:
      case FormFieldDataType.MONOSPACE_FONT:
        return TextAreaFieldRenderer;
      case FormFieldDataType.CONTROLLED_VOCABULARY:
        return SelectFieldRenderer;
      case FormFieldDataType.BOOLEAN:
        return SwitchFieldRenderer;
      case FormFieldDataType.WORD_PROCESSOR:
      case FormFieldDataType.WORD_PROCESSOR_PAGE:
      case FormFieldDataType.WORD_PROCESSOR_CLASSIC:
        return CKEditorFieldRenderer;
      default:
        return TextFieldRenderer; 
    }
  }
}

export default ComponentRegistry;
