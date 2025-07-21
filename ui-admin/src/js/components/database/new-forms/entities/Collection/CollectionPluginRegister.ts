import FormEngineRegistry from '@src/js/components/database/new-forms/engine/FormEngineRegistry.ts';
import { CollectionFormController } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormController.ts';
import { CollectionFormModel } from '@src/js/components/database/new-forms/entities/Collection/CollectionFormModel.ts';
import { EntityKind } from '@src/js/components/database/new-forms/types/form.types.ts';

export function registerCollectionPlugin() {
  FormEngineRegistry.registerController(EntityKind.COLLECTION, (openbisFacade) => new CollectionFormController(openbisFacade));

  FormEngineRegistry.registerAction('collection:save', CollectionFormModel.saveCollectionAction);
}