import { WidgetPlugin, WidgetProps, FormContext } from '../types';

/**
 * Widget Registry for managing widget plugins
 * Supports lazy loading and conditional loading of widgets
 */
class WidgetRegistry {
  private static instance: WidgetRegistry;
  private widgets = new Map<string, WidgetPlugin>();
  private loadedWidgets = new Map<string, React.ComponentType<WidgetProps>>();

  private constructor() {}

  static getInstance(): WidgetRegistry {
    if (!WidgetRegistry.instance) {
      WidgetRegistry.instance = new WidgetRegistry();
    }
    return WidgetRegistry.instance;
  }

  /**
   * Register a widget plugin
   */
  register(plugin: WidgetPlugin): void {
    console.log(`[WidgetRegistry] Registering widget: ${plugin.id}`);
    this.widgets.set(plugin.id, plugin);
  }

  /**
   * Load a widget component based on context
   */
  async loadWidget(id: string, context: FormContext): Promise<React.ComponentType<WidgetProps> | null> {
    const plugin = this.widgets.get(id);
    if (!plugin) {
      console.warn(`[WidgetRegistry] Widget ${id} not found`);
      return null;
    }

    // Check if widget should be loaded based on conditions
    if (plugin.loadCondition && !plugin.loadCondition(context)) {
      console.log(`[WidgetRegistry] Widget ${id} load condition not met`);
      return null;
    }

    // Check if already loaded
    if (this.loadedWidgets.has(id)) {
      return this.loadedWidgets.get(id)!;
    }

    // Check dependencies
    if (plugin.dependencies) {
      for (const depId of plugin.dependencies) {
        const depPlugin = this.widgets.get(depId);
        if (!depPlugin) {
          console.warn(`[WidgetRegistry] Dependency ${depId} not found for widget ${id}`);
          return null;
        }
      }
    }

    // Load the widget component
    try {
      const component = await this.loadWidgetComponent(plugin);
      this.loadedWidgets.set(id, component);
      console.log(`[WidgetRegistry] Widget ${id} loaded successfully`);
      return component;
    } catch (error) {
      console.error(`[WidgetRegistry] Failed to load widget ${id}:`, error);
      return null;
    }
  }

  /**
   * Load widget component (can be overridden for different loading strategies)
   */
  private async loadWidgetComponent(plugin: WidgetPlugin): Promise<React.ComponentType<WidgetProps>> {
    // If component is already a React component, return it
    if (typeof plugin.component === 'function') {
      return plugin.component;
    }

    // If component is a lazy-loaded component, load it
    if (typeof plugin.component === 'object' && 'then' in plugin.component) {
      const module = await plugin.component;
      return module.default || module;
    }

    throw new Error(`Invalid widget component for ${plugin.id}`);
  }

  /**
   * Get widget plugin by ID
   */
  getPlugin(id: string): WidgetPlugin | undefined {
    return this.widgets.get(id);
  }

  /**
   * Get all registered widget plugins
   */
  getAllPlugins(): Map<string, WidgetPlugin> {
    return new Map(this.widgets);
  }

  /**
   * Get all loaded widget components
   */
  getLoadedWidgets(): Map<string, React.ComponentType<WidgetProps>> {
    return new Map(this.loadedWidgets);
  }

  /**
   * Check if a widget is registered
   */
  has(id: string): boolean {
    return this.widgets.has(id);
  }

  /**
   * Check if a widget is loaded
   */
  isLoaded(id: string): boolean {
    return this.loadedWidgets.has(id);
  }

  /**
   * Unregister a widget
   */
  unregister(id: string): boolean {
    const removed = this.widgets.delete(id);
    this.loadedWidgets.delete(id);
    return removed;
  }

  /**
   * Clear all widgets
   */
  clear(): void {
    this.widgets.clear();
    this.loadedWidgets.clear();
  }

  /**
   * Get the number of registered widgets
   */
  size(): number {
    return this.widgets.size;
  }

  /**
   * Get the number of loaded widgets
   */
  loadedSize(): number {
    return this.loadedWidgets.size;
  }
}

export const widgetRegistry = WidgetRegistry.getInstance();
export default widgetRegistry;
