import { Form, FormField } from '@src/js/components/database/new-forms/types/form.types.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';

export interface Conflict {
  fieldId: string;
  fieldName: string;
  localValue: any;
  serverValue: any;
  localField: FormField;
  serverField: FormField;
}

export class ConflictService {
  /**
   * Check if there's a modification date conflict between local and server forms
   */
  checkModificationDateConflict(localForm: Form, serverForm: Form): boolean {
    const localDate = findFormFieldById(localForm.fields, localForm.entityPermId, 'modificationDate', true);
    const serverDate = findFormFieldById(serverForm.fields, serverForm.entityPermId, 'modificationDate', true);
    
    if (!localDate || !serverDate) return false;
    if (typeof localDate !== 'string' || typeof serverDate !== 'string') return false;
    
    return new Date(serverDate) > new Date(localDate);
  }

  /**
   * Find all conflicting fields between local and server forms
   */
  findConflicts(localForm: Form, serverForm: Form): Conflict[] {
    const conflicts: Conflict[] = [];
    
    // Only check editable fields for conflicts
    const editableFields = localForm.fields.filter(field => !field.readOnly);
    
    for (const localField of editableFields) {
      const serverField = serverForm.fields.find(f => f.id === localField.id);
      if (!serverField) continue;

      const localValue = localField.value;
      const serverValue = serverField.value;

      // Compare values (deep equality for objects, strict for primitives)
      const areEqual = this.valuesEqual(localValue, serverValue);
      if (!areEqual) {
        conflicts.push({
          fieldId: localField.id,
          fieldName: localField.label || localField.id,
          localValue,
          serverValue,
          localField,
          serverField
        });
      }
    }

    return conflicts;
  }

  /**
   * Resolve conflicts by merging local and server forms
   * This is a simple implementation - in practice, you might want more sophisticated merging
   */
  resolveConflicts(localForm: Form, serverForm: Form, conflictResolutions: Record<string, 'local' | 'server' | 'custom'> = {}): Form {
    const resolvedFields = localForm.fields.map(localField => {
      const serverField = serverForm.fields.find(f => f.id === localField.id);
      if (!serverField) return localField;

      const resolution = conflictResolutions[localField.id];
      if (!resolution) return localField; // No conflict or no resolution specified

      switch (resolution) {
        case 'local':
          return localField;
        case 'server':
          return serverField;
        case 'custom':
          // In a real implementation, you might have custom merged values
          return localField;
        default:
          return localField;
      }
    });

    return {
      ...localForm,
      fields: resolvedFields,
      version: serverForm.version // Use server version after resolution
    };
  }

  /**
   * Check if two values are equal (handles objects, arrays, primitives)
   */
  private valuesEqual(a: any, b: any): boolean {
    if (a === b) return true;
    if (a === null || b === null) return a === b;
    if (typeof a !== typeof b) return false;
    
    if (typeof a === 'object') {
      try {
        return JSON.stringify(a) === JSON.stringify(b);
      } catch {
        return false;
      }
    }
    
    return false;
  }
}
