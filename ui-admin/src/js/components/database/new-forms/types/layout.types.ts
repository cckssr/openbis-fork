import { Form, FormField } from './form.types';

/**
 * Represents a single item in the layout, which can be a field or a custom component.
 */
export interface LayoutItem {
  id: string; // Corresponds to a FormField id or a custom component key
  type: 'field' | 'custom';
  component?: React.ReactNode; // For custom components
}

/**
 * Represents a section in the form layout.
 */
export interface LayoutSection {
  id: string;
  title: string;
  isCollapsable?: boolean;
  items: LayoutItem[][]; // A 2D array to represent rows and columns of items
}

/**
 * The overall layout definition for a form.
 */
export interface FormLayout {
  sections: LayoutSection[];
}