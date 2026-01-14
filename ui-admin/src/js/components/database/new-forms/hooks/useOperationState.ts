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

  return {
    operationState,
    setLoading,
    setSaving,
    setError,
    clearError,
    resetOperationState,
  };
};

