import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { editAction, cancelEditAction, cancelNewFormAction } from '@src/js/components/database/new-forms/actions/CoreActions.ts';

export function registerCoreActionsPlugin() {
    console.log("Registering Core Actions...");
    FormEngineRegistry.registerAction('edit', editAction);
    FormEngineRegistry.registerAction('cancel', cancelEditAction);
    FormEngineRegistry.registerAction('new-form:cancel', cancelNewFormAction);
}