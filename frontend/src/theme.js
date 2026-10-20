import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: { main: '#1E3A5F' },
    secondary: { main: '#E87722' },
    background: { default: '#F6F7F9' },
  },
  shape: { borderRadius: 10 },
  typography: { fontFamily: 'Roboto, "Segoe UI", Arial, sans-serif' },
  components: {
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiPaper: { defaultProps: { elevation: 0 }, styleOverrides: { root: { border: '1px solid #E5E7EB' } } },
  },
});

export default theme;
