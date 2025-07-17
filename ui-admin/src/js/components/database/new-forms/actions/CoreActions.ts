import { ActionContext, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

export const editAction = (context: ActionContext) => {
    context.setMode(FormMode.EDIT);
};

export const cancelEditAction = (context: ActionContext) => {
    context.setMode(FormMode.VIEW);
    // The logic to reset the form state is now handled in the context provider
};

export const cancelNewFormAction = (context: ActionContext) => {
    context.closeForm();
    // The logic to reset the form state is now handled in the context provider
};