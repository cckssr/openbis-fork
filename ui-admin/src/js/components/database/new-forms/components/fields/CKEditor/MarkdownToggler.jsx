import { Plugin, ButtonView, SwitchButtonView } from 'ckeditor5';

export default class MarkdownToggler extends Plugin {
    /* init() {
        const editor = this.editor;

        // Register the button in the component factory.
        editor.ui.componentFactory.add('markdownToggler', locale => {
            const button = new ButtonView(locale);

            button.set({
                label: 'Toggle Markdown',
                withText: true,
                tooltip: true
            });

            // Update button label based on current markdown state from config
            const updateButtonLabel = () => {
                const markdownEnabled = editor.config.get('markdownEnabled');
                button.set({
                    label: markdownEnabled ? 'Disable Markdown output' : 'Enable Markdown output'
                });
            };

            // Initial label update
            updateButtonLabel();

            button.on('execute', () => {
                // Get the callback function from editor config
                const onMarkdownToggle = editor.config.get('markdownToggleCallback');
                if (onMarkdownToggle && typeof onMarkdownToggle === 'function') {
                    onMarkdownToggle();
                }
            });

            return button;
        });
    } */
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