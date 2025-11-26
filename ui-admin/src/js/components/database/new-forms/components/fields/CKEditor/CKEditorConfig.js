import {
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
	Underline
} from 'ckeditor5';
import MarkdownToggler from './MarkdownToggler.jsx';

export const LICENSE_KEY = 'GPL';

export const ITEMS_INLINE = [
	'heading',
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
	'highlight',
	'|',
	'emoji',
	'|',
	'alignment',
	'|',
	'numberedList',
	'bulletedList',
	'|',
	'link',
	'blockquote',
	'imageUpload',
	'insertTable',
	'|',
	'undo',
	'redo'
];

export const ITEMS_CLASSIC = [
	'undo',
	'redo',
	'|',
	'showBlocks',
	'sourceEditing',
	'fullscreen',
	'markdownToggler',
	'|',
	'heading',
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
	'emoji',
	'horizontalLine',
	'link',
	'imageUpload',
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
];

export const ITEMS_DOCUMENT = [
	'undo',
	'redo',
	'|',
	'showBlocks',
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
	'emoji',
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
];

export const FONT_FAMILY_OPTIONS = [
	'default',
	'Arial, Helvetica, sans-serif',
	'Courier New, Courier, monospace',
	'Georgia, serif',
	'Lucida Sans Unicode, Lucida Grande, sans-serif',
	'Tahoma, Geneva, sans-serif',
	'Times New Roman, Times, serif',
	'Trebuchet MS, Helvetica, sans-serif',
	'Verdana, Geneva, sans-serif',
	'Calibri, sans-serif',
	'Arial Unicode MS, sans-serif',
	'Comic Sans MS/Comic Sans MS, cursive;'
];

export const FONT_SIZE_OPTIONS = [
	9,
	9.5,
	10,
	10.5,
	11,
	11.5,
	12,
	12.5,
	13,
	13.5,
	'default',
	14,
	15,
	16,
	17,
	18,
	19,
	20,
	21,
	22,
	23,
	24,
	25
];

export const DOCUMENT_FONT_SIZE_OPTIONS = [10, 12, 14, 'default', 18, 20, 22];

export const HEADING_OPTIONS = [
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
];

const TOOLBAR_ITEMS = {
	classic: ITEMS_CLASSIC,
	inline: ITEMS_INLINE,
	document: ITEMS_DOCUMENT
};

const COMMON_PLUGINS = [
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
];

const MARKDOWN_PLUGINS = [Markdown, PasteFromMarkdownExperimental];

const MODE_PLUGIN_EXTENSIONS = {
	classic: [Title],
	inline: [],
	document: [Title, Minimap]
};

const MODE_FONT_CONFIG = {
	classic: {
		fontFamily: {
			options: FONT_FAMILY_OPTIONS
		},
		fontSize: {
			options: FONT_SIZE_OPTIONS
		}
	},
	inline: {
		fontFamily: {
			options: FONT_FAMILY_OPTIONS
		},
		fontSize: {
			options: FONT_SIZE_OPTIONS
		}
	},
	document: {
		fontFamily: {
			supportAllValues: true
		},
		fontSize: {
			options: DOCUMENT_FONT_SIZE_OPTIONS,
			supportAllValues: true
		}
	}
};

const FULLSCREEN_CLASS_MAP = {
	classic: [
		'editor-container',
		'editor-container_classic-editor',
		'editor-container_include-style',
		'editor-container_include-fullscreen',
		'main-container'
	],
	inline: [
		'editor-container',
		'editor-container_inline-editor',
		'editor-container_include-fullscreen'
	],
	document: [
		'editor-container',
		'editor-container_document-editor',
		'editor-container_include-minimap',
		'editor-container_include-style',
		'editor-container_include-fullscreen',
		'main-container'
	]
};

const MENTION_CONFIG = {
	feeds: [
		{
			marker: '@',
			feed: []
		}
	]
};

const buildFullscreenConfig = mode => {
	const classes = FULLSCREEN_CLASS_MAP[mode] || FULLSCREEN_CLASS_MAP.classic;
	return {
		onEnterCallback: container => {
			if (!container) {
				return;
			}
			container.classList.add(...classes);
		}
	};
};

export const createCKEditorConfig = ({
	mode = 'inline',
	markdownEnabled = false,
	onToggleMarkdown,
	sessionID,
	initialData,
	minimapContainer
}) => {
	const toolbarItems = TOOLBAR_ITEMS[mode] || TOOLBAR_ITEMS.classic;
	const plugins = [...COMMON_PLUGINS, ...(MODE_PLUGIN_EXTENSIONS[mode] || [])];

	// When markdownEnabled=true, the Markdown plugin changes the data processor
	// to MarkdownGfmDataProcessor, which makes editor.getData() return markdown
	// instead of HTML. This is automatic - no additional configuration needed.
	if (markdownEnabled) {
		plugins.push(...MARKDOWN_PLUGINS);
	}

	const config = {
		toolbar: {
			items: toolbarItems,
			shouldNotGroupWhenFull: false
		},
		plugins,
		markdownToggleCallback: onToggleMarkdown,
		markdownEnabled,
		simpleUpload: {
			uploadUrl: "/openbis/openbis/file-service/eln-lims?type=Files&sessionID=" + sessionID
		},
		licenseKey: LICENSE_KEY,
		balloonToolbar: ['bold', 'italic', '|', 'link', '|', 'bulletedList', 'numberedList'],
		fontFamily: MODE_FONT_CONFIG[mode]?.fontFamily || MODE_FONT_CONFIG.classic.fontFamily,
		fontSize: MODE_FONT_CONFIG[mode]?.fontSize || MODE_FONT_CONFIG.classic.fontSize,
		fullscreen: buildFullscreenConfig(mode),
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
			toolbar: [
				'imageResize:original',
				'|',
				'toggleImageCaption',
				'imageTextAlternative',
				'|',
				'imageStyle:inline',
				'imageStyle:wrapText',
				'imageStyle:breakText'
			]
		},
		initialData: initialData || '',
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
		mention: MENTION_CONFIG,
		placeholder: 'Type or paste your content here!',
		table: {
			contentToolbar: ['tableColumn', 'tableRow', 'mergeTableCells']
		}
	};

	if (mode === 'document') {
		config.minimap = {
			container: minimapContainer,
			extraClasses: 'editor-container_include-minimap ck-minimap__iframe-content'
		};
	}

	return config;
};