import { FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import { Form } from '@src/js/components/database/new-forms/types/formITypes.ts';

/**
 * Defines the contract for all entity-specific form controllers.
 * The controller encapsulates all business logic for a specific entity type.
 */
export interface IFormController {
  /**
   * Fetches necessary data to build or update the form model.
   * This is where you adapt an entity DTO from the V3 API to the unified Form DTO.
   * @param entityPermId The permanent ID of the entity to load.
   * @returns A promise that resolves to the adapted Form object.
   */
  load(entityPermId: string, entityKind?: string, params?: any): Promise<Form>;

  /**
   * Saves the current state of the form to openBIS.
   * @param form The current form state.
   * @returns A promise that resolves with the new version of the entity.
   */
  save(form: Form, mode?: FormMode): Promise<any>;

  /**
   * Checks the current user's permissions for various actions on the form's entity.
   * @param form The form object representing the entity.
   * @returns A promise resolving to a map of user permissions.
   */
  checkPermissions(form: Form): Promise<Record<'canEdit' | 'canDelete' | 'canMove', boolean>>;

  /**
   * Deletes the entity represented by the form.
   * @param form The form object representing the entity to delete.
   * @param context Optional context containing additional information like delete reason and dependent entities.
   * @returns A promise that resolves when the deletion is complete.
   *         Can return { skipped: true, message: string } if deletion was skipped (e.g., non-empty project - only moved entities).
   */
  delete(form: Form, context?: any): Promise<void | { skipped: boolean; message?: string }>;

  /**
   * Gets dependent entities that would be affected by deleting this entity.
   * @param form The form object representing the entity.
   * @returns A promise resolving to an object containing arrays of dependent entities.
   */
  getDependentEntities(form: Form): Promise<any>;

  /**
   * Moves the entity represented by the form.
   * @param form The form object representing the entity to move.
   * @returns A promise that resolves when the move is complete.
   */
  move(form: Form, context?: any, params?: any): Promise<void>;

  /**
   * Handles the "export" process for the entity.
   * @param form the form object to export.
   */
  export(form: Form): Promise<void>;

}