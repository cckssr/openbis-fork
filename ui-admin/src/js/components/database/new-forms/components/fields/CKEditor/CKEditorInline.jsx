import React, { useEffect, useMemo, useRef, useState } from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import { InlineEditor } from 'ckeditor5';

import 'ckeditor5/ckeditor5.css';
import '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorInline.css';
import { createCKEditorConfig } from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorConfig.js';

export default function CKEditorInline({ value, sessionID, onEditorContentChange, disabled, markdownEnabled = false, onToggleMarkdown, onEditorReady }) {
	const editorContainerRef = useRef(null);
	const [isLayoutReady, setIsLayoutReady] = useState(false);

	useEffect(() => {
		setIsLayoutReady(true);

		return () => setIsLayoutReady(false);
	}, []);

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

