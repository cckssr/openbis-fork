import { ActionContext, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

export const saveSpaceAction = async (context: ActionContext) => {
    const { form, openbisFacade, onAfterSave } = context;
    console.log("Saving space:", form);
    // Example of an API call
    // await openbisFacade.updateSpace(form.entityPermId, { ... });
    console.log("Space saved successfully!");
    onAfterSave();
};

export const editSpaceAction = (context: ActionContext) => {
    context.setMode(FormMode.EDIT);
};

export const newProjectAction = (context: ActionContext) => {
    console.log("newProjectAction", context);
    const { onNewProject, form } = context;
    if (onNewProject) {
        console.log("Invoking onNewProject callback...");
        onNewProject(form.entityPermId);
    } else {
        console.warn("onNewProject callback not provided to context.");
    }
};