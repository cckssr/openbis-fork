import { FormField } from '../types/form.types.ts';

export interface SectionGroup {
  section: string;
  fields: FormField[];
}

export function groupFieldsBySection(fields: FormField[]): SectionGroup[] {
  const sectionMap: { [section: string]: FormField[] } = {};
  fields.forEach(field => {
    const section = field.section || 'Other';
    if (!sectionMap[section]) {
      sectionMap[section] = [];
    }
    sectionMap[section].push(field);
  });
  return Object.entries(sectionMap).map(([section, fields]) => ({ section, fields }));
} 