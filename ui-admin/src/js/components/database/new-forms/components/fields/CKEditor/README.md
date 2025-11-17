# CKEditor Implementation Documentation

This document explains the design decisions, architectural choices, and implementation requirements for the CKEditor rich text editor integration in the form system.

## Table of Contents

1. [Overview](#overview)
2. [Why CKEditor?](#why-ckeditor)
3. [Architecture & Design Decisions](#architecture--design-decisions)
4. [Editor Modes](#editor-modes)
5. [Markdown Support](#markdown-support)
6. [Integration with CKEditorFieldRenderer](#integration-with-ckeditorfieldrenderer)
7. [Key Features & Plugins](#key-features--plugins)
8. [Configuration Choices](#configuration-choices)
9. [File Structure](#file-structure)

## Overview

The CKEditor implementation provides a full-featured rich text editing experience for `WORD_PROCESSOR` field types. It supports multiple editor modes, markdown editing, file uploads, and extensive formatting options while maintaining compatibility with the form system's field renderer architecture.

## Why CKEditor?

### Requirements That Led to CKEditor

1. **Rich Text Editing**: The system needed a professional WYSIWYG editor for word processor fields, not just plain text areas
2. **HTML Content Management**: Fields store HTML content that needs to be editable with proper formatting
3. **Markdown Support**: Users need the ability to work with markdown syntax for easier content creation
4. **File Uploads**: Integration with openBIS file service for image and file uploads
5. **Extensibility**: Need for custom plugins (e.g., markdown toggler, title extraction)
6. **Mode Flexibility**: Support for different editor layouts (classic toolbar, document editor, inline)
7. **Professional Features**: Tables, images, links, code blocks, and other advanced formatting

### Why CKEditor 5 Specifically?

- **Modern Architecture**: Built with React in mind, uses modern JavaScript
- **Modular Plugin System**: Easy to add/remove features
- **TypeScript Support**: Better type safety and developer experience
- **Active Development**: Well-maintained with regular updates
- **License Compatibility**: GPL license compatible with the project
- **Documentation**: Comprehensive documentation and examples
- **Customization**: Highly configurable to match application needs

## Architecture & Design Decisions

### Separation of Concerns

The implementation is split into multiple layers:

1. **CKEditorFieldRenderer** (Field Renderer Layer)
   - Handles form integration
   - Manages state (markdown mode, original HTML content)
   - Coordinates between form system and editor
   - Extracts metadata (title, markdown state)

2. **CKEditorClassic/CKEditorDocument** (Editor Component Layer)
   - Wraps CKEditor React component
   - Configures plugins and toolbar
   - Handles editor lifecycle
   - Manages disabled/read-only states

3. **CKEditorConfig** (Configuration Layer)
   - Centralized toolbar configurations
   - Font and heading options
   - Reusable configuration constants

4. **MarkdownToggler** (Custom Plugin Layer)
   - Custom CKEditor plugin
   - Provides markdown toggle button in toolbar
   - Integrates with editor configuration

### State Management Strategy

The implementation uses a sophisticated state management approach:

```typescript
// In CKEditorFieldRenderer.tsx
const [markdownEnabled, setMarkdownEnabled] = useState(false);
const [disabledToolbar, setDisabledToolbar] = useState(true);
const [originalHtmlContent, setOriginalHtmlContent] = useState<string | null>(null);
const editorRef = useRef<any>(null);
```

**Why This Approach?**

1. **Markdown Toggle State**: Tracks whether markdown mode is active
   - Needed to conditionally load Markdown plugins
   - Affects how content is stored and displayed

2. **Original HTML Content**: Preserves HTML when switching to markdown
   - Markdown mode converts HTML to markdown
   - Need to restore original HTML when switching back
   - Prevents data loss during mode transitions

3. **Disabled Toolbar State**: Controls editor interactivity
   - Separate from form mode to allow fine-grained control
   - Can disable toolbar while keeping content visible

4. **Editor Reference**: Direct access to editor instance
   - Needed for title extraction (Title plugin API)
   - Allows programmatic editor control
   - Enables advanced features not available through props

## Editor Modes

### Classic Editor Mode

**File**: `CKEditorClassic.jsx`

**When to Use**: Default mode for most use cases

**Features**:
- Traditional toolbar at the top
- Title plugin support (extracts document title)
- Full feature set
- Better for document-style editing

**Configuration**:
- Uses `ClassicEditor` from CKEditor
- Toolbar: `ITEMS_CLASSIC` from config
- Includes Title plugin
- Fullscreen support with custom container classes

**Use Case**: When you need document-style editing with title extraction

### Document Editor Mode

**File**: `CKEditorDocument.jsx`

**When to Use**: For document-style editing with minimap

**Features**:
- Decoupled toolbar (can be placed anywhere)
- Minimap sidebar for navigation
- Menu bar support
- Better for long documents

**Configuration**:
- Uses `DecoupledEditor` from CKEditor
- Custom toolbar and menu bar placement
- Minimap integration
- More complex DOM structure

**Use Case**: When editing long documents that benefit from minimap navigation

### Inline Editor Mode

**File**: `CKEditorClassic.jsx` (with `mode='inline'`)

**When to Use**: For inline editing within content

**Features**:
- Minimal toolbar
- Inline editing experience
- Balloon toolbar for formatting
- Less intrusive UI

**Configuration**:
- Uses `InlineEditor` from CKEditor
- Toolbar: `ITEMS_INLINE` from config
- Simplified feature set

**Use Case**: When you need inline editing without a full editor UI

### Mode Selection

The mode is determined by `field.meta?.mode`:

```typescript
// In CKEditorFieldRenderer.tsx
mode={field.meta?.mode}  // 'classic', 'document', or 'inline'
```

**Decision**: Allow per-field configuration rather than global setting
- Different fields may need different editor experiences
- Provides flexibility for future use cases
- Defaults to 'classic' if not specified

## Markdown Support

### Why Markdown?

1. **User Preference**: Some users prefer markdown syntax
2. **Easier Writing**: Faster content creation for technical users
3. **Version Control Friendly**: Markdown is easier to diff and review
4. **Export Compatibility**: Easier to export to other formats

### Implementation Strategy

Markdown support is implemented as a **toggleable feature** rather than a separate editor:

**Key Decisions**:

1. **Conditional Plugin Loading**: Markdown plugins only load when enabled
   ```javascript
   if (markdownEnabled) {
     plugins.push(Markdown, PasteFromMarkdownExperimental);
   }
   ```
   - Reduces bundle size when markdown is not used
   - Better performance for HTML-only editing

2. **Content Preservation**: Original HTML is preserved when switching modes
   ```typescript
   if (markdownEnabled) {
     // Switching to markdown: store current HTML
     setOriginalHtmlContent(currentContent);
   } else {
     // Switching to HTML: restore original HTML
     setOriginalHtmlContent(originalHtmlContent);
   }
   ```
   - Prevents data loss during mode switches
   - Allows seamless switching between modes

3. **Custom Toggle Button**: `MarkdownToggler` plugin provides toolbar button
   - Integrated into editor UI
   - Shows current state (HTML/Markdown)
   - Calls parent callback to update state

4. **Metadata Tracking**: Markdown state is stored in field metadata
   ```typescript
   onFieldMetadataChange(field.id, { isMarkdown: markdownEnabled });
   ```
   - Persists user preference
   - Allows restoring markdown state on form load

### Markdown Toggle Flow

```
User clicks markdown toggle
  ↓
MarkdownToggler plugin fires callback
  ↓
CKEditorFieldRenderer.toggleMarkdownMode()
  ↓
If switching TO markdown:
  - Store current HTML in originalHtmlContent
  - Set markdownEnabled = true
  - Editor re-renders with Markdown plugins
  ↓
If switching FROM markdown:
  - Restore original HTML from originalHtmlContent
  - Set markdownEnabled = false
  - Editor re-renders without Markdown plugins
```

## Integration with CKEditorFieldRenderer

### Why Separate Renderer?

The `CKEditorFieldRenderer` serves as an **adapter layer** between the form system and CKEditor:

1. **Form System Compatibility**: Implements `FieldRendererProps` interface
2. **State Coordination**: Manages complex state (markdown, HTML preservation)
3. **Metadata Extraction**: Extracts title and other metadata from editor
4. **Mode Management**: Handles form mode (view/edit) vs editor mode (classic/document)
5. **Callback Coordination**: Coordinates multiple callbacks (content change, metadata change)

### Key Integration Points

#### 1. Editor Instance Access

```typescript
const editorRef = useRef<any>(null);

onEditorReady={(editor: any) => {
  editorRef.current = editor;
}}
```

**Why Needed**: 
- Title plugin API requires direct editor access
- `editor.plugins.get('Title')` is not available through props
- Enables programmatic editor control

#### 2. Content Change Handling

```typescript
const handleEditorChange = useCallback((value: string) => {
  // Extract title if in classic mode
  if (editorMode === 'classic') {
    const titlePlugin = editorRef.current.plugins.get('Title');
    const title = titlePlugin.getTitle();
    onFieldMetadataChange(field.id, { title });
  }
  
  // Update markdown state
  onFieldMetadataChange(field.id, { isMarkdown: markdownEnabled });
  
  // Update field value
  onFieldChange(field.id, value);
}, [onFieldChange, field.id, markdownEnabled, onFieldMetadataChange]);
```

**Why This Approach**:
- Single callback handles multiple concerns
- Ensures metadata stays in sync with content
- Prevents race conditions between updates

#### 3. Mode-Based Toolbar Control

```typescript
useEffect(() => {
  setDisabledToolbar(mode === FormMode.VIEW);
}, [mode]);
```

**Why Separate from Editor Disabled Prop**:
- Form mode (view/edit) is different from editor state
- Allows fine-grained control
- Can disable toolbar while keeping content editable in some scenarios

#### 4. HTML Content Preservation

```typescript
const [originalHtmlContent, setOriginalHtmlContent] = useState<string | null>(null);

useEffect(() => {
  if (originalHtmlContent !== null && onFieldChange) {
    onFieldChange(field.id, originalHtmlContent);
    setOriginalHtmlContent(null);
  }
}, [originalHtmlContent, onFieldChange, field.id]);
```

**Why This Pattern**:
- Markdown mode converts HTML to markdown
- Need to restore HTML when switching back
- Effect ensures restoration happens at right time
- Resets state after application

## Key Features & Plugins

### Plugin Selection Rationale

1. **Comprehensive Feature Set**: Covers all common rich text editing needs
2. **User Expectations**: Matches features users expect from modern editors
3. **OpenBIS Integration**: Image upload integrates with openBIS file service
4. **Extensibility**: Easy to add/remove plugins as needed
5. **Performance**: Only loads plugins that are actually used


### HTML Support Configuration

```javascript
htmlSupport: {
  allow: [{
    name: /^.*$/,
    styles: true,
    attributes: true,
    classes: true
  }]
}
```

**Decision**: Allow all HTML elements, styles, attributes, and classes
- **Why**: Maximum flexibility for content
- **Trade-off**: Less strict validation, but more powerful
- **Security**: Server-side validation should handle security

### Image Upload Configuration

```javascript
simpleUpload: {
  uploadUrl: "/openbis/openbis/file-service/eln-lims?type=Files&sessionID=" + sessionID
}
```

**Decision**: Use openBIS file service for uploads
- Integrates with existing infrastructure
- Session-based authentication
- Consistent with other file operations

### Link Configuration

```javascript
link: {
  addTargetToExternalLinks: true,
  defaultProtocol: 'https://',
  decorators: {
    toggleDownloadable: {
      mode: 'manual',
      label: 'Downloadable',
      attributes: {
        download: 'file'
      }
    }
  }
}
```

**Decision**: 
- External links open in new tab (security)
- Default to HTTPS (security best practice)
- Support downloadable links (common use case)

## File Structure

```
CKEditor/
├── README.md                    # This file
├── CKEditorClassic.jsx          # Classic and inline editor wrapper
├── CKEditorDocument.jsx         # Document editor wrapper
├── CKEditorConfig.js            # Toolbar and configuration constants
├── MarkdownToggler.jsx          # Custom markdown toggle plugin
└── CKEditorClassic.css          # Editor styling
```

### File Responsibilities

**CKEditorClassic.jsx**:
- Wraps ClassicEditor and InlineEditor
- Handles toolbar visibility
- Manages editor lifecycle
- Configures plugins conditionally

**CKEditorDocument.jsx**:
- Wraps DecoupledEditor
- Manages decoupled toolbar/menu bar
- Handles minimap integration
- More complex DOM structure

**CKEditorConfig.js**:
- Centralized configuration
- Toolbar item arrays
- Font and heading options
- Reusable constants

**MarkdownToggler.jsx**:
- Custom CKEditor plugin
- Provides toolbar button
- Integrates with editor config
- Handles toggle callback

**CKEditorClassic.css**:
- Editor container styling
- Read-only state styling
- Custom content styles
- Fullscreen styling

## Implementation Needs & Requirements

### Functional Requirements

1. **Rich Text Editing**: Full WYSIWYG editing with formatting options
2. **HTML Storage**: Store content as HTML in field values
3. **Markdown Support**: Optional markdown editing mode
4. **File Uploads**: Image upload via openBIS file service
5. **Mode Support**: Multiple editor modes (classic, document, inline)
6. **Title Extraction**: Extract document title in classic mode
7. **View Mode**: Read-only display in view mode
8. **Metadata Tracking**: Track markdown state and title

### Technical Requirements

1. **React Integration**: Must work with React component lifecycle
2. **Form System Integration**: Must implement FieldRendererProps
3. **State Management**: Coordinate complex state (markdown, HTML preservation)
4. **Performance**: Lazy load plugins, efficient re-renders
5. **Type Safety**: TypeScript support where possible
6. **Accessibility**: Keyboard navigation, screen reader support

### Design Constraints

1. **Bundle Size**: Minimize impact on application bundle
2. **License Compatibility**: GPL license compatible
3. **Browser Support**: Support modern browsers
4. **Session Management**: Integrate with openBIS session system
5. **Styling**: Match application design system

## Best Practices

### When Adding New Features

1. **Plugin Selection**: Choose existing CKEditor plugins when possible
2. **Custom Plugins**: Create custom plugins for application-specific features
3. **Configuration**: Add configuration to `CKEditorConfig.js` for reusability
4. **State Management**: Keep state in `CKEditorFieldRenderer` for form integration
5. **Testing**: Test mode switching, especially markdown toggle
6. **Performance**: Conditionally load plugins based on features needed

### Common Patterns

1. **Conditional Plugin Loading**: Only load plugins when needed
2. **Editor Reference**: Use refs for direct editor API access
3. **Content Preservation**: Always preserve original content during mode switches
4. **Metadata Sync**: Keep metadata in sync with content changes
5. **Mode-Based Rendering**: Adjust UI based on form mode and editor mode

## Troubleshooting

### Common Issues

1. **Title Plugin Not Available**: Only works in classic mode
2. **Markdown Toggle Not Working**: Check if Markdown plugins are loaded
3. **Content Loss on Mode Switch**: Ensure originalHtmlContent is set
4. **Toolbar Not Hiding**: Check disabledToolbar state and form mode
5. **Upload Not Working**: Verify sessionID is passed correctly

### Debug Tips

1. Check editor instance: `editorRef.current`
2. Verify plugin loading: `editorRef.current.plugins.get('PluginName')`
3. Inspect editor config: Check `editorConfig` in component
4. Monitor state changes: Use React DevTools
5. Check console: CKEditor logs warnings and errors

