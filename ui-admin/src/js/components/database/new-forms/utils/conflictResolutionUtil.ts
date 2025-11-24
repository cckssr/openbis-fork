import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/formFieldUtil.ts';

export const findConflicts = (localForm: Form, serverForm: Form) => {
    const conflicts: [FormField, FormField][] = [];
    localForm.fields
        .filter(localField => !localField.readOnly)
        .forEach(localField => {
            const serverField = serverForm.fields.find(f => f.id === localField.id);
            if (!serverField) return; // Field doesn't exist on server (unlikely)

            const localValue = localField.value;
            const serverValue = serverField.value;

            // Compare values (deep equality for objects, strict for primitives)
            const areEqual = JSON.stringify(localValue) === JSON.stringify(serverValue);
            if (!areEqual) {
                conflicts.push([localField, serverField]);
            }
        });
    return conflicts;
};

// Function to check modificationDate conflict
export const checkModificationDateConflict = (localForm: Form, serverForm: Form): boolean => {
    const localDate = findFormFieldById(localForm.fields, localForm.entityPermId, 'modificationDate', true);
    const serverDate = findFormFieldById(serverForm.fields, serverForm.entityPermId, 'modificationDate', true);
    if (!localDate || !serverDate) return false;
    if (typeof localDate !== 'string' || typeof serverDate !== 'string') return false;
    return new Date(serverDate) > new Date(localDate);
};