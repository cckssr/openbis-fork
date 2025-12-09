import { useEffect, useRef, useCallback } from 'react';
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
  mode: FormMode;
}

/**
 * Hook for handling auto-save restoration logic.
 * Encapsulates all restoration state tracking and prevents infinite loops.
 * 
 * Features:
 * - Restores saved data when entering EDIT mode
 * - Prevents duplicate restorations
 * - Handles entity changes gracefully
 * - Clears stale data automatically
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
        const alreadyRestored = 
          restorationStateRef.current?.entityPermId === currentEntityPermId &&
          restorationStateRef.current?.mode === FormMode.EDIT;

        if (savedData.entityPermId === currentEntityPermId && !alreadyRestored) {
          // Valid saved data for current entity - restore it
          console.log('[useAutoSaveRestore] Restoring saved data for entity:', currentEntityPermId);
          
          // Update refs BEFORE restoring to prevent infinite loop
          formEntityPermIdRef.current = currentEntityPermId;
          restorationStateRef.current = {
            entityPermId: currentEntityPermId,
            mode: FormMode.EDIT
          };
          
          onRestore(savedData);
        } else if (savedData.entityPermId !== currentEntityPermId) {
          // Different entity - clear stale data
          console.log('[useAutoSaveRestore] Clearing stale data for different entity');
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

