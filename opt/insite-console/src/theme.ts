import {createTheme} from '@mui/material/styles';

// Create a theme instance.
const theme = createTheme({
  components: {
    MuiButtonBase: {
      defaultProps: {
        disableRipple: true,
      },
    },
  },
  typography: {
    body1: {
      fontSize: '1rem',
      fontWeight: 400,
      lineHeight: '1.5rem',
    },
    body2: {
      fontSize: '0.875rem',
      fontWeight: 400,
      lineHeight: '1.5rem',
    },
    subtitle1: {
      fontFamily: 'Roboto Condensed',
      fontSize: '1rem',
      fontWeight: 300,
      lineHeight: '1.5rem',
    },
    subtitle2: {
      fontFamily: 'Roboto Condensed',
      fontSize: '0.875rem',
      fontWeight: 300,
      lineHeight: '1.25rem',
    },
    caption: {
      fontSize: '0.75rem',
      fontWeight: 500,
      lineHeight: '1.5rem',
    },
    overline: {
      fontSize: '0.75rem',
      fontWeight: 500,
      lineHeight: '1.65rem',
    },
    h1: {
      fontSize: '6rem',
      fontWeight: 700,
      letterSpacing: '-0.094em',
      lineHeight: '7.002rem',
    },
    h2: {
      fontSize: '3.75rem',
      fontWeight: 700,
      lineHeight: '4.5rem',
    },
    h3: {
      fontSize: '3rem',
      fontWeight: 700,
      lineHeight: '3.501rem',
    },
    h4: {
      fontSize: '2.125rem',
      fontWeight: 700,
      lineHeight: '3.501rem',
    },
    h5: {
      fontSize: '1.5rem',
      fontWeight: 700,
      lineHeight: '1.89rem',
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 700,
      lineHeight: '1.4rem',
    },
  },
  palette: {
    primary: {
      main: '#0B5C98',
      dark: '#004187',
      light: '#D7EBF5',
    },
    secondary: {
      main: '#D76E64',
      dark: '#C8524A',
      light: '#FF9D91',
    },
    action: {
      active: 'rgba(0, 0, 0, 0.54)',
      hover: 'rgba(0, 0, 0, 0.04)',
      selected: 'rgba(0, 0, 0, 0.08)',
      disabled: 'rgba(0, 0, 0, 0.26)',
      disabledBackground: 'rgba(0, 0, 0, 0.12)',
    },
    error: {
      main: '#E51C25',
      contrastText: '#FFFFFF',
    },
    warning: {
      main: '#FFF9C4',
      contrastText: 'rgba(0, 0, 0, 0.87)',
    },
    info: {
      main: '#2196F3',
      dark: '#0B79D0',
      contrastText: '#fff',
    },
    success: {
      main: '#4CAF50',
    },
    background: {
      paper: '#FFFFFF',
    },
    text: {
      primary: 'rgba(0, 0, 0, 0.87)',
      secondary: 'rgba(0, 0, 0, 0.6)',
      disabled: 'rgba(0, 0, 0, 0.38)',
    },
  },
});

export default theme;
