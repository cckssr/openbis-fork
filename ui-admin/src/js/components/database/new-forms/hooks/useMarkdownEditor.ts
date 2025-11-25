import { useCallback, useEffect, useRef, useState } from 'react';

type UseMarkdownEditorProps = {
	value?: string | null;
	initialIsMarkdown?: boolean;
};

export type UseMarkdownEditorResult = {
	editorValue: string;
	isMarkdown: boolean;
	toggleMarkdownMode: () => void;
};

/**
 * Hook to manage CKEditor markdown mode state.
 * 
 * According to CKEditor documentation (https://ckeditor.com/docs/ckeditor5/latest/features/markdown.html#the-markdown-data-processor):
 * - When Markdown plugin is enabled, it automatically changes the data processor to MarkdownGfmDataProcessor
 * - editor.getData() returns markdown (GFM format)
 * - editor.setData() accepts markdown
 * - No external conversion libraries needed - CKEditor handles HTML ↔ Markdown conversion natively
 */
export const useMarkdownEditor = ({
	value,
	initialIsMarkdown = false
}: UseMarkdownEditorProps): UseMarkdownEditorResult => {
	const normalizedInitialValue = value ?? '';
	const [isMarkdown, setIsMarkdown] = useState(initialIsMarkdown);
	const latestValueRef = useRef(normalizedInitialValue);

	// Update when external value changes
	useEffect(() => {
		const normalizedValue = value ?? '';
		if (normalizedValue !== latestValueRef.current) {
			latestValueRef.current = normalizedValue;
		}
	}, [value]);

	// Sync with initial markdown state
	useEffect(() => {
		setIsMarkdown(initialIsMarkdown);
	}, [initialIsMarkdown]);

	const toggleMarkdownMode = useCallback(() => {
		setIsMarkdown(prev => !prev);
	}, []);

	// Pass value directly to editor - CKEditor's Markdown plugin handles conversion automatically
	// When markdownEnabled=true: editor expects markdown, returns markdown
	// When markdownEnabled=false: editor expects HTML, returns HTML
	const editorValue = normalizedInitialValue;

	return {
		editorValue,
		isMarkdown,
		toggleMarkdownMode
	};
};

