import {Form} from "@src/js/components/database/new-forms/types/formITypes.ts";
import {IFormController} from "@src/js/components/database/new-forms/types/IFormController.ts";
import {DialogState} from "@src/js/components/database/new-forms/hooks/useDialogState.ts";
import {useCallback} from "react";
import {formatErrorForLogging, getErrorMessage} from "@src/js/components/database/new-forms/utils/errorUtil.ts";

interface UseExportFlowParams {
    form: Form | null;
    controller: IFormController;
    dialogs: DialogState;
    openExportDialog: (info: any) => void;
    closeExportDialog: () => void;
    loadForm: () => Promise<void>;
    setLoading: (loading: boolean) => void;
    setSaving: (saving: boolean) => void;
    setError: (error: any) => void;
    clearError: () => void;
    externalAppController?: any;
    actionToastContext?: any;
}

interface UseExportFlowResult {
    handleExportRequest: () => Promise<void>,
    handleExportConfirm: () => Promise<void>,
    handleExportCancel: () => Promise<void>,
}

export const useExportFlow = ({
    form,
    controller,
    dialogs,
    openExportDialog,
    closeExportDialog,
    loadForm,
    setLoading,
    setSaving,
    setError,
    clearError,
    externalAppController,
    actionToastContext,
}: UseExportFlowParams): UseExportFlowResult => {
    const handleExportRequest = useCallback(async () => {
        if (!form || !controller) {
            return;
        }

        try {
            setLoading(true);
            clearError();
            openExportDialog(form);
        } catch (error: any) {
            const errorMessage = getErrorMessage(error, 'Failed to prepare export operation');
            setError(errorMessage);
            console.error(formatErrorForLogging(error, 'useExportFlow.handleExportRequest'));
        } finally {
            setLoading(false);
        }
    }, [form, controller, setLoading, clearError, openExportDialog, setError]);

    const handleExportConfirm = useCallback(
        async (exportResult?: any) => {
            if (!form || !controller) {
                return;
            }

            closeExportDialog();

            try {
                if (exportResult && exportResult.success) {
                    setSaving(true);
                    clearError();

                    await loadForm();
                    actionToastContext?.raiseSuccess(`Entity successfully exported to ${exportResult.targetKind}: ${exportResult.targetIdentifier} `);
                    // if (externalAppController?.objectExport) {
                    //     externalAppController.objectExport({
                    //         type: form.entityKind == EntityKind.SAMPLE ? EntityKind.OBJECT : form.entityKind
                    //     });
                    // }
                } else {
                    // noinspection ExceptionCaughtLocallyJS
                    throw exportResult.error;
                }
            } catch (error: any) {
                const errorMessage = getErrorMessage(error, 'Failed to export entity');
                setError(errorMessage);
                console.error(formatErrorForLogging(error, 'useExportFlow.handleExportConfirm'));
            } finally {
                setSaving(false);
            }
        },
        [
            form,
            controller,
            closeExportDialog,
            setSaving,
            clearError,
            loadForm,
            externalAppController,
            dialogs.export.info,
            setError,
        ]
    );

    const handleExportCancel = useCallback(async () => {
        closeExportDialog();
    }, [closeExportDialog]);

    return {
        handleExportRequest,
        handleExportConfirm,
        handleExportCancel,
    };
}