# Action Renderers

Action renderers are React components responsible for rendering form actions (buttons, switches, etc.) in the form UI. They translate action definitions into interactive UI elements.

## Overview

Action renderers receive action definitions and render them as interactive components. They are registered in the `ComponentRegistry` and retrieved based on the action's `component` type.

## Components

### ButtonActionRenderer

Renders actions as standard buttons. This is the default action renderer for most actions.

**File**: `ButtonActionRenderer.tsx`

**Props**: `ActionRendererProps`
- `action`: The action definition containing `name`, `label`, and other metadata from `FormAction` type
- `onAction`: Callback function to execute when the action is triggered
- `mode`: Current form mode (view/edit/create)

**Features**:
- Automatically determines button type based on action name
  - Actions containing `'save'` → `'final'` type (primary action)
  - All other actions → `'neutral'` type
- Passes action name to `onAction` callback when clicked


### SwitchActionRenderer

Renders actions as Material-UI Switch components. Used for toggle-style actions (e.g., auto-save).

**File**: `SwitchActionRenderer.tsx`

**Props**: `ActionRendererProps`
- `action`: The action definition with `name`, `label`, `value` (boolean), and optional `handler`
- `onAction`: Callback function (may not be used if action has its own handler)
- `mode`: Current form mode

**Features**:
- Renders as a Material-UI Switch with label
- Uses `action.value` for checked state
- Calls `action.handler` if provided, otherwise uses `onAction`
- Label placement: `'start'` (label appears before switch)
- Small size switch


## Action Renderer Interface

All action renderers must implement the `ActionRendererProps` interface:

```typescript
interface ActionRendererProps {
  action: FormAction;      // Action definition
  onAction: (actionName: string) => void;  // Action handler callback
  mode: FormMode;          // Current form mode
}
```

## Registration

Action renderers are registered in `ComponentRegistry.getActionRenderer()`:

```typescript
// In ComponentRegistry.ts
static getActionRenderer(componentType: string) {
  switch (componentType) {
    case 'button':
      return ButtonActionRenderer;
    case 'switch':
      return SwitchActionRenderer;
    default:
      return ButtonActionRenderer;  // Default fallback
  }
}
```

## Creating a New Action Renderer

To add a new action renderer:

### 1. Create the Component

```typescript
// NewActionRenderer.tsx
import React from 'react';
import { ActionRendererProps } from '@src/js/components/database/new-forms/types/form.types.ts';

export const NewActionRenderer: React.FC<ActionRendererProps> = ({ 
  action, 
  onAction, 
  mode 
}) => {
  // Your renderer implementation
  return (
    <YourComponent
      onClick={() => onAction(action.name)}
      // ... other props
    />
  );
};
```

### 2. Register in ComponentRegistry

```typescript
// In ComponentRegistry.ts
import { NewActionRenderer } from '@src/js/components/database/new-forms/components/actions/NewActionRenderer.tsx';

static getActionRenderer(componentType: string) {
  switch (componentType) {
    case 'button':
      return ButtonActionRenderer;
    case 'switch':
      return SwitchActionRenderer;
    case 'new-action-type':  // Add your new type
      return NewActionRenderer;
    default:
      return ButtonActionRenderer;
  }
}
```

### 3. Use in Form Definition

When defining actions in your form, specify the component type:

```typescript
const actions = [
  {
    name: 'my-action',
    label: 'My Action',
    component: 'new-action-type'  // This will use NewActionRenderer
  }
];
```

## Best Practices

1. **Consistent Behavior**: All action renderers should call `onAction(action.name)` when triggered
2. **Mode Awareness**: Consider the form mode when rendering (some actions may be disabled in view mode)
3. **Accessibility**: Ensure keyboard navigation and screen reader support
4. **Type Safety**: Use TypeScript interfaces and proper typing
5. **Error Handling**: Handle cases where action data might be incomplete
6. **Styling**: Follow the application's design system and use consistent styling

## Common Patterns

### Conditional Rendering Based on Mode

```typescript
export const ConditionalActionRenderer: React.FC<ActionRendererProps> = ({ 
  action, 
  onAction, 
  mode 
}) => {
  const isDisabled = mode === FormMode.VIEW && action.requiresEdit;
  
  return (
    <Button
      onClick={() => onAction(action.name)}
      disabled={isDisabled}
    />
  );
};
```

### Action with Custom Handler

```typescript
export const CustomHandlerRenderer: React.FC<ActionRendererProps> = ({ 
  action, 
  onAction, 
  mode 
}) => {
  const handleClick = () => {
    if (action.handler) {
      action.handler(action.name);
    } else {
      onAction(action.name);
    }
  };
  
  return <Button onClick={handleClick} />;
};
```

## Related Files

- `ComponentRegistry.ts` - Registration and retrieval of action renderers
- `form.types.ts` - Type definitions (`ActionRendererProps`, `FormAction`)
- `EntityForm.tsx` - Usage of action renderers in forms

