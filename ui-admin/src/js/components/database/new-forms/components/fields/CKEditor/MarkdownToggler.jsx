import { Plugin, SwitchButtonView } from 'ckeditor5';

export default class MarkdownToggler extends Plugin {
    init() {
        const editor = this.editor;

        editor.ui.componentFactory.add('markdownToggler', locale => {
            const switchButton = new SwitchButtonView(locale);

            // Set initial state and label
            const updateButtonState = () => {
                const markdownEnabled = editor.config.get('markdownEnabled');
                switchButton.set({
                    label: 'Markdown output',
                    isOn: !!markdownEnabled,
                    withText: true,
                    tooltip: markdownEnabled ? 'Markdown output is enabled, click to switch to HTML' : 'HTML output is enabled by default, click to switch to markdown'
                });
            };

            updateButtonState();

            // Listen for execute event to toggle state
            switchButton.on('execute', () => {
                const onMarkdownToggle = editor.config.get('markdownToggleCallback');
                if (onMarkdownToggle && typeof onMarkdownToggle === 'function') {
                    onMarkdownToggle();
                }
                // Optionally update the button state after toggling
                updateButtonState();
            });

            return switchButton;
        });
    }
}