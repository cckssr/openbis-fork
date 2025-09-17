// Enhanced Zustand store for New Forms V2 - Three-Layer Architecture

import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { FormStore, FormState, EntityFormConfig, ValidationResult, FormData, FormMode, Permissions } from '@src/js/components/database/new-forms-v2/core/types/index.ts';

export const useFormStore = create<FormStore>()(
  devtools(
    persist(
      (set, get) => ({
        // State
        forms: {},
        activeFormId: null,
        entityControllers: {},
        entityMetadata: {},
        entityPermissions: {},

        // Entity-specific actions
        createEntityForm: (config: EntityFormConfig) => {
          const formId = config.formId;
          const newForm: FormState = {
            entityType: config.entityType,
            entityId: config.entityId,
            mode: config.mode,
            data: config.data,
            schema: config.schema,
            validation: {},
            isDirty: false,
            isValid: true,
            isLoading: false,
          };
          
          set((state) => ({
            forms: {
              ...state.forms,
              [formId]: newForm,
            },
            entityControllers: {
              ...state.entityControllers,
              [formId]: config.controller,
            },
            entityMetadata: {
              ...state.entityMetadata,
              [formId]: config.metadata,
            },
            entityPermissions: {
              ...state.entityPermissions,
              [formId]: config.permissions,
            },
            activeFormId: formId,
          }));
        },

        setEntityController: (formId: string, controller: any) => {
          set((state) => ({
            entityControllers: {
              ...state.entityControllers,
              [formId]: controller,
            },
          }));
        },

        setEntityMetadata: (formId: string, metadata: any) => {
          set((state) => ({
            entityMetadata: {
              ...state.entityMetadata,
              [formId]: metadata,
            },
          }));
        },

        setEntityPermissions: (formId: string, permissions: Permissions) => {
          set((state) => ({
            entityPermissions: {
              ...state.entityPermissions,
              [formId]: permissions,
            },
          }));
        },

        // Form data actions
        updateFormData: (formId: string, data: Partial<FormData>) => {
          set((state) => {
            const form = state.forms[formId];
            if (!form) return state;

            return {
              forms: {
                ...state.forms,
                [formId]: {
                  ...form,
                  data: { ...form.data, ...data },
                  isDirty: true,
                },
              },
            };
          });
        },

        setFormMode: (formId: string, mode: FormMode) => {
          set((state) => {
            const form = state.forms[formId];
            if (!form) return state;

            return {
              forms: {
                ...state.forms,
                [formId]: {
                  ...form,
                  mode,
                },
              },
            };
          });
        },

        // Validation actions
        validateForm: (formId: string): ValidationResult => {
          const form = get().forms[formId];
          if (!form) {
            return { isValid: false, errors: { general: 'Form not found' } };
          }

          const errors: { [fieldId: string]: string } = {};
          let isValid = true;

          // Validate each field
          Object.entries(form.schema.fields).forEach(([fieldId, field]) => {
            const value = form.data[fieldId];
            const validation = field.validation;

            if (validation?.required && (!value || value === '')) {
              errors[fieldId] = `${field.label} is required`;
              isValid = false;
            } else if (validation?.minLength && value && value.length < validation.minLength) {
              errors[fieldId] = `${field.label} must be at least ${validation.minLength} characters`;
              isValid = false;
            } else if (validation?.maxLength && value && value.length > validation.maxLength) {
              errors[fieldId] = `${field.label} must be no more than ${validation.maxLength} characters`;
              isValid = false;
            } else if (validation?.pattern && value && !new RegExp(validation.pattern).test(value)) {
              errors[fieldId] = `${field.label} format is invalid`;
              isValid = false;
            } else if (validation?.custom) {
              const customError = validation.custom(value);
              if (customError) {
                errors[fieldId] = customError;
                isValid = false;
              }
            }
          });

          // Update form validation state
          set((state) => {
            const form = state.forms[formId];
            if (!form) return state;

            const validation: { [fieldId: string]: { isValid: boolean; error?: string } } = {};
            Object.keys(form.schema.fields).forEach(fieldId => {
              validation[fieldId] = {
                isValid: !errors[fieldId],
                error: errors[fieldId],
              };
            });

            return {
              forms: {
                ...state.forms,
                [formId]: {
                  ...form,
                  validation,
                  isValid,
                },
              },
            };
          });

          return { isValid, errors };
        },

        // Save action
        saveForm: async (formId: string): Promise<void> => {
          const form = get().forms[formId];
          const controller = get().entityControllers[formId];
          
          if (!form || !controller) {
            throw new Error(`Form ${formId} not found or controller not available`);
          }

          // Validate before saving
          const validation = get().validateForm(formId);
          if (!validation.isValid) {
            throw new Error('Form validation failed');
          }

          // Set loading state
          get().setFormLoading(formId, true);

          try {
            await controller.save(form.data);
            
            // Clear dirty state after successful save
            get().setFormDirty(formId, false);
          } catch (error) {
            console.error('Save failed:', error);
            throw error;
          } finally {
            get().setFormLoading(formId, false);
          }
        },

        // Utility actions
        deleteForm: (formId: string) => {
          set((state) => {
            const newForms = { ...state.forms };
            const newControllers = { ...state.entityControllers };
            const newMetadata = { ...state.entityMetadata };
            const newPermissions = { ...state.entityPermissions };

            delete newForms[formId];
            delete newControllers[formId];
            delete newMetadata[formId];
            delete newPermissions[formId];

            return {
              forms: newForms,
              entityControllers: newControllers,
              entityMetadata: newMetadata,
              entityPermissions: newPermissions,
              activeFormId: state.activeFormId === formId ? null : state.activeFormId,
            };
          });
        },

        setActiveForm: (formId: string) => {
          set({ activeFormId: formId });
        },

        setFormLoading: (formId: string, isLoading: boolean) => {
          set((state) => {
            const form = state.forms[formId];
            if (!form) return state;

            return {
              forms: {
                ...state.forms,
                [formId]: {
                  ...form,
                  isLoading,
                },
              },
            };
          });
        },

        setFormDirty: (formId: string, isDirty: boolean) => {
          set((state) => {
            const form = state.forms[formId];
            if (!form) return state;

            return {
              forms: {
                ...state.forms,
                [formId]: {
                  ...form,
                  isDirty,
                },
              },
            };
          });
        },

        setFormValid: (formId: string, isValid: boolean) => {
          set((state) => {
            const form = state.forms[formId];
            if (!form) return state;

            return {
              forms: {
                ...state.forms,
                [formId]: {
                  ...form,
                  isValid,
                },
              },
            };
          });
        },
      }),
      {
        name: 'form-store',
        partialize: (state) => ({
          forms: state.forms,
          activeFormId: state.activeFormId,
          entityMetadata: state.entityMetadata,
          entityPermissions: state.entityPermissions,
          // Don't persist controllers as they contain functions
        }),
      }
    ),
    {
      name: 'form-store',
    }
  )
);