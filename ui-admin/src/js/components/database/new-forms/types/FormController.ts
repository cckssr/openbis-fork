import { Form, FormMode } from '@src/js/components/database/new-forms/types/form.types.ts';

/**
 * Defines the contract for all entity-specific form controllers.
 * The controller encapsulates all business logic for a specific entity type.
 */
export interface FormController {
  /**
   * Fetches necessary data to build or update the form model.
   * This is where you adapt an entity DTO from the V3 API to the unified Form DTO.
   * @param entityPermId The permanent ID of the entity to load.
   * @returns A promise that resolves to the adapted Form object.
   */
  load(entityPermId: string): Promise<Form>;

  /**
   * Saves the current state of the form to openBIS.
   * @param form The current form state.
   * @returns A promise that resolves with the new version of the entity.
   */
  save(form: Form, mode: FormMode): Promise<number>;

  /**
   * Checks the current user's permissions for various actions on the form's entity.
   * @param form The form object representing the entity.
   * @returns A promise resolving to a map of user permissions.
   */
  checkPermissions(form: Form): Promise<Record<'canEdit' | 'canDelete' | 'canMove', boolean>>;
  
}