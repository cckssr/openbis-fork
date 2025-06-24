import { Form, FormField, FormFieldDataType } from '../types/form.types';

/**
 * Maps a value to the appropriate FormFieldDataType based on its type
 */
function mapValueToDataType(value: any): FormFieldDataType {
  if (value === null || value === undefined) return FormFieldDataType.VARCHAR;
  
  switch (typeof value) {
    case 'boolean':
      return FormFieldDataType.BOOLEAN;
    case 'number':
      return FormFieldDataType.INTEGER;
    case 'string':
      return value.includes('\n') ? FormFieldDataType.MULTILINE_VARCHAR : FormFieldDataType.VARCHAR;
    case 'object':
      if (value instanceof Date) return FormFieldDataType.TIMESTAMP;
      if (Array.isArray(value)) return FormFieldDataType.VARCHAR;
      return FormFieldDataType.VARCHAR;
    default:
      return FormFieldDataType.VARCHAR;
  }
}

/**
 * Creates a FormField from a key-value pair
 */
function createFormField(key: string, value: any): FormField {
  // Skip internal fields and complex objects
  if (key.startsWith('@') || key === 'fetchOptions' || key === 'raw') {
    return null;
  }

  const dataType = mapValueToDataType(value);
  
  return {
    id: key,
    label: key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1'),
    value: value,
    dataType: dataType,
    isMandatory: false,
    isMultiValue: Array.isArray(value),
    meta: {
      helpText: `Field: ${key}`
    }
  };
}

/**
 * Adapts any DTO object into a Form
 */
export function adaptDtoToForm(dto: any, entityKind: string, entityType: string): Form {
  if (!dto) {
    throw new Error("Invalid DTO: DTO is null or undefined");
  }

  // Extract permId safely
  const permId = dto.permId?.permId || dto.id?.id || 'unknown';
  const code = dto.code || 'unknown';

  // Create fields from all properties
  const fields: FormField[] = Object.entries(dto)
    .map(([key, value]) => createFormField(key, value))
    .filter(field => field !== null);

  return {
    entityPermId: permId,
    entityKind: entityKind,
    entityType: entityType,
    title: `${entityKind}: ${code}`,
    fields: fields,
    version: dto.modificationDate || Date.now(),
    meta: {
      registrationDate: dto.registrationDate,
      modificationDate: dto.modificationDate
    }
  };
}

// Example usage:
/*
const spaceDto = { ... }; // Your raw Space JSON from the API
const spaceForm = adaptDtoToForm(spaceDto, 'SPACE', 'space');

const projectDto = { ... }; // Your raw Project JSON from the API
const projectForm = adaptDtoToForm(projectDto, 'PROJECT', 'project');
*/