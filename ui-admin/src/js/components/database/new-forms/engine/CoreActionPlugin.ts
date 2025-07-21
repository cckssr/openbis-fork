import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { CoreFormModel } from '@src/js/components/database/new-forms/engine/CoreFormModel.ts';

export function registerCoreActionsPlugin() {
    console.log("Registering Core Actions...");
    FormEngineRegistry.registerAction('edit', CoreFormModel.editAction);
    FormEngineRegistry.registerAction('cancel', CoreFormModel.cancelEditAction);
    FormEngineRegistry.registerAction('new-form:cancel', CoreFormModel.cancelNewFormAction);
}