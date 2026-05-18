import { Alert, Box, Button, Paper, TextField, Typography } from '@mui/material';
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/axiosClient';

export default function AddProject() {
  const navigate = useNavigate();
  
  const [name, setName] = useState('');
  const [repoUrl, setRepoUrl] = useState('');
  const [branchName, setBranchName] = useState('main');
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await api.post('/projects', {
        name,
        repoUrl,
        branchName,
        token
      });
      // Если успешно добавили, перекидываем на главную
      navigate('/');
    } catch (err) {
      console.error(err);
      setError('Ошибка при сохранении проекта на сервере');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', mt: 5 }}>
      <Paper elevation={0} sx={{ p: 4, borderRadius: 3 }}>
        <Typography variant="h5" gutterBottom sx={{ fontWeight: 'bold' }}>
          Подключение репозитория
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Укажите данные Git-репозитория. Система сохранит их для последующего статического анализа.
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

        <form onSubmit={handleSubmit}>
          <TextField
            fullWidth label="Название проекта" variant="outlined" margin="normal"
            value={name} onChange={(e) => setName(e.target.value)} required
          />
          <TextField
            fullWidth label="URL репозитория (.git)" variant="outlined" margin="normal"
            value={repoUrl} onChange={(e) => setRepoUrl(e.target.value)} required
            placeholder="https://github.com/user/repo.git"
          />
          <TextField
            fullWidth label="Ветка (Branch)" variant="outlined" margin="normal"
            value={branchName} onChange={(e) => setBranchName(e.target.value)} required
          />
          <TextField
            fullWidth label="Personal Access Token (PAT)" variant="outlined" margin="normal"
            type="password"
            value={token} onChange={(e) => setToken(e.target.value)}
            helperText="Для приватного GitLab (в т.ч. корпоративного) укажите PAT — иначе анализ не сможет клонировать репозиторий. Пусто только для публичных clone без авторизации."
          />
          
          <Box sx={{ display: 'flex', gap: 2, mt: 4 }}>
            <Button variant="outlined" color="inherit" fullWidth onClick={() => navigate('/')}>
              Отмена
            </Button>
            <Button type="submit" variant="contained" color="primary" fullWidth disabled={loading}>
              {loading ? 'Сохранение...' : 'Добавить проект'}
            </Button>
          </Box>
        </form>
      </Paper>
    </Box>
  );
}