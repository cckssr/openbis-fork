import { useState } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/form.types';

export const useConflictResolution = () => {
    const [isConflicted, setConflicted] = useState(false);
    const [conflictingFields, setConflictingFields] = useState<string[]>([]);

    const resolveConflicts = (localForm: Form, serverForm: Form) => {
        const conflicts: string[] = [];
        const mergedFields = localForm.fields.map(localField => {
            const serverField = serverForm.fields.find(f => f.id === localField.id);
            if (!serverField) return localField; // Field doesn't exist on server (unlikely)

            const localValue = JSON.stringify(localField.value);
            const serverValue = JSON.stringify(serverField.value);

            // Check if server value is different from the version we started editing with
            // and also different from our current local value.
            if (serverForm.version > localForm.version && serverValue !== localValue) {
                conflicts.push(localField.id);
                // In a real implementation, you would present a UI to choose.
                // For now, we can flag it.
                return { ...localField, meta: { ...localField.meta, hasConflict: true } };
            }
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
    };

    return { isConflicted, conflictingFields, resolveConflicts };
};