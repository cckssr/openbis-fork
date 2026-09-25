import React, { useState, useRef, useEffect } from 'react';
import { IconButton, TextField, Typography, Box, InputAdornment } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import CloseIcon from '@mui/icons-material/Close';
import { styled } from '@mui/material/styles';
import ids from '@src/js/common/consts/ids.js'
import SmartToyIcon from '@mui/icons-material/SmartToy';

const ChatbotConfig = {
  sessionStorageKey: 'openbis-chatbot-session-id',
  ui: {
    title: 'ChatBIS',
    placeholder: 'Ask me anything about openBIS...',
    welcomeMessage: 'Hello! I\'m your chat bot assistant. How can I help you today?'
  }
};

function getIdsFromResponse(text) {
  const identifierRegexp = /(?:\/[A-Z_-\d]+){2,5}/g
  let ids = text.match(identifierRegexp);
  if (!ids) {
    ids = [];
  }
  const permIdRegexp = /\d{17}-\d+/g
  let permIds = text.match(permIdRegexp);
  if ( !permIds) {
    permIds = [];
  }
  ids = ids.concat(permIds);
  return ids;
}

function renderMarkdown(text) {
  let processedText =  text
      // Headers
      .replace(/^### (.*$)/gim, '<h3>$1</h3>')
      .replace(/^## (.*$)/gim, '<h2>$1</h2>')
      .replace(/^# (.*$)/gim, '<h1>$1</h1>')
      // Bold
      .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
      .replace(/__(.*?)__/gim, '<strong>$1</strong>')
      // Italic
      // .replace(/\*(.*)\*/gim, '<em>$1</em>')
      // .replace(/_(.*?)_/gim, '<em>$1</em>')
      // Code blocks
      .replace(/```([\s\S]*?)```/gim, '<pre><code>$1</code></pre>')
      // Inline code
      .replace(/`([^`]*)`/gim, '<code>$1</code>')
      // Links
      .replace(/\[([^\]]*)\]\(([^\)]*)\)/gim, '<a href="$2" target="_blank">$1</a>')
      // Line breaks
      .replace(/\n/gim, '<br>');

  let ids = getIdsFromResponse(text);
  for(let id of ids) {
    processedText = processedText.replace(id, '<a href="#" class="action-link" data-entity-id='+id+'>'+id+'</a>')
  }
  return processedText;
}

const ChatbotContainer = styled(Box)(({ theme }) => ({
  position: 'fixed',
  bottom: 0,
  right: 0,

  width: 380,
  minWidth: 320,
  maxWidth: 'calc(100vw - 40px)',

  height: 550,

  display: 'flex',
  flexDirection: 'column',

  borderRadius: '12px 12px 0 0',
  boxShadow: '0 8px 32px rgba(0,0,0,0.12)',
  border: '1px solid #e0e0e0',
  borderBottom: 'none',

  overflow: 'hidden',
  zIndex: 1300,
  backgroundColor: 'white',

  '@media (max-width: 600px)': {
    width: 'calc(100vw - 40px)',
    minWidth: 0,
    maxWidth: 'calc(100vw - 40px)',
    height: 'calc(100vh - 140px)',
    left: 20,
    right: 20,
    bottom: 90,
  }
}));

const ChatbotHeader = styled(Box)(({ theme }) => ({
  backgroundColor: theme.palette.primary.main,
  color: 'white',
  padding: theme.spacing(2),
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between'
}));

const MessagesBox = styled(Box)(({ theme }) => ({
  flex: 1,
  overflowY: 'auto',
  padding: 20,
  backgroundColor: '#fafafa',
  minHeight: 0,
}));

const InputBox = styled(Box)(({ theme }) => ({
  padding: '16px 20px',
  borderTop: '1px solid #e0e0e0',
  display: 'flex',
  gap: 12,
  background: 'white',
}));

const MessageDiv = styled('div')(({ role }) => ({
  marginBottom: 16,
  display: 'flex',
  flexDirection: 'column',
  alignItems: role === 'user' ? 'flex-end' : 'flex-start',
  animation: 'fadeIn 0.3s ease-in',
}));

const MessageContent = styled('div')(({ role, theme }) => ({
  maxWidth: '85%',
  padding: '12px 16px',
  borderRadius: 18,
  backgroundColor: role === 'user' ? theme.palette.primary.main : 'white',
  color: role === 'user' ? 'white' : '#333',
  borderBottomRightRadius: role === 'user' ? 4 : 18,
  borderBottomLeftRadius: role === 'assistant' ? 4 : 18,
  border: role === 'assistant' ? '1px solid #e0e0e0' : 'none',
  textAlign: role === 'user' ? 'right' : 'left',
  fontSize: 14,
  lineHeight: 1.4,
  wordWrap: 'break-word',
  userSelect: 'text',

  '& .action-link': {
    color: theme.palette.primary.main,
    textDecoration: 'underline',
    cursor: 'pointer',
    display: 'contents'
  },

}));

const LoadingDots = styled('div')(() => ({
  display: 'inline-flex',
  gap: 4,
  padding: '16px 20px',
  textAlign: 'center',
  borderTop: '1px solid #e0e0e0',
  background: 'white',
  justifyContent: 'center',
}));

const Dot = styled('span')(({ theme }) => ({
  width: 8,
  height: 8,
  borderRadius: '50%',
  backgroundColor: theme.palette.primary.main,
  animation: 'loadingDots 1.4s infinite ease-in-out both',
  display: 'inline-block',
  margin: '0 2px',
}));

const ResizeHandle = styled('div')({
  position: 'absolute',
  top: 0,
  left: 0,
  width: 8,
  height: '100%',
  cursor: 'ew-resize',
  zIndex: 20,
  touchAction: 'none',
});

export default function ChatBotAssistant({ open, setOpen, theme, sendMessageCallback, openEntityCallback }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState(() => localStorage.getItem(ChatbotConfig.sessionStorageKey));
  const [hasShownWelcome, setHasShownWelcome] = useState(false);

  const [chatbotWidth, setChatbotWidth] = useState(380);

  const resizingRef = useRef(false);
  const resizePointerIdRef = useRef(null);

  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (open && !hasShownWelcome && messages.length === 0) {
      setMessages([{ role: 'assistant', content: ChatbotConfig.ui.welcomeMessage }]);
      setHasShownWelcome(true);
    }
  }, [open, hasShownWelcome, messages.length]);

  useEffect(() => {
    if (open && messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, open]);

  useEffect(() => {
    const handlePointerMove = (e) => {
      if (!resizingRef.current) {
        return;
      }

      if (e.pointerId !== resizePointerIdRef.current) {
        return;
      }

      const newWidth = window.innerWidth - e.clientX;

      const minWidth = 320;
      const maxWidth = Math.min(
          1000,
          window.innerWidth - 40
      );

      const width = Math.max(
          minWidth,
          Math.min(newWidth, maxWidth)
      );

      setChatbotWidth(width);
    };

    const stopResizing = (e) => {
      if (
          resizingRef.current &&
          (e.pointerId === resizePointerIdRef.current ||
              e.type === 'blur')
      ) {
        resizingRef.current = false;
        resizePointerIdRef.current = null;

        document.body.style.cursor = '';
        document.body.style.userSelect = '';
      }
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', stopResizing);
    window.addEventListener('pointercancel', stopResizing);
    window.addEventListener('blur', stopResizing);

    return () => {
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', stopResizing);
      window.removeEventListener('pointercancel', stopResizing);
      window.removeEventListener('blur', stopResizing);
    };
  }, []);

  const stopResizing = () => {
    resizingRef.current = false;
    resizePointerIdRef.current = null;

    document.body.style.cursor = '';
    document.body.style.userSelect = '';
  };

  const handleResizeStart = (e) => {
    e.preventDefault();
    e.stopPropagation();

    resizingRef.current = true;
    resizePointerIdRef.current = e.pointerId;

    document.body.style.cursor = 'ew-resize';
    document.body.style.userSelect = 'none';
  };

  const handleResizeMove = (e) => {
    if (!resizingRef.current) {
      return;
    }

    // Ignore events from a different pointer.
    if (e.pointerId !== resizePointerIdRef.current) {
      return;
    }

    const newWidth = window.innerWidth - e.clientX;

    const minWidth = 320;
    const maxWidth = Math.min(1000, window.innerWidth - 40);

    setChatbotWidth(
        Math.min(Math.max(newWidth, minWidth), maxWidth)
    );
  };

  const handleInputChange = (e) => {
    setInput(e.target.value);
  }

  const handleKeyDown = (e) => {
    if (e.key === ' ' || e.keyCode === 32) {
      e.preventDefault();
      setInput(e.target.value + ' ');
      return;
    }
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSend();
    }
  };

  const handleMessageClick = (event) => {
    const link = event.target.closest('.action-link');

    if (!link) {
      return;
    }

    event.preventDefault();

    const entityId = link.dataset.entityId;

    // Perform your action here
    openEntityCallback(entityId);
  };


  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMessage = input.trim();

    setMessages((prev) => [...prev, { role: 'user', content: userMessage }]);
    setInput('');
    setLoading(true);

    try {
      const currentSessionId = sessionId || '';
      const response = await sendMessageCallback(userMessage, currentSessionId);
       
      setLoading(false);
      
      if (response && response.sessionId) {
        setSessionId(response.sessionId);
        localStorage.setItem(ChatbotConfig.sessionStorageKey, response.sessionId);
      }
      
      if (response && response.answer) {
        setMessages((prev) => [...prev, { role: 'assistant', content: response.answer }]);
      } else {
        setMessages((prev) => [...prev, { role: 'assistant', content: 'Sorry, I received an invalid response from the server.' }]);
      }
    } catch (error) {
      setLoading(false);
      let errorMessage = `Sorry, I'm having trouble connecting to the server. `;
      if (error.message && error.message.includes('Failed to fetch')) {
        errorMessage += 'Please check if the chatbot backend is running and accessible.';
      } else {
        errorMessage += 'Please try again later.';
      }
      setMessages((prev) => [...prev, { role: 'assistant', content: errorMessage }]);
    }
  };

  if (!open) return null;

  return (
    <ChatbotContainer
      onClick={(e) => e.stopPropagation()}
      onMouseDown={(e) => e.stopPropagation()}
      onMouseUp={(e) => e.stopPropagation()}
      onKeyDown={(e) => e.stopPropagation()}
      style={{
        width: chatbotWidth,
      }}
    >
      <ResizeHandle
          onPointerDown={handleResizeStart}
      />
      <ChatbotHeader theme={theme}>
        <div style={{ justifyContent: 'start', display: 'flex', alignItems: 'center' }}>
          <SmartToyIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ m: 0, fontWeight: 600 }}>
            {ChatbotConfig.ui.title}
          </Typography>
        </div>
        <IconButton
          aria-label="close"
          onClick={() => setOpen(false)}
          sx={{ color: 'white' }}
          data-close-button
        >
          <CloseIcon />
        </IconButton>
      </ChatbotHeader>
      <MessagesBox>
        {messages.map((msg, idx) => (
          <MessageDiv key={idx} role={msg.role}>
            <MessageContent
              role={msg.role}
              theme={theme}
              onClick={handleMessageClick}
              dangerouslySetInnerHTML={{ __html: msg.role === 'assistant' ? renderMarkdown(msg.content) : msg.content.replace(/\n/g, '<br>') }}
            />
          </MessageDiv>
        ))}
        <div ref={messagesEndRef} />
      </MessagesBox>
      {loading && (
        <LoadingDots>
          <Dot theme={theme} style={{ animationDelay: '-0.32s' }} />
          <Dot theme={theme} style={{ animationDelay: '-0.16s' }} />
          <Dot theme={theme} />
        </LoadingDots>
      )}
      <InputBox>
        <TextField
          fullWidth
          autoFocus
          variant="outlined"
          size="small"
          placeholder={ChatbotConfig.ui.placeholder}
          value={input}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          disabled={loading}
          sx={{
            '& .MuiInputBase-root': { padding: 0 },
            '& .MuiInputBase-input': { padding: '0 0 0 10px' },
          }}
          slotProps={{
            input: {
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton color="primary" onClick={handleSend} disabled={loading || !input.trim()}>
                    <SendIcon />
                  </IconButton>
                </InputAdornment>
              ),
            }
          }}
        />
      </InputBox>
    </ChatbotContainer>
  );
} 