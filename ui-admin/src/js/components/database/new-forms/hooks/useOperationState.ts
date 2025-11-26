import { useState, useCallback } from 'react';

/**
 * Hook for managing async operation states (loading, saving, error)
 * Encapsulates the common pattern of async operations with error handling
 */
export interface OperationState {
  loading: boolean;
  saving: boolean;
  error: any | null;
}

const initialOperationState: OperationState = {
  loading: false,
  saving: false,
  error: null,
};

export const useOperationState = () => {
  const [operationState, setOperationState] = useState<OperationState>(initialOperationState);

  const setLoading = useCallback((loading: boolean) => {
    setOperationState(prev => ({ ...prev, loading, error: loading ? null : prev.error }));
  }, []);

  const setSaving = useCallback((saving: boolean) => {
    setOperationState(prev => ({ ...prev, saving, error: saving ? null : prev.error }));
  }, []);

  const setError = useCallback((error: any | null) => {
    setOperationState(prev => ({ ...prev, error }));
  }, []);

  const clearError = useCallback(() => {
    setOperationState(prev => ({ ...prev, error: null }));
  }, []);

  const resetOperationState = useCallback(() => {
    setOperationState(initialOperationState);
  }, []);

  // Helper to wrap async operations with loading/error handling
  const executeOperation = useCallback(
    async <T,>(
      operation: () => Promise<T>,
      options: { setLoading?: boolean; setSaving?: boolean } = {}
    ): Promise<T | null> => {
      const { setLoading: shouldSetLoading = false, setSaving: shouldSetSaving = false } = options;

      try {
        if (shouldSetLoading) setLoading(true);
        if (shouldSetSaving) setSaving(true);
        setError(null);

        const result = await operation();
        return result;
      } catch (error: any) {
        setError(error);
        return null;
      } finally {
        if (shouldSetLoading) setLoading(false);
        if (shouldSetSaving) setSaving(false);
      }
    },
    [setLoading, setSaving, setError]
  );

  return {
    operationState,
    setLoading,
    setSaving,
    setError,
    clearError,
    resetOperationState,
    executeOperation,
  };
};

