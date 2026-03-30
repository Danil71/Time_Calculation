import CodeIcon from '@mui/icons-material/Code';
import { AppBar, Box, Button, Chip, Container, Toolbar, Typography } from '@mui/material';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Box sx={{ flexGrow: 1, minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky" sx={{ backgroundColor: 'rgba(17, 24, 39, 0.8)', backdropFilter: 'blur(10px)' }}>
        <Toolbar>
          <CodeIcon sx={{ mr: 2, color: 'primary.main' }} />
          <Typography variant="h6" component={Link} to="/" sx={{ flexGrow: 1, textDecoration: 'none', color: 'inherit' }}>
            GitEstimator
          </Typography>
          
          {user && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Chip label={user.role} color="secondary" size="small" variant="outlined" />
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {user.username}
              </Typography>
              <Button color="inherit" onClick={handleLogout}>Выход</Button>
            </Box>
          )}
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" sx={{ mt: 4, mb: 4, flexGrow: 1 }}>
        {/* Сюда будут подставляться страницы в зависимости от URL */}
        <Outlet /> 
      </Container>
    </Box>
  );
}