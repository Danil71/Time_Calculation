import { Alert, Box, Button, MenuItem, Paper, TextField, Typography } from '@mui/material';
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/axiosClient';
import { useAuth } from '../context/AuthContext';

export default function Register() {
  const navigate = useNavigate();
  const { login } = useAuth();
  
  const [username, setUsername] = useState('');
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('MANAGER');
  
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await api.post('/auth/register', { 
        username, 
        password, 
        fullName, 
        role 
      });
      

      login(response.data.token, response.data.username, response.data.fullName, response.data.role);
      navigate('/');
      window.location.reload();
    } catch (err) {
      console.error(err);
      setError('Пользователь с таким логином уже существует');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '80vh' }}>
      <Paper elevation={0} sx={{ p: 5, width: '100%', maxWidth: 400, borderRadius: 3 }}>
        <Typography variant="h4" gutterBottom align="center" sx={{ fontWeight: 'bold' }}>
          Регистрация
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 4 }}>
          Создайте аккаунт для работы с GitEstimator
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

        <form onSubmit={handleRegister}>
          <TextField
            fullWidth label="Логин (username)" variant="outlined" margin="normal"
            required value={username} onChange={(e) => setUsername(e.target.value)}
          />
          <TextField
            fullWidth label="ФИО" variant="outlined" margin="normal"
            required value={fullName} onChange={(e) => setFullName(e.target.value)}
          />
          <TextField
            fullWidth select label="Роль в системе" variant="outlined" margin="normal"
            value={role} onChange={(e) => setRole(e.target.value)}
          >
            <MenuItem value="MANAGER">Менеджер проектов</MenuItem>
            <MenuItem value="DEVELOPER">Разработчик (Только просмотр)</MenuItem>
            <MenuItem value="ADMIN">Администратор (ML и настройки)</MenuItem>
          </TextField>
          <TextField
            fullWidth label="Пароль" variant="outlined" margin="normal"
            type="password" required value={password} onChange={(e) => setPassword(e.target.value)}
          />
          
          <Button 
            type="submit" variant="contained" color="primary" 
            fullWidth sx={{ mt: 4, mb: 2, py: 1.5, fontSize: '1.1rem' }}
            disabled={loading}
          >
            {loading ? 'Создание аккаунта...' : 'Зарегистрироваться'}
          </Button>

          <Typography variant="body2" align="center" color="text.secondary">
            Уже есть аккаунт? <Link to="/login" style={{ color: '#3B82F6', textDecoration: 'none' }}>Войти</Link>
          </Typography>
        </form>
      </Paper>
    </Box>
  );
}