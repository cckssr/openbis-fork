import React, { useState, useEffect, useRef, useMemo } from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import { DecoupledEditor } from 'ckeditor5';
import CircularProgress from '@mui/material/CircularProgress';
import Box from '@mui/material/Box';

import 'ckeditor5/ckeditor5.css';

import '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorDocument.css';
import { createCKEditorConfig } from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorConfig.js';

export default function CKEditorDocument({ value, sessionID, onEditorContentChange, disabled, markdownEnabled = false, onToggleMarkdown, onEditorReady }) {
	const editorContainerRef = useRef(null);
	const editorToolbarRef = useRef(null);
	const editorRef = useRef(null);
	const editorMinimapRef = useRef(null);
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
			mode: 'document',
			markdownEnabled,
			onToggleMarkdown,
			sessionID,
			initialData: value,
			minimapContainer: editorMinimapRef.current
		});
	}, [editorMinimapRef, isLayoutReady, markdownEnabled, onToggleMarkdown, sessionID, value]);

	return (
		<div className="main-editor-container">
			<div
				className="editor-container editor-container_document-editor editor-container_include-minimap editor-container_include-style editor-container_include-fullscreen"
				ref={editorContainerRef}
			>
				<div className="editor-container__toolbar" ref={editorToolbarRef}></div>
				<div className="editor-container__minimap-wrapper">
					<div className="editor-container__editor-wrapper">
						<div className="editor-container__editor">
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
												key={`ckeditor-document-${markdownEnabled ? 'markdown' : 'html'}-${value ? 'with-data' : 'empty'}`}
												onReady={editor => {
													// Call the onEditorReady callback to pass the editor instance to parent
													if (onEditorReady) {
														onEditorReady(editor);
													}
													// Use setTimeout to ensure DOM is ready
													setTimeout(() => {
														// Clean up any existing elements first
														if (editorToolbarRef.current) {
															Array.from(editorToolbarRef.current.children).forEach(child => child.remove());
														}

														const toolbarElement = editor.ui.view.toolbar.element;

														// Append the new elements
														if (editorToolbarRef.current && toolbarElement) {
															editorToolbarRef.current.appendChild(toolbarElement);
														}

														// Set initial display state
														if (disabled) {
															if (toolbarElement) {
																toolbarElement.style.display = 'none';
															}
														} else {
															if (toolbarElement) {
																toolbarElement.style.display = 'flex';
																toolbarElement.style.visibility = 'visible';
															}
														}

														editor.on('change:isReadOnly', (evt, propertyName, isReadOnly) => {
															if (isReadOnly) {
																if (toolbarElement) toolbarElement.style.display = 'none';
															} else {
																if (toolbarElement) {
																	toolbarElement.style.display = 'flex';
																	toolbarElement.style.visibility = 'visible';
																}
															}
														});
														setIsEditorReady(true);
													}, 400);
												}}

												onAfterDestroy={() => {
													setIsEditorReady(false);
													if (editorToolbarRef.current) {
														Array.from(editorToolbarRef.current.children).forEach(child => child.remove());
													}
												}}
												onChange={(event, editor) => {
													onEditorContentChange(editor.getData());
												}}
												editor={DecoupledEditor}
												config={editorConfig}
												disabled={disabled}
											/>
										</div>
									</>
								)}
							</div>
						</div>
					</div>
					<div className="editor-container__sidebar editor-container__sidebar_minimap" ref={editorMinimapRef}></div>
				</div>
			</div>
		</div>
	);
}
