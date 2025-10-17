import React, { useState, useEffect, useRef, useCallback } from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/form.types.ts';
import CKEditorDocument from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorDocument.jsx';
import { FormMode } from '@src/js/components/database/new-forms/types/form.enums.ts';

export const CKEditorFieldRenderer: React.FC<FieldRendererProps> = ({
  field,
  mode,
  onFieldChange,
  params
}) => {
  const [markdownEnabled, setMarkdownEnabled] = useState(false);
  const [disabledToolbar, setDisabledToolbar] = useState(true);
  const [originalHtmlContent, setOriginalHtmlContent] = useState<string | null>(null);
  const editorRef = useRef<any>(null);

  useEffect(() => {
    setDisabledToolbar(mode === FormMode.VIEW);
  }, [mode]);

  // Apply original HTML content when it's available
  useEffect(() => {
    if (originalHtmlContent !== null && onFieldChange && typeof onFieldChange === 'function') {
      onFieldChange(field.id, originalHtmlContent);
      setOriginalHtmlContent(null); // Reset after applying
    }
  }, [originalHtmlContent, onFieldChange, field.id]);

  // Wrapper function to ensure field updates are properly handled
  const handleEditorChange = useCallback((value: string) => {
    console.log(onFieldChange);
    if (onFieldChange && typeof onFieldChange === 'function') {
      onFieldChange(field.id, value);
    }
  }, [onFieldChange, field.id]);

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
    <div style={{ marginTop: '16px' }}>
      <CKEditorDocument
        value={originalHtmlContent !== null ? originalHtmlContent : field.value}
        onEditorContentChange={handleEditorChange}
        sessionID={params.sessionID}
        disabled={disabledToolbar}
        markdownEnabled={markdownEnabled}
        onToggleMarkdown={toggleMarkdownMode}
        onEditorReady={(editor: any) => {
          editorRef.current = editor;
        }}
      />
    </div>
  );
};