import { useState, useCallback } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';

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
  new: {
    isOpen: boolean;
    entityKind: string | null;
    actionName: string | null;
  };
  export: {
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
  new: {
    isOpen: false,
    entityKind: null,
    actionName: null,
  },
  export: {
    isOpen: false,
    info: null,
  },
};

export const useDialogState = () => {
  const [dialogs, setDialogs] = useState<DialogState>(initialDialogState);

  // New dialog actions
  const openNewDialog = useCallback((entityKind: string, actionName: string) => {
    setDialogs(prev => ({
      ...prev,
      new: { isOpen: true, entityKind, actionName },
    }));
  }, []);

  const closeNewDialog = useCallback(() => {
    setDialogs(prev => ({
      ...prev,
      new: { isOpen: false, entityKind: null, actionName: null },
    }));
  }, []);

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

  // Export dialog actions
  const openExportDialog = useCallback((info: any) => {
    setDialogs(prev => ({
      ...prev,
      export: { isOpen: true, info },
    }));
  }, []);

  const closeExportDialog = useCallback(() => {
    setDialogs(prev => ({
      ...prev,
      export: { isOpen: false, info: null },
    }));
  }, []);

  return {
    dialogs,
    // New dialog
    openNewDialog,
    closeNewDialog,
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
    // Export dialog
    openExportDialog,
    closeExportDialog
  };
};

