import { FieldRendererProps } from '../types';

/**
 * Field Registry for managing field renderer components
 * Each entity can register its own field types to avoid loading unused components
 */
class FieldRegistry {
  private static instance: FieldRegistry;
  private fieldRenderers = new Map<string, React.ComponentType<FieldRendererProps>>();

  private constructor() {}

  static getInstance(): FieldRegistry {
    if (!FieldRegistry.instance) {
      FieldRegistry.instance = new FieldRegistry();
    }
    return FieldRegistry.instance;
  }

  /**
   * Register a field renderer component
   */
  register(type: string, component: React.ComponentType<FieldRendererProps>): void {
    console.log(`[FieldRegistry] Registering field type: ${type}`);
    this.fieldRenderers.set(type, component);
  }

  /**
   * Get a field renderer component by type
   */
  get(type: string): React.ComponentType<FieldRendererProps> | undefined {
    return this.fieldRenderers.get(type);
  }

  /**
   * Get all registered field types
   */
  getAll(): Map<string, React.ComponentType<FieldRendererProps>> {
    return new Map(this.fieldRenderers);
  }

  /**
   * Check if a field type is registered
   */
  has(type: string): boolean {
    return this.fieldRenderers.has(type);
  }

  /**
   * Unregister a field type
   */
  unregister(type: string): boolean {
    return this.fieldRenderers.delete(type);
  }

  /**
   * Clear all registered field types
   */
  clear(): void {
    this.fieldRenderers.clear();
  }

  /**
   * Get the number of registered field types
   */
  size(): number {
    return this.fieldRenderers.size;
  }
}

export const fieldRegistry = FieldRegistry.getInstance();
export default fieldRegistry;
