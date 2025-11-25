import React, { useState, useEffect, useRef, useMemo } from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import { ClassicEditor } from 'ckeditor5';
import CircularProgress from '@mui/material/CircularProgress';
import Box from '@mui/material/Box';

import 'ckeditor5/ckeditor5.css';
import '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorClassic.css';
import { createCKEditorConfig } from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorConfig.js';

export default function CKEditorClassic({ value, sessionID, onEditorContentChange, disabled, markdownEnabled = false, onToggleMarkdown, onEditorReady }) {
	const editorContainerRef = useRef(null);
	const editorRef = useRef(null);
	const [isLayoutReady, setIsLayoutReady] = useState(false);
	const [isEditorReady, setIsEditorReady] = useState(false);

	useEffect(() => {
		setIsLayoutReady(true);

		return () => setIsLayoutReady(false);
	}, []);

	// Reset editor ready state when markdown mode changes to force re-initialization
	useEffect(() => {
		setIsEditorReady(false);
	}, [markdownEnabled]);

	const editorConfig = useMemo(() => {
		if (!isLayoutReady) {
			return null;
		}

		return createCKEditorConfig({
			mode: 'classic',
			markdownEnabled,
			onToggleMarkdown,
			sessionID,
			initialData: value
		});
	}, [isLayoutReady, markdownEnabled, onToggleMarkdown, sessionID, value]);

	return (
		<div className="main-editor-container">

			<div
				className="editor-container editor-container_classic-editor editor-container_include-style editor-container_include-fullscreen"
				ref={editorContainerRef}
			>
				<div ref={editorRef}>
					{editorConfig && (
						<>
							{!isEditorReady && (
								<Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '300px' }}>
									<CircularProgress />
								</Box>
							)}
							<div style={{ display: isEditorReady ? 'block' : 'none' }}>
								<CKEditor
									key={`ckeditor-classic-${markdownEnabled ? 'markdown' : 'html'}-${value ? 'with-data' : 'empty'}`}
									editor={ClassicEditor}
									config={editorConfig}
									onReady={editor => {

										// Call the onEditorReady callback to pass the editor instance to parent
										if (onEditorReady) {
											onEditorReady(editor);
										}

										// Use setTimeout to ensure DOM is ready
										setTimeout(() => {
											const toolbarElement = editor.ui.view.toolbar.element;
											const editableElement = editor.ui.view.editable.element;

											// Set initial display state and CSS class
											if (disabled) {
												if (toolbarElement) {
													toolbarElement.style.display = 'none';
												}
												if (editableElement) {
													editableElement.classList.remove('ck-editor__editable_inline');
												}
											} else {
												if (toolbarElement) {
													toolbarElement.style.display = 'flex';
													toolbarElement.style.visibility = 'visible';
												}
												if (editableElement) {
													editableElement.classList.add('ck-editor__editable_inline');
												}
											}

											editor.on('change:isReadOnly', (evt, propertyName, isReadOnly) => {
												if (isReadOnly) {
													if (toolbarElement) toolbarElement.style.display = 'none';
													if (editableElement) {
														editableElement.classList.remove('ck-editor__editable_inline');
													}
												} else {
													if (toolbarElement) {
														toolbarElement.style.display = 'flex';
														toolbarElement.style.visibility = 'visible';
													}
													if (editableElement) {
														editableElement.classList.add('ck-editor__editable_inline');
													}
												}
											});

											setIsEditorReady(true);
										}, 400);
									}}

									onAfterDestroy={() => {
										setIsEditorReady(false);
										// Clean up is handled automatically by CKEditor
									}}
									onChange={(event, editor) => {
										onEditorContentChange(editor.getData());
									}}
									disabled={disabled} />
							</div>
						</>
					)}
				</div>
			</div>
		</div>
	);
}
