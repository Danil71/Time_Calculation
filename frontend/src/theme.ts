import { createTheme } from '@mui/material/styles';

export const darkTechTheme = createTheme({
  palette: {
    mode: 'dark',
    background: { default: '#0B0F19', paper: '#111827' },
    primary: { main: '#3B82F6' },
    secondary: { main: '#10B981' },
    text: { primary: '#F3F4F6', secondary: '#9CA3AF' },
    divider: '#1F2937',
  },
  typography: {
    fontFamily: '"Inter", "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
    h5: { fontWeight: 600, letterSpacing: '-0.02em' },
    h6: { fontWeight: 600, letterSpacing: '-0.01em' },
    button: { textTransform: 'none', fontWeight: 500 },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiCard: { styleOverrides: { root: { backgroundImage: 'none', boxShadow: 'none', border: '1px solid #1F2937' } } },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none', boxShadow: 'none', border: '1px solid #1F2937' } } },
    MuiAppBar: { styleOverrides: { root: { backgroundColor: 'rgba(17, 24, 39, 0.8)', backdropFilter: 'blur(10px)', borderBottom: '1px solid #1F2937', boxShadow: 'none' } } },
  },
});