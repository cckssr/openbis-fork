import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { FormFieldDataType, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import CKEditorClassic from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorClassic.jsx';
import CKEditorInline from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorInline.jsx';
import CKEditorDocument from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorDocument.jsx';
import Box from '@mui/material/Box';
import { Typography } from '@mui/material';
import { useMarkdownEditor } from '@src/js/components/database/new-forms/hooks/useMarkdownEditor.ts';
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx';

export const CKEditorFieldRenderer: React.FC<FieldRendererProps> = ({
  field,
  mode,
  onFieldChange,
  onFieldMetadataChange,
  params
}) => {
  const [disabledToolbar, setDisabledToolbar] = useState(true);
  const [showMarkdownDialog, setShowMarkdownDialog] = useState(false);
  const editorRef = useRef<any>(null);
  const isEditingMode = mode === FormMode.EDIT || mode === FormMode.CREATE;
  const isReadOnly = !isEditingMode || field.readOnly;
  const editorMode = field.dataType === FormFieldDataType.WORD_PROCESSOR ? 'inline' : field.dataType === FormFieldDataType.WORD_PROCESSOR_PAGE ? 'document' : 'classic';

  const {
    editorValue,
    isMarkdown,
    toggleMarkdownMode
  } = useMarkdownEditor({
    value: field.value,
    initialIsMarkdown: field.meta?.isMarkdown ?? false
  });

  useEffect(() => {
    setDisabledToolbar(isReadOnly);
  }, [isReadOnly]);

  // Handle editor content changes
  const handleEditorChange = useCallback((value: string) => {
    const metadata: Record<string, any> = { isMarkdown };

    if (editorRef.current && (editorMode === 'classic' || editorMode === 'document')) {
      try {
        const titlePlugin = editorRef.current.plugins.get('Title');
        if (titlePlugin) {
          const title = titlePlugin.getTitle();
          metadata.title = title;
        }
      } catch (error) {
        console.debug('Title plugin not available:', error);
      }
      metadata.isMarkdown = isMarkdown;
    }

    if (onFieldMetadataChange && typeof onFieldMetadataChange === 'function') {
      onFieldMetadataChange(field.id, metadata);
    }

    if (onFieldChange && typeof onFieldChange === 'function') {
      onFieldChange(field.id, value);
    }
  }, [editorMode, field.id, isMarkdown, onFieldChange, onFieldMetadataChange]);

  const handleToggleMarkdown = useCallback(() => {
    setDisabledToolbar(false);
    // Show confirmation dialog only when enabling markdown (switching from HTML to Markdown)
    if (!isMarkdown) {
      setShowMarkdownDialog(true);
    } else {
      // Switching back to HTML - no confirmation needed
      toggleMarkdownMode();
    }
  }, [isMarkdown, toggleMarkdownMode]);

  const handleConfirmMarkdown = useCallback(() => {
    setShowMarkdownDialog(false);
    toggleMarkdownMode();
  }, [toggleMarkdownMode]);

  const handleCancelMarkdown = useCallback(() => {
    setShowMarkdownDialog(false);
  }, []);

  const EditorComponent = useMemo(() => {
    switch (editorMode) {
      case 'document':
        return CKEditorDocument;
      case 'inline':
        return CKEditorInline;
      default:
        return CKEditorClassic;
    }
  }, [editorMode]);

  return (
    <>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        <Typography variant="body2" component="div" sx={{ color: '#0000008a', fontSize: '0.7rem' }}>{field.label} {field.required ? '*' : ''}</Typography>
        <EditorComponent
          value={editorValue}
          onEditorContentChange={handleEditorChange}
          sessionID={params.sessionID}
          disabled={disabledToolbar}
          markdownEnabled={isMarkdown}
          onToggleMarkdown={handleToggleMarkdown}
          onEditorReady={(editor: any) => {
            editorRef.current = editor;
          }}
        />
      </Box>
      <ConfirmationDialog
        open={showMarkdownDialog}
        type="warning"
        title="Enable Markdown Output"
        content="The conversion from HTML to Markdown (or vice versa) is not guaranteed to be correct. Some formatting may be lost or changed during conversion. Do you want to continue?"
        onConfirm={handleConfirmMarkdown}
        onCancel={handleCancelMarkdown}
      />
    </>
  );
};