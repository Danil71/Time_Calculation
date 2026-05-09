import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import { Alert, Box, Button, CircularProgress, Grid, Paper, Typography } from '@mui/material';
import type { AxiosError } from 'axios';
import { useState } from 'react';
import { api } from '../api/axiosClient';

interface CalibrationResult {
  performedAt: string;
  rmseScore: number;
  newCoefficientA: number;
  newCoefficientB: number;
  projectsAnalyzed: number;
}

export default function AdminPanel() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CalibrationResult | null>(null);
  const[error, setError] = useState('');

  const handleCalibrate = async () => {
    setLoading(true);
    setError('');
    setResult(null);

    try {
      const response = await api.post('/admin/calibrate-model');
      setResult(response.data);
    } catch (err: unknown) {
      console.error('Ошибка ML', err);
      setError((err as AxiosError<{ message: string }>)?.response?.data?.message || 'Для калибровки нужно минимум 3 завершенных проекта с указанными фактическими сроками. Либо Python-сервис недоступен.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ mt: 4, maxWidth: 800, mx: 'auto' }}>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Панель Администратора (ML)
      </Typography>
      
      <Paper sx={{ p: 4, mt: 3, textAlign: 'center' }}>
        <Typography variant="h6" gutterBottom>
          Калибровка математической модели COCOMO II
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
          Нейросеть проанализирует все проекты в статусе "ЗАВЕРШЕН", сравнит наши прогнозы с фактическими сроками сдачи и пересчитает константы A и B методом нелинейной регрессии.
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

        <Button 
          variant="contained" 
          color="secondary" 
          size="large"
          startIcon={loading ? <CircularProgress size={20} /> : <AutoAwesomeIcon />}
          onClick={handleCalibrate}
          disabled={loading}
        >
          {loading ? 'Обучение нейросети...' : 'Запустить переобучение модели'}
        </Button>

        {result && (
          <Box sx={{ mt: 5, textAlign: 'left' }}>
            <Alert severity="success" sx={{ mb: 3 }}>
              Модель успешно обучена на базе {result.projectsAnalyzed} проектов!
            </Alert>
            <Grid container spacing={2}>
              <Grid size={6}>
                <Paper sx={{ p: 2, textAlign: 'center', bgcolor: 'rgba(16, 185, 129, 0.1)' }}>
                  <Typography color="text.secondary">Новая константа A</Typography>
                  <Typography variant="h4" color="success.main">{result.newCoefficientA}</Typography>
                </Paper>
              </Grid>
              <Grid size={6}>
                <Paper sx={{ p: 2, textAlign: 'center', bgcolor: 'rgba(59, 130, 246, 0.1)' }}>
                  <Typography color="text.secondary">Новая константа B</Typography>
                  <Typography variant="h4" color="primary.main">{result.newCoefficientB}</Typography>
                </Paper>
              </Grid>
              <Grid size={12}>
                <Typography align="center" variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  Ошибка модели (RMSE): {result.rmseScore.toFixed(3)}
                </Typography>
              </Grid>
            </Grid>
          </Box>
        )}
      </Paper>
    </Box>
  );
}