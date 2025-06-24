import { useEffect, useRef, useCallback } from 'react';

interface Form {
  [key: string]: any;
}

interface UseAutoSaveProps {
  formData: Form;
  storageKey: string;
  isEnabled: boolean;
  interval?: number;
  onDataRestore?: (data: Form) => void;
}

export const useAutoSave = ({
  formData,
  storageKey,
  isEnabled,
  interval = 60000,
  onDataRestore
}: UseAutoSaveProps) => {
  const formDataRef = useRef(formData);
  const onDataRestoreRef = useRef(onDataRestore);

  // Keep refs updated with latest values
  formDataRef.current = formData;
  onDataRestoreRef.current = onDataRestore;

  // Save data to localStorage
  const saveToStorage = useCallback(() => {
    try {
      const dataToSave = {
        data: formDataRef.current,
        timestamp: Date.now()
      };
      localStorage.setItem(storageKey, JSON.stringify(dataToSave));
    } catch (error) {
      console.warn('Failed to save form data to localStorage:', error);
    }
  }, [storageKey]);

  // Load data from localStorage
  const loadFromStorage = useCallback((): Form | null => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved) {
        const parsed = JSON.parse(saved);
        return parsed.data;
      }
    } catch (error) {
      console.warn('Failed to load form data from localStorage:', error);
    }
    return null;
  }, [storageKey]);

  // Clear saved data
  const clearStorage = useCallback(() => {
    try {
      localStorage.removeItem(storageKey);
    } catch (error) {
      console.warn('Failed to clear form data from localStorage:', error);
    }
  }, [storageKey]);

  // Load saved data on mount if autosave is enabled
  useEffect(() => {
    if (isEnabled) {
      const savedData = loadFromStorage();
      if (savedData && onDataRestoreRef.current) {
        onDataRestoreRef.current(savedData);
      }
    }
  }, [isEnabled, loadFromStorage]);

  // Auto-save interval
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