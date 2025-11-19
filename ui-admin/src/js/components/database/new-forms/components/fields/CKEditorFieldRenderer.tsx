import React, { useState, useEffect, useRef, useCallback } from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/form.types.ts';
import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';
import CKEditorClassic from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorClassic.jsx';
import FormFieldContainer from '@src/js/components/common/form/FormFieldContainer.jsx';
import FormFieldLabel from '@src/js/components/common/form/FormFieldLabel.jsx';
import Box from '@mui/material/Box';
import { Typography } from '@mui/material';

export const CKEditorFieldRenderer: React.FC<FieldRendererProps> = ({
  field,
  mode,
  onFieldChange,
  onFieldMetadataChange,
  params
}) => {
  const [markdownEnabled, setMarkdownEnabled] = useState(false);
  const [disabledToolbar, setDisabledToolbar] = useState(true);
  const [originalHtmlContent, setOriginalHtmlContent] = useState<string | null>(null);
  const editorRef = useRef<any>(null);
  const isEditingMode = mode === FormMode.EDIT || mode === FormMode.CREATE;
  const isReadOnly = !isEditingMode || field.readOnly;

  useEffect(() => {
    setDisabledToolbar(isReadOnly);
  }, [isReadOnly]);

  // Apply original HTML content when it's available
  useEffect(() => {
    if (originalHtmlContent !== null && onFieldChange && typeof onFieldChange === 'function') {
      onFieldChange(field.id, originalHtmlContent);
      setOriginalHtmlContent(null); // Reset after applying
      //setMarkdownEnabled(field.meta?.isMarkdown ?? false);
    }
  }, [originalHtmlContent, onFieldChange, field.id]);

  // Wrapper function to ensure field updates are properly handled
  const handleEditorChange = useCallback((value: string) => {
    if (editorRef.current) {
      // Title plugin is only loaded in classic mode, so check mode first
      const editorMode = field.meta?.mode || 'classic';
      if (editorMode === 'classic') {
        // Use try-catch because plugins.get() throws if plugin doesn't exist
        try {
          const titlePlugin = editorRef.current.plugins.get('Title');
          if (titlePlugin) {
            const title = titlePlugin.getTitle();
            if (onFieldMetadataChange && typeof onFieldMetadataChange === 'function') {
              onFieldMetadataChange(field.id, { title });
            }
          }
        } catch (error) {
          // Title plugin is not loaded, skip title extraction
          console.debug('Title plugin not available:', error);
        }
      }
      
      if (onFieldMetadataChange && typeof onFieldMetadataChange === 'function') {
        onFieldMetadataChange(field.id, { isMarkdown: markdownEnabled });
      }
    }
    if (onFieldChange && typeof onFieldChange === 'function') {
      onFieldChange(field.id, value);
    }
  }, [onFieldChange, field.id, markdownEnabled, onFieldMetadataChange]);

  const toggleMarkdownMode = () => {
    if (editorRef.current) {
      const currentContent = editorRef.current.getData();

      if (markdownEnabled) {
        // Currently in markdown mode, switching to HTML mode
        // Restore the original HTML content
        if (originalHtmlContent) {
          setOriginalHtmlContent(originalHtmlContent);
        } else {
          // If no original HTML content, keep current content
          setOriginalHtmlContent(currentContent);
        }
      } else {
        // Currently in HTML mode, switching to markdown mode
        // Store the current HTML content as original
        setOriginalHtmlContent(currentContent);
      }
    }

    setMarkdownEnabled(prev => !prev);
    setDisabledToolbar(false);
  };

  return (
    <FormFieldContainer
      description={field.meta?.helpText}
    >
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        <Typography variant="body2" component="div" sx={{ color: '#0000008a', fontSize: '0.7rem' }}>{field.label} {field.required ? '*' : ''}</Typography>
        <CKEditorClassic
          value={originalHtmlContent !== null ? originalHtmlContent : field.value}
          onEditorContentChange={handleEditorChange}
          sessionID={params.sessionID}
          disabled={disabledToolbar}
          markdownEnabled={markdownEnabled}
          onToggleMarkdown={toggleMarkdownMode}
          onEditorReady={(editor: any) => {
            editorRef.current = editor;
          }}
          mode={field.meta?.mode}
        />
      </Box>
    </FormFieldContainer>
  );
};