import React, { useState, useEffect, useRef, useCallback, useMemo, lazy, Suspense } from 'react';
import { FieldRendererProps } from '@src/js/components/database/new-forms/types/formITypes.ts';
import { FormFieldDataType, FormMode } from '@src/js/components/database/new-forms/types/formEnums.ts';
import Box from '@mui/material/Box';
import { Typography } from '@mui/material';
import CircularProgress from '@mui/material/CircularProgress';
import { useMarkdownEditor } from '@src/js/components/database/new-forms/hooks/useMarkdownEditor.ts';
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx';
import MultiValueFieldEditor from '@src/js/components/database/new-forms/components/fields/MultiValueFieldEditor.tsx';
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

interface CKEditorSingleEditorProps {
  value: string | null;
  dataType: FormFieldDataType;
  readOnly: boolean;
  onChange: (val: string) => void;
  onMetadataChange?: (meta: any) => void;
  sessionID?: string;
  markdownPreferenceKey?: string | null;
}

const CKEditorSingleEditor: React.FC<CKEditorSingleEditorProps> = ({
  value,
  dataType,
  readOnly,
  onChange,
  onMetadataChange,
  sessionID,
  markdownPreferenceKey,
}) => {
  const [disabledToolbar, setDisabledToolbar] = useState(readOnly);
  const [showMarkdownDialog, setShowMarkdownDialog] = useState(false);
  const editorRef = useRef<any>(null);
  const editorMode = dataType === FormFieldDataType.WORD_PROCESSOR ? 'inline'
    : dataType === FormFieldDataType.WORD_PROCESSOR_PAGE ? 'document'
    : 'classic';

  const initialIsMarkdown = useMemo(() => {
    if (!markdownPreferenceKey) {
      return false;
    }
    try {
      return localStorage.getItem(markdownPreferenceKey) === 'true';
    } catch {
      return false;
    }
  }, [markdownPreferenceKey]);

  const { editorValue, isMarkdown, toggleMarkdownMode } = useMarkdownEditor({
    value,
    initialIsMarkdown
  });

  useEffect(() => {
    setDisabledToolbar(readOnly);
  }, [readOnly]);

  // Persist markdown preference when it changes
  useEffect(() => {
    if (!markdownPreferenceKey) {
      return;
    }
    try {
      if (isMarkdown) {
        localStorage.setItem(markdownPreferenceKey, 'true');
      } else {
        localStorage.removeItem(markdownPreferenceKey);
      }
    } catch {
      // ignore persistence failures (quota/private mode)
    }
  }, [markdownPreferenceKey, isMarkdown]);

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

  const handleEditorChange = useCallback((val: string) => {
    const metadata: Record<string, any> = {};
    if (editorRef.current && (editorMode === 'classic' || editorMode === 'document')) {
      try {
        const titlePlugin = editorRef.current.plugins.get('Title');
        if (titlePlugin) {
          metadata.title = titlePlugin.getTitle();
        }
      } catch (error) {
        console.debug('Title plugin not available:', error);
      }
    }
    if (onMetadataChange) {
      onMetadataChange(metadata);
    }
    onChange(val);
  }, [editorMode, onChange, onMetadataChange]);

  const handleToggleMarkdown = useCallback(() => {
    setDisabledToolbar(false);
    if (!isMarkdown) {
      setShowMarkdownDialog(true);
    } else {
      const html = getEditorDataAs('html');
      if (html !== null) {
        onChange(html);
      }
      toggleMarkdownMode();
    }
  }, [getEditorDataAs, isMarkdown, onChange, toggleMarkdownMode]);

  const handleConfirmMarkdown = useCallback(() => {
    setShowMarkdownDialog(false);
    const markdown = getEditorDataAs('markdown');
    if (markdown !== null) {
      onChange(markdown);
    }
    toggleMarkdownMode();
  }, [getEditorDataAs, onChange, toggleMarkdownMode]);

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
          sessionID={sessionID}
          disabled={disabledToolbar}
          markdownEnabled={isMarkdown}
          onToggleMarkdown={handleToggleMarkdown}
          onEditorReady={(editor: any) => {
            editorRef.current = editor;
          }}
        />
      </Suspense>
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

export const CKEditorFieldRenderer: React.FC<FieldRendererProps> = ({
  field,
  mode,
  onFieldChange,
  onFieldMetadataChange,
  params
}) => {
  const isEditingMode = mode === FormMode.EDIT || mode === FormMode.CREATE;
  const isReadOnly = !isEditingMode || field.readOnly;

  const markdownPreferenceKey = useMemo(() => {
    const user = params?.user;
    const entityPermId = params?.entityPermId;
    if (!user || !entityPermId) {
      return null;
    }
    return `new-forms:ckeditor-markdown-enabled:${user}:${entityPermId}:${field.id}`;
  }, [params?.user, params?.entityPermId, field.id]);

  const label = (
    <Typography variant="body2" component="div" sx={{ color: '#0000008a', fontSize: '0.7rem' }}>
      {field.label} {field.required ? '*' : ''}
    </Typography>
  );

  if (field.isMultiValue) {
    if (isEditingMode && !field.readOnly) {
      return (
        <MultiValueFieldEditor
          required={field.required}
          values={Array.isArray(field.value) ? field.value : []}
          onChange={(vals) => onFieldChange(field.id, vals)}
          renderInput={(val, index, handleChange) => (
            [index === 0 ? label : null,
            <CKEditorSingleEditor
              value={val ?? ''}
              dataType={field.dataType}
              readOnly={false}
              onChange={handleChange}
              sessionID={params?.sessionID}
            />]
          )}
          isEmpty={(v) => v === null || v === undefined || v === ''}
        />
      );
    }

    // View / read-only mode
    const values: any[] = Array.isArray(field.value) ? field.value : [];
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        {label}
        {values.map((val, i) => (
          <CKEditorSingleEditor
            key={i}
            value={val ?? ''}
            dataType={field.dataType}
            readOnly={true}
            onChange={() => {}}
            sessionID={params?.sessionID}
          />
        ))}
      </Box>
    );
  }

  // Single value
  return (
    <>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        {label}
        <CKEditorSingleEditor
          value={field.value}
          dataType={field.dataType}
          readOnly={isReadOnly}
          onChange={(val) => onFieldChange(field.id, val)}
          onMetadataChange={onFieldMetadataChange
            ? (meta) => onFieldMetadataChange(field.id, meta)
            : undefined}
          sessionID={params?.sessionID}
          markdownPreferenceKey={markdownPreferenceKey}
        />
      </Box>
    </>
  );
};
