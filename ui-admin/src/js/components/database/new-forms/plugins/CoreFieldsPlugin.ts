// --- plugins/CoreFieldsPlugin.ts ---
import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { FormFieldDataType } from '@src/js/components/database/new-forms/types/form.types.ts';

// Import all your individual renderer components
import { TextFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextFieldRenderer.tsx';
import { DateFieldRenderer } from '@src/js/components/database/new-forms/components/fields/DateFieldRenderer.tsx';
import { TextAreaFieldRenderer } from '@src/js/components/database/new-forms/components/fields/TextAreaFieldRenderer.tsx';
// ... import others

export function registerCoreFieldsPlugin() {
  console.log("Registering Core Field Renderers...");
  FormEngineRegistry.registerFieldRenderer(FormFieldDataType.VARCHAR, TextFieldRenderer);
  FormEngineRegistry.registerFieldRenderer(FormFieldDataType.TIMESTAMP, DateFieldRenderer);
  FormEngineRegistry.registerFieldRenderer(FormFieldDataType.MULTILINE_VARCHAR, TextAreaFieldRenderer);
  // ... register all other core field types
}