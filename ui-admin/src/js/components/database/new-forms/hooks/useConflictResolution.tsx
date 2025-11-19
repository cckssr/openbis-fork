import { useState } from 'react';
import { Form, FormField } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { findFormFieldById } from '@src/js/components/database/new-forms/utils/Utils.ts';

export const useConflictResolution = () => {
    const [isConflicted, setConflicted] = useState(false);
    const [conflictingFields, setConflictingFields] = useState<string[]>([]);

    const findConflicts = (localForm: Form, serverForm: Form) => {
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
    }

    const resolveConflicts = (localForm: Form, serverForm: Form) => {
        const conflicts: string[] = [];
        const mergedFields = localForm.fields
            .filter(localField => !localField.readOnly)
            .map(localField => {
                const serverField = serverForm.fields.find(f => f.id === localField.id);
                if (!serverField) return localField; // Field doesn't exist on server (unlikely)

                const localValue = localField.value;
                const serverValue = serverField.value;

                return localField; // No conflict for this field
            });

        if (conflicts.length > 0) {
            setConflicted(true);
            setConflictingFields(conflicts);
            // Here you would trigger a UI to show the user the conflicting fields
            // and let them choose which version to keep.
        } else {
            // Silently merge non-conflicting changes by simply using the local form's data
            // and updating its version before the next save attempt.
            setConflicted(false);
        }
        return mergedFields;
    };

    // New function to check modificationDate conflict
    const checkModificationDateConflict = (localForm: Form, serverForm: Form): boolean => {
        const localDate = findFormFieldById(localForm.fields, localForm.entityPermId, 'modificationDate', true);
        const serverDate = findFormFieldById(serverForm.fields, serverForm.entityPermId, 'modificationDate', true);
        if (!localDate || !serverDate) return false;
        if (typeof localDate !== 'string' || typeof serverDate !== 'string') return false;
        return new Date(serverDate) > new Date(localDate);
    };

    return { isConflicted, conflictingFields, resolveConflicts, checkModificationDateConflict, findConflicts };
};