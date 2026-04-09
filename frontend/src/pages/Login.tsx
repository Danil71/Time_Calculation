import { Alert, Box, Button, Paper, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const [username, setUsername] = useState(''); // По умолчанию для удобства тестирования
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await api.post('/auth/login', { username, password });
      // Используем функцию из контекста
      login(response.data.token, response.data.username, response.data.fullName, response.data.role);
      navigate('/'); // Идем на главную
    } catch (err) {
      console.error(err);
      setError('Неверный username или пароль');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
      <Paper sx={{ p: 5, width: '100%', maxWidth: 400, border: '1px solid #1F2937' }}>
        <Typography variant="h4" gutterBottom align="center" sx={{ fontWeight: 'bold' }}>Вход</Typography>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 4 }}>
          Доказательное планирование ПО
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

        <form onSubmit={handleSubmit}>
          <TextField fullWidth label="Логин" type="username" required value={username} onChange={(e) => setUsername(e.target.value)} margin="normal" />
          <TextField fullWidth label="Пароль" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} margin="normal" />
          <Button type="submit" variant="contained" fullWidth sx={{ mt: 4, py: 1.5 }} disabled={loading}>
            {loading ? 'Вход...' : 'Войти'}
          </Button>
          <Typography variant="body2" align="center" color="text.secondary" sx={{ mt: 3 }}>
            Нет аккаунта? <Link to="/register" style={{ color: '#3B82F6', textDecoration: 'none' }}>Зарегистрироваться</Link>
          </Typography>
        </form>
      </Paper>
    </Box>
  );
}