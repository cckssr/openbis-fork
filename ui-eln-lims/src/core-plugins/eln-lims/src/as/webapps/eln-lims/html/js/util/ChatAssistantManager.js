// Chatbot initialization and management

const ChatbotConfig = {

    // Session configuration
    sessionStorageKey: 'openbis-chatbot-session-id',

    // UI configuration
    ui: {
        title: 'Chat Assistant',
        placeholder: 'Ask me anything about openBIS...',
        welcomeMessage: 'Hello! I\'m your personal openBIS assistant. How can I help you today?'
    }
};

document.addEventListener('DOMContentLoaded', function() {

    // Create and inject chatbot HTML
    const chatbotHTML = `
        <!--
        <div id="chatbot-trigger" class="chatbot-trigger" title="Open chat assistant">
            <span class="glyphicon glyphicon-comment"></span>
        </div>
        -->
        <div id="chatbot-container" class="chatbot-container" style="display: none;">
            <div class="chatbot-header">
                <h3>${ChatbotConfig.ui.title}</h3>
                <button id="chatbot-close" class="chatbot-close" title="Close chat">
                    <span class="glyphicon glyphicon-remove"></span>
                </button>
            </div>
            <div id="chatbot-messages" class="chatbot-messages"></div>
            <div class="chatbot-input">
                <input type="text" id="chatbot-input" placeholder="${ChatbotConfig.ui.placeholder}" />
                <button id="chatbot-send" title="Send message">
                    <span class="glyphicon glyphicon-send"></span>
                </button>
            </div>
            <div id="chatbot-loading" class="chatbot-loading" style="display: none;">
                <div class="loading-dots">
                    <span></span><span></span><span></span>
                </div>
            </div>
        </div>
    `;

    // Create container div and inject HTML
    const chatbotWrapper = document.createElement('div');
    chatbotWrapper.id = 'chatbot-wrapper';
    chatbotWrapper.innerHTML = chatbotHTML;
    document.body.appendChild(chatbotWrapper);

    // Get DOM elements
//    const trigger = document.getElementById('chatbot-trigger');
    const container = document.getElementById('chatbot-container');
    const closeBtn = document.getElementById('chatbot-close');
    const input = document.getElementById('chatbot-input');
    const sendBtn = document.getElementById('chatbot-send');
    const messagesContainer = document.getElementById('chatbot-messages');
    const loadingIndicator = document.getElementById('chatbot-loading');

    // Initialize session management
    let sessionId = localStorage.getItem(ChatbotConfig.sessionStorageKey);
    let isFirstMessage = !sessionId;

    // Simple markdown renderer
    function renderMarkdown(text) {
        return text
            // Headers
            .replace(/^### (.*$)/gim, '<h3>$1</h3>')
            .replace(/^## (.*$)/gim, '<h2>$1</h2>')
            .replace(/^# (.*$)/gim, '<h1>$1</h1>')
            // Bold
            .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
            .replace(/__(.*?)__/gim, '<strong>$1</strong>')
            // Italic
            .replace(/\*(.*)\*/gim, '<em>$1</em>')
            .replace(/_(.*?)_/gim, '<em>$1</em>')
            // Code blocks
            .replace(/```([\s\S]*?)```/gim, '<pre><code>$1</code></pre>')
            // Inline code
            .replace(/`([^`]*)`/gim, '<code>$1</code>')
            // Links
            .replace(/\[([^\]]*)\]\(([^\)]*)\)/gim, '<a href="$2" target="_blank">$1</a>')
            // Line breaks
            .replace(/\n/gim, '<br>');
    }

    // Event Listeners
//    trigger.addEventListener('click', function() {
//        const isVisible = container.style.display !== 'none';
//        if (isVisible) {
//            container.style.display = 'none';
//        } else {
//            container.style.display = 'flex';
//            input.focus();
//
//            // Show welcome message on first open
//            if (isFirstMessage && messagesContainer.children.length === 0) {
//                addMessage('assistant', ChatbotConfig.ui.welcomeMessage);
//            }
//        }
//    });

    closeBtn.addEventListener('click', function() {
        container.style.display = 'none';
    });

    // Show/hide loading indicator
    function showLoading() {
        loadingIndicator.style.display = 'block';
        sendBtn.disabled = true;
        input.disabled = true;
    }

    function hideLoading() {
        loadingIndicator.style.display = 'none';
        sendBtn.disabled = false;
        input.disabled = false;
    }

    // Handle message sending
    function sendMessage() {
        const message = input.value.trim();
        if (!message || sendBtn.disabled) return;

        // Add user message to chat
        addMessage('user', message);
        input.value = '';

        // Show loading indicator
        showLoading();
        mainController.serverFacade.sendChatBotMessage(message, sessionId, function(response) {
            localStorage.setItem(ChatbotConfig.sessionStorageKey, response.result.sessionId);
            isFirstMessage = false;

            hideLoading();
            addMessage('assistant', response.result.answer);
        });

    }

    // Add message to chat
    function addMessage(role, content) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `chatbot-message ${role}`;

        // Render markdown for assistant messages, plain text for user messages
        const processedContent = role === 'assistant' ? renderMarkdown(content) : content.replace(/\n/g, '<br>');

        messageDiv.innerHTML = `
            <div class="message-content">
                ${processedContent}
            </div>
        `;

        const messagesContainer = document.getElementById('chatbot-messages');
        messagesContainer.appendChild(messageDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Send button click
    sendBtn.addEventListener('click', sendMessage);

    // Enter key press
    input.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            sendMessage();
        }
    });

    // Focus input when container is shown
    input.addEventListener('focus', function() {
        // Scroll to bottom when input is focused
        setTimeout(() => {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }, 100);
    });

    // Handle window resize to adjust chat position
    window.addEventListener('resize', function() {
        if (container.style.display === 'flex') {
            // Ensure chat stays in view on mobile
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
    });
});

function toggleChatAssistant() {
    const container = document.getElementById('chatbot-container');
    const isVisible = container.style.display !== 'none';
    if (isVisible) {
        container.style.display = 'none';
    } else {
        container.style.display = 'flex';
        input.focus();
    }
}