import React from 'react'
import { createTheme, ThemeProvider } from '@mui/material/styles'
import indigo from '@mui/material/colors/indigo'
import lightBlue from '@mui/material/colors/lightBlue'

const config = {
  typography: {
    useNextVariants: true,
    label: {
      fontSize: '0.7rem',
      color: '#0000008a'
    },
    sourceCode: {
      fontFamily: '"Fira code", "Fira Mono", monospace'
    }
  },
  palette: {
    primary: {
      main: indigo[700]
    },
    secondary: {
      main: lightBlue[600]
    },
    default: {
      main: '#00000089'
    },
    info: {
      main: lightBlue[600]
    },
    warning: {
      main: '#ff9609'
    },
    error: {
      main: '#e64444'
    },
    hint: {
      main: '#bdbdbd'
    },
    background: {
      primary: '#ebebeb',
      secondary: '#f5f5f5',
      field: '#e8e8e8',
      paper: '#ffffff'
    },
    border: {
      primary: '#dbdbdb',
      secondary: '#ebebeb',
      field: '#878787'
    }
  }
}

const theme = createTheme(config);

class ThemeProviderDefault extends React.Component {
  render() {
    return (
      <ThemeProvider theme={theme}>{this.props.children}</ThemeProvider>
    )
  }
}

export default ThemeProviderDefault;
export { config };
