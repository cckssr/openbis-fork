import React from 'react';
import { ActionContext, FieldRendererProps, FormFieldDataType } from '@src/js/components/database/new-forms/types/form.types.ts';

// Define the types for our factories and components
type ControllerFactory = (openbisFacade: any, user?: string) => any;
type FormViewComponent = React.ComponentType<any>;
type FieldRendererComponent = React.ComponentType<FieldRendererProps>;
type ActionHandler = (context: ActionContext) => void;

class FormEngineRegistry {
  private static controllers: Record<string, ControllerFactory> = {};
  private static formViews: Record<string, FormViewComponent> = {};
  private static fieldRenderers: Record<string, FieldRendererComponent> = {};
  private static actions: Record<string, ActionHandler> = {};

  // --- Controller Registration ---
  static registerController(entityKind: string, factory: ControllerFactory) {
    console.log(`[Registry] Registering Controller for entityKind: ${entityKind}`);
    this.controllers[entityKind] = factory;
  }

  static getController(entityKind: string, openbisFacade: any, user?: string) {
    const factory = this.controllers[entityKind];
    if (!factory) throw new Error(`No controller registered for ${entityKind}`);
    return factory(openbisFacade, user);
  }

  // --- Form View Registration ---
  static registerFormView(entityKind: string, component: FormViewComponent) {
    this.formViews[entityKind] = component;
  }

  static getFormView(entityKind: string) {
    return this.formViews[entityKind];
  }

  // --- Field Renderer Registration (NEW) ---
  static registerFieldRenderer(dataType: string, component: FieldRendererComponent) {
    console.log(`[Registry] Registering Field Renderer for type: ${dataType}`);
    this.fieldRenderers[dataType] = component;
  }

  static getFieldRenderer(dataType: string): FieldRendererComponent {
    const component = this.fieldRenderers[dataType];
    if (!component) {
        return this.fieldRenderers[FormFieldDataType.VARCHAR];
    }
    return component;
  }

  // --- Action Registration (NEW) ---
  static registerAction(name: string, handler: ActionHandler) {
    console.log(`[Registry] Registering Action: ${name}`);
    this.actions[name] = handler;
  }

  static getAction(name: string): ActionHandler | undefined {
    console.log(`[Registry] Getting Action: ${name}`);
    return this.actions[name];
  }
}

export default FormEngineRegistry;