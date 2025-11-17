import React, { useState, useEffect, useRef, useMemo } from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import {
	DecoupledEditor,
	Alignment,
	AutoImage,
	AutoLink,
	Autoformat,
	Autosave,
	BalloonToolbar,
	BlockQuote,
	Bold,
	Code,
	CodeBlock,
	Essentials,
	FontBackgroundColor,
	FontColor,
	FontFamily,
	FontSize,
	Fullscreen,
	GeneralHtmlSupport,
	Heading,
	Highlight,
	HorizontalLine,
	HtmlComment,
	ImageBlock,
	ImageCaption,
	ImageEditing,
	ImageInline,
	ImageInsertViaUrl,
	ImageResize,
	ImageStyle,
	ImageTextAlternative,
	ImageToolbar,
	ImageUpload,
	ImageUtils,
	Indent,
	IndentBlock,
	Italic,
	Link,
	LinkImage,
	List,
	Markdown,
	MediaEmbed,
	Minimap,
	Paragraph,
	PasteFromMarkdownExperimental,
	PlainTableOutput,
	ShowBlocks,
	SimpleUploadAdapter,
	SourceEditing,
	Strikethrough,
	Subscript,
	Superscript,
	Table,
	TableCaption,
	TableToolbar,
	TextTransformation,
	Title,
	TodoList,
	Underline
} from 'ckeditor5';
//import Markdown from '@ckeditor/ckeditor5-markdown-gfm';

import 'ckeditor5/ckeditor5.css';

import '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditor_doc.css';
import MarkdownToggler from '@src/js/components/database/new-forms/components/fields/CKEditor/MarkdownToggler.jsx';

const LICENSE_KEY = 'GPL'; // or <YOUR_LICENSE_KEY>.

export default function CKEditorDocument({ value, sessionID, onEditorContentChange, disabled, markdownEnabled = false, onToggleMarkdown, onEditorReady }) {
	const editorContainerRef = useRef(null);
	const editorMenuBarRef = useRef(null);
	const editorToolbarRef = useRef(null);
	const editorRef = useRef(null);
	const editorMinimapRef = useRef(null);
	const [isLayoutReady, setIsLayoutReady] = useState(false);

	useEffect(() => {
		setIsLayoutReady(true);

		return () => setIsLayoutReady(false);
	}, []);


	const { editorConfig } = useMemo(() => {
		if (!isLayoutReady) {
			return {};
		}

		// Create plugins array conditionally
		const plugins = [
			Alignment,
			Autoformat,
			AutoImage,
			AutoLink,
			Autosave,
			BalloonToolbar,
			BlockQuote,
			Bold,
			Code,
			CodeBlock,
			Essentials,
			FontBackgroundColor,
			FontColor,
			FontFamily,
			FontSize,
			Fullscreen,
			GeneralHtmlSupport,
			Heading,
			Highlight,
			HorizontalLine,
			HtmlComment,
			ImageBlock,
			ImageCaption,
			ImageEditing,
			ImageInline,
			ImageInsertViaUrl,
			ImageStyle,
			ImageTextAlternative,
			ImageToolbar,
			ImageUpload,
			ImageUtils,
			ImageResize,
			Indent,
			IndentBlock,
			Italic,
			Link,
			LinkImage,
			List,
			MarkdownToggler,
			MediaEmbed,
			Minimap,
			Paragraph,
			PlainTableOutput,
			SimpleUploadAdapter,
			SourceEditing,
			ShowBlocks,
			Strikethrough,
			Subscript,
			Superscript,
			Table,
			TableCaption,
			TableToolbar,
			TextTransformation,
			Title,
			TodoList,
			Underline
		];

		// Add Markdown and PasteFromMarkdownExperimental plugins only if markdown is enabled
		if (markdownEnabled) {
			plugins.push(Markdown, PasteFromMarkdownExperimental);
		}

		return {
			editorConfig: {
				toolbar: {
					items: [
						'undo',
						'redo',
						'|',
						'showBlocks',
						'sourceEditing',
						'fullscreen',
						'markdownToggler',
						'|',
						'heading',
						'style',
						'|',
						'fontSize',
						'fontFamily',
						'fontColor',
						'fontBackgroundColor',
						'|',
						'bold',
						'italic',
						'underline',
						'strikethrough',
						'subscript',
						'superscript',
						'code',
						'|',
						'horizontalLine',
						'link',
						'insertImageViaUrl',
						'mediaEmbed',
						'insertTable',
						'highlight',
						'blockQuote',
						'codeBlock',
						'|',
						'alignment',
						'|',
						'bulletedList',
						'numberedList',
						'todoList',
						'outdent',
						'indent'
					],
					shouldNotGroupWhenFull: false
				},
				plugins: plugins,
				markdownToggleCallback: onToggleMarkdown,
				markdownEnabled: markdownEnabled,
				simpleUpload: {
					uploadUrl: "/openbis/openbis/file-service/eln-lims?type=Files&sessionID=" + sessionID
				},
				balloonToolbar: ['bold', 'italic', '|', 'link', '|', 'bulletedList', 'numberedList'],
				fontFamily: {
					supportAllValues: true
				},
				fontSize: {
					options: [10, 12, 14, 'default', 18, 20, 22],
					supportAllValues: true
				},
				fullscreen: {
					onEnterCallback: container =>
						container.classList.add(
							'editor-container',
							'editor-container_document-editor',
							'editor-container_include-minimap',
							'editor-container_include-style',
							'editor-container_include-fullscreen',
							'main-container'
						)
				},
				heading: {
					options: [
						{
							model: 'paragraph',
							title: 'Paragraph',
							class: 'ck-heading_paragraph'
						},
						{
							model: 'heading1',
							view: 'h1',
							title: 'Heading 1',
							class: 'ck-heading_heading1'
						},
						{
							model: 'heading2',
							view: 'h2',
							title: 'Heading 2',
							class: 'ck-heading_heading2'
						},
						{
							model: 'heading3',
							view: 'h3',
							title: 'Heading 3',
							class: 'ck-heading_heading3'
						},
						{
							model: 'heading4',
							view: 'h4',
							title: 'Heading 4',
							class: 'ck-heading_heading4'
						}
					]
				},
				htmlSupport: {
					allow: [
						{
							name: /^.*$/,
							styles: true,
							attributes: true,
							classes: true
						}
					]
				},
				image: {
					toolbar: ['toggleImageCaption', 'imageTextAlternative', '|', 'imageStyle:inline', 'imageStyle:wrapText', 'imageStyle:breakText']
				},
				initialData: value || '',
				licenseKey: LICENSE_KEY,
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
				},
				minimap: {
					container: editorMinimapRef.current,
					extraClasses: 'editor-container_include-minimap ck-minimap__iframe-content'
				},
				placeholder: 'Type or paste your content here!',
				table: {
					contentToolbar: ['tableColumn', 'tableRow', 'mergeTableCells']
				}
			}
		};
	}, [isLayoutReady, markdownEnabled, onToggleMarkdown, value, onEditorReady]);

	return (
		<div className="main-container">
			<div
				className="editor-container editor-container_document-editor editor-container_include-minimap editor-container_include-style editor-container_include-fullscreen"
				ref={editorContainerRef}
			>
				<div className="editor-container__menu-bar" ref={editorMenuBarRef}></div>
				<div className="editor-container__toolbar" ref={editorToolbarRef}></div>
				<div className="editor-container__minimap-wrapper">
					<div className="editor-container__editor-wrapper">
						<div className="editor-container__editor">
							<div ref={editorRef}>
								{editorConfig && (
									<CKEditor
										key={`ckeditor-${markdownEnabled ? 'markdown' : 'html'}-${value ? 'with-data' : 'empty'}`}
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
												if (editorMenuBarRef.current) {
													Array.from(editorMenuBarRef.current.children).forEach(child => child.remove());
												}

												const toolbarElement = editor.ui.view.toolbar.element;
												const menuBarElement = editor.ui.view.menuBarView.element;

												// Append the new elements
												if (editorToolbarRef.current && toolbarElement) {
													editorToolbarRef.current.appendChild(toolbarElement);
												}
												if (editorMenuBarRef.current && menuBarElement) {
													editorMenuBarRef.current.appendChild(menuBarElement);
												}
												
												// Set initial display state
												if (disabled) {
													if (toolbarElement) {
														toolbarElement.style.display = 'none';
													}
													if (menuBarElement) {
														menuBarElement.style.display = 'none';
													}
												} else {
													if (toolbarElement) {
														toolbarElement.style.display = 'flex';
														toolbarElement.style.visibility = 'visible';
													}
													if (menuBarElement) {
														menuBarElement.style.display = 'flex';
														menuBarElement.style.visibility = 'visible';
													}
												}

												editor.on('change:isReadOnly', (evt, propertyName, isReadOnly) => {
													if (isReadOnly) {
														if (toolbarElement) toolbarElement.style.display = 'none';
														if (menuBarElement) menuBarElement.style.display = 'none';
													} else {
														if (toolbarElement) {
															toolbarElement.style.display = 'flex';
															toolbarElement.style.visibility = 'visible';
														}
														if (menuBarElement) {
															menuBarElement.style.display = 'flex';
															menuBarElement.style.visibility = 'visible';
														}
													}
												});
											}, 100);
										}}

										onAfterDestroy={() => {
											if (editorToolbarRef.current) {
												Array.from(editorToolbarRef.current.children).forEach(child => child.remove());
											}
											if (editorMenuBarRef.current) {
												Array.from(editorMenuBarRef.current.children).forEach(child => child.remove());
											}
										}}
										onChange={(event, editor) => {
											onEditorContentChange(editor.getData());
										}}
										editor={DecoupledEditor}
										config={editorConfig}
										disabled={disabled}
									/>
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
