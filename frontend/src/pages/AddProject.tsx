import { Box, Button, Paper, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/axiosClient';

export default function AddProject() {
  const navigate = useNavigate();
  const[name, setName] = useState('');
  const [repoUrl, setRepoUrl] = useState('');
  const [branchName, setBranchName] = useState('main');
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api.post('/projects', { name, repoUrl, branchName, token });
      navigate('/');
    } catch (error) {
        console.error('Ошибка бэкенда:', error);
        alert('Ошибка при добавлении проекта');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', mt: 5 }}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" gutterBottom>Подключение репозитория</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Укажите ссылку на Git для анализа кода и профилирования команды.
        </Typography>
        <form onSubmit={handleSubmit}>
          <TextField fullWidth label="Название проекта" required value={name} onChange={(e) => setName(e.target.value)} margin="normal" />
          <TextField fullWidth label="URL репозитория (.git)" required value={repoUrl} onChange={(e) => setRepoUrl(e.target.value)} margin="normal" />
          <TextField fullWidth label="Ветка (Branch)" required value={branchName} onChange={(e) => setBranchName(e.target.value)} margin="normal" />
          <TextField fullWidth label="Токен (PAT)" type="password" value={token} onChange={(e) => setToken(e.target.value)} margin="normal" helperText="Оставьте пустым для Open Source" />
          <Button type="submit" variant="contained" fullWidth sx={{ mt: 3, py: 1.5 }} disabled={loading}>
            {loading ? 'Сохранение...' : 'Добавить проект'}
          </Button>
        </form>
      </Paper>
    </Box>
  );
}