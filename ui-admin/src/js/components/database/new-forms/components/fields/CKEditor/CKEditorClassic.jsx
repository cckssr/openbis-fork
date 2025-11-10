import React, { useState, useEffect, useRef, useMemo } from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import {
	ClassicEditor,
	InlineEditor,
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
	Emoji,
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
	Mention,
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
	TextPartLanguage,
	TextTransformation,
	Title,
	TodoList,
	Underline,
} from 'ckeditor5';

import MarkdownToggler from '@src/js/components/database/new-forms/components/fields/CKEditor/MarkdownToggler.jsx';

import 'ckeditor5/ckeditor5.css';
import '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorClassic.css';
import { ITEMS_CLASSIC, ITEMS_INLINE, FONT_FAMILY_OPTIONS, FONT_SIZE_OPTIONS, HEADING_OPTIONS } from '@src/js/components/database/new-forms/components/fields/CKEditor/CKEditorConfig.js';

const LICENSE_KEY = 'GPL'; // or <YOUR_LICENSE_KEY>.

const itemsConfigMap = {
	classic: ITEMS_CLASSIC,
	inline: ITEMS_INLINE
}

export default function CKEditorClassic({ value, sessionID, onEditorContentChange, disabled, markdownEnabled = false, onToggleMarkdown, onEditorReady, mode = 'classic' }) {
	const editorContainerRef = useRef(null);
	const editorRef = useRef(null);
	const [isLayoutReady, setIsLayoutReady] = useState(false);

	useEffect(() => {
		setIsLayoutReady(true);

		return () => setIsLayoutReady(false);
	}, []);

	const { editorConfig } = useMemo(() => {
		if (!isLayoutReady) {
			return {};
		}

		const plugins = [
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
			Emoji,
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
			MarkdownToggler,
			MediaEmbed,
			Mention,
			Paragraph,
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
			TextPartLanguage,
			TextTransformation,
			TodoList,
			Underline
		]

		// Add Markdown and PasteFromMarkdownExperimental plugins only if markdown is enabled
		if (markdownEnabled) {
			plugins.push(Markdown, PasteFromMarkdownExperimental);
		}

		if (mode === 'classic') {
			plugins.push(Title);
		}

		const itemsConfig = itemsConfigMap[mode];
		if (!itemsConfig) {
			throw new Error(`Invalid mode: ${mode}`);
		}

		return {
			editorConfig: {
				toolbar: {
					items: itemsConfig,
					shouldNotGroupWhenFull: false
				},
				plugins: plugins,
				markdownToggleCallback: onToggleMarkdown,
				markdownEnabled: markdownEnabled,
				simpleUpload: {
					uploadUrl: "/openbis/openbis/file-service/eln-lims?type=Files&sessionID=" + sessionID
				},
				licenseKey: LICENSE_KEY,
				balloonToolbar: ['bold', 'italic', '|', 'link', '|', 'bulletedList', 'numberedList'],
				fontFamily: {
					options: FONT_FAMILY_OPTIONS
				},
				fontSize: {
					options: FONT_SIZE_OPTIONS
				},
				fullscreen: {
					onEnterCallback: container =>
						container.classList.add(
							'editor-container',
							'editor-container_classic-editor',
							'editor-container_include-style',
							'editor-container_include-fullscreen',
							'main-container'
						)
				},
				heading: {
					options: HEADING_OPTIONS
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
					toolbar: ['imageResize:original', '|', 'toggleImageCaption', 'imageTextAlternative', '|', 'imageStyle:inline', 'imageStyle:wrapText', 'imageStyle:breakText']
				},
				initialData: value || '',
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
				mention: {
					feeds: [
						{
							marker: '@',
							feed: [
								/* See: https://ckeditor.com/docs/ckeditor5/latest/features/mentions.html */
							]
						}
					]
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
				className="editor-container editor-container_classic-editor editor-container_include-style editor-container_include-fullscreen"
				ref={editorContainerRef}
			>
				<div ref={editorRef}>
					{editorConfig && (<CKEditor
						key={`ckeditor-${markdownEnabled ? 'markdown' : 'html'}-${value ? 'with-data' : 'empty'}`}
						editor={mode === 'classic' ? ClassicEditor : InlineEditor}
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
							}, 100);
						}}

						onAfterDestroy={() => {
							// Clean up is handled automatically by CKEditor
						}}
						onChange={(event, editor) => {
							onEditorContentChange(editor.getData());
						}}
						disabled={disabled} />
					)}
				</div>
			</div>
		</div>
	);
}
