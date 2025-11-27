import React, { useEffect, useMemo, useRef, useState } from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import { InlineEditor } from 'ckeditor5';

import 'ckeditor5/ckeditor5.css';
import '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorInline.css';
import { createCKEditorConfig } from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorConfig.js';

export default function CKEditorInline({ value, sessionID, onEditorContentChange, disabled, markdownEnabled = false, onToggleMarkdown, onEditorReady }) {
	const editorContainerRef = useRef(null);
	const [isLayoutReady, setIsLayoutReady] = useState(false);

	const editorRef = useRef(null);
	const [isEditorReady, setIsEditorReady] = useState(false);

	useEffect(() => {
		setIsLayoutReady(true);

		return () => setIsLayoutReady(false);
	}, []);

	// Update editor content when value prop changes (but not from user edits)
	useEffect(() => {
		if (editorRef.current && isEditorReady && value !== undefined) {
			const currentData = editorRef.current.getData();
			// Only update if the value is different to avoid unnecessary updates
			if (currentData !== value) {
				editorRef.current.setData(value || '');
			}
		}
	}, [value, isEditorReady]);

	const editorConfig = useMemo(() => {
		if (!isLayoutReady) {
			return null;
		}

		return createCKEditorConfig({
			mode: 'inline',
			markdownEnabled,
			onToggleMarkdown,
			sessionID,
			initialData: value
		});
	}, [isLayoutReady, markdownEnabled, onToggleMarkdown, sessionID, value]);

	return (
		<div className="inline-editor-container" ref={editorContainerRef}>
			{editorConfig && (
				<CKEditor
					key={`ckeditor-inline-${markdownEnabled ? 'markdown' : 'html'}-${value ? 'with-data' : 'empty'}`}
					editor={InlineEditor}
					config={editorConfig}
					onReady={editor => {
						editorRef.current = editor;
						setIsEditorReady(true);
						if (onEditorReady) {
							onEditorReady(editor);
						}
					}}
					onAfterDestroy={() => {}}
					onChange={(event, editor) => {
						onEditorContentChange(editor.getData());
					}}
					disabled={disabled}
				/>
			)}
		</div>
	);
}

