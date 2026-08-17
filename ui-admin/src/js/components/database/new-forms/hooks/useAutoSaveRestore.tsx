import { useEffect, useRef } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';

interface UseAutoSaveRestoreProps {
  form: Form | null;
  mode: FormMode;
  isEnabled: boolean;
  loadFromStorage: () => Form | null;
  onRestore: (savedData: Form) => void;
  onClearStorage?: () => void;
}

interface RestorationState {
  entityPermId: string;
}

/**
 * Hook for handling auto-save restoration logic for EDIT-mode forms.
 * Encapsulates all restoration state tracking and prevents infinite loops.
 *
 * Features:
 * - Restores saved data when entering EDIT mode (e.g. clicking "Edit" on a VIEW form)
 * - Prevents duplicate restorations
 * - Handles entity changes gracefully
 * - Clears stale data automatically
 *
 * CREATE-mode forms do NOT use this hook: they have no VIEW -> EDIT-style transition to key
 * a "just entered" check off (they're already in CREATE mode on mount), and their drafts are
 * offered to the user explicitly via a restore/discard dialog instead of being silently
 * auto-applied - see `useEntityAutoSaveFlow`'s `pendingDraft` handling.
 */
export const useAutoSaveRestore = ({
  form,
  mode,
  isEnabled,
  loadFromStorage,
  onRestore,
  onClearStorage
}: UseAutoSaveRestoreProps) => {
  // Track what we've already restored to prevent infinite loops
  const restorationStateRef = useRef<RestorationState | null>(null);
  const previousModeRef = useRef<FormMode>(mode);
  const formEntityPermIdRef = useRef<string | null>(null);

  // Update form entityPermId ref when form changes
  useEffect(() => {
    formEntityPermIdRef.current = form?.entityPermId || null;
  }, [form?.entityPermId]);

  // Main restoration logic
  useEffect(() => {
    // Only restore when in EDIT mode and autosave is enabled
    if (mode !== FormMode.EDIT || !isEnabled) {
      // Reset restoration tracking when leaving EDIT mode
      if (mode !== FormMode.EDIT) {
        restorationStateRef.current = null;
      }
      previousModeRef.current = mode;
      return;
    }

    if (!form) {
      previousModeRef.current = mode;
      return;
    }

    const currentEntityPermId = form.entityPermId;
    const justEnteredEditMode = previousModeRef.current !== FormMode.EDIT;
    const formEntityChanged = formEntityPermIdRef.current !== currentEntityPermId;

    // Only attempt restore when transitioning TO EDIT mode OR when form entity changes
    if (justEnteredEditMode || formEntityChanged) {
      const savedData = loadFromStorage();

      if (savedData) {
        const alreadyRestored = restorationStateRef.current?.entityPermId === currentEntityPermId;

        if (savedData.entityPermId === currentEntityPermId && !alreadyRestored) {
          // Valid saved data for current entity - restore it
          // Update refs BEFORE restoring to prevent infinite loop
          formEntityPermIdRef.current = currentEntityPermId;
          restorationStateRef.current = { entityPermId: currentEntityPermId };

          onRestore(savedData);
        } else if (savedData.entityPermId !== currentEntityPermId) {
          // Different entity - clear stale data
          onClearStorage?.();
          restorationStateRef.current = null;
          formEntityPermIdRef.current = currentEntityPermId;
        }
      } else {
        // No saved data - just update tracking
        formEntityPermIdRef.current = currentEntityPermId;
      }
    } else {
      // Not restoring - just update tracking
      formEntityPermIdRef.current = currentEntityPermId;
    }

    // Update previous mode ref
    previousModeRef.current = mode;
  }, [mode, isEnabled, form, loadFromStorage, onRestore, onClearStorage]);

  // Reset restoration state when form entity changes (outside EDIT mode)
  useEffect(() => {
    if (mode !== FormMode.EDIT && form?.entityPermId !== restorationStateRef.current?.entityPermId) {
      restorationStateRef.current = null;
    }
  }, [mode, form?.entityPermId]);
};
