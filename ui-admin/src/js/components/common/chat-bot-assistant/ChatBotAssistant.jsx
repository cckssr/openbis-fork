import React, { useState, useRef, useEffect } from 'react';
import { Fab, Dialog, DialogTitle, DialogContent, IconButton, TextField, Paper, Typography, Box, CircularProgress, InputAdornment } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import CloseIcon from '@mui/icons-material/Close';
import { styled } from '@mui/material/styles';
import openbis from '@src/js/services/openbis.js'
import ids from '@src/js/common/consts/ids.js'
import SmartToyIcon from '@mui/icons-material/SmartToy';

const ChatbotConfig = {
  sessionStorageKey: 'openbis-chatbot-session-id',
  ui: {
    title: 'Chat Assistant',
    placeholder: 'Ask me anything about openBIS...',
    welcomeMessage: 'Hello! I\'m your chat bot assistant. How can I help you today?'
  }
};

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

const ChatbotContainer = styled(Box)(({ theme }) => ({
  position: 'fixed',
  bottom: 0,
  right: 0,
  width: 380,
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

export default function ChatBotAssistant({ open, setOpen, theme }) {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState(() => localStorage.getItem(ChatbotConfig.sessionStorageKey));
  const [hasShownWelcome, setHasShownWelcome] = useState(false);
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


  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMessage = input.trim();

    setMessages((prev) => [...prev, { role: 'user', content: userMessage }]);
    setInput('');
    setLoading(true);

    try {
      const response = await sendMessage(userMessage, sessionId);
      setLoading(false);
      if (response.sessionId) {
        setSessionId(response.sessionId);
        localStorage.setItem(ChatbotConfig.sessionStorageKey, response.sessionId);
      }
      setMessages((prev) => [...prev, { role: 'assistant', content: response.answer }]);

    } catch (error) {
      setLoading(false);
      let errorMessage = `Sorry, I'm having trouble connecting to the server. `;
      if (error.message.includes('Failed to fetch')) {
        errorMessage += 'Please check if the chatbot backend is running and accessible.';
      } else {
        errorMessage += 'Please try again later.';
      }
      setMessages((prev) => [...prev, { role: 'assistant', content: errorMessage }]);
    }
  };

  const sendMessage = async (message, sessionId) => {

    const serviceId = new openbis.CustomASServiceCode(ids.CHATBOT_SERVICE)

    const serviceOptions = new openbis.CustomASServiceExecutionOptions()
    serviceOptions.withParameter('method', 'ask')
    serviceOptions.withParameter('query', message)
    serviceOptions.withParameter('session_id', sessionId)

    const result = await openbis.executeService(serviceId, serviceOptions)

    return result
  }

  if (!open) return null;

  return (
    <ChatbotContainer
      onClick={(e) => e.stopPropagation()}
      onMouseDown={(e) => e.stopPropagation()}
      onMouseUp={(e) => e.stopPropagation()}
      onKeyDown={(e) => e.stopPropagation()}
    >
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
            '& .MuiInputBase-root': {
              padding: 0,
            },
            '& .MuiInputBase-input': {
              padding: '0 0 0 10px',
            },
          }}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton color="primary" onClick={handleSend} disabled={loading || !input.trim()}>
                  <SendIcon />
                </IconButton>
              </InputAdornment>
            ),
          }}
        /></InputBox>
    </ChatbotContainer>
  );
} 