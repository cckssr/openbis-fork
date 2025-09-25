import { useState, useCallback } from 'react';
import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import { Form } from '@src/js/components/database/new-forms/types/form.types.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';

interface UseFormLoadingProps {
  controller: IFormController;
  permId: string;
  entityKind: string;
  params?: any;
}

interface UseFormLoadingReturn {
  loading: boolean;
  error: string | null;
  loadForm: () => Promise<Form | null>;
  clearError: () => void;
}

export const useFormLoading = ({ 
  controller, 
  permId, 
  entityKind, 
  params 
}: UseFormLoadingProps): UseFormLoadingReturn => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadForm = useCallback(async (): Promise<Form | null> => {
    setLoading(true);
    setError(null);
    
    try {
      const form = await controller.load(permId, entityKind, params);
      return form;
    } catch (err: any) {
      const errorMessage = err.message || 'Failed to load form';
      setError(errorMessage);
      console.error('Form loading error:', err);
      return null;
    } finally {
      setLoading(false);
    }
  }, [controller, permId, entityKind, params]);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    loading,
    error,
    loadForm,
    clearError
  };
};
