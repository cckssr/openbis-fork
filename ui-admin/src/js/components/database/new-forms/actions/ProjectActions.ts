// ============================================================================
// 5. PLUGIN: A refactored Project Plugin
// Description: The controller is gone. The plugin now just registers actions.
// ============================================================================

// --- actions/ProjectActions.ts ---
import { ActionContext, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

export const saveProjectAction = async (context: ActionContext) => {
    const { form, openbisFacade, onAfterSave } = context;
    console.log("Saving project:", form);
    // Example of an API call
    // await openbisFacade.updateProject(form.entityPermId, { ... });
    await new Promise(resolve => setTimeout(resolve, 1000)); // Simulate async
    console.log("Project saved successfully!");
    onAfterSave();
};
