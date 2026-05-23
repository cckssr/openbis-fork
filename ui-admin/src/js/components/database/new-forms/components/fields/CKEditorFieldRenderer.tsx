import React, { useState, useEffect, useRef, useCallback, useMemo, lazy, Suspense } from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { FormFieldDataType, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import Box from '@mui/material/Box';
import { Typography } from '@mui/material';
import CircularProgress from '@mui/material/CircularProgress';
import { useMarkdownEditor } from '@src/js/components/database/new-forms/hooks/useMarkdownEditor.ts';
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx';
import { HtmlDataProcessor, MarkdownGfmDataProcessor } from 'ckeditor5';

const CKEditorClassic = lazy(() =>
  import('@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorClassic.jsx')
)
const CKEditorInline = lazy(() =>
  import('@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorInline.jsx')
)
const CKEditorDocument = lazy(() =>
  import('@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorDocument.jsx')
)

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

  const markdownPreferenceKey = useMemo(() => {
    const user = params?.user
    const entityPermId = params?.entityPermId
    if (!user || !entityPermId) {
      return null
    }
    // Store only when enabled; key includes entityPermId + field.id for per-entity, per-field scoping.
    return `new-forms:ckeditor-markdown-enabled:${user}:${entityPermId}:${field.id}`
  }, [params?.user, params?.entityPermId, field.id])

  const initialIsMarkdown = useMemo(() => {
    if (!markdownPreferenceKey) {
      return false
    }
    try {
      return localStorage.getItem(markdownPreferenceKey) === 'true'
    } catch {
      return false
    }
  }, [markdownPreferenceKey])

  const {
    editorValue,
    isMarkdown,
    toggleMarkdownMode
  } = useMarkdownEditor({
    value: field.value,
    initialIsMarkdown
  });

  useEffect(() => {
    setDisabledToolbar(isReadOnly);
  }, [isReadOnly]);

  const getEditorDataAs = useCallback((format: 'html' | 'markdown'): string | null => {
    const editor: any = editorRef.current;
    const viewDocument = editor?.editing?.view?.document;
    if (!editor || !viewDocument) {
      return null;
    }
    try {
      const previousProcessor = editor.data?.processor;
      editor.data.processor =
        format === 'html'
          ? new HtmlDataProcessor(viewDocument)
          : new MarkdownGfmDataProcessor(viewDocument);
      const data = editor.getData();
      editor.data.processor = previousProcessor;
      return data;
    } catch {
      return null;
    }
  }, []);

  // Persist preference (store only when true; remove when false)
  useEffect(() => {
    if (!markdownPreferenceKey) {
      return
    }
    try {
      if (isMarkdown) {
        localStorage.setItem(markdownPreferenceKey, 'true')
      } else {
        localStorage.removeItem(markdownPreferenceKey)
      }
    } catch {
      // ignore persistence failures (quota/private mode)
    }
  }, [markdownPreferenceKey, isMarkdown])

  // Handle editor content changes
  const handleEditorChange = useCallback((value: string) => {
    const metadata: Record<string, any> = {};

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
    }

    if (onFieldMetadataChange && typeof onFieldMetadataChange === 'function') {
      onFieldMetadataChange(field.id, metadata);
    }

    if (onFieldChange && typeof onFieldChange === 'function') {
      onFieldChange(field.id, value);
    }
  }, [editorMode, field.id, onFieldChange, onFieldMetadataChange]);

  const handleToggleMarkdown = useCallback(() => {
    setDisabledToolbar(false);
    // Show confirmation dialog only when enabling markdown (switching from HTML to Markdown)
    if (!isMarkdown) {
      setShowMarkdownDialog(true);
    } else {
      // Switching back to HTML - no confirmation needed.
      // Convert markdown -> HTML so the HTML-mode editor doesn't treat markdown as plain text.
      const html = getEditorDataAs('html');
      if (html !== null && onFieldChange && typeof onFieldChange === 'function') {
        onFieldChange(field.id, html);
      }
      toggleMarkdownMode();
    }
  }, [field.id, getEditorDataAs, isMarkdown, onFieldChange, toggleMarkdownMode]);

  const handleConfirmMarkdown = useCallback(() => {
    setShowMarkdownDialog(false);
    // Convert HTML -> markdown so the markdown-mode editor receives markdown input.
    const markdown = getEditorDataAs('markdown');
    if (markdown !== null && onFieldChange && typeof onFieldChange === 'function') {
      onFieldChange(field.id, markdown);
    }
    toggleMarkdownMode();
  }, [field.id, getEditorDataAs, onFieldChange, toggleMarkdownMode]);

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
        <Suspense
          fallback={
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '300px' }}>
              <CircularProgress />
            </Box>
          }
        >
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
        </Suspense>
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