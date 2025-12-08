import { useState, useCallback } from 'react';

/**
 * Centralized hook for managing dialog states
 * Groups related dialog state together for better maintainability
 */
export interface DialogState {
  conflict: {
    isOpen: boolean;
    fields: any[];
    isResolving: boolean;
  };
  delete: {
    isOpen: boolean;
    config: any | null;
  };
  move: {
    isOpen: boolean;
    info: any | null;
  };
}

const initialDialogState: DialogState = {
  conflict: {
    isOpen: false,
    fields: [],
    isResolving: false,
  },
  delete: {
    isOpen: false,
    config: null,
  },
  move: {
    isOpen: false,
    info: null,
  },
};

export const useDialogState = () => {
  const [dialogs, setDialogs] = useState<DialogState>(initialDialogState);

  // Conflict dialog actions
  const openConflictDialog = useCallback((fields: any[]) => {
    setDialogs(prev => ({
      ...prev,
      conflict: { isOpen: true, fields, isResolving: false },
    }));
  }, []);

  const closeConflictDialog = useCallback(() => {
    setDialogs(prev => ({
      ...prev,
      conflict: { ...prev.conflict, isOpen: false, fields: [] },
    }));
  }, []);

  const setConflictResolving = useCallback((isResolving: boolean) => {
    setDialogs(prev => ({
      ...prev,
      conflict: { ...prev.conflict, isResolving },
    }));
  }, []);

  // Delete dialog actions
  const openDeleteDialog = useCallback((config: any) => {
    setDialogs(prev => ({
      ...prev,
      delete: { isOpen: true, config },
    }));
  }, []);

  const closeDeleteDialog = useCallback(() => {
    setDialogs(prev => ({
      ...prev,
      delete: { isOpen: false, config: null },
    }));
  }, []);

  // Move dialog actions
  const openMoveDialog = useCallback((info: any) => {
    setDialogs(prev => ({
      ...prev,
      move: { isOpen: true, info },
    }));
  }, []);

  const closeMoveDialog = useCallback(() => {
    setDialogs(prev => ({
      ...prev,
      move: { isOpen: false, info: null },
    }));
  }, []);

  return {
    dialogs,
    // Conflict dialog
    openConflictDialog,
    closeConflictDialog,
    setConflictResolving,
    // Delete dialog
    openDeleteDialog,
    closeDeleteDialog,
    // Move dialog
    openMoveDialog,
    closeMoveDialog,
  };
};

