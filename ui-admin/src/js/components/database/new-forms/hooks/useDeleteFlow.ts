import { useCallback } from 'react';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { IFormController } from '@src/js/components/database/new-forms/types/IFormController.ts';
import { IExtendedActionContext } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { DialogState } from '@src/js/components/database/new-forms/hooks/useDialogState.ts';
import { EntityKind } from '@src/js/components/database/new-forms/types/formEnums.ts';

/**
 * Normalizes dependent entities structure to a consistent format
 * Different entity types return different structures, so we normalize them here
 */
function normalizeDependentEntities(entityKind: string, dependentEntities: any): {
  experiments: any[];
  samples: any[];
  datasets: any[];
  totalCount: number;
} {
  const normalized = {
    experiments: [] as any[],
    samples: [] as any[],
    datasets: [] as any[],
    totalCount: 0,
  };

  // Handle different entity-specific structures
  switch (entityKind) {
    case EntityKind.SPACE:
      // Space returns { projects, samples }
      normalized.experiments = dependentEntities.projects || [];
      normalized.samples = dependentEntities.samples || [];
      break;

    case EntityKind.PROJECT:
      // Project returns { experiments, samples }
      normalized.experiments = dependentEntities.experiments || [];
      normalized.samples = dependentEntities.samples || [];
      break;

    case EntityKind.COLLECTION:
      // Collection (Experiment) returns { samples, datasets }
      normalized.experiments = []; // Collection itself is an experiment
      normalized.samples = dependentEntities.samples || [];
      normalized.datasets = dependentEntities.datasets || [];
      break;

    case EntityKind.OBJECT:
      // Object (Sample) returns { datasets, children }
      // children are descendant objects (samples)
      normalized.samples = dependentEntities.children || [];
      normalized.datasets = dependentEntities.datasets || [];
      break;

    case EntityKind.DATASET:
      // Dataset returns { datasets, samples } but typically empty
      normalized.datasets = dependentEntities.datasets || [];
      normalized.samples = dependentEntities.samples || [];
      break;

    default:
      // Fallback: try common property names
      normalized.experiments = dependentEntities.experiments || dependentEntities.projects || [];
      normalized.samples = dependentEntities.samples || dependentEntities.children || [];
      normalized.datasets = dependentEntities.datasets || [];
  }

  normalized.totalCount =
    normalized.experiments.length +
    normalized.samples.length +
    normalized.datasets.length;

  return normalized;
}

interface UseDeleteFlowParams {
  form: Form | null;
  controller: IFormController;
  entityKind: string;
  dialogs: DialogState;
  getExtendedActionContext: (reason?: string) => IExtendedActionContext;
  openDeleteDialog: (config: any) => void;
  closeDeleteDialog: () => void;
  setSaving: (saving: boolean) => void;
  setError: (error: any) => void;
  clearError: () => void;
  executeOperation: <T, >(
    operation: () => Promise<T>,
    options?: { setLoading?: boolean; setSaving?: boolean }
  ) => Promise<T | null>;
  externalAppController?: any;
  actionToastContext?: any; // For showing success/error toasts
}

/**
 * Hook to encapsulate delete dialog flow
 * Handles dependency checking, dialog opening, and actual deletion
 */
export const useDeleteFlow = ({
  form,
  controller,
  entityKind,
  dialogs,
  getExtendedActionContext,
  openDeleteDialog,
  closeDeleteDialog,
  setSaving,
  setError,
  clearError,
  executeOperation,
  externalAppController,
  actionToastContext,
}: UseDeleteFlowParams) => {
  const handleDeleteWithDependencyCheck = useCallback(async () => {
    if (!form || !controller) {
      return;
    }

    await executeOperation(
      async () => {
        // Check for existing deletions in trashcan
        try {
          await controller.delete(form, { checkOnly: true });
        } catch (deletionError: any) {
          throw new Error(deletionError.message);
        }

        // Check for dependent entities
        const rawDependentEntities = await controller.getDependentEntities(form);

        // Normalize dependent entities structure based on entity kind
        const normalizedDeps = normalizeDependentEntities(entityKind, rawDependentEntities);

        const deleteConfig = {
          includeReason: true,
          numberOfEntities: normalizedDeps.totalCount,
          bypassesTrashcan: normalizedDeps.totalCount === 0,
          dependentEntities: normalizedDeps,
          rawDependentEntities: rawDependentEntities, // Keep original for controller use and dialog display
          entityKind: entityKind,
        };

        openDeleteDialog(deleteConfig);
      },
      { setLoading: true }
    );
  }, [
    form,
    controller,
    entityKind,
    openDeleteDialog,
    executeOperation,
  ]);

  const handleDeleteConfirm = useCallback(
    async (reason: string, includeDescendants?: boolean) => {
      closeDeleteDialog();
      setSaving(true);
      clearError();

      if (form) {
        const context: IExtendedActionContext = getExtendedActionContext(reason);
        // Add rawDependentEntities and includeDescendants to context for controllers
        const deleteContext = {
          ...context,
          rawDependentEntities: dialogs.delete.config?.rawDependentEntities,
          includeDescendants: includeDescendants || false,
        };
        try {
          const result = await controller.delete(form, deleteContext);

          // Check if deletion was skipped (e.g., non-empty project - only moved entities)
          // Controllers can return { skipped: true } to indicate they only moved entities
          if (result && typeof result === 'object' && 'skipped' in result && result.skipped) {
            // Entities were moved to trashcan, but entity itself was not deleted
            actionToastContext?.raiseSuccess(result.message || 'Entities moved to trashcan successfully');
            // Don't close the form, just end the operation
          } else {
            // Entity was deleted, show success and close form
            actionToastContext?.raiseSuccess('Entity moved to trashcan successfully');
            externalAppController?.objectDelete({
              type: entityKind,
              id: context.form.entityPermId,
            });
          }
        } catch (error: any) {
          setError(error.message ?? error);
        } finally {
          setSaving(false);
        }
      }
    },
    [
      form,
      controller,
      getExtendedActionContext,
      closeDeleteDialog,
      setSaving,
      clearError,
      setError,
      externalAppController,
    ]
  );

  const handleDeleteCancel = useCallback(() => {
    closeDeleteDialog();
  }, [closeDeleteDialog]);

  return {
    handleDeleteWithDependencyCheck,
    handleDeleteConfirm,
    handleDeleteCancel,
  };
};
