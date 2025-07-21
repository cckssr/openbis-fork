import React from 'react';
import { ActionContext, ActionRendererProps, FieldRendererProps, FormFieldDataType } from '@src/js/components/database/new-forms/types/form.types.ts';

// Define the types for our factories and components
type ControllerFactory = (openbisFacade: any, user?: string) => any;
type FormViewComponent = React.ComponentType<any>;
type FieldRendererComponent = React.ComponentType<FieldRendererProps>;
type ActionHandler = (context: ActionContext) => void;
type ActionRendererComponent = React.ComponentType<ActionRendererProps>;

class FormEngineRegistry {
  private static controllers: Record<string, ControllerFactory> = {};
  private static formViews: Record<string, FormViewComponent> = {};
  private static fieldRenderers: Record<string, FieldRendererComponent> = {};
  private static actions: Record<string, ActionHandler> = {};
  private static actionRenderers: Record<string, ActionRendererComponent> = {};

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

  // --- Field Renderer Registration ---
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

  // --- Action Registration ---
  static registerAction(name: string, handler: ActionHandler) {
    console.log(`[Registry] Registering Action: ${name}`);
    this.actions[name] = handler;
  }

  static getAction(name: string): ActionHandler | undefined {
    return this.actions[name];
  }

  static registerActionRenderer(actionComponentType: string, component: ActionRendererComponent) {
    console.log(`[Registry] Registering Action Renderer: ${actionComponentType}`);
    this.actionRenderers[actionComponentType] = component;
  }

  static getActionRenderer(actionComponentType: string): ActionRendererComponent {
    const component = this.actionRenderers[actionComponentType];
    if (!component) {
      return this.actionRenderers['button'];
    }
    return component;
  }
}

export default FormEngineRegistry;