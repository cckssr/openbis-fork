import { FormFieldDataType } from '@src/js/components/database/new-forms/types/formEnums.ts';

import { TextFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextFieldRenderer.tsx';
import { DateFieldRenderer } from '@src/js/components/database/new-forms/components/fields/DateFieldRenderer.tsx';
import { TextAreaFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextAreaFieldRenderer.tsx';
import { SelectFieldRenderer } from '@src/js/components/database/new-forms/components/fields/SelectFieldRender.tsx';
import { SwitchFieldRenderer } from '@src/js/components/database/new-forms/components/fields/SwitchFieldRender.tsx';
import { CKEditorFieldRenderer } from '@src/js/components/database/new-forms/components/fields/CKEditorFieldRenderer.tsx';
import { ObjectFieldRenderer } from '@src/js/components/database/new-forms/components/fields/ObjectFieldRenderer.tsx';

import { ButtonActionRenderer } from '@src/js/components/database/new-forms/components/actions/ButtonActionRenderer.tsx';
import { SwitchActionRenderer } from '@src/js/components/database/new-forms/components/actions/SwitchActionRenderer.tsx';
import { DividerActionRenderer } from '@src/js/components/database/new-forms/components/actions/DividerActionRenderer.tsx';
import { DropdownActionRenderer } from '@src/js/components/database/new-forms/components/actions/DropdownActionRenderer.tsx';
import {
  SourceCodeFieldRenderer
} from "@src/js/components/database/new-forms/components/fields/SourceCodeFieldRenderer.tsx";

class ComponentRegistry {
  static getFieldRenderer(dataType: string) {
    switch (dataType) {
      case FormFieldDataType.VARCHAR:
      case FormFieldDataType.HYPERLINK:
      case FormFieldDataType.INTEGER:
      case FormFieldDataType.REAL:
      case FormFieldDataType.ARRAY_INTEGER:
      case FormFieldDataType.ARRAY_REAL:
      case FormFieldDataType.ARRAY_STRING:
      case FormFieldDataType.ARRAY_TIMESTAMP:
        return TextFieldRenderer;
      case FormFieldDataType.TIMESTAMP:
      case FormFieldDataType.DATE:
        return DateFieldRenderer;
      case FormFieldDataType.MULTILINE_VARCHAR:
      case FormFieldDataType.MONOSPACE_FONT:
        return TextAreaFieldRenderer;
      case FormFieldDataType.JSON:
      case FormFieldDataType.XML:
        return SourceCodeFieldRenderer;
      case FormFieldDataType.CONTROLLEDVOCABULARY:
        return SelectFieldRenderer;
      case FormFieldDataType.BOOLEAN:
        return SwitchFieldRenderer;
      case FormFieldDataType.WORD_PROCESSOR:
      case FormFieldDataType.WORD_PROCESSOR_PAGE:
      case FormFieldDataType.WORD_PROCESSOR_CLASSIC:
        return CKEditorFieldRenderer;
      case FormFieldDataType.SAMPLE:
        return ObjectFieldRenderer;
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
      case 'divider':
        return DividerActionRenderer;
      case 'dropdown':
        return DropdownActionRenderer;
      default:
        return ButtonActionRenderer;
    }
  }
}

export default ComponentRegistry;
