import { useEffect, useRef, useCallback } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';

interface AutoSaveStorageData {
  data: Partial<Form>; // Only dirty fields
  dirtyFields: string[]; // Field IDs that were changed
  timestamp: number;
  entityPermId: string;
  version: number; // Form version for schema validation
}

interface UseAutoSaveProps {
  form: Form | null;
  originalForm: Form | null; // Original form state for comparison
  storageKey: string;
  isEnabled: boolean;
  interval?: number;
  maxAge?: number; // Maximum age of saved data in milliseconds (default: 24 hours)
}

/**
 * Hook for auto-saving form data to localStorage.
 * 
 * Features:
 * - Selective saving: Only saves dirty fields (changed fields)
 * - Periodic auto-save at configurable intervals
 * - Save on page unload
 * - Handles storage quota errors gracefully
 * - Validates data age and schema
 * 
 * @param form - Current form state
 * @param originalForm - Original form state for dirty field detection
 * @param storageKey - Unique key for localStorage
 * @param isEnabled - Whether auto-save is enabled
 * @param interval - Save interval in milliseconds (default: 5000)
 * @param maxAge - Maximum age of saved data in ms (default: 24 hours)
 */
export const useAutoSave = ({
  form,
  originalForm,
  storageKey,
  isEnabled,
  interval = 5000,
  maxAge = 24 * 60 * 60 * 1000 // 24 hours
}: UseAutoSaveProps) => {
  const formRef = useRef(form);
  const originalFormRef = useRef(originalForm);

  // Keep refs updated with latest values
  formRef.current = form;
  originalFormRef.current = originalForm;

  /**
   * Gets only the dirty (changed) fields from the form
   */
  const getDirtyFields = useCallback((): Partial<Form> | null => {
    if (!formRef.current || !originalFormRef.current) {
      return null;
    }

    const currentFields = formRef.current.fields;
    const originalFields = originalFormRef.current.fields;
    const dirtyFields: string[] = [];
    const dirtyFieldData: Record<string, any> = {};

    // Compare each field to find changes
    currentFields.forEach((currentField) => {
      const originalField = originalFields.find(f => f.id === currentField.id);
      
      if (!originalField) {
        // New field - consider it dirty
        dirtyFields.push(currentField.id);
        dirtyFieldData[currentField.id] = currentField.value;
        return;
      }

      // Compare values (handle objects/arrays)
      const currentValue = currentField.value;
      const originalValue = originalField.value;

      if (!areValuesEqual(currentValue, originalValue)) {
        dirtyFields.push(currentField.id);
        dirtyFieldData[currentField.id] = currentValue;
      }
    });

    // If no dirty fields, return null
    if (dirtyFields.length === 0) {
      return null;
    }

    // Return minimal form structure with only dirty fields
    return {
      entityPermId: formRef.current.entityPermId,
      entityKind: formRef.current.entityKind,
      entityType: formRef.current.entityType,
      version: formRef.current.version,
      fields: currentFields.map(field => {
        if (dirtyFields.includes(field.id)) {
          return {
            id: field.id,
            value: field.value
          };
        }
        return null;
      }).filter(Boolean) as any[]
    };
  }, []);

  /**
   * Compares two values for equality (handles objects/arrays)
   */
  const areValuesEqual = (valueA: any, valueB: any): boolean => {
    if (valueA === valueB) {
      return true;
    }

    const isObject =
      typeof valueA === 'object' && valueA !== null &&
      typeof valueB === 'object' && valueB !== null;

    if (isObject) {
      try {
        return JSON.stringify(valueA) === JSON.stringify(valueB);
      } catch (error) {
        console.warn('[useAutoSave] Failed to compare values', { error });
        return false;
      }
    }

    return false;
  };

  /**
   * Saves dirty fields to localStorage
   */
  const saveToStorage = useCallback(() => {
    try {
      if (!formRef.current || !originalFormRef.current) {
        return;
      }

      const dirtyData = getDirtyFields();
      
      // Only save if there are dirty fields
      if (!dirtyData) {
        return;
      }

      const storageData: AutoSaveStorageData = {
        data: dirtyData,
        dirtyFields: dirtyData.fields?.map((f: any) => f.id) || [],
        timestamp: Date.now(),
        entityPermId: formRef.current.entityPermId,
        version: formRef.current.version
      };

      localStorage.setItem(storageKey, JSON.stringify(storageData));
      console.log('[useAutoSave] Saved dirty fields:', storageData.dirtyFields.length);
    } catch (error: any) {
      // Handle quota exceeded or other storage errors
      if (error.name === 'QuotaExceededError') {
        console.warn('[useAutoSave] Storage quota exceeded, clearing old data');
        // Try to clear and retry once
        try {
          localStorage.removeItem(storageKey);
          const dirtyData = getDirtyFields();
          if (dirtyData && formRef.current) {
            const storageData: AutoSaveStorageData = {
              data: dirtyData,
              dirtyFields: dirtyData.fields?.map((f: any) => f.id) || [],
              timestamp: Date.now(),
              entityPermId: formRef.current.entityPermId,
              version: formRef.current.version
            };
            localStorage.setItem(storageKey, JSON.stringify(storageData));
          }
        } catch (retryError) {
          console.error('[useAutoSave] Failed to save after clearing storage:', retryError);
        }
      } else {
        console.warn('[useAutoSave] Failed to save form data:', error);
      }
    }
  }, [storageKey, getDirtyFields]);

  /**
   * Loads saved data from localStorage
   * Returns null if data is stale, invalid, or doesn't exist
   */
  const loadFromStorage = useCallback((): Form | null => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (!saved) {
        return null;
      }

      const parsed: AutoSaveStorageData = JSON.parse(saved);

      // Validate data age
      const age = Date.now() - parsed.timestamp;
      if (age > maxAge) {
        console.warn('[useAutoSave] Saved data is too old, discarding');
        localStorage.removeItem(storageKey);
        return null;
      }

      // Validate entity matches
      if (!formRef.current) {
        return null;
      }

      if (parsed.entityPermId !== formRef.current.entityPermId) {
        console.warn('[useAutoSave] Saved data entity mismatch');
        return null;
      }

      // Validate schema version (if form version changed, data might be incompatible)
      if (parsed.version !== formRef.current.version) {
        console.warn('[useAutoSave] Form version mismatch, data might be incompatible');
        // Still return it, but let the caller decide
      }

      // Reconstruct form with saved dirty fields
      if (!parsed.data || !parsed.data.fields) {
        return null;
      }

      // Merge saved dirty fields back into current form
      const restoredForm: Form = {
        ...formRef.current,
        fields: formRef.current.fields.map(field => {
          const savedField = parsed.data.fields?.find((sf: any) => sf.id === field.id);
          if (savedField) {
            return {
              ...field,
              value: savedField.value
            };
          }
          return field;
        })
      };

      return restoredForm;
    } catch (error) {
      console.warn('[useAutoSave] Failed to load form data from localStorage:', error);
      // Clear corrupted data
      try {
        localStorage.removeItem(storageKey);
      } catch (clearError) {
        console.error('[useAutoSave] Failed to clear corrupted data:', clearError);
      }
      return null;
    }
  }, [storageKey, maxAge]);

  /**
   * Clears saved data from localStorage
   */
  const clearStorage = useCallback(() => {
    try {
      localStorage.removeItem(storageKey);
      console.log('[useAutoSave] Cleared saved data');
    } catch (error) {
      console.warn('[useAutoSave] Failed to clear form data from localStorage:', error);
    }
  }, [storageKey]);

  // Periodic auto-save interval
  useEffect(() => {
    if (!isEnabled) {
      return;
    }

    const handle = setInterval(() => {
      saveToStorage();
    }, interval);

    return () => clearInterval(handle);
  }, [isEnabled, interval, saveToStorage]);

  // Save on page unload
  useEffect(() => {
    if (!isEnabled) {
      return;
    }

    const handleBeforeUnload = () => {
      saveToStorage();
    };

    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [isEnabled, saveToStorage]);

  return {
    saveToStorage,
    loadFromStorage,
    clearStorage
  };
};
